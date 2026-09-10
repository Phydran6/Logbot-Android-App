# Release und Deployment

Ziel: einen Tag pushen, alles andere läuft von selbst.

```bash
git tag v1.0.0 && git push origin v1.0.0
```

[← Doku-Übersicht](README.md) ·
[Veröffentlichen](STORE.md) ·
[Abläufe](../.github/README.md)

---

## Versionsnummern

`MAJOR.MINOR.PATCH` nach [Semantic Versioning](https://semver.org/lang/de/),
Vorabversionen als `1.1.0-beta.2`.

> **Das ist neu.** Bis einschließlich `2026.05.29.22.30.00` trug Logbot
> datumsbasierte Versionen. Apple nimmt die nicht: In
> `CFBundleShortVersionString` sind nur Ziffern und höchstens drei durch
> Punkte getrennte Teile erlaubt, ein fünfteiliges Datum wird mit
> **ITMS-90060** abgelehnt — und zwar erst beim Upload, nach der vollen
> Bauzeit. Mit dem Schritt in Richtung App Store wechselt das Schema deshalb
> auf SemVer. Die alte Historie bleibt im [Changelog](CHANGELOG.md) stehen.

Die **Build-Nummer** ist immer `github.run_number`; sie steigt monoton, wie es
beide Stores verlangen. Der Android-`versionCode` ist `100 + run_number` — die
letzte von Hand gebaute Release trug die 4, und Play nimmt keinen
Rückwärtsschritt an.

Für iOS wird der Vorabzusatz abgeschnitten: `1.1.0-beta.2` wird zu `1.1.0`.
Verwechslungsgefahr gibt es nicht, innerhalb einer Version unterscheidet die
Build-Nummer die Uploads.

---

## Vor jedem Tag

1. Abschnitt in [`CHANGELOG.md`](CHANGELOG.md) anlegen
2. Prüfen, dass er gefunden wird:
   ```bash
   bash scripts/extract_changelog.sh 1.1.0
   ```
3. Committen, dann taggen

Die Version selbst steht **nirgends im Repository**. `versionName`,
`versionCode`, `MARKETING_VERSION` und `CURRENT_PROJECT_VERSION` setzt die CI
aus Tag und Lauf-Nummer. Die Werte in `android/app/build.gradle.kts` und
`ios/project.yml` gelten nur für lokale Builds.

---

## Ablauf

| Schritt | Wo | Was passiert |
|:--|:--|:--|
| 1 | `release.yml` | Version aus dem Tag lesen, Notizen aus dem Changelog schneiden |
| 2 | `release.yml` | Android: signiertes AAB + APK |
| 3 | `release.yml` | iOS: signiertes IPA über `fastlane match` |
| 4 | `release.yml` | GitHub-Release mit Artefakten und Notizen |
| 5 | `deploy.yml` | AAB → Play-Track `internal`, IPA → TestFlight |

Fehlen Store-Secrets, überspringen sich die betroffenen Schritte mit einer
Warnung. Die Kette bleibt grün, es entstehen unsignierte Artefakte.

Android muss durchlaufen, damit das Release erscheint. iOS darf fehlen — ohne
Apple-Zugang wird dort nur unsigniert gebaut, und das soll das Release nicht
aufhalten.

---

## Branches

| Branch | Zweck |
|:--|:--|
| `main` | Produktiv. Nur von hier wird getaggt |
| Arbeitszweige | Ein Zweig je Vorhaben, danach weg |

Der Weg ist immer derselbe: Zweig aufmachen, Pull Request nach `main`, taggen,
Zweig löschen. Jeder Zweig durchläuft dieselbe CI.

---

## Einmalige Einrichtung

### Android-Signatur

Upload-Keystore erzeugen:

```bash
keytool -genkey -v -keystore upload-keystore.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

Für lokale Release-Builds eine `android/signing.properties` anlegen:

```properties
storeFile=upload-keystore.jks
storePassword=…
keyAlias=upload
keyPassword=…
```

`storeFile` wird relativ zum Modul `:app` aufgelöst — die `.jks` gehört also
nach `android/app/`.

> **Beides ist in [`.gitignore`](../.gitignore).** Ein Keystore im Repository
> ist ein Keystore in fremder Hand. Siehe [SICHERHEIT.md](SICHERHEIT.md#signaturschlüssel).

Fehlt die Datei, registriert der Build die Release-Signatur gar nicht erst
und `assembleDebug` läuft trotzdem.

### Google Play

1. Play-Entwicklerkonto anlegen (einmalig 25 $)
2. App mit Package **`de.phytech.logbot`** anlegen
3. Erstes AAB **von Hand** hochladen — Google verlangt das beim ersten Mal
4. Service-Account in Google Cloud anlegen, in der Play Console freischalten,
   JSON-Schlüssel herunterladen

### Apple

Die vollständige Reihenfolge steht in [STORE.md](STORE.md#apple-die-reihenfolge).
Kurz:

1. Apple Developer Program (99 $/Jahr) — liegt vor
2. App-ID `de.phytech.logbot` in App Store Connect anlegen
3. App-Store-Connect-API-Key (`.p8`) erzeugen
4. Privates Repository für die Zertifikate anlegen und
   `Actions → iOS Signatur einrichten` einmal laufen lassen

---

## GitHub-Secrets

`Settings → Secrets and variables → Actions`

### Android

| Secret | Inhalt |
|:--|:--|
| `ANDROID_KEYSTORE_BASE64` | `base64 -w0 upload-keystore.jks` |
| `ANDROID_KEYSTORE_PASSWORD` | Store-Passwort |
| `ANDROID_KEY_ALIAS` | z. B. `upload` |
| `ANDROID_KEY_PASSWORD` | Key-Passwort |
| `PLAY_SERVICE_ACCOUNT_JSON` | kompletter Inhalt des Service-Account-JSON |

### iOS

| Secret | Inhalt |
|:--|:--|
| `APPSTORE_API_KEY_ID` | Key-ID des App-Store-Connect-Keys |
| `APPSTORE_API_ISSUER_ID` | Issuer-ID, dieselbe Seite |
| `APPSTORE_API_PRIVATE_KEY` | Inhalt der `.p8`, **base64-kodiert** |
| `MATCH_GIT_URL` | HTTPS-URL des privaten Zertifikats-Repositorys |
| `MATCH_GIT_TOKEN` | Token mit Schreibrecht auf dieses Repository |
| `MATCH_PASSWORD` | frei wählbar, verschlüsselt die Zertifikate |

Base64 unter Windows, Ergebnis liegt in der Zwischenablage:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("upload-keystore.jks")) | Set-Clipboard
```

`scripts/check_apple_secrets.sh` prüft alle sechs Apple-Secrets einzeln und
erkennt insbesondere den häufigsten stillen Fehler: die `.p8` roh eingefügt
statt base64-kodiert.

### Sonstiges

| Secret | Wofür |
|:--|:--|
| `GITLAB_TOKEN` | Spiegelung nach GitLab für F-Droid |

---

## Store-Promotion

Von `internal` weiter:

```bash
cd android && fastlane promote_beta
cd android && fastlane promote_production
```

---

## Ohne Store verteilen

Das APK aus dem GitHub-Release ist direkt verteilbar, kein Play-Konto nötig.
Für iOS gilt das **nicht**: Apple verlangt die Entwickler-Mitgliedschaft auch
zum Sideloaden, der Weg läuft über TestFlight.
