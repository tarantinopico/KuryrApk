package com.example

import com.example.data.local.AccountEntity
import com.example.data.local.AccountGroupEntity
import com.example.data.local.AttachmentEntity
import com.example.data.local.ContactEntity
import com.example.data.local.ContactGroupEntity
import com.example.data.local.DigestQueueEntity
import com.example.data.local.EmailMessageEntity
import com.example.data.local.FolderEntity
import com.example.data.local.KuryrDao
import com.example.data.local.NotificationRuleEntity
import com.example.data.model.FolderType
import com.example.data.model.OutboxStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeKuryrDao : KuryrDao {
    private var nextId = 1L
    private val accountGroups = MutableStateFlow<List<AccountGroupEntity>>(emptyList())
    private val accounts = MutableStateFlow<List<AccountEntity>>(emptyList())
    private val folders = MutableStateFlow<List<FolderEntity>>(emptyList())
    private val messages = MutableStateFlow<List<EmailMessageEntity>>(emptyList())
    private val attachments = MutableStateFlow<List<AttachmentEntity>>(emptyList())
    private val contactGroups = MutableStateFlow<List<ContactGroupEntity>>(emptyList())
    private val contacts = MutableStateFlow<List<ContactEntity>>(emptyList())
    private val rules = MutableStateFlow<List<NotificationRuleEntity>>(emptyList())
    private val digestQueue = MutableStateFlow<List<DigestQueueEntity>>(emptyList())

    override fun observeAccountGroups(): Flow<List<AccountGroupEntity>> = accountGroups

    override suspend fun insertAccountGroup(group: AccountGroupEntity): Long {
        val id = if (group.id == 0L) nextId++ else group.id
        val saved = group.copy(id = id)
        accountGroups.value = accountGroups.value.filterNot { it.id == id } + saved
        return id
    }

    override suspend fun deleteAccountGroup(group: AccountGroupEntity) {
        accountGroups.value = accountGroups.value.filterNot { it.id == group.id }
    }

    override fun observeAccounts(): Flow<List<AccountEntity>> = accounts

    override suspend fun getAllAccountsOnce(): List<AccountEntity> = accounts.value

    override suspend fun getAccountById(accountId: Long): AccountEntity? =
        accounts.value.firstOrNull { it.id == accountId }

    override suspend fun insertAccount(account: AccountEntity): Long {
        val id = if (account.id == 0L) nextId++ else account.id
        val saved = account.copy(id = id)
        accounts.value = accounts.value.filterNot { it.id == id } + saved
        return id
    }

    override suspend fun updateAccount(account: AccountEntity) {
        accounts.value = accounts.value.map { if (it.id == account.id) account else it }
    }

    override suspend fun deleteAccountById(accountId: Long) {
        accounts.value = accounts.value.filterNot { it.id == accountId }
        folders.value = folders.value.filterNot { it.accountId == accountId }
        messages.value = messages.value.filterNot { it.accountId == accountId }
    }

    override fun observeAllFolders(): Flow<List<FolderEntity>> = folders

    override fun observeFoldersByAccount(accountId: Long): Flow<List<FolderEntity>> =
        folders.map { list -> list.filter { it.accountId == accountId } }

    override suspend fun getFoldersByAccountOnce(accountId: Long): List<FolderEntity> =
        folders.value.filter { it.accountId == accountId }

    override suspend fun getFolderByType(accountId: Long, type: FolderType): FolderEntity? =
        folders.value.firstOrNull { it.accountId == accountId && it.type == type }

    override suspend fun insertFolder(folder: FolderEntity): Long {
        val id = if (folder.id == 0L) nextId++ else folder.id
        val saved = folder.copy(id = id)
        folders.value = folders.value.filterNot { it.id == id } + saved
        return id
    }

    override suspend fun insertFolders(folders: List<FolderEntity>): List<Long> =
        folders.map { insertFolder(it) }

    override suspend fun updateFolder(folder: FolderEntity) {
        folders.value = folders.value.map { if (it.id == folder.id) folder else it }
    }

    override fun observeAllMessages(): Flow<List<EmailMessageEntity>> =
        messages.map { it.sortedByDescending { m -> m.receivedAt } }

    override fun observeMessagesByFolder(folderId: Long): Flow<List<EmailMessageEntity>> =
        messages.map { list -> list.filter { it.folderId == folderId }.sortedByDescending { it.receivedAt } }

    override fun observeMessagesByAccount(accountId: Long): Flow<List<EmailMessageEntity>> =
        messages.map { list -> list.filter { it.accountId == accountId }.sortedByDescending { it.receivedAt } }

    override fun observeMessageById(messageId: Long): Flow<EmailMessageEntity?> =
        messages.map { list -> list.firstOrNull { it.id == messageId } }

    override suspend fun getMessageById(messageId: Long): EmailMessageEntity? =
        messages.value.firstOrNull { it.id == messageId }

    override suspend fun getMessagesByOutboxStatus(status: OutboxStatus): List<EmailMessageEntity> =
        messages.value.filter { it.outboxStatus == status }

    override fun searchMessages(query: String): Flow<List<EmailMessageEntity>> =
        messages.map { list ->
            val q = query.lowercase()
            list.filter {
                it.subject.lowercase().contains(q) ||
                    it.fromName.lowercase().contains(q) ||
                    it.fromEmail.lowercase().contains(q) ||
                    it.bodyPlain.lowercase().contains(q)
            }
        }

    override suspend fun insertMessage(message: EmailMessageEntity): Long {
        val id = if (message.id == 0L) nextId++ else message.id
        val saved = message.copy(id = id)
        messages.value = messages.value.filterNot { it.id == id } + saved
        return id
    }

    override suspend fun insertMessages(messages: List<EmailMessageEntity>) {
        messages.forEach { insertMessage(it) }
    }

    override suspend fun updateMessage(message: EmailMessageEntity) {
        messages.value = messages.value.map { if (it.id == message.id) message else it }
    }

    override suspend fun setMessageRead(messageId: Long, isRead: Boolean) {
        messages.value = messages.value.map { if (it.id == messageId) it.copy(isRead = isRead) else it }
    }

    override suspend fun setMessageStarred(messageId: Long, isStarred: Boolean) {
        messages.value = messages.value.map { if (it.id == messageId) it.copy(isStarred = isStarred) else it }
    }

    override suspend fun moveMessageToFolder(messageId: Long, targetFolderId: Long) {
        messages.value = messages.value.map { if (it.id == messageId) it.copy(folderId = targetFolderId) else it }
    }

    override suspend fun deleteMessageById(messageId: Long) {
        messages.value = messages.value.filterNot { it.id == messageId }
    }

    override fun observeAttachmentsForMessage(messageId: Long): Flow<List<AttachmentEntity>> =
        attachments.map { list -> list.filter { it.messageId == messageId } }

    override suspend fun insertAttachments(attachments: List<AttachmentEntity>) {
        val saved = attachments.map { if (it.id == 0L) it.copy(id = nextId++) else it }
        this.attachments.value = this.attachments.value + saved
    }

    override fun observeContactGroups(): Flow<List<ContactGroupEntity>> = contactGroups

    override suspend fun insertContactGroup(group: ContactGroupEntity): Long {
        val id = if (group.id == 0L) nextId++ else group.id
        val saved = group.copy(id = id)
        contactGroups.value = contactGroups.value.filterNot { it.id == id } + saved
        return id
    }

    override suspend fun deleteContactGroup(group: ContactGroupEntity) {
        contactGroups.value = contactGroups.value.filterNot { it.id == group.id }
    }

    override fun observeContacts(): Flow<List<ContactEntity>> = contacts

    override suspend fun getAllContactsOnce(): List<ContactEntity> = contacts.value

    override fun searchContacts(query: String): Flow<List<ContactEntity>> =
        contacts.map { list ->
            val q = query.lowercase()
            list.filter {
                it.displayName.lowercase().contains(q) ||
                    it.email.lowercase().contains(q) ||
                    it.organization.lowercase().contains(q)
            }
        }

    override suspend fun insertContact(contact: ContactEntity): Long {
        val id = if (contact.id == 0L) nextId++ else contact.id
        val saved = contact.copy(id = id)
        contacts.value = contacts.value.filterNot { it.id == id } + saved
        return id
    }

    override suspend fun insertContacts(contacts: List<ContactEntity>) {
        contacts.forEach { insertContact(it) }
    }

    override suspend fun updateContact(contact: ContactEntity) {
        contacts.value = contacts.value.map { if (it.id == contact.id) contact else it }
    }

    override suspend fun deleteContactById(contactId: Long) {
        contacts.value = contacts.value.filterNot { it.id == contactId }
    }

    override fun observeNotificationRules(): Flow<List<NotificationRuleEntity>> = rules

    override suspend fun getEnabledNotificationRules(): List<NotificationRuleEntity> =
        rules.value.filter { it.isEnabled }

    override suspend fun insertNotificationRule(rule: NotificationRuleEntity): Long {
        val id = if (rule.id == 0L) nextId++ else rule.id
        val saved = rule.copy(id = id)
        rules.value = rules.value.filterNot { it.id == id } + saved
        return id
    }

    override suspend fun updateNotificationRule(rule: NotificationRuleEntity) {
        rules.value = rules.value.map { if (it.id == rule.id) rule else it }
    }

    override suspend fun deleteNotificationRule(ruleId: Long) {
        rules.value = rules.value.filterNot { it.id == ruleId }
    }

    override fun observePendingDigestItems(): Flow<List<DigestQueueEntity>> =
        digestQueue.map { list -> list.filter { !it.isProcessed } }

    override suspend fun getPendingDigestItemsOnce(): List<DigestQueueEntity> =
        digestQueue.value.filter { !it.isProcessed }

    override suspend fun insertDigestItem(item: DigestQueueEntity): Long {
        val id = if (item.id == 0L) nextId++ else item.id
        val saved = item.copy(id = id)
        digestQueue.value = digestQueue.value.filterNot { it.id == id } + saved
        return id
    }

    override suspend fun markAllDigestItemsProcessed() {
        digestQueue.value = digestQueue.value.map { it.copy(isProcessed = true) }
    }
}
