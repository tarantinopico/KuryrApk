# Oprava a vylepšení GitHub Actions CI/CD workflow pro Kurýr

Tento plán řeší chybu validace souboru `.github/workflows/android-build.yml` (`Unrecognized named-value: 'secrets'`) a zároveň zajišťuje 100% spolehlivé sestavení Debug i Release APK na čistém GitHub Actions runneru.

### User Review & Critical Decisions

> [!IMPORTANT]
> Níže jsou klíčové technické opravy, které zajistí bezchybnou validaci workflow na GitHubu a úspěšný průběh kompilace v CI prostředí.

- **Oprava chyby `Unrecognized named-value: 'secrets'` (řádky 57 a 71)**: GitHub Actions neumožňuje přímé použití kontextu `secrets.*` uvnitř výrazů `if:` na úrovni kroků (`steps[*].if`). Tajné proměnné (`KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`) proto namapujeme do bloku `env:` na úrovni jobu a v podmínkách `if:` použijeme platný kontext `env.KEYSTORE_BASE64 != ''`.
- **Zajištění `debug.keystore` v CI prostředí**: Protože je soubor `debug.keystore` uveden v `.gitignore` a `app/build.gradle.kts` jej vyžaduje pro `debugConfig`, přidáme do workflow krok, který na GitHub runneru před kompilací automaticky vygeneruje standardní `debug.keystore` pomocí `keytool` (pokud v repozitáři neexistuje).
- **Přesná verze Gradle (`9.3.1`) a Android SDK licence**: Protože repozitář neobsahuje skript `gradlew`, nastavíme v `gradle/actions/setup-gradle@v4` explicitně `gradle-version: '9.3.1'` (shodně s `gradle-wrapper.properties`) a přidáme automatické potvrzení licencí Android SDK pro `compileSdk 36`.

---

### 1. Overview & Core Concept

- **Co tato úprava řeší**: Odstraňuje syntaktickou chybu GitHub Actions analyzátoru a pokrývá všechna úskalí čistého CI prostředí (chybějící `gradlew`, ignorovaný `debug.keystore`, licence Android SDK a podmíněný Release build).
- **Výsledek**: Workflow projde validací GitHubu bez jediné chyby či varování, spustí unit a Robolectric testy, zkompiluje APK a nahraje jej do GitHub Artifacts.

---

### 2. User Experience & Visual Design

- Samotné uživatelské rozhraní aplikace **Kurýr** (Material 3, Královská modrá a břidlicová paleta, Světlý/Tmavý/AMOLED režim, 17 poskytovatelů, Glance widgety) zůstává plně zachováno.
- Vylepšení se soustředí na vývojářskou zkušenost (DX) a bezproblémové CI/CD nasazení na GitHubu.

---

### 3. Key Product Decisions & Trade-Offs

- **Rozhodnutí 1: Mapování `secrets` přes job-level `env` kontext**
  - *Zvolený přístup*: Definice `KEYSTORE_BASE64`, `STORE_PASSWORD` a `KEY_PASSWORD` v sekci `env:` na úrovni jobu `build-and-test` a vyhodnocení podmínky `if: ${{ env.KEYSTORE_BASE64 != '' && env.STORE_PASSWORD != '' && env.KEY_PASSWORD != '' }}`.
  - *Proč*: Striktně odpovídá oficiální specifikaci GitHub Actions schématu, kde kontext `secrets` nelze číst přímo ve výrazu `if:` kroku.

- **Rozhodnutí 2: Automatická příprava `debug.keystore` a `.env` v CI**
  - *Zvolený přístup*: Krok v CI zkontroluje přítomnost `debug.keystore` (případně jej obnoví z `debug.keystore.base64` nebo vygeneruje přes `keytool`) a zajistí existenci `.env` ze šablony `.env.example` pro Secrets Gradle Plugin.
  - *Proč*: Zabraňuje selhání úlohy `:app:validateSigningDebug` a pluginu `com.google.android.libraries.mapsplatform.secrets-gradle-plugin` na čistém serveru po naklonování z GitHubu.

---

### 4. Technical Architecture & Data Strategy

#### Tok CI/CD Pipeline v GitHub Actions

```
┌──────────────────────────────────────────────────────────────────────────┐
│                   GitHub Actions Trigger (push / manual)                 │
└────────────────────────────────────┬─────────────────────────────────────┘
                                     ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ 1. Checkout & Prostředí                                                  │
│    - actions/checkout@v4                                                 │
│    - actions/setup-java@v4 (Temurin JDK 21)                              │
│    - Potvrzení Android SDK licencí & příprava .env + debug.keystore      │
│    - gradle/actions/setup-gradle@v4 (gradle-version: 9.3.1)              │
└────────────────────────────────────┬─────────────────────────────────────┘
                                     ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ 2. Testování & Debug Sestavení                                           │
│    - gradle :app:testDebugUnitTest (Unit + Robolectric UI testy)         │
│    - gradle :app:assembleDebug                                           │
│    - actions/upload-artifact@v4 -> kuryr-debug-apk                       │
└────────────────────────────────────┬─────────────────────────────────────┘
                                     ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ 3. Podmíněné Release Sestavení (if: env.KEYSTORE_BASE64 != '')           │
│    - Dekódování produkčního keystore ze Secretu                          │
│    - gradle :app:assembleRelease                                         │
│    - actions/upload-artifact@v4 -> kuryr-release-apk                     │
└──────────────────────────────────────────────────────────────────────────┘
```

#### Seznam kroků k provedení po schválení
1. Aktualizovat `.github/workflows/android-build.yml` s platným `env` kontextem pro `secrets`, explicitní verzí Gradle `9.3.1`, přípravou `debug.keystore` a `.env` a bezchybnou syntaxí YAML.
2. Odstranit kompilátorem hlášené varování o deprecaci `fallbackToDestructiveMigration()` v `KuryrDatabase.kt` nahrazením za `fallbackToDestructiveMigration(dropAllTables = true)`.
3. Ověřit kompilaci projektu (`compile_applet`) a platnost YAML workflow.
