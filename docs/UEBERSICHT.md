# Logbot Android – Übersicht

Kompakte Doku zum Umbau vom WebView-Wrapper zur eigenständigen nativen App.

## Was es ist
Nativer Android-Client für den selbst gehosteten [Logbot-Server](https://github.com/Phydran6/Logbot-Server)
(FastAPI + PostgreSQL). Holt Daten über die REST-API und bildet die Server-Oberfläche
1:1 nativ ab. **Der frühere WebView-Wrapper ist vollständig ersetzt.**

## Status
Funktional vollständig, CI-grün. Alle Server-Screens nativ umgesetzt.

## Tech-Stack
- Kotlin · Jetpack Compose · Material 3 · Single-Activity
- Navigation-Compose · **Hilt** (DI) · **KSP**
- **Retrofit/OkHttp** + **kotlinx.serialization**
- EncryptedSharedPreferences (Token) · androidx.biometric
- Build: AGP 9.1 mit **Built-in-Kotlin** · Gradle 9.3 · JDK 17/21 · minSdk 24 / targetSdk 36

## Architektur (pro Screen)
`DTO (@Serializable)` → `Retrofit-API` (in `NetworkModule`) → `Repository` (`safeApiCall` → `UiState`)
→ `@HiltViewModel` (Compose-`mutableStateOf`) → `Screen` (`when(UiState){Loading/Error/Success}`),
Route im `MainShell`-NavHost (Drawer = Server-Sidebar, Admin-Gating aus `/api/auth/me`).

Paketstruktur: `core/{network,auth,security,designsystem,navigation,ui}`,
`data/{api,repository}`, `feature/<screen>`.

## Funktionen
- **Auth**: Instanz-URL, Passwort-Login, MFA (TOTP/Backup-Code), QR-App-Login (Token-Exchange),
  verschlüsselter Token-Speicher, 401→Logout, **Biometrie-/PIN-Lock**.
- **Dashboard**: Log-Statistiken. **Health**: CPU/RAM/Disk/Uptime/DB.
- **Logs**: Liste + Filter (Suche/Level/Host/Source) + Detail + „Mehr laden".
- **Agents**: Liste + Löschen. **Users**: CRUD + MFA-Reset (Admin).
- **Webhooks**: CRUD + Aufruf-URL (kopierbar) + Token neu.
- **Settings**: Werte editieren + DB-Infos. **Branding**: Whitelabel bearbeiten.
- Überall Lade-/Fehler-/Leer-Zustände mit Retry.

## Versionierung
Automatisch, kein manuelles Bumpen:
- `versionCode` = Git-Commit-Anzahl.
- **Vorschau-Builds**: `JAHR.MONAT.TAG.STD.MIN.SEK-alpha.<sha>`.
- **Finale Release**: sauberer `JAHR.MONAT.TAG.STD.MIN.SEK` (ohne `-alpha`).
- In der App sichtbar (Setup-Screen + Drawer-Footer).

## Build & Download (GitHub Actions, `build-debug.yml`)
- Jeder Push auf `native-rewrite` → Build + **rollende Vorschau-Release** (Tag `native-latest`).
- **Finale Release**: Git-Tag `v<JAHR.MONAT.TAG.STD.MIN.SEK>` pushen → sauber versioniertes Release
  (alternativ Actions → „Build & Release" → `do_release=true`).
- APK-Name: `Logbot-<version>.apk`. Download über die Releases-Seite.
- **Diagnose**: Bei Build-Fehler postet die CI die Gradle-Fehlerursache als Commit-Kommentar
  (lesbar über die öffentliche API).
- Lokal nicht baubar in der Entwicklungsumgebung (Java 8, kein SDK) → Verifikation läuft über CI.

## Wichtige Stolpersteine (gelöst)
- **AGP 9 = Built-in-Kotlin**: KEIN `org.jetbrains.kotlin.android`-Plugin anwenden.
- **KSP + Built-in-Kotlin**: `android.disallowKotlinSourceSets=false` in `gradle.properties`.
- **Compose-Compiler-Plugin** muss zur eingebauten Kotlin-Version passen (2.2.10).
- **Gradle-Kotlin-DSL**: `java.time` etc. **importieren**, nicht voll-qualifiziert inline nutzen.
- **JVM-Signatur-Clash**: Property `x` + Methode `setX(...)` kollidieren → Methoden anders
  benennen (`showX`, `toggleX`).

## Branches / Docs
- Rewrite auf Branch **`native-rewrite`**; **`main`** = letzter funktionierender WebView-Stand.
- Details: [ARCHITECTURE.md](ARCHITECTURE.md) · [ROADMAP.md](ROADMAP.md) · [CHANGELOG](../CHANGELOG.md).

## Offen (optionale Kür)
Offline-Caching, Pull-to-refresh, Certificate-Pinning, Agent-Retention-UI,
Branding-Logo-Upload, Settings-Retention-Aktionen, Logs-Endlos-Scroll.
