package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AccountEntity
import com.example.data.local.AccountGroupEntity
import com.example.data.local.ContactEntity
import com.example.data.local.ContactGroupEntity
import com.example.data.local.EmailMessageEntity
import com.example.data.local.FolderEntity
import com.example.data.model.FolderType
import com.example.data.model.ListDensity
import com.example.data.model.OutboxStatus
import com.example.data.model.SwipeAction
import com.example.ui.ComposeDraftState
import com.example.ui.InboxQuickFilter
import com.example.ui.theme.JetBrainsMonoFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedInboxScreen(
    accounts: List<AccountEntity>,
    accountGroups: List<AccountGroupEntity>,
    folders: List<FolderEntity>,
    allMessages: List<EmailMessageEntity>,
    selectedAccountId: Long?,
    selectedAccountGroupId: Long?,
    selectedFolderType: FolderType,
    quickFilter: InboxQuickFilter,
    listDensity: ListDensity,
    swipeLeftAction: SwipeAction,
    swipeRightAction: SwipeAction,
    isSyncing: Boolean,
    onSelectAccount: (Long?) -> Unit,
    onSelectAccountGroup: (Long?) -> Unit,
    onSelectFolderType: (FolderType) -> Unit,
    onSelectQuickFilter: (InboxQuickFilter) -> Unit,
    onOpenMessage: (EmailMessageEntity) -> Unit,
    onToggleStar: (EmailMessageEntity) -> Unit,
    onExecuteSwipe: (EmailMessageEntity, SwipeAction) -> Unit,
    onSyncNow: () -> Unit,
    onAddAccountClick: () -> Unit
) {
    val allowedAccountIds = remember(accounts, selectedAccountId, selectedAccountGroupId) {
        when {
            selectedAccountId != null -> setOf(selectedAccountId)
            selectedAccountGroupId != null -> accounts.filter { it.groupId == selectedAccountGroupId }.map { it.id }.toSet()
            else -> accounts.map { it.id }.toSet()
        }
    }

    val matchingFolderIds = remember(folders, allowedAccountIds, selectedFolderType) {
        folders
            .filter { it.accountId in allowedAccountIds && it.type == selectedFolderType }
            .map { it.id }
            .toSet()
    }

    val filteredMessages = remember(allMessages, matchingFolderIds, quickFilter) {
        allMessages
            .filter { it.folderId in matchingFolderIds }
            .filter { msg ->
                when (quickFilter) {
                    InboxQuickFilter.ALL -> true
                    InboxQuickFilter.UNREAD -> !msg.isRead
                    InboxQuickFilter.STARRED -> msg.isStarred
                    InboxQuickFilter.VIP -> msg.isVipSender
                    InboxQuickFilter.ATTACHMENTS -> msg.hasAttachments
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("unified_inbox_screen")
    ) {
        // 1. Pás účtů a skupin účtů
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedAccountId == null && selectedAccountGroupId == null,
                    onClick = { onSelectAccount(null) },
                    label = { Text("Sjednocená pošta (${accounts.size})") },
                    modifier = Modifier.testTag("chip_unified_all")
                )
            }
            items(accountGroups, key = { "grp_${it.id}" }) { group ->
                FilterChip(
                    selected = selectedAccountGroupId == group.id,
                    onClick = { onSelectAccountGroup(group.id) },
                    label = { Text("Skupina: ${group.name}") }
                )
            }
            items(accounts, key = { "acct_${it.id}" }) { account ->
                FilterChip(
                    selected = selectedAccountId == account.id,
                    onClick = { onSelectAccount(account.id) },
                    label = { Text(account.accountName) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(account.colorHex))
                        )
                    }
                )
            }
        }

        // 2. Pás složek (Doručené, Odeslané, Koncepty, Archiv, Koš, Nevyžádané)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                listOf(
                    FolderType.INBOX,
                    FolderType.SENT,
                    FolderType.DRAFTS,
                    FolderType.ARCHIVE,
                    FolderType.TRASH,
                    FolderType.SPAM
                ),
                key = { it.name }
            ) { folderType ->
                FilterChip(
                    selected = selectedFolderType == folderType,
                    onClick = { onSelectFolderType(folderType) },
                    label = { Text(folderType.displayName) },
                    modifier = Modifier.testTag("folder_chip_${folderType.name}")
                )
            }
        }

        // 3. Rychlé filtry (Nepřečtené, S hvězdičkou, VIP, S přílohou)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(InboxQuickFilter.entries, key = { it.name }) { filter ->
                FilterChip(
                    selected = quickFilter == filter,
                    onClick = { onSelectQuickFilter(filter) },
                    label = { Text(filter.label) },
                    modifier = Modifier.testTag("quick_filter_${filter.name}")
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

        if (accounts.isEmpty()) {
            EmptyInboxState(
                title = "Žádný účet není nakonfigurován",
                subtitle = "Databáze je čistá. Přidejte svůj první účet IMAP, POP3 nebo JMAP a připojte se přímo k poštovnímu serveru.",
                buttonText = "Přidat e-mailový účet",
                onActionClick = onAddAccountClick
            )
        } else if (filteredMessages.isEmpty()) {
            EmptyInboxState(
                title = "Složka „${selectedFolderType.displayName}“ je prázdná",
                subtitle = if (isSyncing) {
                    "Probíhá přímá synchronizace se vzdáleným poštovním serverem…"
                } else {
                    "Spusťte přímou synchronizaci pro stažení nových zpráv ze serveru nebo napište novou zprávu."
                },
                buttonText = if (isSyncing) "Synchronizuji…" else "Synchronizovat se serverem",
                onActionClick = onSyncNow
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("message_list"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredMessages, key = { it.id }) { message ->
                    val accountColor = accounts.firstOrNull { it.id == message.accountId }?.colorHex ?: 0xFF1E40AF
                    val accountLabel = accounts.firstOrNull { it.id == message.accountId }?.accountName ?: "Účet"
                    SwipeableEmailRow(
                        message = message,
                        accountLabel = accountLabel,
                        accountColorHex = accountColor,
                        density = listDensity,
                        swipeLeftAction = swipeLeftAction,
                        swipeRightAction = swipeRightAction,
                        onClick = { onOpenMessage(message) },
                        onToggleStar = { onToggleStar(message) },
                        onSwipeAction = { action -> onExecuteSwipe(message, action) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyInboxState(
    title: String,
    subtitle: String,
    buttonText: String,
    onActionClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .width(240.dp)
                .height(135.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            )
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_onboarding_hero),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onActionClick,
            modifier = Modifier.testTag("empty_inbox_action_button")
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(buttonText)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableEmailRow(
    message: EmailMessageEntity,
    accountLabel: String,
    accountColorHex: Long,
    density: ListDensity,
    swipeLeftAction: SwipeAction,
    swipeRightAction: SwipeAction,
    onClick: () -> Unit,
    onToggleStar: () -> Unit,
    onSwipeAction: (SwipeAction) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (swipeRightAction != SwipeAction.NONE) {
                        onSwipeAction(swipeRightAction)
                    }
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (swipeLeftAction != SwipeAction.NONE) {
                        onSwipeAction(swipeLeftAction)
                    }
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    val timeLabel = remember(message.receivedAt) {
        SimpleDateFormat("d. M. HH:mm", Locale.getDefault()).format(Date(message.receivedAt))
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val action = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> swipeRightAction
                SwipeToDismissBoxValue.EndToStart -> swipeLeftAction
                else -> SwipeAction.NONE
            }
            val align = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                Alignment.CenterStart
            } else {
                Alignment.CenterEnd
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = align
            ) {
                Text(
                    text = action.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .testTag("message_item_${message.id}"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (!message.isRead) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                }
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (!message.isRead) 2.dp else 0.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = density.verticalPaddingDp.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(accountColorHex))
                        )
                        Text(
                            text = message.fromName.ifBlank { message.fromEmail },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (!message.isRead) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (message.isVipSender) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "VIP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (message.outboxStatus == OutboxStatus.QUEUED_PENDING_NETWORK) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Čeká na síť",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (message.hasAttachments) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = "Příloha",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                            onClick = onToggleStar,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (message.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Hvězdička",
                                tint = if (message.isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Text(
                    text = message.subject,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (!message.isRead) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = message.snippet.ifBlank { message.bodyPlain },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = density.maxLinesPreview,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = accountLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = { onSwipeAction(swipeRightAction) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(swipeRightAction.displayName, style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(
                            onClick = { onSwipeAction(swipeLeftAction) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(swipeLeftAction.displayName, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailScreen(
    message: EmailMessageEntity?,
    onBack: () -> Unit,
    onReply: (EmailMessageEntity) -> Unit,
    onToggleRead: (EmailMessageEntity) -> Unit,
    onToggleStar: (EmailMessageEntity) -> Unit,
    onArchive: (EmailMessageEntity) -> Unit,
    onDelete: (EmailMessageEntity) -> Unit
) {
    var showRawHeaders by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail zprávy") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = {
                    if (message != null) {
                        IconButton(onClick = { onToggleRead(message) }) {
                            Icon(
                                if (message.isRead) Icons.Default.MarkEmailUnread else Icons.Default.MarkEmailRead,
                                contentDescription = "Přepnout přečtení"
                            )
                        }
                        IconButton(onClick = { onArchive(message) }) {
                            Icon(Icons.Default.Archive, contentDescription = "Archivovat")
                        }
                        IconButton(onClick = { onDelete(message) }) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Smazat",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (message == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Zpráva nebyla nalezena.")
            }
        } else {
            val formattedDate = remember(message.receivedAt) {
                SimpleDateFormat("d. MMMM yyyy HH:mm", Locale.getDefault()).format(Date(message.receivedAt))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .testTag("message_detail_screen"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = message.subject,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onToggleStar(message) }) {
                        Icon(
                            if (message.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Hvězdička",
                            tint = if (message.isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Od: ${message.fromName} <${message.fromEmail}>",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Komu: ${message.toRecipients}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (message.ccRecipients.isNotBlank()) {
                            Text(
                                text = "Kopie (Cc): ${message.ccRecipients}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistChip(
                                onClick = {},
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("SPF: ${message.spfStatus} • DKIM: ${message.dkimStatus}") }
                            )
                            TextButton(
                                onClick = { showRawHeaders = !showRawHeaders },
                                modifier = Modifier.testTag("toggle_raw_headers_button")
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (showRawHeaders) "Skrýt hlavičky" else "Zobrazit RFC hlavičky")
                            }
                        }

                        AnimatedVisibility(visible = showRawHeaders) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = message.rawHeaders.ifBlank {
                                        "Message-ID: ${message.messageIdHeader}\nThread-ID: ${message.threadId}\nFrom: ${message.fromEmail}\nTo: ${message.toRecipients}"
                                    },
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                if (message.hasAttachments || message.attachmentNames.isNotBlank()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null)
                            Text(
                                text = "Přílohy: ${message.attachmentNames.ifBlank { "přiložený dokument" }}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = message.bodyPlain,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Button(
                    onClick = { onReply(message) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reply_message_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Odpovědět odesílateli")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ComposeEmailScreen(
    accounts: List<AccountEntity>,
    contacts: List<ContactEntity>,
    contactGroups: List<ContactGroupEntity>,
    draftState: ComposeDraftState,
    isSending: Boolean,
    onBack: () -> Unit,
    onSendEmail: (Long, String, String, String, String, String, List<String>) -> Unit,
    onSaveDraft: (Long, String, String, String, String) -> Unit
) {
    var selectedAccountId by remember(accounts) {
        mutableStateOf(accounts.firstOrNull()?.id ?: 0L)
    }
    var accountMenuExpanded by remember { mutableStateOf(false) }

    var toText by remember(draftState) { mutableStateOf(draftState.initialTo) }
    var ccText by remember { mutableStateOf("") }
    var bccText by remember { mutableStateOf("") }
    var showCcBcc by remember { mutableStateOf(false) }
    var subjectText by remember(draftState) { mutableStateOf(draftState.initialSubject) }
    var bodyText by remember(draftState) { mutableStateOf(draftState.initialBody) }
    val attachments = remember { mutableStateListOf<String>() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            attachments.add(uri.lastPathSegment ?: "priloha_${attachments.size + 1}.bin")
        }
    }

    val matchingContacts = remember(toText, contacts) {
        val lastToken = toText.substringAfterLast(',').trim().lowercase()
        if (lastToken.length < 2) {
            contacts.take(4)
        } else {
            contacts.filter {
                it.displayName.lowercase().contains(lastToken) ||
                    it.email.lowercase().contains(lastToken)
            }.take(5)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nová zpráva") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("compose_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.testTag("compose_attach_button")
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Přiložit soubor")
                    }
                    IconButton(
                        onClick = {
                            if (selectedAccountId != 0L) {
                                onSaveDraft(selectedAccountId, toText, ccText, subjectText, bodyText)
                            }
                        },
                        modifier = Modifier.testTag("save_draft_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Uložit koncept")
                    }
                    Button(
                        onClick = {
                            if (selectedAccountId != 0L) {
                                onSendEmail(
                                    selectedAccountId,
                                    toText,
                                    ccText,
                                    bccText,
                                    subjectText,
                                    bodyText,
                                    attachments.toList()
                                )
                            }
                        },
                        enabled = !isSending && accounts.isNotEmpty(),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("send_email_button")
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Odeslat")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("compose_email_screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (accounts.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Pro odeslání e-mailu nejprve přidejte alespoň jeden účet v průvodci.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                val activeAccount = accounts.firstOrNull { it.id == selectedAccountId } ?: accounts.first()
                Box {
                    OutlinedButton(
                        onClick = { accountMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Odesílatel: ${activeAccount.displayName} <${activeAccount.emailAddress}> (${activeAccount.protocol.name})")
                    }
                    DropdownMenu(
                        expanded = accountMenuExpanded,
                        onDismissRequest = { accountMenuExpanded = false }
                    ) {
                        accounts.forEach { acct ->
                            DropdownMenuItem(
                                text = { Text("${acct.accountName} (${acct.emailAddress})") },
                                onClick = {
                                    selectedAccountId = acct.id
                                    accountMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = toText,
                    onValueChange = { toText = it },
                    label = { Text("Komu (e-maily oddělené čárkou) *") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("compose_to_input")
                )
                TextButton(onClick = { showCcBcc = !showCcBcc }) {
                    Text(if (showCcBcc) "Skrýt Kopii" else "Kopie / BCC")
                }
            }

            // Rychlé vložení kontaktů nebo celé skupiny kontaktů
            if (contacts.isNotEmpty() || contactGroups.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    contactGroups.forEach { group ->
                        val groupEmails = contacts.filter { it.groupId == group.id }.map { it.email }
                        if (groupEmails.isNotEmpty()) {
                            AssistChip(
                                onClick = {
                                    val joined = groupEmails.joinToString(", ")
                                    toText = if (toText.isBlank()) joined else "$toText, $joined"
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("${group.name} (${groupEmails.size})") }
                            )
                        }
                    }
                    matchingContacts.forEach { contact ->
                        AssistChip(
                            onClick = {
                                toText = if (toText.isBlank()) contact.email else "$toText, ${contact.email}"
                            },
                            leadingIcon = {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(contact.displayName) }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showCcBcc) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = ccText,
                        onValueChange = { ccText = it },
                        label = { Text("Kopie (Cc)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("compose_cc_input")
                    )
                    OutlinedTextField(
                        value = bccText,
                        onValueChange = { bccText = it },
                        label = { Text("Skrytá kopie (Bcc)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("compose_bcc_input")
                    )
                }
            }

            OutlinedTextField(
                value = subjectText,
                onValueChange = { subjectText = it },
                label = { Text("Předmět zprávy") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("compose_subject_input")
            )

            if (attachments.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    attachments.forEach { fileName ->
                        InputChip(
                            selected = true,
                            onClick = { attachments.remove(fileName) },
                            label = { Text(fileName) },
                            leadingIcon = { Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = bodyText,
                onValueChange = { bodyText = it },
                label = { Text("Text e-mailu") },
                minLines = 10,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("compose_body_input")
            )
        }
    }
}
