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
Telefon in der Hand und will drei Dinge wissen: Läuft er noch? Was ist als
Letztes passiert? Und ging die Mail raus, auf die jemand wartet?

Genau diese drei Fragen beantwortet die App — jede einen Fingertipp entfernt,
statt sich durch eine Weboberfläche zu hangeln, die für einen doppelt so
breiten Bildschirm gebaut ist.

## Was sie kann

Vier Bereiche, eine Leiste unten.

**Status** — Läuft der Server, und wie geht es ihm dabei? Prozessor,
Arbeitsspeicher und Platte als Balken, dazu Datenbank, Agenten online und das
Logaufkommen der letzten 24 Stunden. Aktualisiert sich von selbst, solange die
Ansicht offen ist.

**Logs** — Die Einträge, lesbar auf einem Telefon. Die Nachricht steht groß,
Zeit, Host und Quelle klein darunter, der Schweregrad ist ein Farbstreifen am
Rand. Suchen, nach Stufe und Logtyp filtern, beim Scrollen lädt die nächste
Seite nach.

**Mail** — Ob Postfix läuft, was in der Warteschlange hängt, und der Knopf,
den man am Telefon sonst nie findet: Passwort zurücksetzen. Darunter die
letzten Mail-Logzeilen, damit man sieht, ob die Mail wirklich rausging.

**Weboberfläche** — Alles Übrige (Benutzer, Webhooks, Branding, Updates)
bleibt die volle Weboberfläche des Servers, eingebettet und abgesichert.
Nichts wird doppelt gebaut, was der Server schon kann.

## Wie sie sich anfühlt

| | |
|:--|:--|
| **Vier Bereiche, eine Leiste** | Kein ausklappbares Menü, kein Suchen |
| **Zustand bleibt** | Filter, Scrollposition und Web-Verlauf überleben den Wechsel |
| **Wenig Bewegung** | Keine Übergangsanimation bei jedem Tipp |
| **Breite nutzt Luft** | Auf Tablets wächst der Rand, nicht die Textzeile |
| **Hell und dunkel** | Folgt dem Gerät; nur die Schweregrad-Farben bleiben fest |

## Einrichten

Vorausgesetzt wird eine laufende
[Logbot-Server](https://github.com/Phydran6/Logbot-Server)-Instanz über
**https**.

1. In der Weboberfläche des Servers anmelden
2. Dort **App verbinden** öffnen — der Server zeigt einen QR-Code
3. In der App **QR-Code scannen** antippen (Android) oder den QR-Inhalt
   einfügen (iOS)

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
| `.github/` | Die Abläufe: CI, Release, Deploy, Signatur | [README](.github/README.md) |

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
