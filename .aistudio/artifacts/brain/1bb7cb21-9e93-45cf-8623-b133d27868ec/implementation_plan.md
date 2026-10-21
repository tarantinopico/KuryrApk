# Kurýr – Plnohodnotný multiprotokolový e-mailový klient pro Android

**Kurýr** je moderní, soukromí respektující e-mailový klient pro Android postavený na Jetpack Compose a Material 3, který kombinuje podporu protokolů IMAP4rev1/rev2, POP3, SMTP a JMAP (RFC 8620/8621) s lokální správou kontaktů, pokročilými pravidly notifikací a domovskými widgety bez závislosti na OAuth či cloudu třetích stran.

### User Review & Critical Decisions

> [!IMPORTANT]
> Níže jsou potvrzená rozhodnutí z úvodního upřesnění požadavků, která určují chování aplikace, síťové vrstvy a výchozího vizuálního stylu.

- **Potvrzené rozhodnutí 1 – Čistý výchozí stav a průvodce (Onboarding)**: Aplikace startuje se zcela prázdnou databází. Při prvním spuštění (nebo pokud není nakonfigurován žádný účet) je uživatel plynule proveden uvítacím průvodcem a obrazovkou pro přidání prvního e-mailového účtu s výběrem ze 17 přednastavených poskytovatelů nebo ruční konfigurací.
- **Potvrzené rozhodnutí 2 – Výhradně přímé síťové připojení**: Synchronizace schránek a odesílání zpráv probíhá výhradně přes přímé síťové spojení se vzdálenými servery (IMAP/POP3/SMTP přes TLS/STARTTLS a JMAP přes HTTPS) bez falešné offline simulace odeslání. Při výpadku spojení zůstávají rozepsané či čekající zprávy bezpečně uložené v lokální frontě k pozdějšímu reálnému odeslání.
- **Potvrzené rozhodnutí 3 – Královská modrá, břidlicová a čistý redakční styl**: Výchozí vizuální identita využívá klidnou redakční typografii a paletu královské modré (`#1E40AF` / `#3B82F6`) v kombinaci s břidlicově šedým pozadím (`#F8FAFC` ve světlém, `#0F172A` v tmavém a `#000000` v AMOLED režimu), s možností přepnutí na dynamické barvy Material You nebo vlastní akcentní barvu.

---

### 1. Overview & Core Concept

- **Co aplikace dělá**: Sjednocuje správu neomezeného počtu e-mailových účtů (IMAP, POP3, JMAP, SMTP) do jednoho přehledného prostředí s offline-first lokální mezipamětí (Room), integrovaným adresářem kontaktů (včetně obousměrného napojení na systémový adresář Androidu), chytrými notifikacemi s tichými hodinami a denními/týdenními souhrny a dvěma interaktivními widgety na plochu.
- **Cílová skupina**: Uživatelé vyžadující plnou kontrolu nad svými e-maily, soukromím a přihlašovacími údaji (autentizace výhradně jménem a heslem / aplikačním heslem bez OAuth telemetrie), kteří používají kombinaci běžných i nezávislých poskytovatelů (Seznam, Gmail, Fastmail, Posteo, mailbox.org, Sterna Mail, vlastní servery).
- **Hlavní přínos**: Rychlá odezva díky lokálnímu úložišti a fulltextovému vyhledávání, nulová telemetrie ve výchozím stavu, šifrované uložení hesel přes Android Keystore a špičková přizpůsobitelnost rozhraní (hustota seznamu, swipe gesta, vlastní pravidla upozornění).

---

### 2. User Experience & Visual Design

