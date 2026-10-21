package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AccountEntity
import com.example.data.local.AccountGroupEntity
import com.example.data.local.FolderEntity
import com.example.data.model.FolderType
import com.example.data.model.MailProtocol
import com.example.data.model.ProviderCatalog
import com.example.data.model.ProviderPreset
import com.example.data.model.SecurityMode
import com.example.ui.theme.KuryrAccentPalette

@Composable
fun OnboardingScreen(
    onStartAccountSetup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("onboarding_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_onboarding_hero),
                    contentDescription = "Ilustrace aplikace Kurýr",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Text(
                text = "Kurýr",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Nezávislý multiprotokolový e-mailový klient navržený pro soukromí, klidnou práci a plnou kontrolu nad vašimi účty.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OnboardingFeatureCard(
                icon = Icons.Default.Storage,
                title = "IMAP4rev2, POP3, SMTP a JMAP",
                description = "Přímé šifrované spojení se 17 přednastavenými poskytovateli (Seznam, Gmail, Fastmail, Posteo, mailbox.org, Sterna Mail) i vlastními servery."
            )

            OnboardingFeatureCard(
                icon = Icons.Default.Lock,
                title = "Bez OAuth a bez telemetrie",
                description = "Autentizace výhradně jménem a heslem / aplikačním heslem. Hesla jsou chráněna klíčem v Android Keystore (AES-256 GCM)."
            )

            OnboardingFeatureCard(
                icon = Icons.Default.NotificationsActive,
                title = "Chytré notifikace, kontakty a widgety",
                description = "Tiché hodiny, denní a týdenní souhrny, VIP pravidla, skupiny kontaktů a dva interaktivní widgety na plochu."
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onStartAccountSetup,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("onboarding_start_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.MailOutline, contentDescription = null)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Přidat první e-mailový účet",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun OnboardingFeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddOrEditAccountScreen(
    existingAccount: AccountEntity?,
    accountGroups: List<AccountGroupEntity>,
    isTestingConnection: Boolean,
    connectionFeedback: String?,
    onBack: () -> Unit,
    onTestConnection: (AccountEntity, String) -> Unit,
    onSaveAccount: (AccountEntity, String) -> Unit
) {
    val defaultPreset = remember(existingAccount) {
        existingAccount?.let { acct ->
            ProviderCatalog.presets.firstOrNull { it.id == acct.providerId }
        } ?: ProviderCatalog.presets.first()
    }

    var selectedPreset by remember { mutableStateOf(defaultPreset) }
    var accountName by remember { mutableStateOf(existingAccount?.accountName ?: "") }
    var displayName by remember { mutableStateOf(existingAccount?.displayName ?: "") }
    var emailAddress by remember { mutableStateOf(existingAccount?.emailAddress ?: "") }
    var username by remember { mutableStateOf(existingAccount?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var protocol by remember { mutableStateOf(existingAccount?.protocol ?: defaultPreset.protocol) }
    var incomingHost by remember { mutableStateOf(existingAccount?.incomingHost ?: defaultPreset.incomingHost) }
    var incomingPortText by remember { mutableStateOf((existingAccount?.incomingPort ?: defaultPreset.incomingPort).toString()) }
    var incomingSecurity by remember { mutableStateOf(existingAccount?.incomingSecurity ?: defaultPreset.incomingSecurity) }
    var smtpHost by remember { mutableStateOf(existingAccount?.smtpHost ?: defaultPreset.smtpHost) }
    var smtpPortText by remember { mutableStateOf((existingAccount?.smtpPort ?: defaultPreset.smtpPort).toString()) }
    var smtpSecurity by remember { mutableStateOf(existingAccount?.smtpSecurity ?: defaultPreset.smtpSecurity) }
    var jmapEndpoint by remember { mutableStateOf(existingAccount?.jmapEndpoint ?: defaultPreset.jmapEndpoint) }
    var signature by remember { mutableStateOf(existingAccount?.signature ?: "Odesláno z aplikace Kurýr") }
    var syncInterval by remember { mutableIntStateOf(existingAccount?.syncIntervalMinutes ?: 15) }
    var selectedGroupId by remember { mutableStateOf(existingAccount?.groupId) }
    var badgeColorHex by remember { mutableLongStateOf(existingAccount?.colorHex ?: defaultPreset.badgeColorHex) }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun applyPreset(preset: ProviderPreset) {
        selectedPreset = preset
        protocol = preset.protocol
        if (preset.incomingHost.isNotBlank()) incomingHost = preset.incomingHost
        incomingPortText = preset.incomingPort.toString()
        incomingSecurity = preset.incomingSecurity
        if (preset.smtpHost.isNotBlank()) smtpHost = preset.smtpHost
        smtpPortText = preset.smtpPort.toString()
        smtpSecurity = preset.smtpSecurity
        jmapEndpoint = preset.jmapEndpoint
        badgeColorHex = preset.badgeColorHex
    }

    fun buildAccountEntity(): AccountEntity {
        val cleanEmail = emailAddress.trim()
        val effectiveUser = username.trim().ifBlank { cleanEmail }
        val effectiveTitle = accountName.trim().ifBlank { selectedPreset.name }
        val effectiveSenderName = displayName.trim().ifBlank { cleanEmail.substringBefore('@') }
        return AccountEntity(
            id = existingAccount?.id ?: 0L,
            accountName = effectiveTitle,
            displayName = effectiveSenderName,
            emailAddress = cleanEmail,
            username = effectiveUser,
            providerId = selectedPreset.id,
            protocol = protocol,
            incomingHost = incomingHost.trim(),
            incomingPort = incomingPortText.toIntOrNull() ?: 993,
            incomingSecurity = incomingSecurity,
            smtpHost = smtpHost.trim(),
            smtpPort = smtpPortText.toIntOrNull() ?: 587,
            smtpSecurity = smtpSecurity,
            jmapEndpoint = jmapEndpoint.trim(),
            signature = signature.trim(),
            syncIntervalMinutes = syncInterval,
            colorHex = badgeColorHex,
            groupId = selectedGroupId,
            isDefault = existingAccount?.isDefault ?: false
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (existingAccount == null) "Přidat e-mailový účet" else "Nastavení účtu")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("account_setup_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("add_account_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "1. Vyberte poskytovatele (17 předvoleb)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(ProviderCatalog.presets, key = { it.id }) { preset ->
                    FilterChip(
                        selected = selectedPreset.id == preset.id,
                        onClick = { applyPreset(preset) },
                        label = { Text(preset.name) },
                        leadingIcon = if (selectedPreset.id == preset.id) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("preset_chip_${preset.id}")
                    )
                }
            }

            if (selectedPreset.requiresAppPasswordNote.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = selectedPreset.requiresAppPasswordNote,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Text(
                text = "2. Přihlašovací údaje (bez OAuth)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = emailAddress,
                onValueChange = { newValue ->
                    emailAddress = newValue
                    if (username.isBlank()) {
                        ProviderCatalog.detectPresetByEmail(newValue)?.let { detected ->
                            applyPreset(detected)
                        }
                    }
                },
                label = { Text("E-mailová adresa *") },
                placeholder = { Text("jan.novak@${selectedPreset.domainHint.substringBefore(' ')}") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_email_input")
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = {
                    Text(
                        if (existingAccount == null) "Heslo nebo Aplikační heslo (App Password) *"
                        else "Nové heslo (ponechte prázdné pro zachování)"
                    )
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_password_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("Název účtu") },
                    placeholder = { Text(selectedPreset.name) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("account_name_input")
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Jméno odesílatele") },
                    placeholder = { Text("Jan Novák") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("account_display_name_input")
                )
            }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Uživatelské jméno (volitelné, výchozí je e-mail)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_username_input")
            )

            Text(
                text = "3. Protokol a servery",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MailProtocol.entries.forEach { proto ->
                    FilterChip(
                        selected = protocol == proto,
                        onClick = {
                            protocol = proto
                            if (proto == MailProtocol.POP3 && incomingPortText == "993") {
                                incomingPortText = "995"
                            } else if (proto == MailProtocol.IMAP && incomingPortText == "995") {
                                incomingPortText = "993"
                            }
                        },
                        label = { Text(proto.displayName) },
                        modifier = Modifier.testTag("protocol_chip_${proto.name}")
                    )
                }
            }

            if (protocol == MailProtocol.JMAP) {
                OutlinedTextField(
                    value = jmapEndpoint,
                    onValueChange = { jmapEndpoint = it },
                    label = { Text("JMAP Session URL (RFC 8620)") },
                    placeholder = { Text("https://api.fastmail.com/jmap/session") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("jmap_endpoint_input")
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = incomingHost,
                        onValueChange = { incomingHost = it },
                        label = { Text("${protocol.name} server") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(2f)
                            .testTag("incoming_host_input")
                    )
                    OutlinedTextField(
                        value = incomingPortText,
                        onValueChange = { incomingPortText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Port") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("incoming_port_input")
                    )
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecurityMode.entries.forEach { mode ->
                        FilterChip(
                            selected = incomingSecurity == mode,
                            onClick = { incomingSecurity = mode },
                            label = { Text(mode.displayName) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = smtpHost,
                        onValueChange = { smtpHost = it },
                        label = { Text("SMTP server") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(2f)
                            .testTag("smtp_host_input")
                    )
                    OutlinedTextField(
                        value = smtpPortText,
                        onValueChange = { smtpPortText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("SMTP port") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("smtp_port_input")
                    )
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecurityMode.entries.forEach { mode ->
                        FilterChip(
                            selected = smtpSecurity == mode,
                            onClick = { smtpSecurity = mode },
                            label = { Text("SMTP: ${mode.displayName}") }
                        )
                    }
                }
            }

            Text(
                text = "4. Podpis, skupina účtů a interval synchronizace",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = signature,
                onValueChange = { signature = it },
                label = { Text("Automatický podpis pod zprávou") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_signature_input")
            )

            if (accountGroups.isNotEmpty()) {
                Text("Skupina účtů:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedGroupId == null,
                        onClick = { selectedGroupId = null },
                        label = { Text("Bez skupiny") }
                    )
                    accountGroups.forEach { grp ->
                        FilterChip(
                            selected = selectedGroupId == grp.id,
                            onClick = { selectedGroupId = grp.id },
                            label = { Text(grp.name) }
                        )
                    }
                }
            }

            Text("Interval synchronizace na pozadí (WorkManager):", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 60, 120).forEach { mins ->
                    FilterChip(
                        selected = syncInterval == mins,
                        onClick = { syncInterval = mins },
                        label = { Text("$mins min") }
                    )
                }
            }

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (connectionFeedback != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = connectionFeedback,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (emailAddress.isBlank()) {
                            validationError = "Zadejte platnou e-mailovou adresu."
                        } else {
                            validationError = null
                            onTestConnection(buildAccountEntity(), password)
                        }
                    },
                    enabled = !isTestingConnection,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("verify_connection_button")
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Ověřit server")
                }

                Button(
                    onClick = {
                        when {
                            !emailAddress.contains("@") -> {
                                validationError = "Zadejte platnou e-mailovou adresu obsahující @."
                            }
                            existingAccount == null && password.isBlank() -> {
                                validationError = "Zadejte heslo nebo aplikační heslo pro šifrované uložení."
                            }
                            protocol != MailProtocol.JMAP && incomingHost.isBlank() -> {
                                validationError = "Zadejte adresu příchozího serveru."
                            }
                            else -> {
                                validationError = null
                                onSaveAccount(buildAccountEntity(), password)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_account_button")
                ) {
                    Text(if (existingAccount == null) "Uložit účet" else "Uložit změny")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AccountsAndGroupsScreen(
    accounts: List<AccountEntity>,
    accountGroups: List<AccountGroupEntity>,
    folders: List<FolderEntity>,
    onAddAccount: () -> Unit,
    onEditAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (Long) -> Unit,
    onSyncAccount: (Long) -> Unit,
    onCreateAccountGroup: (String, Long) -> Unit,
    onDeleteAccountGroup: (AccountGroupEntity) -> Unit
) {
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("accounts_screen"),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "E-mailové účty",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Hesla jsou šifrována v Android Keystore (AES-256)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onAddAccount,
                    modifier = Modifier.testTag("accounts_add_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Přidat účet")
                }
            }
        }

        if (accounts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Zatím není připojen žádný e-mailový účet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Přidejte účet IMAP, POP3 nebo JMAP pro zahájení přímé synchronizace.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(accounts, key = { it.id }) { account ->
                val unreadCount = folders
                    .filter { it.accountId == account.id && it.type == FolderType.INBOX }
                    .sumOf { it.unreadCount }
                val groupName = accountGroups.firstOrNull { it.id == account.groupId }?.name

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_card_${account.id}"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color(account.colorHex))
                                )
                                Column {
                                    Text(
                                        text = account.accountName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = account.emailAddress,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "$unreadCount nepřečtených",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = {},
                                label = { Text(account.protocol.displayName) }
                            )
                            if (groupName != null) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Skupina: $groupName") }
                                )
                            }
                            AssistChip(
                                onClick = {},
                                label = { Text("Interval: ${account.syncIntervalMinutes}m") }
                            )
                        }

                        Text(
                            text = account.lastSyncStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { onSyncAccount(account.id) }) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Synchronizovat")
                            }
                            TextButton(onClick = { onEditAccount(account) }) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Upravit")
                            }
                            IconButton(onClick = { onDeleteAccount(account.id) }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Smazat účet",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Skupiny účtů (Složky účtů)",
                    style = MaterialTheme.typography.titleLarge
                )
                OutlinedButton(
                    onClick = { showCreateGroupDialog = true },
                    modifier = Modifier.testTag("add_account_group_button")
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nová skupina")
                }
            }
        }

        if (accountGroups.isEmpty()) {
            item {
                Text(
                    text = "Rozdělte si pracovní, osobní či projektové účty do vlastních skupin pro rychlé filtrování v doručené poště.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(accountGroups, key = { it.id }) { group ->
                val memberCount = accounts.count { it.groupId == group.id }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(group.colorHex))
                            )
                            Column {
                                Text(group.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "$memberCount přiřazených účtů",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { onDeleteAccountGroup(group) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Smazat skupinu")
                        }
                    }
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        var groupName by remember { mutableStateOf("") }
        var selectedColor by remember { mutableLongStateOf(KuryrAccentPalette.first().colorHex) }

        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Nová skupina účtů") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Název skupiny (např. Práce, Projekty)") },
                        singleLine = true,
                        modifier = Modifier.testTag("account_group_name_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KuryrAccentPalette.forEach { option ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(option.colorHex))
                                    .border(
                                        width = if (selectedColor == option.colorHex) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = option.colorHex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupName.isNotBlank()) {
                            onCreateAccountGroup(groupName, selectedColor)
                            showCreateGroupDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_account_group_button")
                ) {
                    Text("Vytvořit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) {
                    Text("Zrušit")
                }
            }
        )
    }
}
