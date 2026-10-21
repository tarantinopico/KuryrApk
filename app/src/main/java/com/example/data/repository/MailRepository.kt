package com.example.data.repository

import com.example.data.local.AccountEntity
import com.example.data.local.AccountGroupEntity
import com.example.data.local.AttachmentEntity
import com.example.data.local.DigestQueueEntity
import com.example.data.local.EmailMessageEntity
import com.example.data.local.FolderEntity
import com.example.data.local.KuryrDao
import com.example.data.local.NotificationRuleEntity
import com.example.data.model.FolderType
import com.example.data.model.MailProtocol
import com.example.data.model.OutboxStatus
import com.example.data.model.SwipeAction
import com.example.data.security.PasswordVault
import com.example.network.EmailHeaderParser
import com.example.network.ImapSmtpPop3Client
import com.example.network.JmapClient
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MailRepository @Inject constructor(
    private val dao: KuryrDao,
    private val passwordVault: PasswordVault,
    private val imapSmtpPop3Client: ImapSmtpPop3Client,
    private val jmapClient: JmapClient
) {
    val accountsFlow: Flow<List<AccountEntity>> = dao.observeAccounts()
    val accountGroupsFlow: Flow<List<AccountGroupEntity>> = dao.observeAccountGroups()
    val foldersFlow: Flow<List<FolderEntity>> = dao.observeAllFolders()
    val allMessagesFlow: Flow<List<EmailMessageEntity>> = dao.observeAllMessages()
    val notificationRulesFlow: Flow<List<NotificationRuleEntity>> = dao.observeNotificationRules()
    val pendingDigestFlow: Flow<List<DigestQueueEntity>> = dao.observePendingDigestItems()

    fun observeMessagesByFolder(folderId: Long): Flow<List<EmailMessageEntity>> =
        dao.observeMessagesByFolder(folderId)

    fun observeMessageById(messageId: Long): Flow<EmailMessageEntity?> =
        dao.observeMessageById(messageId)

    fun observeAttachmentsForMessage(messageId: Long): Flow<List<AttachmentEntity>> =
        dao.observeAttachmentsForMessage(messageId)

    fun searchMessages(query: String): Flow<List<EmailMessageEntity>> =
        if (query.isBlank()) allMessagesFlow else dao.searchMessages(query.trim())

    /**
     * Přidá nový e-mailový účet, uloží jeho heslo do šifrovaného Android Keystore trezoru
     * a vytvoří standardní sadu složek v lokální Room cache.
     */
    suspend fun addAccountWithFolders(
        account: AccountEntity,
        password: String
    ): Long {
        val existingCount = dao.getAllAccountsOnce().size
        val accountToSave = account.copy(isDefault = existingCount == 0 || account.isDefault)
        val accountId = dao.insertAccount(accountToSave)
        passwordVault.savePassword(accountId, password)

        val defaultFolders = listOf(
            FolderEntity(accountId = accountId, name = "Doručené", serverPath = "INBOX", type = FolderType.INBOX),
            FolderEntity(accountId = accountId, name = "Odeslané", serverPath = "Sent", type = FolderType.SENT),
            FolderEntity(accountId = accountId, name = "Koncepty", serverPath = "Drafts", type = FolderType.DRAFTS),
            FolderEntity(accountId = accountId, name = "Archiv", serverPath = "Archive", type = FolderType.ARCHIVE),
            FolderEntity(accountId = accountId, name = "Koš", serverPath = "Trash", type = FolderType.TRASH),
            FolderEntity(accountId = accountId, name = "Nevyžádané", serverPath = "Spam", type = FolderType.SPAM)
        )
        dao.insertFolders(defaultFolders)
        return accountId
    }

    suspend fun updateAccount(account: AccountEntity, newPassword: String? = null) {
        dao.updateAccount(account)
        if (!newPassword.isNullOrBlank()) {
            passwordVault.savePassword(account.id, newPassword)
        }
    }

    suspend fun deleteAccount(accountId: Long) {
        passwordVault.deletePassword(accountId)
        dao.deleteAccountById(accountId)
    }

    fun getAccountPassword(accountId: Long): String? = passwordVault.getPassword(accountId)

    suspend fun saveAccountGroup(group: AccountGroupEntity): Long = dao.insertAccountGroup(group)

    suspend fun deleteAccountGroup(group: AccountGroupEntity) = dao.deleteAccountGroup(group)

    /**
     * Ověří přímé síťové spojení se zadaným serverem (IMAP/POP3/JMAP).
     */
    suspend fun verifyConnection(account: AccountEntity, password: String): Result<String> {
        return when (account.protocol) {
            MailProtocol.IMAP -> imapSmtpPop3Client.testIncomingConnection(
                host = account.incomingHost,
                port = account.incomingPort,
                security = account.incomingSecurity,
                username = account.username,
                password = password,
                isPop3 = false
            )
            MailProtocol.POP3 -> imapSmtpPop3Client.testIncomingConnection(
                host = account.incomingHost,
                port = account.incomingPort,
                security = account.incomingSecurity,
                username = account.username,
                password = password,
                isPop3 = true
            )
            MailProtocol.JMAP -> jmapClient.fetchSession(
                sessionEndpointUrl = account.jmapEndpoint.ifBlank { "https://${account.incomingHost}/.well-known/jmap" },
                username = account.username,
                password = password
            ).map { "JMAP Session ověřena (${it.apiUrl})" }
        }
    }

    /**
     * Provede přímou síťovou synchronizaci účtu se vzdáleným IMAP, POP3 nebo JMAP serverem.
     * Žádná falešná offline simulace: vrací skutečný výsledek síťové komunikace.
     */
    suspend fun synchronizeAccount(accountId: Long): Result<List<EmailMessageEntity>> {
        val account = dao.getAccountById(accountId)
            ?: return Result.failure(IllegalArgumentException("Účet nebyl nalezen."))
        val password = passwordVault.getPassword(accountId).orEmpty()
        if (password.isBlank()) {
            return Result.failure(IllegalStateException("V Keystore chybí heslo pro účet ${account.emailAddress}."))
        }

        val inboxFolder = dao.getFolderByType(accountId, FolderType.INBOX)
            ?: FolderEntity(
                accountId = accountId,
                name = "Doručené",
                serverPath = "INBOX",
                type = FolderType.INBOX
            ).let { folder ->
                val id = dao.insertFolder(folder)
                folder.copy(id = id)
            }

        val vipEmails = dao.getAllContactsOnce()
            .filter { it.isVip }
            .map { it.email.lowercase(Locale.ROOT) }
            .toSet()

        val timeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        val syncResult: Result<List<EmailMessageEntity>> = when (account.protocol) {
            MailProtocol.IMAP -> {
                imapSmtpPop3Client.fetchImapInboxMessages(account, password).map { remoteItems ->
                    remoteItems.map { item ->
                        val parsed = EmailHeaderParser.parseHeaders(item.rawHeaders)
                        EmailMessageEntity(
                            accountId = accountId,
                            folderId = inboxFolder.id,
                            remoteUid = item.uid,
                            messageIdHeader = parsed.messageId,
                            threadId = parsed.threadId,
                            subject = parsed.subject,
                            fromName = parsed.fromName,
                            fromEmail = parsed.fromEmail,
                            toRecipients = parsed.to.ifBlank { account.emailAddress },
                            ccRecipients = parsed.cc,
                            replyTo = parsed.replyTo,
                            snippet = item.bodyPlain.lineSequence().firstOrNull().orEmpty().take(140),
                            bodyPlain = item.bodyPlain,
                            rawHeaders = item.rawHeaders,
                            spfStatus = parsed.spfStatus,
                            dkimStatus = parsed.dkimStatus,
                            receivedAt = parsed.dateEpochMillis,
                            isRead = item.isRead,
                            isStarred = item.isStarred,
                            isVipSender = parsed.fromEmail.lowercase(Locale.ROOT) in vipEmails
                        )
                    }
                }
            }
            MailProtocol.POP3 -> {
                imapSmtpPop3Client.fetchPop3Messages(account, password).map { remoteItems ->
                    remoteItems.map { item ->
                        val parsed = EmailHeaderParser.parseHeaders(item.rawHeaders)
                        EmailMessageEntity(
                            accountId = accountId,
                            folderId = inboxFolder.id,
                            remoteUid = item.uid,
                            messageIdHeader = parsed.messageId,
                            threadId = parsed.threadId,
                            subject = parsed.subject,
                            fromName = parsed.fromName,
                            fromEmail = parsed.fromEmail,
                            toRecipients = parsed.to.ifBlank { account.emailAddress },
                            ccRecipients = parsed.cc,
                            replyTo = parsed.replyTo,
                            snippet = item.bodyPlain.lineSequence().firstOrNull().orEmpty().take(140),
                            bodyPlain = item.bodyPlain,
                            rawHeaders = item.rawHeaders,
                            spfStatus = parsed.spfStatus,
                            dkimStatus = parsed.dkimStatus,
                            receivedAt = parsed.dateEpochMillis,
                            isRead = item.isRead,
                            isStarred = item.isStarred,
                            isVipSender = parsed.fromEmail.lowercase(Locale.ROOT) in vipEmails
                        )
                    }
                }
            }
            MailProtocol.JMAP -> {
                val endpoint = account.jmapEndpoint.ifBlank { "https://${account.incomingHost}/.well-known/jmap" }
                jmapClient.fetchSession(endpoint, account.username, password).mapCatching { session ->
                    val dtos = jmapClient.fetchRecentEmails(session, account.username, password).getOrThrow()
                    dtos.map { dto ->
                        EmailMessageEntity(
                            accountId = accountId,
                            folderId = inboxFolder.id,
                            remoteUid = dto.id,
                            messageIdHeader = "<${dto.id}@jmap>",
                            threadId = dto.threadId,
                            subject = dto.subject,
                            fromName = dto.fromName,
                            fromEmail = dto.fromEmail,
                            toRecipients = dto.toFormatted.ifBlank { account.emailAddress },
                            snippet = dto.preview.take(140),
                            bodyPlain = dto.bodyPlain,
                            rawHeaders = "X-JMAP-ID: ${dto.id}\r\nFrom: ${dto.fromName} <${dto.fromEmail}>\r\nSubject: ${dto.subject}",
                            receivedAt = dto.receivedAtEpochMillis,
                            isRead = dto.isSeen,
                            isStarred = dto.isFlagged,
                            isVipSender = dto.fromEmail.lowercase(Locale.ROOT) in vipEmails,
                            hasAttachments = dto.hasAttachment
                        )
                    }
                }
            }
        }

        syncResult.onSuccess { fetchedMessages ->
            if (fetchedMessages.isNotEmpty()) {
                dao.insertMessages(fetchedMessages)
            }
            val unread = fetchedMessages.count { !it.isRead }
            dao.updateFolder(
                inboxFolder.copy(
                    unreadCount = unread,
                    totalCount = fetchedMessages.size
                )
            )
            dao.updateAccount(
                account.copy(
                    lastSyncTimestamp = System.currentTimeMillis(),
                    lastSyncStatus = "Synchronizováno v $timeFormatted (${fetchedMessages.size} zpráv)"
                )
            )
        }.onFailure { err ->
            dao.updateAccount(
                account.copy(
                    lastSyncTimestamp = System.currentTimeMillis(),
                    lastSyncStatus = "Chyba spojení ($timeFormatted): ${err.message ?: "Server nedostupný"}"
                )
            )
        }

        return syncResult
    }

    /**
     * Odešle e-mail výhradně přes přímé síťové spojení (SMTP nebo JMAP).
     * Pokud přímé spojení selže, uloží zprávu do složky Koncepty se stavem QUEUED_PENDING_NETWORK
     * a vrátí chybu sítě (bez falešné offline simulace odeslání).
     */
    suspend fun sendEmailDirect(
        accountId: Long,
        toRecipients: List<String>,
        ccRecipients: List<String>,
        bccRecipients: List<String>,
        subject: String,
        bodyPlain: String,
        attachmentNames: List<String> = emptyList(),
        inReplyToMessageId: String? = null
    ): Result<Long> {
        val account = dao.getAccountById(accountId)
            ?: return Result.failure(IllegalArgumentException("Zvolený účet neexistuje."))
        val password = passwordVault.getPassword(accountId).orEmpty()
        if (password.isBlank()) {
            return Result.failure(IllegalStateException("Chybí heslo pro účet ${account.emailAddress}."))
        }

        val bodyWithSignature = if (account.signature.isNotBlank()) {
            "$bodyPlain\n\n--\n${account.signature}"
        } else {
            bodyPlain
        }

        val networkSendResult: Result<String> = when (account.protocol) {
            MailProtocol.JMAP -> {
                val endpoint = account.jmapEndpoint.ifBlank { "https://${account.incomingHost}/.well-known/jmap" }
                jmapClient.fetchSession(endpoint, account.username, password).mapCatching { session ->
                    jmapClient.sendEmail(
                        session = session,
                        username = account.username,
                        password = password,
                        fromName = account.displayName.ifBlank { account.accountName },
                        fromEmail = account.emailAddress,
                        toEmails = toRecipients,
                        subject = subject,
                        bodyPlain = bodyWithSignature
                    ).getOrThrow()
                }
            }
            MailProtocol.IMAP, MailProtocol.POP3 -> {
                imapSmtpPop3Client.sendSmtpEmail(
                    account = account,
                    password = password,
                    toRecipients = toRecipients,
                    ccRecipients = ccRecipients,
                    bccRecipients = bccRecipients,
                    subject = subject,
                    bodyPlain = bodyWithSignature,
                    inReplyToMessageId = inReplyToMessageId
                )
            }
        }

        return networkSendResult.fold(
            onSuccess = { remoteMsgId ->
                val sentFolder = dao.getFolderByType(accountId, FolderType.SENT)
                val folderId = sentFolder?.id ?: dao.getFoldersByAccountOnce(accountId).first().id
                val (_, rawMime) = EmailHeaderParser.buildRfc5322Message(
                    fromName = account.displayName,
                    fromEmail = account.emailAddress,
                    to = toRecipients,
                    cc = ccRecipients,
                    bcc = bccRecipients,
                    subject = subject,
                    bodyPlain = bodyWithSignature,
                    inReplyToMessageId = inReplyToMessageId
                )
                val msgId = dao.insertMessage(
                    EmailMessageEntity(
                        accountId = accountId,
                        folderId = folderId,
                        remoteUid = remoteMsgId,
                        messageIdHeader = remoteMsgId,
                        threadId = inReplyToMessageId ?: remoteMsgId,
                        subject = subject.ifBlank { "(Bez předmětu)" },
                        fromName = account.displayName.ifBlank { account.accountName },
                        fromEmail = account.emailAddress,
                        toRecipients = toRecipients.joinToString(", "),
                        ccRecipients = ccRecipients.joinToString(", "),
                        bccRecipients = bccRecipients.joinToString(", "),
                        snippet = bodyPlain.take(140),
                        bodyPlain = bodyWithSignature,
                        rawHeaders = rawMime.substringBefore("\r\n\r\n"),
                        receivedAt = System.currentTimeMillis(),
                        isRead = true,
                        hasAttachments = attachmentNames.isNotEmpty(),
                        attachmentNames = attachmentNames.joinToString(", "),
                        outboxStatus = OutboxStatus.SENT
                    )
                )
                if (attachmentNames.isNotEmpty()) {
                    dao.insertAttachments(
                        attachmentNames.map { name ->
                            AttachmentEntity(
                                messageId = msgId,
                                fileName = name,
                                mimeType = "application/octet-stream",
                                sizeBytes = 24_576L
                            )
                        }
                    )
                }
                Result.success(msgId)
            },
            onFailure = { error ->
                // Uložíme zprávu do složky Koncepty jako čekající na síť, ale neoznačíme ji jako odeslanou
                val draftsFolder = dao.getFolderByType(accountId, FolderType.DRAFTS)
                if (draftsFolder != null) {
                    dao.insertMessage(
                        EmailMessageEntity(
                            accountId = accountId,
                            folderId = draftsFolder.id,
                            remoteUid = "pending-${System.currentTimeMillis()}",
                            messageIdHeader = "<pending@kuryr.local>",
                            threadId = inReplyToMessageId ?: "<pending@kuryr.local>",
                            subject = subject.ifBlank { "(Bez předmětu)" },
                            fromName = account.displayName.ifBlank { account.accountName },
                            fromEmail = account.emailAddress,
                            toRecipients = toRecipients.joinToString(", "),
                            ccRecipients = ccRecipients.joinToString(", "),
                            bccRecipients = bccRecipients.joinToString(", "),
                            snippet = bodyPlain.take(140),
                            bodyPlain = bodyWithSignature,
                            receivedAt = System.currentTimeMillis(),
                            isRead = true,
                            hasAttachments = attachmentNames.isNotEmpty(),
                            attachmentNames = attachmentNames.joinToString(", "),
                            outboxStatus = OutboxStatus.QUEUED_PENDING_NETWORK
                        )
                    )
                }
                Result.failure(error)
            }
        )
    }

    suspend fun saveDraft(
        accountId: Long,
        toRecipients: String,
        ccRecipients: String,
        subject: String,
        bodyPlain: String
    ): Long {
        val account = dao.getAccountById(accountId) ?: return -1L
        val draftsFolder = dao.getFolderByType(accountId, FolderType.DRAFTS)
            ?: dao.getFoldersByAccountOnce(accountId).firstOrNull() ?: return -1L
        return dao.insertMessage(
            EmailMessageEntity(
                accountId = accountId,
                folderId = draftsFolder.id,
                remoteUid = "draft-${System.currentTimeMillis()}",
                messageIdHeader = "<draft-${System.currentTimeMillis()}@kuryr.local>",
                threadId = "<draft-${System.currentTimeMillis()}@kuryr.local>",
                subject = subject.ifBlank { "(Koncept bez předmětu)" },
                fromName = account.displayName.ifBlank { account.accountName },
                fromEmail = account.emailAddress,
                toRecipients = toRecipients,
                ccRecipients = ccRecipients,
                snippet = bodyPlain.take(120),
                bodyPlain = bodyPlain,
                receivedAt = System.currentTimeMillis(),
                isRead = true,
                outboxStatus = OutboxStatus.DRAFT
            )
        )
    }

    /**
     * Provede konfigurovatelnou swipe akci nad zprávou v seznamu.
     */
    suspend fun executeSwipeAction(message: EmailMessageEntity, action: SwipeAction) {
        when (action) {
            SwipeAction.ARCHIVE -> {
                val archiveFolder = dao.getFolderByType(message.accountId, FolderType.ARCHIVE)
                if (archiveFolder != null) {
                    dao.moveMessageToFolder(message.id, archiveFolder.id)
                }
            }
            SwipeAction.DELETE -> {
                val trashFolder = dao.getFolderByType(message.accountId, FolderType.TRASH)
                if (trashFolder != null && message.folderId != trashFolder.id) {
                    dao.moveMessageToFolder(message.id, trashFolder.id)
                } else {
                    dao.deleteMessageById(message.id)
                }
            }
            SwipeAction.TOGGLE_READ -> {
                dao.setMessageRead(message.id, !message.isRead)
            }
            SwipeAction.TOGGLE_STAR -> {
                dao.setMessageStarred(message.id, !message.isStarred)
            }
            SwipeAction.NONE -> Unit
        }
    }

    suspend fun toggleRead(message: EmailMessageEntity) {
        dao.setMessageRead(message.id, !message.isRead)
    }

    suspend fun toggleStarred(message: EmailMessageEntity) {
        dao.setMessageStarred(message.id, !message.isStarred)
    }

    suspend fun moveMessageToFolderType(message: EmailMessageEntity, targetType: FolderType) {
        val folder = dao.getFolderByType(message.accountId, targetType)
        if (folder != null) {
            dao.moveMessageToFolder(message.id, folder.id)
        }
    }

    suspend fun deleteMessagePermanently(messageId: Long) {
        dao.deleteMessageById(messageId)
    }

    // --- Notification Rules ---
    suspend fun saveNotificationRule(rule: NotificationRuleEntity): Long =
        dao.insertNotificationRule(rule)

    suspend fun toggleNotificationRule(rule: NotificationRuleEntity) =
        dao.updateNotificationRule(rule.copy(isEnabled = !rule.isEnabled))

    suspend fun deleteNotificationRule(ruleId: Long) =
        dao.deleteNotificationRule(ruleId)

    suspend fun clearProcessedDigestQueue() =
        dao.markAllDigestItemsProcessed()
}
