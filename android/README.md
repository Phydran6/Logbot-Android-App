# android/ — die Android-App

Kotlin, Jetpack Compose, Material 3. Single-Activity, Navigation-Compose,
Hilt für die Abhängigkeiten, Retrofit gegen die Server-REST-API.

[← Zurück zur Übersicht](../README.md) ·
[Architektur](../docs/ARCHITEKTUR.md) ·
[Server-Schnittstelle](../docs/SERVER-API.md) ·
[Roadmap](../docs/ROADMAP.md)

---

## Bauen

```bash
cd android
./gradlew assembleDebug
```

Das APK liegt danach unter `app/build/outputs/apk/debug/`.

> **Nicht in jeder Umgebung baubar.** Gradle 9.3 und AGP 9.1 brauchen JDK 17+
> und das Android SDK. Android Studio bringt beides mit; auf einem Rechner mit
> blankem Java 8 scheitert der Build. Die verlässliche Prüfung ist die CI —
> und die meldet eine Fehlerursache als Commit-Kommentar, siehe
> [.github/ABLAEUFE.md](../.github/ABLAEUFE.md).

Ein Release-Build braucht eine `signing.properties` neben dieser Datei; fehlt
sie, signiert der Build mit der Debug-Signatur statt unsigniert zu bleiben.
Einzelheiten in [docs/RELEASE.md](../docs/RELEASE.md).

---

## Paketstruktur

```
de.phytech.logbot
├── core/
│   ├── network/        Retrofit, OkHttp, Interceptors, NetworkModule
│   ├── auth/           CredentialStore (verschlüsselt), Session
│   ├── notifications/  Kanäle, Push (UnifiedPush), Hintergrundabfrage
│   ├── security/       Biometrie-/PIN-Lock
│   ├── designsystem/   Material-3-Theme, Farben, Typografie
│   ├── navigation/     Destinations, NavHost-Verdrahtung
│   ├── ui/             LoadingState, ErrorState, EmptyState, StatCard
│   └── util/
├── data/
│   ├── api/            Retrofit-Schnittstellen + DTOs (@Serializable)
│   └── repository/     safeApiCall → UiState
└── feature/            ein Paket je Screen
    ├── setup/  login/  lock/        Einrichtung, Login+MFA, App-Sperre
    ├── shell/                       Drawer = Server-Sidebar
    ├── dashboard/  health/  logs/   Lesen
    ├── chat/  notifications/        KI-Assistent, Meldungen
    ├── agents/  users/  webhooks/   Geräte, Verwalten
    ├── settings/  branding/
    └── common/
```

---

## Muster je Screen

Immer dieselbe Kette — wer einen Screen kennt, kennt alle:

```
DTO (@Serializable)
  → Retrofit-API          (in NetworkModule bereitgestellt)
  → Repository            (safeApiCall → UiState)
  → @HiltViewModel        (Compose-mutableStateOf)
  → Screen                (when (UiState) { Loading / Error / Success })
  → Route im MainShell-NavHost
```

Bausteine für die Zustände liegen in `core/ui`: `LoadingState`, `ErrorState`
(mit Wiederholen), `EmptyState`, `StatCard`.

Der Drawer bildet die Sidebar des Servers nach. Admin-Einträge blendet er
anhand der Rolle aus `/api/auth/me` aus.

---

## Screens

| Bereich | Inhalt |
|:--|:--|
| Setup | Server-Adresse (wird angepingt), QR-App-Login |
| Login | E-Mail-Link (PKCE, Code für fremdes Gerät), Passwort, MFA (TOTP/Backup-Code), Passwort vergessen |
| Übersicht | Urteil, Geräte, Probleme heute, offene Meldungen, Server, KI-Schnellfragen |
| Meldungen | Liste vom Server, gelesen markieren, Benachrichtigungs-Einstellung |
| KI-Assistent | Chat, optional auf ein Gerät beschränkt, mit Transparenz über gesendete Logzeilen |
| Logs | Cursor-Paging (`/api/app/logs`), Filter-Chips, Suche, Detailblatt |
| Geräte | Online-Zustand, Suche, Aktionen (Logs, KI, entfernen) |
| Server-Zustand | CPU/RAM/Platte als Balken, Datenbank, Laufzeit; 20-s-Takt |
| Benutzer | Anlegen/Ändern/Löschen, MFA zurücksetzen (Admin) |
| Webhooks | Anlegen/Ändern/Löschen, Aufruf-URL kopieren, Token neu (Admin) |
| Einstellungen | App-Sperre, Benachrichtigungen, Server; einfache Server-Werte (Admin) |
| Branding | Whitelabel-Einstellungen (Admin) |

