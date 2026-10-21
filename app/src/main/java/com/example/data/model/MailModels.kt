package com.example.data.model

enum class MailProtocol(val displayName: String) {
    IMAP("IMAP4rev1/rev2"),
    POP3("POP3"),
    JMAP("JMAP (RFC 8620/8621)")
}

enum class SecurityMode(val displayName: String) {
    IMPLICIT_TLS("Implicitní TLS (SSL/TLS)"),
    STARTTLS("STARTTLS"),
    NONE("Nešifrováno (pouze lokální test)")
}

enum class FolderType(val displayName: String) {
    INBOX("Doručené"),
    SENT("Odeslané"),
    DRAFTS("Koncepty"),
    ARCHIVE("Archiv"),
    TRASH("Koš"),
    SPAM("Nevyžádané"),
    CUSTOM("Vlastní složka")
}

enum class ThemeMode(val displayName: String) {
    SYSTEM("Podle systému"),
    LIGHT("Světlý"),
    DARK("Tmavý"),
    AMOLED("AMOLED černý")
}

enum class ListDensity(val displayName: String, val maxLinesPreview: Int, val verticalPaddingDp: Int) {
    COMPACT("Kompaktní", 1, 6),
    DEFAULT("Výchozí", 2, 12),
    COMFORTABLE("Komfortní", 3, 16)
}

enum class SwipeAction(val displayName: String) {
    ARCHIVE("Archivovat"),
    DELETE("Smazat do koše"),
    TOGGLE_READ("Označit přečtené/nepřečtené"),
    TOGGLE_STAR("Přidat/odebrat hvězdičku"),
    NONE("Žádná akce")
}

enum class DigestFrequency(val displayName: String) {
    OFF("Vypnuto"),
    DAILY("Denní souhrn"),
    WEEKLY("Týdenní souhrn")
}

enum class OutboxStatus {
    SENT,
    QUEUED_PENDING_NETWORK,
    DRAFT,
    FAILED
}

data class ProviderPreset(
    val id: String,
    val name: String,
    val domainHint: String,
    val protocol: MailProtocol,
    val incomingHost: String,
    val incomingPort: Int,
    val incomingSecurity: SecurityMode,
    val smtpHost: String,
    val smtpPort: Int,
    val smtpSecurity: SecurityMode,
    val jmapEndpoint: String = "",
    val requiresAppPasswordNote: String = "",
    val badgeColorHex: Long = 0xFF1E40AF
)

