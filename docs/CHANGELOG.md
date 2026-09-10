# Changelog

Alle nennenswerten Änderungen an diesem Projekt werden in dieser Datei dokumentiert.

Das Format orientiert sich an [Keep a Changelog](https://keepachangelog.com/de/1.1.0/).

Ab `1.0.0` gilt [Semantic Versioning](https://semver.org/lang/de/). Davor
verwendete das Projekt [datumsbasierte Versionen](#versionsschema); warum
gewechselt wurde, steht in [RELEASE.md](RELEASE.md#versionsnummern).

## [Unreleased]

## [1.0.0] - 2026-09-10

Stichwort: **Native Bereiche, iOS und Store-Vorbereitung**

### Hinzugefügt
- **Status-Bereich**: Serverzustand aus `GET /api/health/detailed` — Erreichbarkeit, Prozessor, Arbeitsspeicher und Platte als Balken, Datenbank, Agenten online, Logaufkommen. Aktualisiert sich alle 20 Sekunden, solange die Ansicht sichtbar ist, dazu Ziehen zum Neuladen.
- **Logs-Bereich**: native Liste statt Tabelle im WebView. Nachricht groß, Zeit/Host/Quelle klein darunter, Schweregrad als Farbstreifen, Trennzeile bei Tageswechsel. Volltextsuche, Filter nach Schweregrad-Gruppe und Logtyp (Werte kommen aus `GET /api/logs/filter-options`, also ohne App-Update erweiterbar), Nachladen beim Scrollen, Detailblatt mit Rohtext aus `GET /api/logs/{id}`.
- **Mail-Bereich**: Postfix-Zustand und Warteschlange aus dem optionalen `GET /api/mail/status`, Passwort-Reset über das optionale `POST /api/mail/password-reset`, dazu die letzten Mail-Logzeilen aus `GET /api/logs?category=mail`. Kennt der Server die `/api/mail`-Endpunkte nicht (404), zeigt der Bereich einen Hinweis statt einer Fehlermeldung und bleibt benutzbar. Kontrakt: [SERVER-API.md](SERVER-API.md#optional-apimail).
- **iOS-App** in `ios/`: SwiftUI ab iOS 16 mit denselben vier Bereichen, Schlüsselbund für die Zugangsdaten, App-Sperre über Face ID / Touch ID / Code. Das Xcode-Projekt entsteht aus `ios/project.yml` über XcodeGen und liegt nicht im Repository.
- **Apple- und Play-Kette**: [`ios-signing.yml`](../.github/workflows/ios-signing.yml) (einmalig, erzeugt Zertifikat und Profil über `fastlane match`), [`release.yml`](../.github/workflows/release.yml) (signiertes AAB, APK und IPA je Tag), [`deploy.yml`](../.github/workflows/deploy.yml) (Play-Track `internal` und TestFlight). Fehlen Secrets, überspringen sich die Schritte mit einer Warnung statt zu scheitern.
- **Prüfskripte** in `scripts/`: `check_apple_secrets.sh`, `check_match_repo.sh`, `run_fastlane.sh`, `ensure_ios_project.sh`, `extract_changelog.sh`.
- `LICENSE` (MIT) — die README nannte die Lizenz seit jeher, die Datei fehlte.
- Eigene README je Verzeichnis, dazu `docs/` mit Architektur, Server-Schnittstelle, Release, Store und Sicherheit.

### Geändert
- **Menüführung**: vier Bereiche in einer Leiste unten (Status, Logs, Mail, Weboberfläche) statt Vollbild-WebView. Bereiche werden einmal angelegt und danach nur ein- und ausgeblendet — Filter, Scrollposition und Web-Verlauf überleben den Wechsel, und es läuft keine Übergangsanimation bei jedem Tipp.
- **Repository-Struktur**: Der Gradle-Build liegt jetzt unter `android/`, daneben `ios/`, `docs/`, `scripts/`. Wer aus einem älteren Klon baut, ruft `./gradlew` ab sofort in `android/` auf.
- **Versionsschema** auf Semantic Versioning. Datumsbasierte Versionen sind für Apple ungültig (nur Ziffern und höchstens drei Punkte, sonst ITMS-90060).
- `versionName` und `versionCode` kommen aus der CI (`-Plogbot.versionName`, `-Plogbot.versionCode`); der `versionCode` ist `100 + Lauf-Nummer` und bleibt damit über der zuletzt von Hand gebauten 4.
- Zugangsdaten-Zugriff aus der `MainActivity` in `data/Credentials` herausgelöst; `SetupActivity` und `LogbotBridge` nutzen ihn mit.
- Die Versionsanzeige über der WebView ist weg — die Version steht im Überlaufmenü unter „Über Logbot".
- `build-debug.yml` heißt jetzt `ci.yml` und läuft bei jedem Push und jedem Pull Request, mit Unit-Tests, Lint und einem unsignierten iOS-Übersetzungslauf.
- Breite Geräte bekommen mehr Seitenrand statt eines zweiten Layouts (`values-w600dp`, `values-w840dp`).

### Entfernt
- `.idea/` aus der Versionsverwaltung — IDE-Einstellungen gehören nicht ins Repository.
- Eine PKCS#12-Datei mit dem Alias `logbot` lag in der Wurzel des Repositorys und ist entfernt. Sie bleibt in der Git-Historie und ist als kompromittiert zu behandeln — was zu tun ist, steht in [SICHERHEIT.md](SICHERHEIT.md#signaturschlüssel).

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
- GitHub-Actions-Workflow [`.github/workflows/build-debug.yml`](../.github/workflows/ci.yml) (inzwischen `ci.yml`): bei jedem Push auf `main`, jedem PR und auf manuellem Trigger wird `assembleDebug` ausgeführt und die APK 30 Tage als Artifact bereitgestellt.
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

**Ab `1.0.0`:** `MAJOR.MINOR.PATCH` nach [Semantic Versioning](https://semver.org/lang/de/), Vorabversionen als `1.1.0-beta.2`. Die Build-Nummer hängt die CI an.

**Bis `2026.05.29.22.30.00`:** `JAHR.MONAT.TAG.STUNDE.MINUTE.SEKUNDE` — markierte den Zeitpunkt, zu dem eine Release-fähige Codebasis vorlag (Lokalzeit Europe/Berlin). Aufgegeben, weil App Store Connect solche Versionen mit ITMS-90060 ablehnt.

[Unreleased]: https://github.com/Phydran6/Logbot-Android-App/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/Phydran6/Logbot-Android-App/compare/v2026.05.29.22.30.00...v1.0.0
[2026.05.29.22.30.00]: https://github.com/Phydran6/Logbot-Android-App/compare/v2026.05.29.21.28.36...v2026.05.29.22.30.00
[2026.05.29.21.28.36]: https://github.com/Phydran6/Logbot-Android-App/releases/tag/v2026.05.29.21.28.36
[2026.04.17.16.31.30]: https://github.com/Phydran6/Logbot-Android-App/releases/tag/app
[2026.04.11.13.38.42]: https://github.com/Phydran6/Logbot-Android-App/releases/tag/log
