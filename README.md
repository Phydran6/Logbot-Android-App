<div align="center">

<img src="android/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="112" alt="Logbot">

# Logbot

### Der Blick auf deine Server, in der Hosentasche

Logbot ist die mobile Oberfläche zum
[Logbot-Server](https://github.com/Phydran6/Logbot-Server) — dem zentralen
Sammelpunkt für die Logs deiner Rechner, Container und Netzwerkgeräte.

<br>

[![Android](https://img.shields.io/badge/Android-APK_herunterladen-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Phydran6/Logbot-Android-App/releases/latest)
[![iOS](https://img.shields.io/badge/iOS-über_TestFlight-0D96F6?style=for-the-badge&logo=apple&logoColor=white)](docs/STORE.md)

[![CI](https://github.com/Phydran6/Logbot-Android-App/actions/workflows/ci.yml/badge.svg)](https://github.com/Phydran6/Logbot-Android-App/actions/workflows/ci.yml)
[![Lizenz](https://img.shields.io/badge/Lizenz-MIT-blue)](LICENSE)

<br>

**[Was sie kann](#was-sie-kann)** ·
**[Einrichten](#einrichten)** ·
**[Sicherheit](docs/SICHERHEIT.md)** ·
**[Technik](docs/README.md)** ·
**[Veröffentlichen](docs/STORE.md)**

</div>

---

Ein Logserver läuft, bis er es nicht mehr tut. Dann steht man irgendwo mit dem
Telefon in der Hand und will drei Dinge wissen: Läuft er noch? Was ist als
Letztes passiert? Und ging die Mail raus, auf die jemand wartet?

**Genau diese drei Fragen beantwortet Logbot** — jede einen Fingertipp
entfernt, ohne sich durch eine Weboberfläche zu hangeln, die für einen
Bildschirm gebaut ist, der doppelt so breit ist wie ein Telefon.

---

## Was sie kann

<table>
<tr>
<td width="50%" valign="top">

### Status

Läuft der Server, und wie geht es ihm dabei?

Prozessor, Arbeitsspeicher und Platte als Balken, dazu Datenbank, Agenten
online und das Logaufkommen der letzten 24 Stunden. Aktualisiert sich von
selbst, solange die Ansicht offen ist.

</td>
<td width="50%" valign="top">

### Logs

Die Einträge, lesbar auf einem Telefon.

Die Nachricht steht groß, Zeit, Host und Quelle klein darunter, der
Schweregrad ist ein Farbstreifen am Rand. Suchen, nach Stufe und Logtyp
filtern, beim Scrollen lädt die nächste Seite nach.

</td>
</tr>
<tr>
<td width="50%" valign="top">

### Mail und Postfix

Ob Postfix läuft, was in der Warteschlange hängt — und der Knopf, den man
sonst am Telefon nie findet: **Passwort zurücksetzen**.

Darunter die letzten Mail-Logzeilen, damit man sieht, ob die Mail wirklich
rausging.

</td>
<td width="50%" valign="top">

### Weboberfläche

Alles Übrige — Benutzer, Webhooks, Branding, Updates — bleibt die volle
Weboberfläche des Servers, eingebettet und abgesichert.

Nichts wird doppelt gebaut, was der Server schon kann.

</td>
</tr>
</table>

---

## Wie sie sich anfühlt

|  |  |
|:--|:--|
| **Vier Bereiche, eine Leiste** | Status, Logs, Mail, Web. Kein ausklappbares Menü, kein Suchen |
| **Zustand bleibt** | Filter, Scrollposition und Web-Verlauf überleben den Wechsel zwischen den Bereichen |
| **Wenig Bewegung** | Keine Übergangsanimation bei jedem Tipp. Was sich bewegt, hat einen Grund |
| **Breite nutzt Luft** | Auf Tablets wird der Rand größer, nicht die Textzeile länger |
| **Hell und dunkel** | Folgt dem Gerät. Nur die Schweregrad-Farben bleiben fest, weil sie Bedeutung tragen |

---

## Einrichten

Man braucht eine laufende [Logbot-Server](https://github.com/Phydran6/Logbot-Server)-Instanz
über **https**.

1. In der Weboberfläche des Servers anmelden
2. Dort **App verbinden** öffnen — der Server zeigt einen QR-Code
3. In der App **QR-Code scannen** antippen (Android) bzw. den QR-Inhalt
   einfügen (iOS)

Fertig. Instanz-URL und Token liegen danach verschlüsselt auf dem Gerät —
unter Android in `EncryptedSharedPreferences` mit Schlüssel im Android
Keystore, unter iOS im Schlüsselbund, gebunden an genau dieses Gerät.

Wer will, schaltet dabei die **App-Sperre** ein: Beim Start fragt das Gerät
nach Fingerabdruck, Gesicht oder Code, bevor überhaupt ein Token entschlüsselt
wird.

---

## Herunterladen

| | Wo | Hinweis |
|:--|:--|:--|
| **Android** | [APK im letzten Release](https://github.com/Phydran6/Logbot-Android-App/releases/latest) | Antippen, installieren, fertig |
| **Play Store** | in Vorbereitung — [Stand und Schritte](docs/STORE.md) | Das AAB im Release ist zum Hochladen gedacht |
| **iOS** | über TestFlight — [Stand und Schritte](docs/STORE.md) | Entwicklerkonto liegt vor, Einrichtung läuft |
| **Jeder Commit** | [Debug-APK aus der CI](https://github.com/Phydran6/Logbot-Android-App/actions/workflows/ci.yml) | Installierbar neben der Release-Fassung |
| **Was ist neu** | [Changelog](docs/CHANGELOG.md) | Jede Änderung, nachvollziehbar |

---

## Aufbau des Repositorys

Ein Verzeichnis je Sache, jedes mit eigener README.

| Verzeichnis | Was drin liegt | |
|:--|:--|:--|
| `android/` | Die Android-App: Kotlin, Gradle, Ressourcen | [README](android/README.md) |
| `ios/` | Die iOS-App: SwiftUI, XcodeGen, fastlane | [README](ios/README.md) |
| `docs/` | Architektur, Server-Schnittstelle, Release, Stores, Sicherheit | [README](docs/README.md) |
| `scripts/` | Helfer für Release und Signatur | [README](scripts/README.md) |
| `.github/` | Die vier Abläufe: CI, Release, Deploy, Signatur | [README](.github/README.md) |

---

## Mitmachen

Rückmeldungen und Pull Requests sind willkommen. Bei größeren Vorhaben vorher
kurz ein Issue aufmachen.

[Fehler melden](https://github.com/Phydran6/Logbot-Android-App/issues) ·
[Sicherheitshinweise](docs/SICHERHEIT.md) ·
[Server-Schnittstelle](docs/SERVER-API.md)

<br>

<div align="center">

### [→ Technische Dokumentation](docs/README.md)

<sub>Aufbau, Schnittstelle, Release-Prozess, Veröffentlichung</sub>

<br><br>

<sub><a href="LICENSE">MIT-Lizenz</a> · Logbot-Server: <a href="https://github.com/Phydran6/Logbot-Server">Phydran6/Logbot-Server</a></sub>

</div>