object ProviderCatalog {
    val presets: List<ProviderPreset> = listOf(
        ProviderPreset(
            id = "seznam",
            name = "Seznam.cz",
            domainHint = "seznam.cz / email.cz",
            protocol = MailProtocol.IMAP,
            incomingHost = "imap.seznam.cz",
            incomingPort = 993,
            incomingSecurity = SecurityMode.IMPLICIT_TLS,
            smtpHost = "smtp.seznam.cz",
            smtpPort = 465,
            smtpSecurity = SecurityMode.IMPLICIT_TLS,
            requiresAppPasswordNote = "Povolte IMAP/SMTP v nastavení Seznam E-mailu.",
            badgeColorHex = 0xFFDC2626
        ),
        ProviderPreset(
            id = "gmail",
            name = "Gmail",
            domainHint = "gmail.com",
            protocol = MailProtocol.IMAP,
            incomingHost = "imap.gmail.com",
            incomingPort = 993,
            incomingSecurity = SecurityMode.IMPLICIT_TLS,
            smtpHost = "smtp.gmail.com",
            smtpPort = 587,
            smtpSecurity = SecurityMode.STARTTLS,
            requiresAppPasswordNote = "Vyžaduje 16místné Heslo aplikace (App Password) v účtu Google.",
            badgeColorHex = 0xFFEA4335
        ),
        ProviderPreset(
            id = "icloud",
            name = "iCloud Mail",
            domainHint = "icloud.com / me.com",
            protocol = MailProtocol.IMAP,
            incomingHost = "imap.mail.me.com",
            incomingPort = 993,
            incomingSecurity = SecurityMode.IMPLICIT_TLS,
            smtpHost = "smtp.mail.me.com",
            smtpPort = 587,
            smtpSecurity = SecurityMode.STARTTLS,
            requiresAppPasswordNote = "Vygenerujte heslo pro aplikaci na appleid.apple.com.",
            badgeColorHex = 0xFF0284C7
        ),
        ProviderPreset(
            id = "yahoo",
            name = "Yahoo Mail",
            domainHint = "yahoo.com",
            protocol = MailProtocol.IMAP,
            incomingHost = "imap.mail.yahoo.com",
            incomingPort = 993,
            incomingSecurity = SecurityMode.IMPLICIT_TLS,
            smtpHost = "smtp.mail.yahoo.com",
            smtpPort = 465,
            smtpSecurity = SecurityMode.IMPLICIT_TLS,
            requiresAppPasswordNote = "Vyžaduje App Password v zabezpečení účtu Yahoo.",
            badgeColorHex = 0xFF7C3AED
        ),
        ProviderPreset(
            id = "fastmail",
            name = "Fastmail",
            domainHint = "fastmail.com",
            protocol = MailProtocol.JMAP,
            incomingHost = "imap.fastmail.com",
            incomingPort = 993,
            incomingSecurity = SecurityMode.IMPLICIT_TLS,
            smtpHost = "smtp.fastmail.com",
            smtpPort = 465,
            smtpSecurity = SecurityMode.IMPLICIT_TLS,
            jmapEndpoint = "https://api.fastmail.com/jmap/session",
            requiresAppPasswordNote = "Podporuje nativní JMAP API token nebo App Password.",
            badgeColorHex = 0xFF1E40AF
        ),
        ProviderPreset(
            id = "zoho",
            name = "Zoho Mail",
            domainHint = "zohomail.eu / zoho.com",
            protocol = MailProtocol.IMAP,
            incomingHost = "imappro.zoho.eu",
            incomingPort = 993,
            incomingSecurity = SecurityMode.IMPLICIT_TLS,
            smtpHost = "smtppro.zoho.eu",
            smtpPort = 465,
            smtpSecurity = SecurityMode.IMPLICIT_TLS,
            requiresAppPasswordNote = "Povolte IMAP přístup a vygenerujte aplikační heslo.",
            badgeColorHex = 0xFFD97706
        ),
        ProviderPreset(
            id = "gmx",
            name = "GMX Mail",
            domainHint = "gmx.com / gmx.de",
            protocol = MailProtocol.IMAP,
            incomingHost = "imap.gmx.com",
            incomingPort = 993,
            incomingSecurity = SecurityMode.IMPLICIT_TLS,
            smtpHost = "mail.gmx.com",
            smtpPort = 587,
            smtpSecurity = SecurityMode.STARTTLS,
            badgeColorHex = 0xFF1D4ED8
        ),
        ProviderPreset(
            id = "mailru",
            name = "Mail.ru",
            domainHint = "mail.ru",
            protocol = MailProtocol.IMAP,
            incomingHost = "imap.mail.ru",
            incomingPort = 993,
            incomingSecurity = SecurityMode.IMPLICIT_TLS,
            smtpHost = "smtp.mail.ru",
            smtpPort = 465,
            smtpSecurity = SecurityMode.IMPLICIT_TLS,
            badgeColorHex = 0xFF2563EB
        ),
        ProviderPreset(
            id = "yandex",
            name = "Yandex Mail",
            domainHint = "yandex.com",
            protocol = MailProtocol.IMAP,
            incomingHost = "imap.yandex.com",
            incomingPort = 993,
            incomingSecurity = SecurityMode.IMPLICIT_TLS,
            smtpHost = "smtp.yandex.com",
            smtpPort = 465,
            smtpSecurity = SecurityMode.IMPLICIT_TLS,
            badgeColorHex = 0xFFEF4444
        ),
        ProviderPreset(
            id = "posteo",
            name = "Posteo.de",
            domainHint = "posteo.de / posteo.net",
            protocol = MailProtocol.IMAP,
            incomingHost = "posteo.de",
            incomingPort = 993,
            incomingSecurity = SecurityMode.IMPLICIT_TLS,
            smtpHost = "posteo.de",
            smtpPort = 587,
            smtpSecurity = SecurityMode.STARTTLS,
            badgeColorHex = 0xFF15803D
        ),
        ProviderPreset(
            id = "mailbox_org",
            name = "mailbox.org",
            domainHint = "mailbox.org",
            protocol = MailProtocol.IMAP,
            incomingHost = "imap.mailbox.org",
            incomingPort = 993,
            incomingSecurity = SecurityMode.IMPLICIT_TLS,
            smtpHost = "smtp.mailbox.org",
            smtpPort = 465,
            smtpSecurity = SecurityMode.IMPLICIT_TLS,
            badgeColorHex = 0xFF0F766E
        ),
        ProviderPreset(
            id = "disroot",
            name = "Disroot",
            domainHint = "disroot.org",
            protocol = MailProtocol.IMAP,
            incomingHost = "disroot.org",
            incomingPort = 993,
            incomingSecurity = SecurityMode.IMPLICIT_TLS,
            smtpHost = "disroot.org",
            smtpPort = 587,
            smtpSecurity = SecurityMode.STARTTLS,
            badgeColorHex = 0xFF4338CA
        ),
        ProviderPreset(
            id = "sterna_mail",
            name = "Sterna Mail",
            domainHint = "sternamail.org",
            protocol = MailProtocol.JMAP,
            incomingHost = "jmap.sternamail.org",
            incomingPort = 443,
            incomingSecurity = SecurityMode.IMPLICIT_TLS,
            smtpHost = "smtp.sternamail.org",
            smtpPort = 587,
            smtpSecurity = SecurityMode.STARTTLS,
            jmapEndpoint = "https://jmap.sternamail.org/.well-known/jmap",
            badgeColorHex = 0xFF0369A1
        ),
        ProviderPreset(
            id = "gridlink_mail",
            name = "Gridlink Mail",
            domainHint = "gridlink.io",
            protocol = MailProtocol.IMAP,
            incomingHost = "imap.gridlink.io",
            incomingPort = 993,
            incomingSecurity = SecurityMode.IMPLICIT_TLS,
            smtpHost = "smtp.gridlink.io",
            smtpPort = 465,
            smtpSecurity = SecurityMode.IMPLICIT_TLS,
            badgeColorHex = 0xFF334155
        ),
        ProviderPreset(
            id = "lttrs",
            name = "Ltt.rs JMAP",
            domainHint = "ltt.rs",
            protocol = MailProtocol.JMAP,
            incomingHost = "mail.ltt.rs",
            incomingPort = 443,
            incomingSecurity = SecurityMode.IMPLICIT_TLS,
            smtpHost = "mail.ltt.rs",
            smtpPort = 587,
            smtpSecurity = SecurityMode.STARTTLS,
            jmapEndpoint = "https://mail.ltt.rs/.well-known/jmap",
            badgeColorHex = 0xFF0D9488
        ),
        ProviderPreset(
            id = "custom_imap",
            name = "Vlastní IMAP / POP3 / SMTP",
            domainHint = "vlastni-domena.cz",
            protocol = MailProtocol.IMAP,
            incomingHost = "",
            incomingPort = 993,
            incomingSecurity = SecurityMode.IMPLICIT_TLS,
            smtpHost = "",
            smtpPort = 587,
            smtpSecurity = SecurityMode.STARTTLS,
            badgeColorHex = 0xFF475569
        ),
        ProviderPreset(
            id = "custom_jmap",
            name = "Vlastní JMAP (RFC 8620)",
            domainHint = "jmap.vlastni-domena.cz",
            protocol = MailProtocol.JMAP,
            incomingHost = "",
            incomingPort = 443,
            incomingSecurity = SecurityMode.IMPLICIT_TLS,
            smtpHost = "",
            smtpPort = 587,
            smtpSecurity = SecurityMode.STARTTLS,
            jmapEndpoint = "https://",
            badgeColorHex = 0xFF1E3A8A
        )
    )

