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
- **Auth**: Instanz-URL (wird angepingt), **E-Mail-Link** (PKCE, Code fürs fremde Gerät), Passwort-Login
  (Benutzername oder E-Mail), MFA (TOTP/Backup-Code), QR-App-Login, Passwort vergessen,
  verschlüsselter Token-Speicher, zentrale 401-Abmeldung, **Biometrie-/PIN-Lock** (auch nach Hintergrund).
- **Übersicht**: Urteil, Geräte, Probleme, Meldungen, Server, KI-Schnellfragen. **Server-Zustand**: Balken, 20-s-Takt.
- **KI-Assistent**: Chat über `POST /api/ai/chat`, der Server prüft selbst die Logs; optional je Gerät.
- **Meldungen**: Liste + gelesen; Benachrichtigungen per UnifiedPush oder WorkManager-Abfrage (15 min).
- **Logs**: Cursor-Paging über `/api/app/logs`, Filter-Chips, Suche, Detailblatt, „KI fragen“.
- **Geräte**: Online-Zustand, Suche, Logs/KI/Entfernen. **Benutzer**, **Webhooks**, **Branding**: Admin.
- **Einstellungen**: App (Sperre, Benachrichtigungen, Server) + einfache Server-Werte (Admin).

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
- Lokal baubar mit JDK 17+ und Android-SDK (Plattform 36); verbindlich ist die CI.

## Wichtige Stolpersteine (gelöst)
- **AGP 9 = Built-in-Kotlin**: KEIN `org.jetbrains.kotlin.android`-Plugin anwenden.
- **KSP + Built-in-Kotlin**: `android.disallowKotlinSourceSets=false` in `gradle.properties`.
- **Compose-Compiler-Plugin** muss zur eingebauten Kotlin-Version passen (2.2.10).
- **Gradle-Kotlin-DSL**: `java.time` etc. **importieren**, nicht voll-qualifiziert inline nutzen.
- **JVM-Signatur-Clash**: Property `x` + Methode `setX(...)` kollidieren → Methoden anders
  benennen (`showX`, `toggleX`).

## Branches / Docs
- Rewrite auf Branch **`native-rewrite`**; **`main`** = letzter funktionierender WebView-Stand.
- Details: [ARCHITEKTUR.md](ARCHITEKTUR.md) · [ROADMAP.md](ROADMAP.md) · [CHANGELOG](CHANGELOG.md).

## Offen
Siehe [ROADMAP.md](ROADMAP.md#offen).
