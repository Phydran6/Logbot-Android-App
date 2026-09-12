# Veröffentlichen: Play Store und App Store

Was im Repository steht, ist fertig. Was hier steht, muss ein Mensch klicken —
Apple und Google lassen sich beides nicht per Skript anlegen.

**Ein eigener Mac ist nicht nötig.** Alle Builds laufen auf dem
macOS-Runner von GitHub Actions.

[← Doku-Übersicht](README.md) ·
[Release-Prozess](RELEASE.md) ·
[Abläufe](../.github/README.md)

---

## Stand

| | |
|:--|:--|
| **Android, direkt** | läuft — APK und AAB liegen in jedem Release |
| **Google Play** | vorbereitet, wartet auf Konto und ersten Upload von Hand |
| **Apple / TestFlight** | vorbereitet, Entwicklerkonto liegt vor, Secrets fehlen noch |

---

## Was schon fertig ist

| | |
|:--|:--|
| Bundle-ID / Package | `de.phytech.logbot`, auf beiden Plattformen gleich |
| Signatur-Ablauf iOS | [`ios-signing.yml`](../.github/workflows/ios-signing.yml), einmalig per Knopfdruck |
| Release-Build | [`release.yml`](../.github/workflows/release.yml), signiertes AAB, APK und IPA bei jedem Tag |
| Verteilung | [`deploy.yml`](../.github/workflows/deploy.yml), Play-Track `internal` und TestFlight |
| Export-Einstellungen | [`ios/ExportOptions.plist`](../ios/ExportOptions.plist) |
| Exportregelung | `ITSAppUsesNonExemptEncryption = false` in der `Info.plist` |
| Face-ID-Text | `NSFaceIDUsageDescription`, ohne den lehnt Apple ab |
| App-Icon 1024×1024 | im Asset-Katalog, ohne Transparenz |
| Play-Lanes | [`android/fastlane/Fastfile`](../android/fastlane/Fastfile): `internal`, `promote_beta`, `promote_production` |

---

## Google Play

### 1. App anlegen

Play Console → App erstellen, Package **`de.phytech.logbot`**.

### 2. Erstes AAB von Hand hochladen

Google verlangt das beim ersten Mal. Die Datei liegt im GitHub-Release als
`Logbot-<version>-android-playstore.aab`.

### 3. Service-Account einrichten

Google Cloud → Service-Account anlegen → in der Play Console unter
**Nutzer und Berechtigungen** freischalten → JSON-Schlüssel herunterladen →
als Secret `PLAY_SERVICE_ACCOUNT_JSON` eintragen.

Ab dann lädt [`deploy.yml`](../.github/workflows/deploy.yml) nach jedem
Release automatisch in den Track `internal` — als **Entwurf**. Der letzte
Klick bleibt beim Menschen, auch intern.

### 4. Was nur von Hand geht

**Datensicherheits-Erklärung.** Antworten, die zum Code passen:

| Frage | Antwort |
|:--|:--|
| Werden Daten erfasst oder geteilt? | **Nein** |
| Daten bei der Übertragung verschlüsselt? | Ja — ausschließlich https |
| Können Nutzer Löschung verlangen? | Nicht zutreffend, es werden keine Daten erhoben |

Die App sendet nichts an Dritte. Was sie überträgt, geht ausschließlich an die
Instanz, die der Nutzer selbst eingetragen hat. Es gibt keine Analytik, keine
Werbe-IDs, keine Absturzberichte.

**Alterseinstufung:** überall „Nein". Erwartetes Ergebnis: **USK 0 / 3+**.

**Kategorie:** Tools. **Preis:** kostenlos.

---

## Apple: die Reihenfolge

### 1. Secrets in GitHub setzen

`Settings → Secrets and variables → Actions → New repository secret`

| Secret | Woher |
|:--|:--|
| `APPSTORE_API_KEY_ID` | App Store Connect → Users and Access → Integrations |
| `APPSTORE_API_ISSUER_ID` | dieselbe Seite, oben |
| `APPSTORE_API_PRIVATE_KEY` | die `.p8`-Datei, **base64-kodiert** |
| `MATCH_PASSWORD` | frei wählbar — verschlüsselt die Zertifikate |
| `MATCH_GIT_URL` | HTTPS-URL des privaten Zertifikats-Repositorys |
| `MATCH_GIT_TOKEN` | Token mit `repo`-Recht, damit die CI hineinkommt |

