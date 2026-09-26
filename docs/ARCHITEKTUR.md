# Logbot Android – Architektur (native Neuausrichtung)

> Status: **Phase 1 (Konzept)**. Dieses Dokument ist der Bauplan für den Umbau vom
> WebView-Wrapper zur eigenständigen nativen App. Es wird pro Phase fortgeschrieben.

## 1. Ziel & Prinzipien

Die App ist ein **nativer Client für einen selbst gehosteten
[Logbot-Server](https://github.com/Phydran6/Logbot-Server)** (FastAPI + PostgreSQL).
Sie ersetzt die bisherige WebView durch eine native Oberfläche, die die Server-Daten
direkt über die REST-API holt und die **Menüführung und Ansichten des Servers 1:1
nachbaut**.

Leitplanken:

- **Schnell**: native Listen, Paging, kein WebView-Overhead, flüssige Material-3-UI.
- **Benutzerfreundlich**: vertraute Navigation (gleiche Menüpunkte wie der Server),
  klare Lade-/Leer-/Fehlerzustände, Pull-to-refresh, Dark/Light.
- **Sicher**: verschlüsselter Token-Speicher, Biometrie-Lock, HTTPS-only.
- **Pragmatisch sauber**: MVVM + Repository, keine Über-Abstraktion. Ein Modul,
  klare Schichten, Feature-Pakete.
- **Funktionsparität**: alle bisherigen App-Funktionen bleiben erhalten; alle
  Server-Screens werden abgedeckt.

## 2. Tech-Stack

| Bereich          | Wahl                                                        |
|------------------|-------------------------------------------------------------|
| Sprache          | Kotlin                                                      |
| UI               | Jetpack Compose + Material 3                                |
| Architektur      | MVVM + Repository, unidirektionaler Datenfluss (UDF)        |
| Navigation       | Navigation-Compose, **Single-Activity**                    |
| DI               | Hilt                                                        |
| Networking       | Retrofit + OkHttp + kotlinx.serialization                  |
| Async            | Coroutines + Flow / StateFlow                              |
| Paging (Logs)    | Paging 3 (`paging-compose`)                                |
| Bilder           | Coil (Branding-Logo, Base64-QR-Render)                     |
| QR-Scan          | ZXing (`zxing-android-embedded`, bereits vorhanden)        |
| Charts (Dashboard)| Vico *(Vorschlag, finale Wahl in Phase 3)*                |
| Token-Speicher   | EncryptedSharedPreferences (hinter `CredentialStore`)      |
| Sonst. Prefs     | DataStore (Preferences)                                     |
| Biometrie        | `androidx.biometric` (bereits vorhanden)                   |

> Build wird in Phase 2 modernisiert: Kotlin-Android-, Compose-Compiler-,
> Serialization-, KSP- und Hilt-Plugins ergänzen (aktuell fehlt sogar das
> Kotlin-Plugin im Version-Catalog).

## 3. Modul- & Paketstruktur

Ein Gradle-Modul (`:app`), Paket-nach-Feature mit gemeinsamen `core`-/`data`-Schichten:

```
de.phytech.logbot
├── LogbotApp.kt            // @HiltAndroidApp
├── MainActivity.kt         // Single-Activity, Compose-Host, NavHost
├── core/
│   ├── network/            // Retrofit-Setup, OkHttp, Interceptors, ApiResult, Fehler-Mapping
│   ├── auth/               // SessionManager, CredentialStore, AuthInterceptor/Authenticator
│   ├── security/           // BiometricGate, Keystore-Krypto
│   ├── designsystem/       // Theme, Farben, Typo, wiederverwendbare Compose-Komponenten
│   └── navigation/         // Routen, NavHost, Drawer, Rollen-Gating
├── data/
│   ├── api/                // Retrofit-Service-Interfaces + DTOs (an OpenAPI orientiert)
│   ├── repository/         // LogsRepository, AgentsRepository, UsersRepository, ...
│   └── model/              // Domänenmodelle (UI-unabhängig)
└── feature/
    ├── setup/              // Instanz-URL + QR-App-Login
    ├── login/              // Passwort-Login + MFA
    ├── dashboard/
    ├── logs/               // Liste (Paging) + Filter + Detail
    ├── agents/
    ├── users/
    ├── webhooks/
    ├── health/
    ├── settings/
    └── branding/
```

## 4. Schichten & Datenfluss

```
Compose-Screen ──Intent/Event──▶ ViewModel ──▶ Repository ──▶ ApiService (Retrofit)
      ▲                              │                              │
      └────────  UiState  ◀──────────┘   ◀────────  DTO→Model  ◀────┘
```

- **UI**: zustandslose Composables; `ViewModel` hält `StateFlow<UiState>`.
  `UiState` als sealed/data class (Loading / Content / Empty / Error).
- **Repository**: einzige Datenquelle pro Domäne; mappt DTO→Domänenmodell,
  kapselt Paging und (später) Caching. ViewModels kennen kein Retrofit.
- **Domain**: schlank gehalten — eigene Use-Cases nur wo sinnvoll, sonst direkt
  Repository. Keine Use-Case-Schicht „auf Vorrat".
- **Fehler**: einheitlicher `ApiResult<T>` (Success / HttpError / NetworkError);
  401 wird zentral abgefangen (siehe §6).

## 5. Navigation – die Server-Sidebar nativ

Der Server nutzt eine Sidebar. Auf dem Handy bildet ein **ModalNavigationDrawer**
(Hamburger) diese am treuesten ab — gleiche Reihenfolge, gleiche Beschriftungen.

**Auth-Graph** (kein Drawer): `Setup → Login → MFA`
**Haupt-Graph** (mit Drawer):

| Drawer-Eintrag | Route        | Sichtbar für        |
|----------------|--------------|---------------------|
| Dashboard      | `dashboard`  | alle                |
| Logs           | `logs`       | alle                |
| Agents         | `agents`     | alle                |
| Users          | `users`      | **admin**           |
| Webhooks       | `webhooks`   | alle (ggf. admin)   |
| Health         | `health`     | alle                |
| Einstellungen  | `settings`   | alle                |
| └ Branding     | `branding`   | **admin**           |

- **Rollen-Gating**: `UserResponse.role == "admin"` blendet Admin-Einträge ein/aus
  (analog `isAdmin` im Server-Frontend). Server bleibt die Autorität — die UI
  versteckt nur, was der Nutzer nicht darf.
- Öffentliche Inhalte (Impressum/Datenschutz) als statische Screens aus dem Drawer-Footer.

## 6. Networking, Auth & Session

**Dynamische Base-URL**: Die Instanz-URL ist erst nach dem Setup bekannt. Der
OkHttp-Client wird mit einem Interceptor gebaut, der Host/Schema aus dem
`CredentialStore` liest und in jede Anfrage einsetzt (Retrofit-Base-URL ist ein
Platzhalter). Wechselt die Instanz → keine Neuinitialisierung des Graphen nötig.

**Auth-Interceptor**: hängt `Authorization: Bearer <jwt>` an, sofern vorhanden.

**401-Handling (zentral)**: Ein OkHttp-`Authenticator`/Interceptor erkennt 401,
löscht das Token über den `SessionManager` und löst Navigation zurück zum Login aus
(wie das Server-Frontend, das bei 401 ausloggt). Kein vollständiger Setup-Reset mehr
nötig — die Instanz-URL bleibt erhalten, nur das JWT wird verworfen.

### Login-Flows (gegen die echten Server-Endpoints)

1. **QR-App-Login** (Setup → „QR scannen"):
   - QR enthält `{"url": <api_url>, "token": <64-Hex>, "type": "logbot_app_auth_v1"}`.
   - App ruft `POST {url}/api/auth/app-token/exchange` mit `{ "token": <64-Hex> }`.
   - Antwort: `{ access_token }` → als JWT speichern, `url` als Instanz-URL speichern.
   - ⚠️ **Korrektur ggü. Alt-App**: Der QR-Token ist ein **einmaliger 15-Min-Token**,
     kein Dauer-Bearer. Er **muss getauscht** werden — die alte App nutzte ihn
     fälschlich direkt als Authorization-Header.

2. **Passwort-Login** (Setup → URL eingeben → Login-Screen):
   - `POST /api/auth/login` (form-data `username`, `password`).
   - Antwort entweder `{ access_token }` **oder** `{ mfa_required: true, mfa_token,
     expires_in_seconds }`.
   - Bei MFA: `POST /api/auth/login/mfa` mit `{ mfa_token, code }` (6-stelliger TOTP
     **oder** 10-stelliger Backup-Code) → `{ access_token }`.
   - Nach Login: `GET /api/auth/me` → `UserResponse` (Rolle für Gating cachen).

**Token-Lebensdauer**: JWT läuft serverseitig ab → 401 → erneuter Login. (Kein
Refresh-Token-Endpoint vorhanden.)

**Speicher**: `instance_url` + `access_token` verschlüsselt (EncryptedSharedPreferences,
hinter `CredentialStore`-Interface — austauschbar). Biometrie-Flag wie gehabt.

**Biometrie-Lock**: beim Kaltstart vor Entschlüsselung des Tokens (unverändert zur
Alt-App, nur nach Compose portiert).

## 7. Screen ↔ API-Mapping

| Screen     | Zweck                              | Kern-Endpoints |
|------------|------------------------------------|----------------|
| Dashboard  | Statistik-Kacheln, Charts          | `GET /api/logs/stats`, `GET /api/health/detailed` |
| Logs       | Paginierte Liste, Filter, Detail   | `GET /api/logs` (page/page_size/filter), `GET /api/logs/{id}` |
| Agents     | Geräte, Online-Status, Retention   | `GET/PUT/DELETE /api/agents`, decommission |
| Users      | Benutzer + Rollen, MFA-Reset       | `GET/POST/PUT/DELETE /api/users`, `/users/{id}/mfa/reset` |
| Webhooks   | CRUD, Filter, Aufruf-Statistik     | `GET/POST/PUT/DELETE /api/webhooks` |
| Health     | CPU/RAM/Disk/Uptime, DB-Status     | `GET /api/health/detailed` |
| Settings   | Allgemein, Retention, DB-Infos     | `GET/PUT /api/settings`, `/settings/database` |
| Branding   | Whitelabel, Dark/Light, Logo       | `GET/PUT /api/branding` |
| Login/MFA  | Anmeldung inkl. QR-App-Login       | `GET /api/auth/*` (siehe §6) |

> Die genauen Query-Parameter/Felder werden je Screen in der jeweiligen Phase aus
> der OpenAPI-Spec (`/api/openapi.json`) bzw. den Server-Routen verifiziert.

## 8. Sicherheit

- **HTTPS-only**: Instanz-URL muss `https://` sein (cleartext via Network-Security-Config verbieten).
- **Token** verschlüsselt at-rest (Keystore-gebundener Master-Key).
- **Biometrie-Lock** vor Token-Zugriff.
- **Kein Logging** von Tokens/Passwörtern (OkHttp-Logger nur im Debug, Header redacted).
- **Certificate Pinning**: weiterhin **nicht** implementiert (bewusste Entscheidung,
  siehe README) — bei direkten API-Calls noch relevanter, daher als Roadmap-Option vermerkt.

## 9. Was bleibt / was ersetzt wird

**Bleibt (nach Compose portiert):** Setup (URL/Token/QR), verschlüsselter Speicher,
Biometrie-Lock, QR-Scan, HTTPS-Erzwingung, 401-Erkennung.

**Entfällt:** WebView + `WebViewClient`/`WebChromeClient`-Hardening, JS-Bridge
`LogbotApp` (Biometrie wird nativ in den App-Einstellungen umgeschaltet statt aus der
Web-UI), `localStorage`-Token-Injektion, Versions-Overlay über der WebView.

## 10. Roadmap

Siehe [ROADMAP.md](ROADMAP.md). Kurz:
Phase 0 Discovery ✅ → **Phase 1 Architektur/Beschreibung** → Phase 2 Gerüst
(Build, Theme, Navigation, API-Client, Auth/MFA/QR) → Phase 3a Read-Screens →
Phase 3b Management-Screens → Phase 4 Feinschliff.

## 11. Offene Entscheidungen (je Phase)

- **Charts-Bibliothek** fürs Dashboard: Vico vs. eigenes Canvas (Phase 3).
- **API-Client**: handgeschriebene Retrofit-Interfaces (klein, kontrolliert) vs.
  Generierung aus OpenAPI. Aktueller Plan: handgeschrieben, OpenAPI als Referenz.
- **Offline-Caching** (Room) für Logs/Agents: optional in Phase 4.
- **Certificate Pinning**: optional, abhängig vom Einsatzumfeld.
