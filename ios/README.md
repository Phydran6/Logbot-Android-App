# ios/ — die iOS-App

SwiftUI, ab iOS 16. Gleiche vier Bereiche, gleiche Schnittstelle, gleiche
Farben wie unter Android — wer beide Geräte benutzt, soll nicht umdenken
müssen.

[← Zurück zur Übersicht](../README.md) ·
[Veröffentlichen](../docs/STORE.md) ·
[Release-Prozess](../docs/RELEASE.md)

---

## Bauen

Das Xcode-Projekt liegt **nicht** im Repository. Es entsteht aus
[`project.yml`](project.yml):

```bash
bash scripts/ensure_ios_project.sh
open ios/Logbot.xcodeproj
```

Das Skript holt sich XcodeGen per Homebrew, falls es fehlt.

> **Warum generiert?** Eine `project.pbxproj` wird von Xcode bei jeder
> Änderung neu durchgewürfelt. Im Repository erzeugt sie Konflikte, die
> niemand sinnvoll auflösen kann. Die YAML-Datei ist lesbar und vergleichbar.

Ein Mac ist zum **Veröffentlichen** nicht nötig — alle Builds laufen auf dem
macOS-Runner von GitHub Actions. Zum Entwickeln schon.

---

## Wo was liegt

| Datei | Inhalt |
|:--|:--|
| `Logbot/LogbotApp.swift` | Einstieg, App-Sperre, Tab-Leiste |
| `Logbot/SetupView.swift` | Erste Einrichtung |
| `Logbot/StatusView.swift` | Serverzustand |
| `Logbot/LogsView.swift` | Logliste, Filter, Detailblatt |
| `Logbot/MailView.swift` | Postfix-Zustand, Passwort-Reset, Mail-Logzeilen |
| `Logbot/WebPane.swift` | `WKWebView` mit der Weboberfläche |
| `Logbot/LogbotApi.swift` | REST-Zugriff und Datentypen |
| `Logbot/Credentials.swift` | Schlüsselbund |
| `Logbot/Theme.swift` | Farben und Zeitformate |
| `project.yml` | Der Bauplan für das Xcode-Projekt |
| `ExportOptions.plist` | Export-Einstellungen für den App-Store-Build |
| `fastlane/` | Signatur, TestFlight, Store-Einreichung |

---

## Unterschiede zur Android-Fassung

| | Android | iOS |
|:--|:--|:--|
| Zugangsdaten | `EncryptedSharedPreferences` | Schlüsselbund, `ThisDeviceOnly` |
| App-Sperre | `BiometricPrompt` | `LAContext`, Face ID / Touch ID / Code |
| Einrichtung | QR-Scan mit der Kamera | QR-Inhalt einfügen |
| Weboberfläche | `WebView` mit JS-Brücke | `WKWebView`, ohne Brücke |

**Kein QR-Scan auf iOS.** Der Kamera-Zugriff wäre eine Berechtigung mehr und
ein Punkt mehr im App-Review, für einen Token, den man genauso gut einfügt.
Der eingefügte QR-Inhalt (`{"url":"…","token":"…"}`) wird erkannt und
auseinandergenommen.

**Keine JS-Brücke.** Unter Android kann die Weboberfläche über
`window.LogbotApp` die App-Sperre umschalten. Auf iOS ist der Schalter in den
Einstellungen der App — eine Brücke wäre für diese eine Einstellung zu viel
Angriffsfläche.

---

## Signatur

Läuft über `fastlane match`: Zertifikat und Profil liegen verschlüsselt in
einem privaten Repository, die CI holt sie sich vor jedem Build.

Einmalig einrichten: `Actions → iOS Signatur einrichten → Run workflow`.
Die vollständige Reihenfolge steht in [docs/STORE.md](../docs/STORE.md).

Bundle-ID: `de.phytech.logbot` — dieselbe wie das Android-Paket.
