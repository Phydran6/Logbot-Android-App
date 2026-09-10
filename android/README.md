# android/ — die Android-App

Kotlin, Gradle, Material 3. Kein Compose, keine Netzwerk-Bibliothek: Die App
kommt mit dem aus, was im System liegt.

[← Zurück zur Übersicht](../README.md) ·
[Architektur](../docs/ARCHITEKTUR.md) ·
[Server-Schnittstelle](../docs/SERVER-API.md)

---

## Bauen

```bash
cd android
./gradlew assembleDebug
```

Das APK liegt danach unter `app/build/outputs/apk/debug/app-debug.apk`. Die
Debug-Fassung heißt im Launcher **Logbot-Debug** und lässt sich neben der
Release-Fassung installieren.

Ein Release-Build braucht eine `signing.properties` neben dieser Datei — siehe
[Release](../docs/RELEASE.md#android-signatur). Fehlt sie, wird die
Signatur-Konfiguration gar nicht erst registriert und `assembleDebug` läuft
trotzdem.

---

## Wo was liegt

| Pfad | Inhalt |
|:--|:--|
| `app/src/main/java/…/MainActivity.kt` | Rahmen: App-Sperre, Titelleiste, vier Bereiche |
| `app/src/main/java/…/SetupActivity.kt` | Erste Einrichtung: URL, Token, QR-Scan |
| `app/src/main/java/…/LogbotBridge.kt` | `window.LogbotApp` für die Weboberfläche |
| `app/src/main/java/…/data/` | Zugangsdaten, HTTP-Client, Datentypen |
| `app/src/main/java/…/ui/` | Die vier Bereiche und die Listendarstellung |
| `app/src/main/res/layout/` | Layouts |
| `app/src/main/res/values*/` | Texte, Farben, Ränder — je Variante |
| `fastlane/` | Play-Store-Upload (`internal`, `beta`, `production`) |

---

## Die vier Bereiche

| Bereich | Klasse | Quelle |
|:--|:--|:--|
| Status | `ui/StatusFragment.kt` | `GET /api/health/detailed`, alle 20 s |
| Logs | `ui/LogsFragment.kt` | `GET /api/logs` mit Filtern, seitenweise |
| Mail | `ui/MailFragment.kt` | `GET /api/mail/status` (optional) + `GET /api/logs?category=mail` |
| Web | `ui/WebFragment.kt` | Die Weboberfläche des Servers |

Bereiche werden einmal angelegt und danach nur ein- und ausgeblendet. Deshalb
überleben Filter, Scrollposition und Web-Verlauf einen Wechsel — und es läuft
keine Übergangsanimation bei jedem Tipp.

---

## Entscheidungen, die Erklärung verdienen

**Kein Retrofit, kein OkHttp.** Es sind fünf GET-Aufrufe gegen genau eine
Instanz. `HttpURLConnection` und `org.json` liegen im System; das spart rund
ein Megabyte APK und eine Abhängigkeit, die gepflegt werden müsste. Der
Client steht in `data/LogbotApi.kt` und ist knapp 200 Zeilen lang.

**Kein Coroutines-Zusatz.** Ein kleiner Thread-Pool mit Rückmeldung auf dem
Hauptthread reicht für das, was hier passiert.

**Kein eigenes Tablet-Layout.** Ab 600 dp und ab 840 dp Breite wächst nur der
Seitenrand (`res/values-w600dp/`, `res/values-w840dp/`). Eine Textzeile über
die volle Breite eines Tablets liest sich schlecht; eine Spalte mit Luft
daneben liest sich gut.

**Version kommt aus der CI.** `versionName` und `versionCode` lassen sich per
`-Plogbot.versionName` und `-Plogbot.versionCode` setzen. Lokale Builds nehmen
die Werte aus `app/build.gradle.kts`. So muss niemand von Hand hochzählen.

---

## Abhängigkeiten

Alle Versionen stehen in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

| Bibliothek | Wofür |
|:--|:--|
| `androidx.appcompat`, `material` | Oberfläche, Material 3 |
| `androidx.security-crypto` | Verschlüsselte Zugangsdaten |
| `androidx.biometric` | App-Sperre |
| `androidx.recyclerview`, `swiperefreshlayout` | Logliste, Ziehen zum Neuladen |
| `zxing-android-embedded` | QR-Scan bei der Einrichtung |

---

## Berechtigungen

| Berechtigung | Wofür | Pflicht |
|:--|:--|:--|
| `INTERNET` | Zugriff auf die eigene Instanz | ja |
| `CAMERA` | QR-Code bei der Einrichtung | nein — ohne Kamera geht die Eingabe von Hand |

Mehr Berechtigungen fragt die App nicht an. Kein Standort, keine Kontakte,
keine Benachrichtigungen.
