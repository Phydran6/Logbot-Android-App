<div align="center">

<img src="docs/assets/logo.png" width="96" alt="">

# Logbot

**Der Blick auf deine Server, in der Hosentasche**

[![Android](https://img.shields.io/badge/Android-APK-3DDC84?logo=android&logoColor=white)](https://github.com/Phydran6/Logbot-Android-App/releases/latest)
[![iOS](https://img.shields.io/badge/iOS-TestFlight-0D96F6?logo=apple&logoColor=white)](docs/STORE.md)
[![CI](https://github.com/Phydran6/Logbot-Android-App/actions/workflows/ci.yml/badge.svg)](https://github.com/Phydran6/Logbot-Android-App/actions/workflows/ci.yml)
[![Lizenz](https://img.shields.io/badge/Lizenz-MIT-blue)](LICENSE)

[Was sie kann](#was-sie-kann) ·
[Einrichten](#einrichten) ·
[Herunterladen](#herunterladen) ·
[Technik](docs/README.md) ·
[Sicherheit](docs/SICHERHEIT.md)

</div>

---

Logbot ist die mobile Oberfläche zum
[Logbot-Server](https://github.com/Phydran6/Logbot-Server) — dem zentralen
Sammelpunkt für die Logs deiner Rechner, Container und Netzwerkgeräte.

Ein Logserver läuft, bis er es nicht mehr tut. Dann steht man irgendwo mit dem
Telefon in der Hand und will wissen: Läuft er noch? Was ist als Letztes
passiert? Wer hängt noch dran?

Früher war die App dafür ein Rahmen um die Weboberfläche — praktisch gebaut,
aber am Telefon unbrauchbar, weil diese Oberfläche für einen doppelt so
breiten Bildschirm gedacht ist. Inzwischen ist sie **eine eigenständige native
App**: Sie holt die Daten über die REST-API und bildet die Bereiche des Servers
selbst ab.

## Was sie kann

Die App bildet die Oberfläche des Servers nativ nach — dieselbe Seitenleiste,
dieselben Bereiche, nur für den Daumen gebaut statt für die Maus.

**Dashboard und Health** — Log-Statistiken auf einen Blick, dazu Prozessor,
Arbeitsspeicher, Platte, Laufzeit und Datenbank-Zustand.

**Logs** — Liste mit Filtern nach Suchbegriff, Level, Host und Quelle,
Detailansicht und Nachladen. Die Einträge sind auf einem Telefon lesbar, nicht
eine Tabelle mit acht Spalten.

**Agents, Users, Webhooks** — Geräte einsehen und löschen, Benutzer anlegen,
ändern, löschen und MFA zurücksetzen, Webhooks samt Aufruf-URL und Token
verwalten. Was nur Administratoren dürfen, blendet die App anhand der Rolle aus.

**Settings und Branding** — Server-Einstellungen bearbeiten, Datenbank-Infos
einsehen, Whitelabel-Einstellungen pflegen.

Überall Lade-, Fehler- und Leer-Zustände mit einem Knopf zum Wiederholen.

## Wie sie sich anfühlt

|  |  |
|:--|:--|
| **Nativ, nicht eingebettet** | Kein WebView mehr. Die Daten kommen über die REST-API |
| **Seitenleiste wie am Server** | Wer die Weboberfläche kennt, findet sich sofort zurecht |
| **Sicher verwahrt** | Token verschlüsselt auf dem Gerät, optionale Sperre per Fingerabdruck, Gesicht oder Code |
| **Hell und dunkel** | Material 3, folgt dem Gerät |

## Einrichten

Vorausgesetzt wird eine laufende
[Logbot-Server](https://github.com/Phydran6/Logbot-Server)-Instanz über
**https**.

1. In der Weboberfläche des Servers anmelden
2. Dort **App verbinden** öffnen — der Server zeigt einen QR-Code
3. In der App **QR-Code scannen** antippen (Android) oder den QR-Inhalt
   einfügen (iOS)

Alternativ geht die Anmeldung direkt in der App: Instanz-URL eintragen, dann
Benutzername und Passwort — mit MFA über TOTP oder Backup-Code, falls der
Server das verlangt.

Instanz-URL und Token liegen danach verschlüsselt auf dem Gerät: unter Android
in `EncryptedSharedPreferences` mit Schlüssel im Android Keystore, unter iOS im
Schlüsselbund, gebunden an genau dieses Gerät.

Wer mag, schaltet dabei die **App-Sperre** ein. Dann fragt das Gerät beim Start
nach Fingerabdruck, Gesicht oder Code, bevor überhaupt ein Token entschlüsselt
wird.

## Herunterladen

| | Wo | Hinweis |
|:--|:--|:--|
| **Android** | [letztes Release](https://github.com/Phydran6/Logbot-Android-App/releases/latest) | Antippen, installieren, fertig — sobald das Paket signiert ist, siehe unten |
| **Play Store** | in Vorbereitung, [Schritte](docs/STORE.md) | Das AAB im Release ist zum Hochladen gedacht |
| **iOS** | über TestFlight, [Schritte](docs/STORE.md) | Entwicklerkonto liegt vor, Einrichtung läuft |
| **Jeder Commit** | [Debug-APK aus der CI](https://github.com/Phydran6/Logbot-Android-App/actions/workflows/ci.yml) | Installierbar neben der Release-Fassung |
| **Änderungen** | [Changelog](docs/CHANGELOG.md) | Jede Version, nachvollziehbar |

> [!NOTE]
> Solange die Signatur-Secrets im Repository fehlen, heißt das Paket im Release
> `…-android-testsignatur.apk`. Es **lässt sich installieren** — es ist ein
> normaler Release-Build, nur mit der Debug-Signatur statt einer eigenen.
>
> Zwei Dinge kann es nicht: in den Play Store, und ein früheres Release
> ersetzen (die Debug-Signatur entsteht bei jedem Bau neu, Android verweigert
> dann das Überschreiben — einmal deinstallieren genügt). Sobald die vier
> Secrets aus [docs/RELEASE.md](docs/RELEASE.md#github-secrets) hinterlegt sind,
> greift beim nächsten Lauf automatisch die echte Signatur und die Datei heißt
> wieder `…-android.apk`.

## Aufbau des Repositorys

Ein Verzeichnis je Sache, jedes mit eigener README.

| Verzeichnis | Inhalt | |
|:--|:--|:--|
| `android/` | Die Android-App: Kotlin, Gradle, Ressourcen | [README](android/README.md) |
| `ios/` | Die iOS-App: SwiftUI, XcodeGen, fastlane | [README](ios/README.md) |
| `docs/` | Architektur, Server-Schnittstelle, Release, Stores, Sicherheit | [README](docs/README.md) |
| `scripts/` | Helfer für Release und Signatur | [README](scripts/README.md) |
| `.github/` | Die Abläufe: CI, Release, Deploy, Signatur | [Übersicht](.github/ABLAEUFE.md) |

## Mitmachen

Rückmeldungen und Pull Requests sind willkommen. Bei größeren Vorhaben vorher
kurz ein Issue aufmachen.

[Fehler melden](https://github.com/Phydran6/Logbot-Android-App/issues) ·
[Server-Schnittstelle](docs/SERVER-API.md) ·
[Sicherheitshinweise](docs/SICHERHEIT.md)

<div align="center">
<sub>

[MIT-Lizenz](LICENSE) · Server:
[Phydran6/Logbot-Server](https://github.com/Phydran6/Logbot-Server)

</sub>
</div>
