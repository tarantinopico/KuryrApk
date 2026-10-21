# Kurýr – Plnohodnotný multiprotokolový e-mailový klient pro Android

**Kurýr** je moderní, soukromí respektující e-mailový klient pro Android napsaný 100% v jazyce **Kotlin** s uživatelským rozhraním v **Jetpack Compose (Material 3)**. Podporuje protokoly **IMAP4rev1/rev2**, **POP3**, **SMTP (Implicitní TLS i STARTTLS)** a **JMAP (RFC 8620 / RFC 8621)**, správu kontaktů a skupin kontaktů, chytré notifikace s tichými hodinami a denními/týdenními souhrny a dva domovské widgety postavené na **Jetpack Glance**.

---

## Klíčové vlastnosti

- **100 % bez OAuth a bez telemetrie**: Autentizace probíhá výhradně pomocí uživatelského jména a hesla / aplikačního hesla (App Password). Ve výchozím stavu je jakákoli telemetrie vypnuta (opt-in).
- **Bezpečné uložení hesel**: Všechna hesla jsou uložena v šifrovaném trezoru `AndroidKeystoreCredentialVault` využívajícím **Android Keystore (`AES256_GCM`)** a `EncryptedSharedPreferences`.
- **Offline-first architektura**: Lokální databáze **Room** uchovává účty, skupiny účtů, složky, zprávy, přílohy, kontakty, skupiny kontaktů, notifikační pravidla a frontu souhrnů.
- **Přímá síťová synchronizace na pozadí**: Periodická i jednorázová synchronizace pomocí **WorkManager** (`MailSyncWorker`) bez falešné offline simulace.
- **Vzhled a přizpůsobení (Material 3)**:
  - Světlý, Tmavý, čistě černý **AMOLED** režim a podpora **Material You** (dynamické barvy Android 12+).
  - Výběr vlastní akcentní barvy (výchozí Královská modrá a břidlicová v čistém redakčním stylu).
  - 3 úrovně hustoty seznamu zpráv (*Kompaktní*, *Výchozí*, *Komfortní*).
  - Konfigurovatelné swipe akce pro potažení doleva a doprava (*Archivovat*, *Smazat do koše*, *Označit přečtené/nepřečtené*, *Hvězdička*).
- **Kontakty a skupiny kontaktů**:
  - Lokální správa kontaktů, označení VIP odesílatelů, rozdělení do barevných skupin/složek a fulltextové vyhledávání.
  - Přímý import kontaktů ze systémového `ContactsContract` (`ContentResolver`) s runtime oprávněním `READ_CONTACTS`.
  - Našeptávání kontaktů a rychlé vložení celé skupiny kontaktů při psaní e-mailu.
- **Chytré notifikace a souhrny**:
  - Tři Android Notification Channels (*VIP a prioritní zprávy*, *Nová pošta*, *Denní a týdenní souhrny*).
  - Nastavitelné tiché hodiny (např. 22:00–07:00) s automatickým řazením zpráv do fronty denního nebo týdenního souhrnu (digestu).
  - Vlastní pravidla podle VIP stavu, odesílatele, klíčového slova v předmětu a přítomnosti přílohy.
- **Domovské widgety (Jetpack Glance)**:
  1. **Unified Inbox Widget** (`UnifiedInboxGlanceWidget`) – přehled doručených zpráv ze všech účtů.
  2. **Accounts Unread Widget** (`AccountsUnreadGlanceWidget`) – seznam účtů s počty nepřečtených zpráv.
  - Včetně konfigurační aktivity `WidgetConfigurationActivity`.

---

## Architektura projektu (MVVM + Clean Architecture)

- **`data/local`**: Room entity (`AccountEntity`, `AccountGroupEntity`, `FolderEntity`, `EmailMessageEntity`, `AttachmentEntity`, `ContactEntity`, `ContactGroupEntity`, `NotificationRuleEntity`, `DigestQueueEntity`), `KuryrDao` a `KuryrDatabase`.
- **`data/security`**: `AndroidKeystoreCredentialVault` (Android Keystore `MasterKey` + `EncryptedSharedPreferences`).
- **`data/preferences`**: `AppPreferencesRepository` nad Jetpack **DataStore**.
- **`network`**:
  - `ImapSmtpPop3Client`: Přímý TLS/STARTTLS klient pro IMAP4rev1/rev2 (`LOGIN`, `SELECT`, `LIST`, `FETCH`), POP3 (`STLS`, `USER`, `PASS`, `STAT`, `RETR`) a SMTP (`EHLO`, `STARTTLS`, `AUTH PLAIN`, `MAIL FROM`, `RCPT TO`, `DATA`).
  - `JmapClient`: Pure-Kotlin JMAP klient podle RFC 8620/8621 (`/.well-known/jmap` session discovery, `Mailbox/get`, `Email/query`, `Email/get`, `Email/set`, `EmailSubmission/set`).
  - `EmailHeaderParser`: RFC 5322 a RFC 2047 dekodér hlaviček, detekce `SPF`/`DKIM` a generátor MIME zpráv.