- **Klíčové uživatelské toky (User Flows)**:
  1. **První spuštění a Onboarding**: Uvítací průvodce představí klíčové pilíře (Soukromí bez OAuth, Multiprotokolová podpora, Chytré notifikace a kontakty) a přejde rovnou do průvodce přidáním účtu.
  2. **Přidání účtu s autodetekcí poskytovatele**: Uživatel zvolí jednoho ze 17 přednastavených poskytovatelů (Gmail, Seznam, iCloud, Yahoo, Fastmail, Zoho, GMX, Mail.ru, Yandex, Posteo, mailbox.org, Disroot, Sterna Mail, Gridlink Mail, Ltt.rs, vlastní IMAP/SMTP, vlastní JMAP). Serverové adresy, porty a typ šifrování se předvyplní automaticky; uživatel zadá e-mail, heslo / app password a ověří přímé spojení se serverem.
  3. **Sjednocená doručená pošta (Unified Inbox) a správa složek**: Přehled zpráv ze všech účtů (nebo vybraného účtu/skupiny účtů) s rychlými filtry (Nepřečtené, S hvězdičkou, S přílohou, VIP), konfigurovatelnými swipe gesty (archivovat, smazat, označit jako přečtené, přesunout) a třemi úrovněmi hustoty zobrazení (Kompaktní, Výchozí, Komfortní).
  4. **Čtení a psaní e-mailu (Detail & Compose)**: Čisté zobrazení celé konverzace, kompletních e-mailových hlaviček (včetně SPF/DKIM/Message-ID analýzy), stahování příloh a editor nové zprávy s našeptáváním lokálních kontaktů či celých skupin kontaktů (Do, Kopie, Skrytá kopie), podporou příloh a reálným odesláním přes SMTP nebo JMAP.
  5. **Adresář kontaktů a skupiny**: Lokální správa kontaktů rozdělených do barevných skupin/složek, okamžité fulltextové vyhledávání a jednorázový či opakovaný import ze systémového `ContactsContract` (s runtime oprávněním `READ_CONTACTS`).
  6. **Notifikace, souhrny a widgety**: Nastavení vlastních pravidel upozornění (VIP odesílatelé, klíčová slova v předmětu, přítomnost přílohy), definice tichých hodin a plánovaných denních/týdenních souhrnů, plus dva widgety na plochu (Unified Inbox a Přehled účtů s počty nepřečtených).

- **Vizuální identita a téma**:
  - *Estetický směr*: Redakční čistota a klidné pracovní prostředí. Žádné křiklavé přechody ani neony – důraz na čitelnost, rytmus typografie a jemné vrstvení ploch pomocí Material 3 `tonalElevation`.
  - *Barevná paleta*:
    - Primární akcent: Královská modrá (`#1E40AF` světlý / `#60A5FA` tmavý), sekundární břidlicová (`#475569` / `#94A3B8`).
    - Pozadí a povrchy: Břidlicově bílá (`#F8FAFC`) a čistě bílé karty ve světlém režimu; hluboká břidlice (`#0F172A`) v tmavém režimu; čistá černá (`#000000`) s jemnými obrysy (`#1E293B`) v režimu AMOLED.
    - Volitelné přepnutí na Material You (dynamické barvy Androidu 12+) nebo výběr z palety vlastních akcentních barev.
  - *Typografie a hierarchie*: Lokálně přibalené rodiny písem Google Fonts – výrazné, čitelné písmo pro nadpisy a předměty zpráv v kombinaci s neutrálním bezpatkovým písmem pro tělo zpráv a strojovým písmem pro technické hlavičky e-mailů.
  - *Odezva a mikrointerakce*: Plynulé pružinové animace (`spring`) při rozbalování vláken, hmatová a vizuální odezva při překročení prahu swipe gesta a srozumitelné prázdné stavy s akčními tlačítky.

---

### 3. Key Product Decisions & Trade-Offs

- **Rozhodnutí 1: Multiprotokolová síťová vrstva bez OAuth**
  - *Zvolený přístup*: Jednotné rozhraní protokolu (`MailProtocolClient`) s konkrétními implementacemi pro IMAP4rev1/rev2 + POP3 + SMTP (přímé TLS/STARTTLS socketové a protokolové operace s kompletním RFC 5322/2047 parserem a MIME sestavováním) a čistě Kotlinovým JMAP klientem (RFC 8620/8621 přes OkHttp/Moshi).
  - *Proč*: Zaručuje 100% nezávislost na proprietárních OAuth tocích, spolehlivou kompilaci, plnou kontrolu nad TLS/STARTTLS handshakem a snadné testování proti mock serverům v unit testech.
  - *Zvažované alternativy*: Použití uzavřených OAuth SDK třetích stran – zamítnuto kvůli požadavku na čistou autentizaci jménem a heslem / aplikačním heslem.

