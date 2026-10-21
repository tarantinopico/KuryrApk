package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.KuryrDestination
import com.example.ui.KuryrViewModel
import com.example.ui.screens.AccountsAndGroupsScreen
import com.example.ui.screens.AddOrEditAccountScreen
import com.example.ui.screens.AppSettingsScreen
import com.example.ui.screens.ComposeEmailScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.GlobalSearchScreen
import com.example.ui.screens.MessageDetailScreen
import com.example.ui.screens.NotificationsAndDigestsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.UnifiedInboxScreen
import com.example.ui.theme.KuryrTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val kuryrViewModel: KuryrViewModel = viewModel(
                factory = KuryrViewModel.provideFactory(context)
            )
            KuryrApp(viewModel = kuryrViewModel)
        }
    }
}

private data class TopLevelNavItem(
    val destination: KuryrDestination,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

private val TopLevelDestinations = listOf(
    TopLevelNavItem(KuryrDestination.UNIFIED_INBOX, "Doručené", Icons.Default.Inbox, "nav_inbox"),
    TopLevelNavItem(KuryrDestination.ACCOUNTS, "Účty", Icons.Default.AccountCircle, "nav_accounts"),
    TopLevelNavItem(KuryrDestination.CONTACTS, "Kontakty", Icons.Default.Contacts, "nav_contacts"),
    TopLevelNavItem(KuryrDestination.NOTIFICATIONS, "Notifikace", Icons.Default.Notifications, "nav_notifications"),
    TopLevelNavItem(KuryrDestination.SETTINGS, "Nastavení", Icons.Default.Settings, "nav_settings")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KuryrApp(viewModel: KuryrViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val accountGroups by viewModel.accountGroups.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val allMessages by viewModel.allMessages.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val contactGroups by viewModel.contactGroups.collectAsStateWithLifecycle()
    val notificationRules by viewModel.notificationRules.collectAsStateWithLifecycle()
    val pendingDigestItems by viewModel.pendingDigestItems.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    KuryrTheme(
        themeMode = preferences.themeMode,
        useMaterialYou = preferences.useMaterialYou,
        accentColorHex = preferences.accentColorHex
    ) {
        val currentDest = uiState.currentDestination
        val isFullScreenSubroute = currentDest in setOf(
            KuryrDestination.ONBOARDING,
            KuryrDestination.ADD_ACCOUNT,
            KuryrDestination.MESSAGE_DETAIL,
            KuryrDestination.COMPOSE,
            KuryrDestination.SEARCH
        )

        if (currentDest != KuryrDestination.UNIFIED_INBOX && currentDest != KuryrDestination.ONBOARDING) {
            BackHandler {
                if (accounts.isEmpty()) {
                    viewModel.navigateTo(KuryrDestination.ONBOARDING)
                } else {
                    viewModel.navigateTo(KuryrDestination.UNIFIED_INBOX)
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    if (!isFullScreenSubroute) {
                        TopAppBar(
                            title = {
                                Text(
                                    text = when (currentDest) {
                                        KuryrDestination.UNIFIED_INBOX -> "Kurýr • Pošta"
                                        KuryrDestination.ACCOUNTS -> "Účty a skupiny"
                                        KuryrDestination.CONTACTS -> "Adresář kontaktů"
                                        KuryrDestination.NOTIFICATIONS -> "Notifikace a souhrny"
                                        KuryrDestination.SETTINGS -> "Nastavení"
                                        else -> "Kurýr"
                                    },
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = { viewModel.navigateTo(KuryrDestination.SEARCH) },
                                    modifier = Modifier.testTag("top_search_button")
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = "Hledat")
                                }
                                IconButton(
                                    onClick = { viewModel.synchronizeAccountNow() },
                                    modifier = Modifier.testTag("top_sync_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Synchronizovat nyní")
                                }
                            }
                        )
                    }
                },
                floatingActionButton = {
                    if (currentDest == KuryrDestination.UNIFIED_INBOX) {
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.openCompose() },
                            icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            text = { Text("Nová zpráva") },
                            modifier = Modifier.testTag("fab_compose")
                        )
                    }
                },
                bottomBar = {
                    if (!isFullScreenSubroute && !isExpandedScreen) {
                        NavigationBar {
                            TopLevelDestinations.forEach { item ->
                                NavigationBarItem(
                                    selected = currentDest == item.destination,
                                    onClick = { viewModel.navigateTo(item.destination) },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) },
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (!isFullScreenSubroute && isExpandedScreen) {
                        NavigationRail {
                            TopLevelDestinations.forEach { item ->
                                NavigationRailItem(
                                    selected = currentDest == item.destination,
                                    onClick = { viewModel.navigateTo(item.destination) },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) },
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }

                    when (currentDest) {
                        KuryrDestination.ONBOARDING -> {
                            OnboardingScreen(
                                onStartAccountSetup = {
                                    viewModel.completeOnboardingAndOpenAccountSetup()
                                }
                            )
                        }
                        KuryrDestination.ADD_ACCOUNT -> {
                            AddOrEditAccountScreen(
                                existingAccount = uiState.editingAccount,
                                accountGroups = accountGroups,
                                isTestingConnection = uiState.isTestingConnection,
                                connectionFeedback = uiState.connectionTestFeedback,
                                onBack = {
                                    if (accounts.isEmpty()) {
                                        viewModel.navigateTo(KuryrDestination.ONBOARDING)
                                    } else {
                                        viewModel.navigateTo(KuryrDestination.UNIFIED_INBOX)
                                    }
                                },
                                onTestConnection = { draft, pwd ->
                                    viewModel.testAccountConnection(draft, pwd)
                                },
                                onSaveAccount = { draft, pwd ->
                                    viewModel.saveAccount(draft, pwd)
                                }
                            )
                        }
                        KuryrDestination.UNIFIED_INBOX -> {
                            UnifiedInboxScreen(
                                accounts = accounts,
                                accountGroups = accountGroups,
                                folders = folders,
                                allMessages = allMessages,
                                selectedAccountId = uiState.selectedAccountId,
                                selectedAccountGroupId = uiState.selectedAccountGroupId,
                                selectedFolderType = uiState.selectedFolderType,
                                quickFilter = uiState.quickFilter,
                                listDensity = preferences.listDensity,
                                swipeLeftAction = preferences.swipeLeftAction,
                                swipeRightAction = preferences.swipeRightAction,
                                isSyncing = uiState.isSyncing,
                                onSelectAccount = { viewModel.selectAccountFilter(it) },
                                onSelectAccountGroup = { viewModel.selectAccountGroupFilter(it) },
                                onSelectFolderType = { viewModel.selectFolderType(it) },
                                onSelectQuickFilter = { viewModel.selectQuickFilter(it) },
                                onOpenMessage = { viewModel.openMessageDetail(it) },
                                onToggleStar = { viewModel.toggleMessageStarred(it) },
                                onExecuteSwipe = { msg, action -> viewModel.executeSwipeAction(msg, action) },
                                onSyncNow = { viewModel.synchronizeAccountNow() },
                                onAddAccountClick = { viewModel.startAddAccount() }
                            )
                        }
                        KuryrDestination.MESSAGE_DETAIL -> {
                            val selectedMessage = allMessages.firstOrNull { it.id == uiState.selectedMessageId }
                            MessageDetailScreen(
                                message = selectedMessage,
                                onBack = { viewModel.navigateTo(KuryrDestination.UNIFIED_INBOX) },
                                onReply = { msg ->
                                    val replySubject = if (msg.subject.startsWith("Re:", ignoreCase = true)) {
                                        msg.subject
                                    } else {
                                        "Re: ${msg.subject}"
                                    }
                                    viewModel.openCompose(
                                        to = msg.replyTo.ifBlank { msg.fromEmail },
                                        subject = replySubject,
                                        body = "\n\n--- Původní zpráva od ${msg.fromName} ---\n${msg.bodyPlain}",
                                        replyToMessageId = msg.messageIdHeader
                                    )
                                },
                                onToggleRead = { viewModel.toggleMessageRead(it) },
                                onToggleStar = { viewModel.toggleMessageStarred(it) },
                                onArchive = { viewModel.moveMessageToFolderType(it, com.example.data.model.FolderType.ARCHIVE) },
                                onDelete = { viewModel.deleteMessage(it) }
                            )
                        }
                        KuryrDestination.COMPOSE -> {
                            ComposeEmailScreen(
                                accounts = accounts,
                                contacts = contacts,
                                contactGroups = contactGroups,
                                draftState = uiState.composeDraft,
                                isSending = uiState.isSendingEmail,
                                onBack = { viewModel.navigateTo(KuryrDestination.UNIFIED_INBOX) },
                                onSendEmail = { acctId, to, cc, bcc, subj, body, attach ->
                                    viewModel.sendEmail(acctId, to, cc, bcc, subj, body, attach)
                                },
                                onSaveDraft = { acctId, to, cc, subj, body ->
                                    viewModel.saveDraft(acctId, to, cc, subj, body)
                                }
                            )
                        }
                        KuryrDestination.ACCOUNTS -> {
                            AccountsAndGroupsScreen(
                                accounts = accounts,
                                accountGroups = accountGroups,
                                folders = folders,
                                onAddAccount = { viewModel.startAddAccount() },
                                onEditAccount = { viewModel.startEditAccount(it) },
                                onDeleteAccount = { viewModel.deleteAccount(it) },
                                onSyncAccount = { viewModel.synchronizeAccountNow(it) },
                                onCreateAccountGroup = { name, color -> viewModel.saveAccountGroup(name, color) },
                                onDeleteAccountGroup = { viewModel.deleteAccountGroup(it) }
                            )
                        }
                        KuryrDestination.CONTACTS -> {
                            ContactsScreen(
                                contacts = contacts,
                                contactGroups = contactGroups,
                                searchQuery = uiState.contactSearchQuery,
                                selectedGroupId = uiState.selectedContactGroupId,
                                onSearchQueryChange = { viewModel.updateContactSearchQuery(it) },
                                onSelectGroup = { viewModel.selectContactGroup(it) },
                                onSaveContact = { viewModel.saveContact(it) },
                                onToggleVip = { viewModel.toggleContactVip(it) },
                                onDeleteContact = { viewModel.deleteContact(it) },
                                onSaveGroup = { name, desc, col -> viewModel.saveContactGroup(name, desc, col) },
                                onDeleteGroup = { viewModel.deleteContactGroup(it) },
                                onImportSystemContacts = { viewModel.importSystemContacts(context.contentResolver) },
                                onComposeToContact = { email -> viewModel.openCompose(to = email) }
                            )
                        }
                        KuryrDestination.NOTIFICATIONS -> {
                            NotificationsAndDigestsScreen(
                                preferences = preferences,
                                rules = notificationRules,
                                pendingDigestItems = pendingDigestItems,
                                onUpdateQuietHours = { en, s, e -> viewModel.setQuietHours(en, s, e) },
                                onUpdateDigestFrequency = { viewModel.setDigestFrequency(it) },
                                onDispatchDigestNow = { viewModel.dispatchDigestSummaryNow() },
                                onSaveRule = { viewModel.saveNotificationRule(it) },
                                onToggleRule = { viewModel.toggleNotificationRule(it) },
                                onDeleteRule = { viewModel.deleteNotificationRule(it) }
                            )
                        }
                        KuryrDestination.SETTINGS -> {
                            AppSettingsScreen(
                                preferences = preferences,
                                onThemeModeChange = { viewModel.setThemeMode(it) },
                                onMaterialYouChange = { viewModel.setUseMaterialYou(it) },
                                onAccentColorChange = { viewModel.setAccentColor(it) },
                                onListDensityChange = { viewModel.setListDensity(it) },
                                onSwipeLeftChange = { viewModel.setSwipeLeftAction(it) },
                                onSwipeRightChange = { viewModel.setSwipeRightAction(it) },
                                onSyncIntervalChange = { viewModel.setSyncIntervalMinutes(it) },
                                onTelemetryOptInChange = { viewModel.setTelemetryOptIn(it) }
                            )
                        }
                        KuryrDestination.SEARCH -> {
                            GlobalSearchScreen(
                                query = uiState.globalSearchQuery,
                                messages = allMessages,
                                contacts = contacts,
                                onQueryChange = { viewModel.updateGlobalSearchQuery(it) },
                                onBack = { viewModel.navigateTo(KuryrDestination.UNIFIED_INBOX) },
                                onOpenMessage = { viewModel.openMessageDetail(it) },
                                onComposeToContact = { email -> viewModel.openCompose(to = email) }
                            )
                        }
                    }
                }
            }
        }
    }
}