- **`notifications` & `sync` & `widget`**: `NotificationRuleEngine`, `MailSyncWorker` (WorkManager) a Glance widgety v `KuryrWidgets.kt`.
- **`ui`**: Reaktivní `KuryrViewModel` (`StateFlow`) a obrazovky v Jetpack Compose (`OnboardingScreen`, `AddOrEditAccountScreen`, `UnifiedInboxScreen`, `MessageDetailScreen`, `ComposeEmailScreen`, `AccountsAndGroupsScreen`, `ContactsScreen`, `NotificationsAndDigestsScreen`, `AppSettingsScreen`, `GlobalSearchScreen`).

---

## Podporovaní přednastavení poskytovatelé (17 předvoleb)

| Poskytovatel | Protokol | Příchozí server | Odchozí SMTP / JMAP Endpoint |
| :--- | :--- | :--- | :--- |
| **Seznam.cz** | IMAP4 / SMTP | `imap.seznam.cz:993` (TLS) | `smtp.seznam.cz:465` (TLS) |
| **Gmail** | IMAP4 / SMTP | `imap.gmail.com:993` (TLS) | `smtp.gmail.com:587` (STARTTLS) |
| **iCloud Mail** | IMAP4 / SMTP | `imap.mail.me.com:993` (TLS) | `smtp.mail.me.com:587` (STARTTLS) |
| **Yahoo Mail** | IMAP4 / SMTP | `imap.mail.yahoo.com:993` (TLS) | `smtp.mail.yahoo.com:465` (TLS) |
| **Fastmail** | JMAP / IMAP | `api.fastmail.com/jmap/session` | JMAP Submission / `smtp.fastmail.com:465` |
| **Zoho Mail** | IMAP4 / SMTP | `imappro.zoho.eu:993` (TLS) | `smtppro.zoho.eu:465` (TLS) |
| **GMX Mail** | IMAP4 / SMTP | `imap.gmx.com:993` (TLS) | `mail.gmx.com:587` (STARTTLS) |
| **Mail.ru** | IMAP4 / SMTP | `imap.mail.ru:993` (TLS) | `smtp.mail.ru:465` (TLS) |
| **Yandex Mail** | IMAP4 / SMTP | `imap.yandex.com:993` (TLS) | `smtp.yandex.com:465` (TLS) |
| **Posteo.de** | IMAP4 / SMTP | `posteo.de:993` (TLS) | `posteo.de:587` (STARTTLS) |
| **mailbox.org** | IMAP4 / SMTP | `imap.mailbox.org:993` (TLS) | `smtp.mailbox.org:465` (TLS) |
| **Disroot** | IMAP4 / SMTP | `disroot.org:993` (TLS) | `disroot.org:587` (STARTTLS) |
| **Sterna Mail** | JMAP | `https://jmap.sternamail.org/.well-known/jmap` | JMAP `EmailSubmission/set` |
| **Gridlink Mail** | IMAP4 / SMTP | `imap.gridlink.io:993` (TLS) | `smtp.gridlink.io:465` (TLS) |
| **Ltt.rs JMAP** | JMAP | `https://mail.ltt.rs/.well-known/jmap` | JMAP `EmailSubmission/set` |
| **Vlastní IMAP / POP3 / SMTP** | IMAP4 / POP3 | Vlastní hostitel a port | Vlastní SMTP server (TLS / STARTTLS) |
| **Vlastní JMAP (RFC 8620)** | JMAP | Vlastní JMAP Session URL | JMAP `EmailSubmission/set` |

---

## Návod na sestavení a spuštění testů

### Požadavky
- **JDK 21** (nebo JDK 17+)
- **Android SDK** (`compileSdk 36`, `minSdk 26`)

### Sestavení Debug APK
```bash
gradle :app:assembleDebug
```
Výstupní APK naleznete v `app/build/outputs/apk/debug/app-debug.apk`.

### Spuštění Unit a Robolectric UI testů
```bash
gradle :app:testDebugUnitTest
```

### GitHub Actions CI/CD
Soubor `.github/workflows/android-build.yml` automaticky spouští testy a sestavuje APK při každém pushi do větve `main` nebo při manuálním spuštění (`workflow_dispatch`).