- **Rozhodnutí 2: Offline-first architektura s Room a bezpečným úložištěm Keystore**
  - *Zvolený přístup*: Veškeré účty (metadata), složky, hlavičky i těla zpráv, kontakty, skupiny a notifikační pravidla jsou uloženy v lokální databázi Room s reaktivními toky `Flow`. Hesla a aplikační hesla nejsou nikdy uložena v čistém textu v databázi, ale v šifrovaném úložišti chráněném klíčem v Android Keystore (`AES/GCM/NoPadding`).
  - *Proč*: Okamžité spuštění aplikace a procházení dříve stažených e-mailů i bez signálu, při zachování přísného zabezpečení přihlašovacích údajů.

- **Rozhodnutí 3: Synchronizace a souhrny přes WorkManager**
  - *Zvolený přístup*: Periodické úlohy `WorkManager` spouštěné podle uživatelem nastaveného intervalu (15 min, 30 min, 1 hod, manuálně) s kontrolou síťových podmínek, následované vyhodnocením notifikačních pravidel a případným zařazením do denního/týdenního souhrnu v době tichých hodin.
  - *Proč*: Respektuje systémové limity baterie (Doze mode) a spolehlivě doručí notifikace i po restartu zařízení.

---

### 4. Technical Architecture & Data Strategy

#### Architektura a hierarchie komponent

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        UI VRSTVA (Jetpack Compose + M3)                     │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌────────────────────┐  │
│  │  Onboarding  │ │ UnifiedInbox │ │MessageDetail │ │   ComposeEmail     │  │
│  │  & Přidání   │ │  & Složky /  │ │ & Inspekce   │ │ & Našeptávač       │  │
│  │    účtu      │ │  Vyhledávání │ │   hlaviček   │ │     kontaktů       │  │
│  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘ └─────────┬──────────┘  │
│  ┌──────┴───────┐ ┌──────┴───────┐ ┌──────┴───────┐ ┌─────────┴──────────┐  │
│  │ Účty & Skupi-│ │  Kontakty &  │ │ Notifikace,  │ │Domovské widgety    │  │
│  │ ny účtů      │ │  Skupiny     │ │ Tiché hodiny │ │(Unified & Accounts)│  │
│  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘ └─────────┬──────────┘  │
└─────────┼────────────────┼────────────────┼───────────────────┼─────────────┘
          │ StateFlow      │ StateFlow      │ StateFlow         │ Stav widgetu
┌─────────▼────────────────▼────────────────▼───────────────────▼─────────────┐
│                    DOMÉNOVÁ VRSTVA & VIEWMODELY (MVVM)                      │
│  ┌────────────────────────┐ ┌──────────────────────┐ ┌───────────────────┐  │
│  │  Synchronizační &      │ │  Engine notifikačních│ │  Parser RFC 5322  │  │
│  │  Odesílací UseCases    │ │  pravidel a souhrnů  │ │  hlaviček & MIME  │  │
│  └───────────┬────────────┘ └──────────┬───────────┘ └─────────┬─────────┘  │
└──────────────┼─────────────────────────┼───────────────────────┼────────────┘
               │                         │                       │
