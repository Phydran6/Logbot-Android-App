# Changelog

Alle nennenswerten Änderungen an diesem Projekt werden in dieser Datei dokumentiert.

Das Format orientiert sich an [Keep a Changelog](https://keepachangelog.com/de/1.1.0/),
das Projekt verwendet [datumsbasiertes Versioning](#versionsschema) im Schema
`JAHR.MONAT.TAG.STUNDE.MINUTE.SEKUNDE`.

## [Unreleased]

Stichwort: **Neuausrichtung – native App statt WebView**

Die App wird von einem WebView-Wrapper zu einer eigenständigen nativen App
(Jetpack Compose) umgebaut, die die Server-API direkt nutzt und die
Server-Oberfläche nativ nachbaut. Umsetzung in Phasen (siehe Roadmap).

### Hinzugefügt
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md): Bauplan der nativen App (Tech-Stack
  Compose/Hilt/Retrofit, Paketstruktur, MVVM+Repository, Navigation als Server-Sidebar,
  Auth-/Login-Flows inkl. korrektem QR-Token-Exchange, Screen↔API-Mapping).
- [`docs/ROADMAP.md`](docs/ROADMAP.md): Phasenplan (0 Discovery ✅, 1 Architektur ✅,
  2 Gerüst, 3a/3b Screens, 4 Feinschliff) mit Funktionsparitäts-Checkliste.

### Geändert
- README komplett überarbeitet: beschreibt die App jetzt eindeutig als **nativen
  Client für den Logbot-Server** (statt generischer „Log-App"-Beschreibung) inkl.
  Umbau-Status.

### Hinweise
- Architektur-Entscheidung: **Android-only mit Jetpack Compose** (KMP/iOS verworfen).
- Korrektur dokumentiert: Der QR-App-Login-Token ist ein einmaliger 15-Min-Token und
  muss über `POST /api/auth/app-token/exchange` gegen ein JWT getauscht werden — die
  bisherige WebView-Variante nutzte den gescannten Token fälschlich direkt als Bearer.

### Phase 2a – natives Skeleton (Branch `native-rewrite`)
- Build modernisiert: Kotlin-Android- und Compose-Compiler-Plugin ergänzt, Compose-BOM,
  Material 3, Navigation-Compose, Activity-/Lifecycle-Compose; Java/Kotlin-Target auf 17.
- **WebView entfernt**: `MainActivity` ist jetzt ein Single-Activity-Compose-Host mit
  Root-NavHost (Auth-Graph → Haupt-Graph). Gelöscht: `SetupActivity`, `LogbotBridge`,
  `activity_main.xml`, `activity_setup.xml` und die WebView-Proguard-Regeln.
- Material-3-Theme (`LogbotTheme`, Hell/Dunkel + dynamische Farben ab Android 12).
- **Navigations-Drawer als Server-Sidebar**: Dashboard, Logs, Agents, Users, Webhooks,
  Health, Einstellungen, Branding (Platzhalter-Screens) + Abmelden; Admin-Gating vorbereitet.
- Noch ohne Server-Anbindung — Login/MFA/QR und echte Inhalte folgen in 2b/Phase 3.

### Versionierung (automatisch)
- `versionCode`/`versionName` werden jetzt **automatisch aus Git** abgeleitet (kein
  manuelles Bumpen mehr): `versionCode` = Commit-Anzahl (monoton), `versionName` =
  Datum des letzten Commits (`JAHR.MONAT.TAG.STD.MIN.SEK`) + `-alpha` + Kurz-SHA.
  CI checkt dafür mit `fetch-depth: 0` aus.
- Version wird wieder in der App angezeigt (Setup-Screen unten + Drawer-Footer).
- Statische `versionName 2026.05.29.22.30.00` / `versionCode 4` entfernt (war der Stand
  der alten WebView-Release und damit im Rewrite irreführend).

### Phase 2b – Auth & Netzwerk (nativ)
- **DI/Netzwerk-Toolchain**: Hilt + KSP (statt kapt; `android.disallowKotlinSourceSets=false`
  für AGP-9-Built-in-Kotlin), Retrofit/OkHttp + kotlinx.serialization-Converter.
- **CredentialStore**: verschlüsselt (EncryptedSharedPreferences) – Instanz-URL, Token, Biometrie-Flag.
- **Netzwerk**: dynamische Instanz-URL via `HostSelectionInterceptor`, Bearer via `AuthInterceptor`.
- **Auth-Flow**: Setup (Instanz-URL **oder** QR-App-Login mit Token-Exchange) → Passwort-Login →
  **MFA** (TOTP/Backup-Code) → Hauptmenü. Rolle aus `/api/auth/me` (Admin-Gating), 401 → Logout.
- CI postet bei Build-Fehlern die Gradle-Fehlerursache als Commit-Kommentar (Diagnose).
- *Offen in 2b*: Biometrie-/PIN-App-Lock.

## [2026.05.29.22.30.00] - 2026-05-29

Stichwort: **App-Lock & Versionierung**

### Hinzugefügt
- App-Lock per Geräte-Biometrie/PIN via `BiometricPrompt` (`BIOMETRIC_STRONG` + `DEVICE_CREDENTIAL` als Fallback). Bei Abbruch wird die App geschlossen, ohne das Token zu entschlüsseln.
- JavaScript-Bridge `window.LogbotApp` für die Server-Web-UI mit `isBiometricAvailable()`, `isBiometricEnabled()`, `setBiometricEnabled(bool)`, `getAppVersion()`. Im normalen Browser nicht vorhanden — Settings-Toggle bleibt nur in der App sichtbar.
- Switch "App nach Anmeldung mit Biometrie sperren" im Setup-Screen (nur sichtbar, wenn das Gerät Biometrie oder PIN/Pattern enrolled hat).
- Dezente App-Versionsanzeige im Setup-Screen und als Overlay unten rechts in der WebView.
- `CHANGELOG.md` mit kompletter Historie ab dem ersten Push.
- GitHub-Actions-Workflow [`.github/workflows/build-debug.yml`](.github/workflows/build-debug.yml): bei jedem Push auf `main`, jedem PR und auf manuellem Trigger wird `assembleDebug` ausgeführt und die APK 30 Tage als Artifact bereitgestellt.
- `app/src/debug/res/values/strings.xml`: Debug-Variante erscheint im Launcher als "Logbot-Debug" und kann parallel zur Release-Variante installiert werden.

### Geändert
- `versionCode` 3 → 4, `versionName` auf `2026.05.29.22.30.00`.

### Behoben
- `app/build.gradle.kts` warf eine `NullPointerException` (`file(null)`) im Configure-Schritt, wenn `signing.properties` nicht vorhanden war — selbst bei `assembleDebug`. Die `release`-Signing-Config wird jetzt nur registriert, wenn die Properties-Datei existiert.

## [2026.05.29.21.28.36] - 2026-05-29

Stichwort: **App-Icon**

### Geändert
- App-Icon: aus `Logbot.png` neue Mipmap-Dichten (`mdpi` … `xxxhdpi`, square + round) als PNG erzeugt, Standard-Android-Roboter-WebPs entfernt.
- Adaptive Icon: neuer Foreground (`drawable-xxxhdpi/ic_launcher_foreground.png`, 432×432) und solider dunkler Background `#000820`. Der `monochrome`-Eintrag wurde aus dem Adaptive-Icon-XML entfernt, weil er bei der Color-PNG entstellt aussehen würde.

> Hinweis: Diese Release wurde retroaktiv auf den Icon-Commit `8722c6d` getagged. Die APK trägt intern noch `versionName 2026.04.17.16.31.30`, weil der eigentliche Versions-Bump erst mit der nächsten Release `2026.05.29.22.30.00` kam.

## [2026.04.17.16.31.30] - 2026-04-17

Stichwort: **Sicherheits-Hardening & Release-Signing**

### Hinzugefügt
- Release-Build-Signing über `signing.properties` (gitignored).
- HTTPS erzwungen (`MIXED_CONTENT_NEVER_ALLOW`), 401-Erkennung mit Setup-Reset, expliziter Hinweis im README zum fehlenden Certificate-Pinning.

### Geändert
- `androidx-security-crypto` auf `1.1.0-alpha06`, damit die `MasterKey`-API funktioniert.
- `versionName` auf `2026.04.17.16.31.30`.

## [2026.04.11.13.38.42] - 2026-04-11

Stichwort: **Initial Release**

### Hinzugefügt
- Setup-Screen mit URL- und Auth-Token-Eingabe oder QR-Code-Scan.
- Haupt-Activity mit WebView, die die Logbot-Web-UI mit `Authorization: Bearer …` lädt und das Token zusätzlich in `localStorage` injiziert.
- Verschlüsselte Speicherung von URL und Token über `EncryptedSharedPreferences` mit Android-Keystore-MasterKey.
- README mit Projekt-Beschreibung und Setup-Anleitung.

## Versionsschema

`JAHR.MONAT.TAG.STUNDE.MINUTE.SEKUNDE` — markiert den Zeitpunkt, zu dem eine Release-fähige Codebasis vorlag (Lokalzeit Europe/Berlin).

[Unreleased]: https://github.com/Phydran6/Logbot-Android-App/compare/v2026.05.29.22.30.00...HEAD
[2026.05.29.22.30.00]: https://github.com/Phydran6/Logbot-Android-App/compare/v2026.05.29.21.28.36...v2026.05.29.22.30.00
[2026.05.29.21.28.36]: https://github.com/Phydran6/Logbot-Android-App/releases/tag/v2026.05.29.21.28.36
[2026.04.17.16.31.30]: https://github.com/Phydran6/Logbot-Android-App/releases/tag/app
[2026.04.11.13.38.42]: https://github.com/Phydran6/Logbot-Android-App/releases/tag/log
