package com.example.ui

import android.content.ContentResolver
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccountEntity
import com.example.data.local.AccountGroupEntity
import com.example.data.local.ContactEntity
import com.example.data.local.ContactGroupEntity
import com.example.data.local.DigestQueueEntity
import com.example.data.local.EmailMessageEntity
import com.example.data.local.FolderEntity
import com.example.data.local.KuryrDatabase
import com.example.data.local.NotificationRuleEntity
import com.example.data.model.DigestFrequency
import com.example.data.model.FolderType
import com.example.data.model.ListDensity
import com.example.data.model.MailProtocol
import com.example.data.model.SecurityMode
import com.example.data.model.SwipeAction
import com.example.data.model.ThemeMode
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.preferences.UserPreferences
import com.example.data.repository.ContactRepository
import com.example.data.repository.MailRepository
import com.example.data.security.AndroidKeystoreCredentialVault
import com.example.network.ImapSmtpPop3Client
import com.example.network.JmapClient
import com.example.notifications.NotificationRuleEngine
import com.example.sync.MailSyncWorker
import com.example.widget.KuryrWidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class KuryrDestination {
    ONBOARDING,
    ADD_ACCOUNT,
    UNIFIED_INBOX,
    MESSAGE_DETAIL,
    COMPOSE,
    ACCOUNTS,
    CONTACTS,
    NOTIFICATIONS,
    SEARCH,
    SETTINGS
}

enum class InboxQuickFilter(val label: String) {
    ALL("Vše"),
    UNREAD("Nepřečtené"),
    STARRED("S hvězdičkou"),
    VIP("VIP"),
    ATTACHMENTS("S přílohou")
}

data class ComposeDraftState(
    val replyToMessageId: String? = null,
    val initialTo: String = "",
    val initialSubject: String = "",
    val initialBody: String = ""
)

data class KuryrUiState(
    val currentDestination: KuryrDestination = KuryrDestination.ONBOARDING,
    val selectedAccountId: Long? = null, // null = Unified Inbox (všechny účty)
    val selectedAccountGroupId: Long? = null,
    val selectedFolderType: FolderType = FolderType.INBOX,
    val quickFilter: InboxQuickFilter = InboxQuickFilter.ALL,
    val selectedMessageId: Long? = null,
    val editingAccount: AccountEntity? = null,
    val composeDraft: ComposeDraftState = ComposeDraftState(),
    val globalSearchQuery: String = "",
    val contactSearchQuery: String = "",
    val selectedContactGroupId: Long? = null,
    val isSyncing: Boolean = false,
    val isTestingConnection: Boolean = false,
    val isSendingEmail: Boolean = false,
    val connectionTestFeedback: String? = null,
    val snackbarMessage: String? = null
)

