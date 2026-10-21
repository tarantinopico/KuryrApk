package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ContactEntity
import com.example.data.local.ContactGroupEntity
import com.example.data.local.DigestQueueEntity
import com.example.data.local.EmailMessageEntity
import com.example.data.local.NotificationRuleEntity
import com.example.data.model.DigestFrequency
import com.example.data.model.ListDensity
import com.example.data.model.SwipeAction
import com.example.data.model.ThemeMode
import com.example.data.preferences.UserPreferences
import com.example.ui.theme.KuryrAccentPalette

@Composable
fun ContactsScreen(
    contacts: List<ContactEntity>,
    contactGroups: List<ContactGroupEntity>,
    searchQuery: String,
    selectedGroupId: Long?,
    onSearchQueryChange: (String) -> Unit,
    onSelectGroup: (Long?) -> Unit,
    onSaveContact: (ContactEntity) -> Unit,
    onToggleVip: (ContactEntity) -> Unit,
    onDeleteContact: (Long) -> Unit,
    onSaveGroup: (String, String, Long) -> Unit,
    onDeleteGroup: (ContactGroupEntity) -> Unit,
    onImportSystemContacts: () -> Unit,
    onComposeToContact: (String) -> Unit
) {
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showAddGroupDialog by remember { mutableStateOf(false) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onImportSystemContacts()
        }
    }

    val filteredContacts = remember(contacts, searchQuery, selectedGroupId) {
        contacts
            .filter { c -> selectedGroupId == null || c.groupId == selectedGroupId }
            .filter { c ->
                if (searchQuery.isBlank()) true
                else {
                    val q = searchQuery.lowercase()
                    c.displayName.lowercase().contains(q) ||
                        c.email.lowercase().contains(q) ||
                        c.organization.lowercase().contains(q) ||
                        c.notes.lowercase().contains(q)
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("contacts_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Fulltextové vyhledávání kontaktů") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("contacts_search_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showAddContactDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .testTag("add_contact_button")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nový kontakt")
            }

            OutlinedButton(
                onClick = { showAddGroupDialog = true },
                modifier = Modifier.testTag("add_contact_group_button")
            ) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Skupina")
            }

            OutlinedButton(
                onClick = {
                    contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                },
                modifier = Modifier.testTag("import_contacts_button")
            ) {
                Icon(Icons.Default.ContactPhone, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import")
            }
        }

        // Složky / skupiny kontaktů
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedGroupId == null,
                    onClick = { onSelectGroup(null) },
                    label = { Text("Všechny kontakty (${contacts.size})") }
                )
            }
            items(contactGroups, key = { it.id }) { grp ->
                FilterChip(
                    selected = selectedGroupId == grp.id,
                    onClick = { onSelectGroup(grp.id) },
                    label = { Text(grp.name) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Smazat skupinu",
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onDeleteGroup(grp) }
                        )
                    }
                )
            }
        }

        if (filteredContacts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Žádné kontakty k zobrazení", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Přidejte kontakt ručně nebo klepněte na „Import“ pro načtení kontaktů z Android ContactsProvideru.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredContacts, key = { it.id }) { contact ->
                    val groupName = contactGroups.firstOrNull { it.id == contact.groupId }?.name
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_card_${contact.id}"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(contact.avatarColorHex),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = contact.displayName.take(1).uppercase(),
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = contact.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (contact.isVip) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "VIP",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = contact.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (groupName != null || contact.organization.isNotBlank()) {
                                        Text(
                                            text = listOfNotNull(
                                                groupName?.let { "Skupina: $it" },
                                                contact.organization.takeIf { it.isNotBlank() }
                                            ).joinToString(" • "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onToggleVip(contact) }) {
                                    Icon(
                                        if (contact.isVip) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "VIP kontakt",
                                        tint = if (contact.isVip) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onComposeToContact(contact.email) }) {
                                    Icon(
                                        Icons.Default.Email,
                                        contentDescription = "Napsat e-mail",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { onDeleteContact(contact.id) }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Smazat kontakt",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddContactDialog) {
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var org by remember { mutableStateOf("") }
        var groupId by remember { mutableStateOf<Long?>(selectedGroupId) }
        var isVip by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = { Text("Přidat kontakt") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Jméno a příjmení *") },
                        singleLine = true,
                        modifier = Modifier.testTag("contact_name_input")
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mailová adresa *") },
                        singleLine = true,
                        modifier = Modifier.testTag("contact_email_input")
                    )
                    OutlinedTextField(
                        value = org,
                        onValueChange = { org = it },
                        label = { Text("Organizace / Poznámka") },
                        singleLine = true
                    )
                    if (contactGroups.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(contactGroups, key = { it.id }) { grp ->
                                FilterChip(
                                    selected = groupId == grp.id,
                                    onClick = { groupId = if (groupId == grp.id) null else grp.id },
                                    label = { Text(grp.name) }
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Označit jako VIP odesílatele")
                        Switch(checked = isVip, onCheckedChange = { isVip = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && email.contains("@")) {
                            onSaveContact(
                                ContactEntity(
                                    displayName = name.trim(),
                                    email = email.trim(),
                                    organization = org.trim(),
                                    groupId = groupId,
                                    isVip = isVip
                                )
                            )
                            showAddContactDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_save_contact_button")
                ) {
                    Text("Uložit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) {
                    Text("Zrušit")
                }
            }
        )
    }

    if (showAddGroupDialog) {
        var groupName by remember { mutableStateOf("") }
        var groupDesc by remember { mutableStateOf("") }
        var colorHex by remember { mutableLongStateOf(KuryrAccentPalette.first().colorHex) }

        AlertDialog(
            onDismissRequest = { showAddGroupDialog = false },
            title = { Text("Nová skupina kontaktů") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Název skupiny (např. Rodina, Klienti)") },
                        singleLine = true,
                        modifier = Modifier.testTag("contact_group_name_input")
                    )
                    OutlinedTextField(
                        value = groupDesc,
                        onValueChange = { groupDesc = it },
                        label = { Text("Popis skupiny") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupName.isNotBlank()) {
                            onSaveGroup(groupName, groupDesc, colorHex)
                            showAddGroupDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_contact_group_button")
                ) {
                    Text("Vytvořit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGroupDialog = false }) {
                    Text("Zrušit")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationsAndDigestsScreen(
    preferences: UserPreferences,
    rules: List<NotificationRuleEntity>,
    pendingDigestItems: List<DigestQueueEntity>,
    onUpdateQuietHours: (Boolean, Int, Int) -> Unit,
    onUpdateDigestFrequency: (DigestFrequency) -> Unit,
    onDispatchDigestNow: () -> Unit,
    onSaveRule: (NotificationRuleEntity) -> Unit,
    onToggleRule: (NotificationRuleEntity) -> Unit,
    onDeleteRule: (Long) -> Unit
) {
    var showAddRuleDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onDispatchDigestNow()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("notifications_screen"),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Notifikace, tiché hodiny a souhrny",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Nastavte si klidný režim bez vyrušování, denní nebo týdenní souhrny a prioritní VIP filtry.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Tiché hodiny
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tiché hodiny (Do Not Disturb)", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Od ${preferences.quietHoursStartHour}:00 do ${preferences.quietHoursEndHour}:00 (mimo VIP pravidla)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.quietHoursEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateQuietHours(
                                    enabled,
                                    preferences.quietHoursStartHour,
                                    preferences.quietHoursEndHour
                                )
                            },
                            modifier = Modifier.testTag("quiet_hours_switch")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(21 to 7, 22 to 7, 23 to 8).forEach { (start, end) ->
                            FilterChip(
                                selected = preferences.quietHoursStartHour == start && preferences.quietHoursEndHour == end,
                                onClick = { onUpdateQuietHours(preferences.quietHoursEnabled, start, end) },
                                label = { Text("$start:00 – $end:00") }
                            )
                        }
                    }
                }
            }
        }

        // Denní a týdenní souhrny (Digests)
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Denní a týdenní souhrny (Digests)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Zprávy přijaté během tichých hodin se zařadí do přehledného souhrnu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DigestFrequency.entries.forEach { freq ->
                            FilterChip(
                                selected = preferences.digestFrequency == freq,
                                onClick = { onUpdateDigestFrequency(freq) },
                                label = { Text(freq.displayName) },
                                modifier = Modifier.testTag("digest_chip_${freq.name}")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ve frontě souhrnu: ${pendingDigestItems.size} zpráv",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= 33) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onDispatchDigestNow()
                                }
                            },
                            modifier = Modifier.testTag("send_digest_now_button")
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Odeslat souhrn nyní")
                        }
                    }
                }
            }
        }

        // Vlastní notifikační pravidla
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vlastní pravidla upozornění", style = MaterialTheme.typography.titleLarge)
                Button(
                    onClick = { showAddRuleDialog = true },
                    modifier = Modifier.testTag("add_notification_rule_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nové pravidlo")
                }
            }
        }

        if (rules.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Text(
                        text = "Zatím nemáte žádná vlastní pravidla. Vytvořte pravidlo podle VIP stavu, odesílatele, klíčového slova v předmětu nebo přílohy.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(rules, key = { it.id }) { rule ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_card_${rule.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(rule.name, style = MaterialTheme.typography.titleMedium)
                            val conditions = buildList {
                                if (rule.vipOnly) add("Pouze VIP")
                                if (rule.senderContains.isNotBlank()) add("Odesílatel: „${rule.senderContains}“")
                                if (rule.subjectContains.isNotBlank()) add("Předmět: „${rule.subjectContains}“")
                                if (rule.requiresAttachment) add("S přílohou")
                                if (rule.bypassQuietHours) add("Obchází tiché hodiny")
                            }
                            Text(
                                text = conditions.joinToString(" • ").ifBlank { "Všechny příchozí zprávy" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = rule.isEnabled,
                            onCheckedChange = { onToggleRule(rule) }
                        )
                        IconButton(onClick = { onDeleteRule(rule.id) }) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Smazat pravidlo",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddRuleDialog) {
        var ruleName by remember { mutableStateOf("") }
        var senderContains by remember { mutableStateOf("") }
        var subjectContains by remember { mutableStateOf("") }
        var vipOnly by remember { mutableStateOf(false) }
        var requiresAttachment by remember { mutableStateOf(false) }
        var bypassQuietHours by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAddRuleDialog = false },
            title = { Text("Nové notifikační pravidlo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = ruleName,
                        onValueChange = { ruleName = it },
                        label = { Text("Název pravidla *") },
                        singleLine = true,
                        modifier = Modifier.testTag("rule_name_input")
                    )
                    OutlinedTextField(
                        value = senderContains,
                        onValueChange = { senderContains = it },
                        label = { Text("Odesílatel obsahuje") },
                        singleLine = true,
                        modifier = Modifier.testTag("rule_sender_input")
                    )
                    OutlinedTextField(
                        value = subjectContains,
                        onValueChange = { subjectContains = it },
                        label = { Text("Předmět obsahuje") },
                        singleLine = true,
                        modifier = Modifier.testTag("rule_subject_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pouze VIP odesílatelé")
                        Switch(checked = vipOnly, onCheckedChange = { vipOnly = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vyžadovat přílohu")
                        Switch(checked = requiresAttachment, onCheckedChange = { requiresAttachment = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Upozornit i v tichých hodinách")
                        Switch(checked = bypassQuietHours, onCheckedChange = { bypassQuietHours = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (ruleName.isNotBlank()) {
                            onSaveRule(
                                NotificationRuleEntity(
                                    name = ruleName.trim(),
                                    vipOnly = vipOnly,
                                    senderContains = senderContains.trim(),
                                    subjectContains = subjectContains.trim(),
                                    requiresAttachment = requiresAttachment,
                                    bypassQuietHours = bypassQuietHours,
                                    highPriority = true
                                )
                            )
                            showAddRuleDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_save_rule_button")
                ) {
                    Text("Uložit pravidlo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRuleDialog = false }) {
                    Text("Zrušit")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppSettingsScreen(
    preferences: UserPreferences,
    onThemeModeChange: (ThemeMode) -> Unit,
    onMaterialYouChange: (Boolean) -> Unit,
    onAccentColorChange: (Long) -> Unit,
    onListDensityChange: (ListDensity) -> Unit,
    onSwipeLeftChange: (SwipeAction) -> Unit,
    onSwipeRightChange: (SwipeAction) -> Unit,
    onSyncIntervalChange: (Int) -> Unit,
    onTelemetryOptInChange: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Nastavení aplikace Kurýr", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Vzhled Material 3, hustota seznamu, swipe akce, synchronizace na pozadí a ochrana soukromí.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Barevný režim (Světlý / Tmavý / AMOLED / Systém)
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Barevný motiv", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = preferences.themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                label = { Text(mode.displayName) },
                                modifier = Modifier.testTag("theme_mode_${mode.name}")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dynamické barvy Material You", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Přizpůsobí paletu tapetě systému Android 12+",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.useMaterialYou,
                            onCheckedChange = onMaterialYouChange,
                            modifier = Modifier.testTag("material_you_switch")
                        )
                    }

                    Text("Vlastní akcentní barva:", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        KuryrAccentPalette.forEach { option ->
                            val isSelected = preferences.accentColorHex == option.colorHex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(option.colorHex))
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                    .clickable { onAccentColorChange(option.colorHex) }
                                    .testTag("accent_color_${option.colorHex}"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = option.name,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Hustota seznamu zpráv (3 úrovně)
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Hustota seznamu zpráv (3 úrovně)", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ListDensity.entries.forEach { density ->
                            FilterChip(
                                selected = preferences.listDensity == density,
                                onClick = { onListDensityChange(density) },
                                label = { Text(density.displayName) },
                                modifier = Modifier.testTag("density_${density.name}")
                            )
                        }
                    }
                }
            }
        }

        // 3. Konfigurovatelné swipe akce
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Gesta potažení (Swipe akce)", style = MaterialTheme.typography.titleMedium)

                    Text("Potažení doleva:", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SwipeAction.entries.forEach { action ->
                            FilterChip(
                                selected = preferences.swipeLeftAction == action,
                                onClick = { onSwipeLeftChange(action) },
                                label = { Text(action.displayName) }
                            )
                        }
                    }

                    Text("Potažení doprava:", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SwipeAction.entries.forEach { action ->
                            FilterChip(
                                selected = preferences.swipeRightAction == action,
                                onClick = { onSwipeRightChange(action) },
                                label = { Text(action.displayName) }
                            )
                        }
                    }
                }
            }
        }

        // 4. Synchronizace a soukromí
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Synchronizace na pozadí a bezpečnost", style = MaterialTheme.typography.titleMedium)
                    Text("Globální interval WorkManager synchronizace:", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 30, 60, 120).forEach { mins ->
                            FilterChip(
                                selected = preferences.globalSyncIntervalMinutes == mins,
                                onClick = { onSyncIntervalChange(mins) },
                                label = { Text("$mins min") }
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Anonymní diagnostická telemetrie (Opt-in)", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Ve výchozím stavu striktně vypnuto. Žádná data neopouštějí zařízení.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.telemetryOptIn,
                            onCheckedChange = onTelemetryOptInChange,
                            modifier = Modifier.testTag("telemetry_opt_in_switch")
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Úložiště hesel: Android Keystore + EncryptedSharedPreferences (AES256-GCM)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    query: String,
    messages: List<EmailMessageEntity>,
    contacts: List<ContactEntity>,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpenMessage: (EmailMessageEntity) -> Unit,
    onComposeToContact: (String) -> Unit
) {
    val matchingMessages = remember(query, messages) {
        if (query.isBlank()) emptyList()
        else {
            val q = query.lowercase()
            messages.filter {
                it.subject.lowercase().contains(q) ||
                    it.fromName.lowercase().contains(q) ||
                    it.fromEmail.lowercase().contains(q) ||
                    it.bodyPlain.lowercase().contains(q)
            }
        }
    }

    val matchingContacts = remember(query, contacts) {
        if (query.isBlank()) emptyList()
        else {
            val q = query.lowercase()
            contacts.filter {
                it.displayName.lowercase().contains(q) ||
                    it.email.lowercase().contains(q) ||
                    it.organization.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = { Text("Hledat v e-mailech a kontaktech…") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("global_search_input")
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (query.isBlank()) {
                item {
                    Text(
                        text = "Zadejte hledaný výraz pro okamžité fulltextové prohledání zpráv i kontaktů.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                item {
                    Text(
                        text = "Nalezené zprávy (${matchingMessages.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(matchingMessages, key = { "msg_${it.id}" }) { msg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenMessage(msg) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(msg.subject, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${msg.fromName} <${msg.fromEmail}>",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nalezené kontakty (${matchingContacts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(matchingContacts, key = { "cnt_${it.id}" }) { contact ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onComposeToContact(contact.email) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(contact.displayName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    contact.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
