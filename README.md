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

**Übersicht** — ein Satz, ob alles läuft („Alles in Ordnung“ / „2 Geräte
melden sich nicht“), dazu Geräte online, Probleme heute, offene Meldungen,
Zustand des Servers und Schnellfragen an die KI.

**KI-Assistent** — Fragen in Alltagssprache: „Ist im WLAN alles gut?“,
„Welche Geräte sind offline und warum?“. Der Server sieht selbst in den Logs
nach und antwortet mit Belegen — auf Wunsch nur für ein Gerät. Unter jeder
Antwort steht, wie viele Logzeilen dafür an die KI gingen. Angebunden wird
die KI auf dem Server: Claude, ChatGPT, OpenRouter, eine eigene KI über
Open WebUI oder ein n8n-Workflow.

**Meldungen und Push** — was der Server als „ansehen“ einstuft, landet in
den Meldungen und auf Wunsch als Benachrichtigung auf dem Handy: sofort per
Push über [UnifiedPush](https://unifiedpush.org) (z.B. mit der ntfy-App, ganz
ohne Google-Dienste) oder ohne Einrichtung alle 15 Minuten.

**Logs** — lesbare Liste mit Endlos-Scrollen, Filter nach Schweregrad, Gerät,
Zeitraum und Suchbegriff, Detailblatt mit zerlegten Feldern, Rohzeile und
„KI fragen“.

**Geräte** — alles, was Logs liefert, mit Online-Zustand; je Gerät dessen Logs,
die KI-Frage „Ist alles in Ordnung?“ und — für Administratoren — Entfernen.

**Verwaltung** — Benutzer, Webhooks, Branding und die einfachen
Server-Einstellungen für Administratoren. Was nur Administratoren dürfen,
blendet die App anhand der Rolle aus.

Überall Lade-, Fehler- und Leer-Zustände, Ziehen zum Aktualisieren, und
Fehlermeldungen im Wortlaut des Servers.

## Wie sie sich anfühlt

|  |  |
|:--|:--|
| **Nativ, nicht eingebettet** | Kein WebView mehr. Die Daten kommen über die REST-API |
| **Seitenleiste wie am Server** | Wer die Weboberfläche kennt, findet sich sofort zurecht |
| **Sicher verwahrt** | Token verschlüsselt auf dem Gerät, optionale Sperre per Fingerabdruck, Gesicht oder Code |
| **Ohne Google** | Push über UnifiedPush, Inhalt Ende-zu-Ende verschlüsselt; keine Telemetrie |
| **Hell und dunkel** | Material 3, folgt dem Gerät |

## Einrichten

Vorausgesetzt wird eine laufende
[Logbot-Server](https://github.com/Phydran6/Logbot-Server)-Instanz über
**https**.

1. App öffnen, die Adresse des Servers eintragen (`https://` darf fehlen)
2. **Anmelde-Link per E-Mail** antippen und die E-Mail-Adresse des Kontos
   eingeben — oder mit Benutzername/E-Mail und Passwort anmelden (mit MFA über
   TOTP oder Backup-Code, falls eingeschaltet)
3. Den Link aus der Mail auf dem Handy öffnen. Mail am Rechner gelesen? Dort
   den Code eingeben, den die App anzeigt — die App meldet sich von selbst an

Alternativ: In der Weboberfläche **App verbinden** öffnen und den QR-Code mit
der App scannen.

Der E-Mail-Link braucht auf dem Server eingerichteten Mailversand und eine
E-Mail-Adresse am Konto.

Instanz-URL und Token liegen danach verschlüsselt auf dem Gerät: unter Android
in `EncryptedSharedPreferences` mit Schlüssel im Android Keystore, unter iOS im
Schlüsselbund, gebunden an genau dieses Gerät.

In den Einstellungen der App lassen sich die **App-Sperre** (Fingerabdruck,
Gesicht oder Code beim Öffnen) und die **Benachrichtigungen** einschalten. Für
Sofort-Push eine UnifiedPush-Verteiler-App wie
[ntfy](https://play.google.com/store/apps/details?id=io.heckel.ntfy)
installieren; ohne sie sieht die App alle 15 Minuten nach.

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