Die `.p8` kodieren, Ergebnis landet in der Zwischenablage:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("AuthKey_XXXXXXXXXX.p8")) | Set-Clipboard
```

### 2. Team-ID prüfen

In [`ios/project.yml`](../ios/project.yml),
[`ios/ExportOptions.plist`](../ios/ExportOptions.plist) und
[`ios/fastlane/Appfile`](../ios/fastlane/Appfile) steht `352N56D836`.

> **Das ist die Team-ID des vorhandenen Apple-Entwicklerkontos.** Sie ist kein
> Geheimnis — sie steht in jedem Provisioning-Profil. Vor dem ersten Lauf
> trotzdem gegenprüfen: developer.apple.com → Membership. Stimmt sie nicht,
> an diesen drei Stellen ersetzen, sonst scheitert die Signatur.

### 3. Bundle-ID registrieren

developer.apple.com → Certificates, Identifiers & Profiles → Identifiers → **+**

- App IDs → App
- Description: `Logbot`
- Bundle ID: **Explicit** → `de.phytech.logbot`
- Capabilities: keine anhaken. Die App braucht keine

### 4. Signatur einrichten

Privates Repository für die Zertifikate anlegen, zum Beispiel
`Phydran6/logbot-certificates`. Leer lassen, `match` füllt es.

Dann in GitHub: `Actions → iOS Signatur einrichten → Run workflow`.

Der Lauf erzeugt bei Apple ein Verteilungs-Zertifikat und ein
App-Store-Profil und legt beides verschlüsselt ab. **Einmalig** — alle
späteren Builds holen es nur noch.

Scheitert der Lauf mit „Diese Secrets fehlen noch", ist Schritt 1
unvollständig. `scripts/check_match_repo.sh` unterscheidet dabei Tippfehler in
der URL, abgelaufenen Token und fehlendes Schreibrecht.

### 5. App in App Store Connect anlegen

appstoreconnect.apple.com → Apps → **+** → New App

- Plattform: iOS
- Name: `Logbot`
- Primärsprache: **Deutsch**
- Bundle ID: `de.phytech.logbot`
- SKU: frei wählbar

### 6. Was nur von Hand geht

**Alterseinstufung** (App Information → Age Rating): überall „Keine" bzw.
„Nein". Erwartetes Ergebnis: **4+**.

**App-Datenschutz** (App Privacy). Antworten, die zum Code passen:

| Frage | Antwort |
|:--|:--|
| Erfasst die App Daten? | **Nein** |
| Wird zum Tracking genutzt? | **Nein** |

Die App überträgt nur an die Instanz, die der Nutzer selbst eingetragen hat.
Apple fragt nach Daten, die **der Anbieter** erhält — das ist hier niemand.

**Testkonto für die Prüfung.** Die App verlangt eine Instanz-URL und einen
Token; ohne beides sieht der Prüfer nur den Einrichtungsbildschirm und lehnt
nach Richtlinie 2.1 ab. Also vor der Einreichung eine erreichbare
Demo-Instanz bereitstellen und URL plus Token unter **App Review Information →
Notes** hinterlegen. Der Token muss zum Zeitpunkt der Prüfung gültig sein —
App-Token laufen ab.

**Preis:** kostenlos.

### 7. TestFlight

Passiert von selbst: Ein Tag `v1.0.0` startet Release, danach läuft Deploy und
schickt den Build zu TestFlight. Verarbeitung dauert 15–60 Minuten.

Interne Tester (bis 100, kein Review nötig) unter Users and Access hinzufügen.

### 8. Einreichen

```bash
cd ios && fastlane release_store version:1.0.0 build_number:42
```

---

## Ein Wort zum Zeitpunkt

**TestFlight zuerst.** Apple lehnt Apps ohne funktionierendes Testkonto nach
Richtlinie 2.1 ab, und Logbot ist ohne eigene Serverinstanz nicht sinnvoll
benutzbar. Das ist die eigentliche Hürde — nicht der Build.

Für Google Play gilt das ähnlich, dort aber entspannter: Der interne Track
verlangt keine Prüfung.

---

## Wenn alles live ist

- In [`../README.md`](../README.md) die Store-Knöpfe eintragen und die
  App-Store-ID einsetzen
- In diesem Dokument den Abschnitt **Stand** aktualisieren