class KuryrViewModel(
    private val appContext: Context,
    private val mailRepository: MailRepository,
    private val contactRepository: ContactRepository,
    private val preferencesRepository: AppPreferencesRepository,
    private val notificationRuleEngine: NotificationRuleEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(KuryrUiState())
    val uiState: StateFlow<KuryrUiState> = _uiState.asStateFlow()

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    val accounts: StateFlow<List<AccountEntity>> = mailRepository.accountsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accountGroups: StateFlow<List<AccountGroupEntity>> = mailRepository.accountGroupsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<FolderEntity>> = mailRepository.foldersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMessages: StateFlow<List<EmailMessageEntity>> = mailRepository.allMessagesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<ContactEntity>> = contactRepository.contactsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contactGroups: StateFlow<List<ContactGroupEntity>> = contactRepository.contactGroupsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notificationRules: StateFlow<List<NotificationRuleEntity>> = mailRepository.notificationRulesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingDigestItems: StateFlow<List<DigestQueueEntity>> = mailRepository.pendingDigestFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        notificationRuleEngine.ensureNotificationChannels(appContext)
        viewModelScope.launch {
            mailRepository.accountsFlow.collect { acctList ->
                val current = _uiState.value.currentDestination
                if (acctList.isNotEmpty() && current == KuryrDestination.ONBOARDING) {
                    _uiState.update { it.copy(currentDestination = KuryrDestination.UNIFIED_INBOX) }
                }
            }
        }
    }

    fun navigateTo(destination: KuryrDestination) {
        _uiState.update {
            it.copy(
                currentDestination = destination,
                connectionTestFeedback = null
            )
        }
    }

    fun startAddAccount() {
        _uiState.update {
            it.copy(
                editingAccount = null,
                connectionTestFeedback = null,
                currentDestination = KuryrDestination.ADD_ACCOUNT
            )
        }
    }

    fun startEditAccount(account: AccountEntity) {
        _uiState.update {
            it.copy(
                editingAccount = account,
                connectionTestFeedback = null,
                currentDestination = KuryrDestination.ADD_ACCOUNT
            )
        }
    }

    fun completeOnboardingAndOpenAccountSetup() {
        startAddAccount()
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
        }
    }

    fun selectAccountFilter(accountId: Long?) {
        _uiState.update { it.copy(selectedAccountId = accountId, selectedAccountGroupId = null) }
    }

    fun selectAccountGroupFilter(groupId: Long?) {
        _uiState.update { it.copy(selectedAccountGroupId = groupId, selectedAccountId = null) }
    }

    fun selectFolderType(folderType: FolderType) {
        _uiState.update { it.copy(selectedFolderType = folderType) }
    }

    fun selectQuickFilter(filter: InboxQuickFilter) {
        _uiState.update { it.copy(quickFilter = filter) }
    }

    fun openMessageDetail(message: EmailMessageEntity) {
        _uiState.update {
            it.copy(
                selectedMessageId = message.id,
                currentDestination = KuryrDestination.MESSAGE_DETAIL
            )
        }
        viewModelScope.launch {
            if (!message.isRead) {
                mailRepository.toggleRead(message)
            }
        }
    }

    fun openCompose(
        to: String = "",
        subject: String = "",
        body: String = "",
        replyToMessageId: String? = null
    ) {
        _uiState.update {
            it.copy(
                composeDraft = ComposeDraftState(
                    replyToMessageId = replyToMessageId,
                    initialTo = to,
                    initialSubject = subject,
                    initialBody = body
                ),
                currentDestination = KuryrDestination.COMPOSE
            )
        }
    }

    fun testAccountConnection(
        accountDraft: AccountEntity,
        password: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, connectionTestFeedback = "Ověřuji přímé spojení se serverem…") }
            val result = mailRepository.verifyConnection(accountDraft, password)
            _uiState.update {
                it.copy(
                    isTestingConnection = false,
                    connectionTestFeedback = result.fold(
                        onSuccess = { msg -> "Spojení úspěšné: $msg" },
                        onFailure = { err -> "Přímé spojení se nezdařilo: ${err.message}" }
                    )
                )
            }
        }
    }

    fun saveAccount(
        accountDraft: AccountEntity,
        password: String,
        attemptImmediateSync: Boolean = true
    ) {
        _uiState.update {
            it.copy(
                editingAccount = null,
                currentDestination = KuryrDestination.UNIFIED_INBOX,
                snackbarMessage = "Účet ${accountDraft.emailAddress} byl bezpečně uložen."
            )
        }
        viewModelScope.launch {
            val isEditing = accountDraft.id != 0L
            val savedAccountId = if (isEditing) {
                mailRepository.updateAccount(accountDraft, password.takeIf { it.isNotBlank() })
                accountDraft.id
            } else {
                mailRepository.addAccountWithFolders(accountDraft, password)
            }
            launch {
                preferencesRepository.setOnboardingCompleted(true)
                MailSyncWorker.schedulePeriodicSync(appContext, accountDraft.syncIntervalMinutes)
                KuryrWidgetUpdater.updateAllWidgets(appContext)
            }
            if (attemptImmediateSync) {
                synchronizeAccountNow(savedAccountId)
            }
        }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            mailRepository.deleteAccount(accountId)
            KuryrWidgetUpdater.updateAllWidgets(appContext)
            _uiState.update { it.copy(snackbarMessage = "Účet byl odstraněn.") }
        }
    }

    fun saveAccountGroup(name: String, colorHex: Long) {
        if (name.isBlank()) return
        viewModelScope.launch {
            mailRepository.saveAccountGroup(AccountGroupEntity(name = name.trim(), colorHex = colorHex))
            _uiState.update { it.copy(snackbarMessage = "Skupina účtů „$name“ vytvořena.") }
        }
    }

    fun deleteAccountGroup(group: AccountGroupEntity) {
        viewModelScope.launch {
            mailRepository.deleteAccountGroup(group)
        }
    }

    fun synchronizeAccountNow(accountId: Long? = _uiState.value.selectedAccountId) {
        viewModelScope.launch {
            val targetAccounts = if (accountId != null) {
                accounts.value.filter { it.id == accountId }
            } else {
                accounts.value
            }
            if (targetAccounts.isEmpty()) {
                _uiState.update { it.copy(snackbarMessage = "Nejprve přidejte e-mailový účet.") }
                return@launch
            }

            _uiState.update { it.copy(isSyncing = true) }
            var lastError: String? = null
            var syncedCount = 0

            for (account in targetAccounts) {
                val res = mailRepository.synchronizeAccount(account.id)
                res.onSuccess { list -> syncedCount += list.size }
                    .onFailure { err -> lastError = err.message ?: "Chyba spojení" }
            }

            KuryrWidgetUpdater.updateAllWidgets(appContext)
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    snackbarMessage = if (lastError != null) {
                        "Synchronizace: $lastError"
                    } else {
                        "Synchronizace dokončena ($syncedCount zpráv)."
                    }
                )
            }
        }
    }

    fun sendEmail(
        accountId: Long,
        toRaw: String,
        ccRaw: String,
        bccRaw: String,
        subject: String,
        body: String,
        attachments: List<String>
    ) {
        val toList = toRaw.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }
        val ccList = ccRaw.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }
        val bccList = bccRaw.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }

        if (toList.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "Vyplňte alespoň jednoho příjemce v poli Komu.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingEmail = true) }
            val result = mailRepository.sendEmailDirect(
                accountId = accountId,
                toRecipients = toList,
                ccRecipients = ccList,
                bccRecipients = bccList,
                subject = subject,
                bodyPlain = body,
                attachmentNames = attachments,
                inReplyToMessageId = _uiState.value.composeDraft.replyToMessageId
            )

            KuryrWidgetUpdater.updateAllWidgets(appContext)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSendingEmail = false,
                            currentDestination = KuryrDestination.UNIFIED_INBOX,
                            snackbarMessage = "Zpráva byla úspěšně odeslána přes server."
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSendingEmail = false,
                            currentDestination = KuryrDestination.UNIFIED_INBOX,
                            snackbarMessage = "Přímé odeslání selhalo (${err.message}). Uloženo do Konceptů k odeslání."
                        )
                    }
                }
            )
        }
    }

    fun saveDraft(
        accountId: Long,
        toRaw: String,
        ccRaw: String,
        subject: String,
        body: String
    ) {
        viewModelScope.launch {
            mailRepository.saveDraft(accountId, toRaw, ccRaw, subject, body)
            _uiState.update {
                it.copy(
                    currentDestination = KuryrDestination.UNIFIED_INBOX,
                    snackbarMessage = "Koncept byl uložen do lokální databáze."
                )
            }
        }
    }

    fun executeSwipeAction(message: EmailMessageEntity, action: SwipeAction) {
        if (action == SwipeAction.NONE) return
        viewModelScope.launch {
            mailRepository.executeSwipeAction(message, action)
            KuryrWidgetUpdater.updateAllWidgets(appContext)
            _uiState.update {
                it.copy(snackbarMessage = "${action.displayName}: ${message.subject}")
            }
        }
    }

    fun toggleMessageRead(message: EmailMessageEntity) {
        viewModelScope.launch {
            mailRepository.toggleRead(message)
            KuryrWidgetUpdater.updateAllWidgets(appContext)
        }
    }

    fun toggleMessageStarred(message: EmailMessageEntity) {
        viewModelScope.launch {
            mailRepository.toggleStarred(message)
        }
    }

    fun moveMessageToFolderType(message: EmailMessageEntity, folderType: FolderType) {
        viewModelScope.launch {
            mailRepository.moveMessageToFolderType(message, folderType)
            KuryrWidgetUpdater.updateAllWidgets(appContext)
            _uiState.update {
                it.copy(
                    currentDestination = KuryrDestination.UNIFIED_INBOX,
                    snackbarMessage = "Přesunuto do složky ${folderType.displayName}"
                )
            }
        }
    }

    fun deleteMessage(message: EmailMessageEntity) {
        viewModelScope.launch {
            mailRepository.executeSwipeAction(message, SwipeAction.DELETE)
            KuryrWidgetUpdater.updateAllWidgets(appContext)
            _uiState.update {
                it.copy(
                    currentDestination = KuryrDestination.UNIFIED_INBOX,
                    snackbarMessage = "Zpráva přesunuta do koše."
                )
            }
        }
    }

    // --- Contacts ---
    fun updateContactSearchQuery(query: String) {
        _uiState.update { it.copy(contactSearchQuery = query) }
    }

    fun selectContactGroup(groupId: Long?) {
        _uiState.update { it.copy(selectedContactGroupId = groupId) }
    }

    fun saveContact(contact: ContactEntity) {
        viewModelScope.launch {
            contactRepository.saveContact(contact)
            _uiState.update { it.copy(snackbarMessage = "Kontakt ${contact.displayName} uložen.") }
        }
    }

    fun toggleContactVip(contact: ContactEntity) {
        viewModelScope.launch {
            contactRepository.toggleVip(contact)
        }
    }

    fun deleteContact(contactId: Long) {
        viewModelScope.launch {
            contactRepository.deleteContact(contactId)
            _uiState.update { it.copy(snackbarMessage = "Kontakt byl smazán.") }
        }
    }

    fun saveContactGroup(name: String, description: String, colorHex: Long) {
        if (name.isBlank()) return
        viewModelScope.launch {
            contactRepository.saveContactGroup(
                ContactGroupEntity(
                    name = name.trim(),
                    description = description.trim(),
                    colorHex = colorHex
                )
            )
            _uiState.update { it.copy(snackbarMessage = "Skupina kontaktů „$name“ vytvořena.") }
        }
    }

    fun deleteContactGroup(group: ContactGroupEntity) {
        viewModelScope.launch {
            contactRepository.deleteContactGroup(group)
        }
    }

    fun importSystemContacts(contentResolver: ContentResolver) {
        viewModelScope.launch {
            val count = contactRepository.importFromSystemContacts(contentResolver)
            _uiState.update {
                it.copy(
                    snackbarMessage = if (count > 0) {
                        "Naimportováno $count nových kontaktů ze zařízení."
                    } else {
                        "Žádné nové e-mailové kontakty k importu nebyly nalezeny."
                    }
                )
            }
        }
    }

    // --- Notifications & Digests ---
    fun saveNotificationRule(rule: NotificationRuleEntity) {
        viewModelScope.launch {
            mailRepository.saveNotificationRule(rule)
            _uiState.update { it.copy(snackbarMessage = "Notifikační pravidlo „${rule.name}“ uloženo.") }
        }
    }

    fun toggleNotificationRule(rule: NotificationRuleEntity) {
        viewModelScope.launch {
            mailRepository.toggleNotificationRule(rule)
        }
    }

    fun deleteNotificationRule(ruleId: Long) {
        viewModelScope.launch {
            mailRepository.deleteNotificationRule(ruleId)
        }
    }

    fun setQuietHours(enabled: Boolean, startHour: Int, endHour: Int) {
        viewModelScope.launch {
            preferencesRepository.setQuietHours(enabled, startHour, endHour)
        }
    }

    fun setDigestFrequency(frequency: DigestFrequency) {
        viewModelScope.launch {
            preferencesRepository.setDigestFrequency(frequency)
        }
    }

    fun dispatchDigestSummaryNow() {
        viewModelScope.launch {
            val count = notificationRuleEngine.dispatchPendingDigestNotification(
                context = appContext,
                frequency = preferences.value.digestFrequency
            )
            _uiState.update {
                it.copy(
                    snackbarMessage = if (count > 0) {
                        "Odeslán souhrn pro $count zpráv."
                    } else {
                        "Fronta souhrnu je aktuálně prázdná."
                    }
                )
            }
        }
    }

    // --- Appearance & Settings ---
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setUseMaterialYou(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setUseMaterialYou(enabled)
        }
    }

    fun setAccentColor(colorHex: Long) {
        viewModelScope.launch {
            preferencesRepository.setAccentColor(colorHex)
        }
    }

    fun setListDensity(density: ListDensity) {
        viewModelScope.launch {
            preferencesRepository.setListDensity(density)
        }
    }

    fun setSwipeLeftAction(action: SwipeAction) {
        viewModelScope.launch {
            preferencesRepository.setSwipeLeftAction(action)
        }
    }

    fun setSwipeRightAction(action: SwipeAction) {
        viewModelScope.launch {
            preferencesRepository.setSwipeRightAction(action)
        }
    }

    fun setSyncIntervalMinutes(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.setSyncIntervalMinutes(minutes)
            MailSyncWorker.schedulePeriodicSync(appContext, minutes)
        }
    }

    fun setTelemetryOptIn(optIn: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setTelemetryOptIn(optIn)
        }
    }

    fun updateGlobalSearchQuery(query: String) {
        _uiState.update { it.copy(globalSearchQuery = query) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appCtx = context.applicationContext
                    val db = KuryrDatabase.getInstance(appCtx)
                    val dao = db.kuryrDao()
                    val vault = AndroidKeystoreCredentialVault(appCtx)
                    val mailRepo = MailRepository(
                        dao = dao,
                        passwordVault = vault,
                        imapSmtpPop3Client = ImapSmtpPop3Client(),
                        jmapClient = JmapClient()
                    )
                    val contactRepo = ContactRepository(dao)
                    val prefsRepo = AppPreferencesRepository(appCtx)
                    val ruleEngine = NotificationRuleEngine(dao)
                    return KuryrViewModel(
                        appContext = appCtx,
                        mailRepository = mailRepo,
                        contactRepository = contactRepo,
                        preferencesRepository = prefsRepo,
                        notificationRuleEngine = ruleEngine
                    ) as T
                }
            }
    }
}
