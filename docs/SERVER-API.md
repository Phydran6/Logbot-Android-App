# Server-Schnittstelle

Welche Endpunkte die App benutzt.

[← Doku-Übersicht](README.md) ·
[Architektur](ARCHITEKTUR.md) ·
[Logbot-Server](https://github.com/Phydran6/Logbot-Server)

---

## Anmeldung

Jede Anfrage trägt den Token als Kopfzeile:

```http
Authorization: Bearer <token>
```

Drei Wege führen dorthin:

| Weg | Ablauf |
|:--|:--|
| **E-Mail-Link** | App würfelt `code_verifier`, schickt `POST /api/auth/magic-link/request` mit E-Mail und `code_challenge` (S256). Der Server mailt einen Link auf `/api/auth/magic-link/open`, der per `logbot://magic-login?rid=…&t=…` zurück in die App führt. Eingelöst wird mit `POST /api/auth/magic-link/exchange` (Request-ID, Verifier, Token). Am Rechner geöffnet: dort den Code aus der App eingeben, die App fragt mit `exchange` ohne Token nach, bis bestätigt ist. Antwort `status`: `pending`, `ok` (mit `access_token`) oder `mfa_required` (weiter wie beim Passwort) |
| **Passwort** | `POST /api/auth/login` – Benutzername **oder** E-Mail; verlangt der Server MFA, antwortet er mit einem `mfa_token`, den `POST /api/auth/login/mfa` gegen TOTP oder Backup-Code einlöst. `POST /api/auth/password/forgot` schickt einen Link zum Zurücksetzen |
| **QR-Code** | Die Weboberfläche zeigt unter *App verbinden* einen kurzlebigen Token. `POST /api/auth/app-token/exchange` tauscht ihn gegen einen Access-Token |

`GET /api/app/bootstrap` liefert Rolle, Fähigkeiten des Servers und Filterwerte.
Danach richtet sich, welche Einträge die Seitenleiste zeigt — Administratoren
sehen Benutzer, Webhooks und Branding, andere nicht. Ältere Server ohne
`/api/app` fallen auf `GET /api/auth/me` zurück.

**HTTP 401** auf eine angemeldete Anfrage heißt: Token abgelaufen oder
zurückgezogen. Die App verwirft den Token (die Server-Adresse bleibt) und führt
zurück zur Anmeldung – egal, aus welchem Bildschirm die Anfrage kam.

---

## Genutzte Endpunkte

Alles unten wird tatsächlich aufgerufen; die Zuordnung zum Screen steht dabei.

### Lesen

| Endpunkt | Screen |
|:--|:--|
| `GET /api/health` | Einrichtung – antwortet unter der Adresse ein Logbot-Server? (ohne Anmeldung) |
| `GET /api/app/bootstrap` | Start: Rolle, Fähigkeiten, Gerätenamen für Filter |
| `GET /api/app/summary` | Übersicht |
| `GET /api/health/detailed` | Übersicht, Server-Zustand |
| `GET /api/app/logs` | Logs – Cursor `before_id`, Filter `min_severity`, `hostname`, `search`, `since_hours` |
| `GET /api/app/logs/{id}` | Logs, Detailblatt (zerlegte Felder und Rohzeile) |
| `GET /api/app/devices` | Geräte |
| `GET /api/app/notifications` | Meldungen, Hintergrundabfrage (`since_id`) |
| `GET /api/ai/status` | KI-Assistent: eingerichtet und freigegeben? |
| `GET /api/users` | Benutzer |
| `GET /api/webhooks` | Webhooks |
| `GET /api/settings`, `GET /api/settings/database` | Einstellungen |
| `GET /api/branding/config` | Branding |

### Schreiben

| Endpunkt | Screen |
|:--|:--|
| `POST /api/ai/chat` | KI-Assistent – Verlauf plus optional `hostname`; der Server prüft selbst die Logs |
| `POST /api/app/notifications/read` | Meldungen – gelesen markieren |
| `GET /api/app/push/vapid`, `POST`/`DELETE /api/app/push`, `POST /api/app/push/test` | Benachrichtigungen – UnifiedPush-Endpunkt an- und abmelden |
| `POST`, `PUT`, `DELETE /api/users[/{id}]` | Benutzer — anlegen, ändern, löschen |
| `POST /api/users/{id}/mfa/reset` | Benutzer — MFA zurücksetzen |
| `POST`, `PUT`, `DELETE /api/webhooks[/{id}]` | Webhooks |
| `POST /api/webhooks/{id}/regenerate-token` | Webhooks — Token neu |
| `DELETE /api/agents/{id}?purge_logs=` | Geräte — entfernen, wahlweise samt Logs |
| `PUT /api/settings/{key}` | Einstellungen — nur einfache Werte; verschachtelte lehnt der Server mit 409 ab |
| `PUT /api/branding/config`, `POST /api/branding/reset` | Branding |

Schreibende Aufrufe sind auf Administratoren beschränkt; die App blendet sie
anhand der Rolle aus, der Server prüft sie erneut.

---

## Form der Antworten

Die Datentypen liegen als `@Serializable`-DTOs unter
`android/app/src/main/java/de/phytech/logbot/data/api/`. Sie sind die
verbindliche Beschreibung — wer wissen will, welche Felder ankommen, liest
dort, nicht hier: eine zweite Abschrift wäre nur eine, die irgendwann
abweicht.

Der Server veröffentlicht seine Spezifikation ohnehin selbst:

- OpenAPI: `/api/openapi.json`
- Swagger: `/api/docs`

**Zeitstempel** kommen als ISO-8601 in UTC, teils ohne Zonenangabe
(`2026-05-29T22:30:00.123456`). Die App liest sie als UTC und zeigt sie in der
Zeitzone des Geräts.

---

## Fehlerbehandlung

Jeder Aufruf läuft durch `safeApiCall` im Repository und endet in einem
`UiState`:

| Fall | Was der Nutzer sieht |
|:--|:--|
| Erfolg | Die Daten |
| Leere Liste | Ein Hinweis, dass nichts da ist |
| HTTP 401 | Abmeldung, zurück zur Anmeldung |
| HTTP 404 bei `/api/app/notifications` | Hinweis, dass der Server für Meldungen ein Update braucht |
| Anderer Fehler | Der Text aus `detail` der Server-Antwort, sonst eine allgemeine Meldung – mit Knopf zum Wiederholen |

Es gibt keinen Pfad, der einen Fehler verschluckt und mit leeren Daten
weiterläuft.

---

## Was die App nicht anfasst

Server neu starten, Updates auslösen, Caddy oder die Netzwerk-Einstellungen
ändern, Aufbewahrung ausführen, KI-, Mail-, LDAP- oder Archiv-Anbindung
einrichten. Das bleibt der Weboberfläche vorbehalten — ein Fehlgriff ist auf
einem Telefon zu leicht passiert, und diese Bereiche brauchen die Prüfungen,
die der Server dort vornimmt.