┌──────────────▼─────────────────────────▼───────────────────────▼────────────┐
│                     DATOVÁ, BEZPEČNOSTNÍ A SÍŤOVÁ VRSTVA                    │
│  ┌──────────────────┐ ┌──────────────────┐ ┌─────────────────────────────┐  │
│  │   Room Database  │ │ DataStore +      │ │   Síťoví klienti protokolů  │  │
│  │ (Účty, Zprávy,   │ │ Android Keystore │ │ - IMAP4rev1/rev2 & POP3     │  │
│  │  Složky, Kontakty│ │ Šifrovaný trezor │ │ - SMTP (STARTTLS / TLS)     │  │
│  │  Pravidla, Fronta│ │ hesel & Nastavení│ │ - JMAP (RFC 8620/8621 JSON) │  │
│  └──────────────────┘ └──────────────────┘ └─────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Datový model a stav
- **Účty a skupiny účtů (`AccountEntity`, `AccountGroupEntity`)**: Uchovávají e-mailovou adresu, zobrazované jméno, podpis, přiřazenou skupinu účtů, barvu štítku, protokol (`IMAP`, `POP3`, `JMAP`), příchozí a odchozí hostitele/porty, režim zabezpečení (`TLS`, `STARTTLS`) a interval synchronizace. Hesla jsou uložena odděleně v šifrovaném úložišti pod ID účtu.
- **Složky a zprávy (`FolderEntity`, `EmailMessageEntity`, `AttachmentEntity`)**: Lokální mezipaměť složek (Doručené, Odeslané, Koncepty, Koš, Archiv, Spam) a zpráv včetně parsovaných hlaviček (`Message-ID`, `In-Reply-To`, `References`, `SPF`, `DKIM`), příznaků (`isRead`, `isStarred`, `isFlagged`, `hasAttachments`), textu/HTML a stavu odeslání.
- **Kontakty a skupiny kontaktů (`ContactEntity`, `ContactGroupEntity`, vazební tabulka)**: Jméno, e-mailové adresy, telefon, poznámka, příznak VIP, zdroj (lokální vs. import z Androidu) a zařazení do vlastních skupin kontaktů.
- **Notifikační pravidla a fronta souhrnů (`NotificationRuleEntity`, `DigestQueueEntity`)**: Vlastní filtry podle VIP stavu, odesílatele, klíčového slova v předmětu a přílohy; fronta zachycených zpráv během tichých hodin pro vygenerování denního nebo týdenního souhrnu.
- **Nastavení aplikace (`AppPreferences` v DataStore)**: Režim motivu (Světlý, Tmavý, AMOLED, Systémový), přepínač Material You, vlastní akcentní barva, hustota seznamu (Kompaktní, Výchozí, Komfortní), akce pro potažení doleva/doprava, tiché hodiny (od–do), zapnutí denního/týdenního souhrnu a opt-in přepínač diagnostiky (výchozí vypnuto).

#### Mapování interaktivních komponent a stavů
- **Průvodce přidáním účtu**: Výběr poskytovatele ze seznamu 17 předvoleb automaticky vyplní hostitele, porty a typ protokolu; tlačítko „Ověřit a uložit“ provede reálný síťový test spojení (nebo uložení ověřené konfigurace), uloží heslo do Keystore trezoru, vytvoří výchozí složky v Room a přesměruje do Unified Inboxu.
- **Unified Inbox a Swipe akce**: Potažení karty zprávy doleva či doprava vykoná zvolenou akci (Archivovat, Smazat, Označit jako přečtené/nepřečtené, Přidat hvězdičku) s okamžitým zápisem do Room a možností vrácení zpět (`Snackbar` s akcí Zpět).
- **Editor zprávy (Compose)**: Psaní do polí Komu/Kopie/Skrytá kopie živě filtruje kontakty a skupiny kontaktů z Room; výběr skupiny vloží všechny členy skupiny. Tlačítko Odeslat sestaví RFC 5322 MIME zprávu a odešle ji přímo přes SMTP nebo JMAP `EmailSubmission/set`.
- **Domovské widgety**: Každá změna počtu nepřečtených zpráv nebo doručení nového e-mailu vyvolá aktualizaci obou widgetů (Unified Inbox s posledními zprávami a Accounts přehled s počty nepřečtených).

#### Plán realizace a kontrolní body (Checkpoints)
1. **Checkpoint 1 – Struktura projektu, závislosti a datová vrstva**: Konfigurace Gradle, entit Room, DAO, DataStore, šifrovaného Keystore trezoru hesel, katalogu 17 poskytovatelů a repozitářů; ověření kompilace.
2. **Checkpoint 2 – Síťová vrstva a parsování**: Implementace IMAP4rev1/rev2, POP3, SMTP (STARTTLS/TLS) a JMAP (RFC 8620/8621) klientů, RFC 5322/2047 parseru hlaviček a unit testů pro síťové protokoly a hlavičky.
3. **Checkpoint 3 – UI vrstva v Jetpack Compose**: Implementace kompletního Material 3 rozhraní (Onboarding, Unified Inbox, Detail zprávy s inspektorem hlaviček, Compose editor, Účty a Skupiny účtů, Kontakty s importem a skupinami, Hledání, Nastavení vzhledu a účtů).
4. **Checkpoint 4 – Notifikace, WorkManager a Widgety**: Implementace notifikačních kanálů, pravidel (VIP, předmět, odesílatel, příloha), tichých hodin, denních/týdenních souhrnů a obou domovských widgetů včetně konfigurační aktivity.
5. **Checkpoint 5 – Testy, CI/CD a dokumentace**: Dokončení unit a UI testů (Robolectric Compose testy pro onboarding, přidání účtu, psaní e-mailu a přepnutí tmavého režimu), GitHub Actions workflow (`.github/workflows/android-build.yml`) a dokumentace `README.md`.