Dazu quer über alles: verschlüsselter Token-Speicher, Biometrie-/PIN-Sperre
(auch nach einer Minute im Hintergrund), zentrale Abmeldung bei HTTP 401
(`SessionManager`), Deep Link `logbot://magic-login` (`LinkInbox`).

### Benachrichtigungen

`core/notifications/`: `Notifier` (drei Kanäle), `LogbotPushService`
(UnifiedPush-Connector 3.x, Nachrichten nach RFC 8291 entschlüsselt),
`PushManager` (Verteiler wählen, registrieren), `NotificationCheckWorker`
(WorkManager, alle 15 Minuten – auch als Rückfall, wenn Push ausfällt) und
`PushRegisterWorker` (Endpunkt beim Server anmelden, mit Wiederholung).
Doppelte Benachrichtigungen verhindert die zuletzt gemeldete ID.

**Tink:** `security-crypto` bringt `tink-android` mit, der UnifiedPush-Connector
das reine `tink` – beide enthalten dieselben Klassen. Deshalb ist `tink` beim
Connector ausgeschlossen und `tink-android` in aktueller Fassung eingebunden.

---

## Stolpersteine, die Zeit gekostet haben

**AGP 9 bringt Kotlin schon mit.** Das Plugin `org.jetbrains.kotlin.android`
darf **nicht** angewendet werden — es kollidiert mit dem eingebauten. Für
Compose kommt `org.jetbrains.kotlin.plugin.compose` dazu, und zwar in genau
der eingebauten Kotlin-Version (**2.2.10**), sonst lehnt der
Compose-Compiler ab.

**kapt geht nicht**, KSP schon. Hilt läuft deshalb über KSP. Damit KSP seine
erzeugten Quellen anmelden darf, steht `android.disallowKotlinSourceSets=false`
in `gradle.properties` — ohne das bricht der Build mit „Using kotlin.sourceSets
DSL … not allowed" ab.

**Im Gradle-Kotlin-DSL keine voll qualifizierten JDK-Namen inline.**
`java.time.ZonedDateTime.now(...)` ergibt „Unresolved reference 'time'".
Oben importieren und unqualifiziert benutzen.

**Keine BOM in `libs.versions.toml`.** TOML lehnt sie ab („Unexpected
'\ufeff'"). Windows-PowerShell schreibt sie mit `Set-Content -Encoding utf8`
unbemerkt hinein.

**JVM-Signatur-Clash.** Property `x` und Methode `setX(...)` ergeben dieselbe
JVM-Signatur. Methoden anders benennen, etwa `toggleX` oder `showX`.

---

## Versionierung

`versionCode` und `versionName` kommen aus der CI, per
`-Plogbot.versionCode` und `-Plogbot.versionName`. Lokale Builds ohne diese
Werte nehmen einen Datums-Fallback mit `-alpha-dev`. Von Hand hochzählen muss
niemand.

---

## Berechtigungen

| Berechtigung | Wofür | Pflicht |
|:--|:--|:--|
| `INTERNET` | Zugriff auf die eigene Instanz | ja |
| `CAMERA` | QR-Code bei der Einrichtung | nein |
| `POST_NOTIFICATIONS` | Benachrichtigungen (ab Android 13 erfragt) | nein |
| `WAKE_LOCK` | Push-Zustellung (kommt vom UnifiedPush-Connector) | nein |
