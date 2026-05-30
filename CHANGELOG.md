# Changelog

Alle nennenswerten Änderungen an diesem Projekt werden in dieser Datei dokumentiert.

Das Format orientiert sich an [Keep a Changelog](https://keepachangelog.com/de/1.1.0/),
das Projekt verwendet [datumsbasiertes Versioning](#versionsschema) im Schema
`JAHR.MONAT.TAG.STUNDE.MINUTE.SEKUNDE`.

## [Unreleased]

### Hinweis
- Ab [Logbot-Server v2026.05.30.17.22.26](https://github.com/Phydran6/Logbot-Server/releases) unterstützt der Server **MFA via TOTP** (Google Authenticator, Authy, Aegis, 1Password u. a.) sowie Backup-Codes. Die App benötigt dafür **keine Änderungen**: der zweistufige Login (Passwort → TOTP/Backup-Code) wird im WebView automatisch angezeigt. Der QR-Code-Setup-Flow ist weiterhin nutzbar, weil der App-Token nur aus einer bereits authentifizierten Web-Session erzeugt werden kann.

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