    fun detectPresetByEmail(email: String): ProviderPreset? {
        val domain = email.substringAfter('@', "").lowercase().trim()
        if (domain.isEmpty()) return null
        return when {
            domain in listOf("seznam.cz", "email.cz", "post.cz", "spoluzaci.cz", "stream.cz") ->
                presets.first { it.id == "seznam" }
            domain in listOf("gmail.com", "googlemail.com") ->
                presets.first { it.id == "gmail" }
            domain in listOf("icloud.com", "me.com", "mac.com") ->
                presets.first { it.id == "icloud" }
            domain.startsWith("yahoo.") || domain == "ymail.com" ->
                presets.first { it.id == "yahoo" }
            domain.startsWith("fastmail.") ->
                presets.first { it.id == "fastmail" }
            domain.startsWith("zoho.") || domain.startsWith("zohomail.") ->
                presets.first { it.id == "zoho" }
            domain.startsWith("gmx.") ->
                presets.first { it.id == "gmx" }
            domain == "mail.ru" || domain == "inbox.ru" || domain == "bk.ru" ->
                presets.first { it.id == "mailru" }
            domain.startsWith("yandex.") || domain == "ya.ru" ->
                presets.first { it.id == "yandex" }
            domain.startsWith("posteo.") ->
                presets.first { it.id == "posteo" }
            domain == "mailbox.org" ->
                presets.first { it.id == "mailbox_org" }
            domain == "disroot.org" ->
                presets.first { it.id == "disroot" }
            domain.contains("sterna") ->
                presets.first { it.id == "sterna_mail" }
            domain.contains("gridlink") ->
                presets.first { it.id == "gridlink_mail" }
            domain.contains("ltt.rs") ->
                presets.first { it.id == "lttrs" }
            else -> null
        }
    }
}
