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

Zwei Wege führen dorthin:

| Weg | Ablauf |
|:--|:--|
| **Passwort** | `POST /api/auth/login`; verlangt der Server MFA, antwortet er mit einem `mfa_token`, den `POST /api/auth/login/mfa` gegen TOTP oder Backup-Code einlöst |
| **QR-Code** | Die Weboberfläche zeigt unter *App verbinden* einen kurzlebigen Token. `POST /api/auth/app-token/exchange` tauscht ihn gegen einen Access-Token |

`GET /api/auth/me` liefert die Rolle. Danach richtet sich, welche Einträge die
Seitenleiste zeigt — Administratoren sehen Users, Settings und Branding, andere
nicht.

**HTTP 401** heißt: Token abgelaufen oder zurückgezogen. Die App verwirft die
Zugangsdaten und führt zurück zur Anmeldung.

---

## Genutzte Endpunkte

Alles unten wird tatsächlich aufgerufen; die Zuordnung zum Screen steht dabei.

### Lesen

| Endpunkt | Screen |
|:--|:--|
| `GET /api/logs/stats` | Dashboard |
| `GET /api/health/detailed` | Health |
| `GET /api/logs` | Logs — Parameter für Suche, Level, Host, Quelle und Seitenweise |
| `GET /api/logs/{id}` | Logs, Detailansicht |
| `GET /api/agents` | Agents |
| `GET /api/users` | Users |
| `GET /api/webhooks` | Webhooks |
| `GET /api/settings`, `GET /api/settings/database` | Settings |
| `GET /api/branding/config` | Branding |

### Schreiben

| Endpunkt | Screen |
|:--|:--|
| `POST`, `PUT`, `DELETE /api/users[/{id}]` | Users — anlegen, ändern, löschen |
| `POST /api/users/{id}/mfa/reset` | Users — MFA zurücksetzen |
| `POST`, `PUT`, `DELETE /api/webhooks[/{id}]` | Webhooks |
| `POST /api/webhooks/{id}/regenerate-token` | Webhooks — Token neu |
| `DELETE /api/agents/{id}` | Agents — löschen |
| `PUT /api/settings/{key}` | Settings — Wert ändern |
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
| Anderer Fehler | Fehlertext mit einem Knopf zum Wiederholen |

Es gibt keinen Pfad, der einen Fehler verschluckt und mit leeren Daten
weiterläuft.

---

## Was die App nicht anfasst

Server neu starten, Updates auslösen, Caddy oder die Netzwerk-Einstellungen
ändern, Aufbewahrung ausführen. Das bleibt der Weboberfläche vorbehalten — ein
Fehlgriff ist auf einem Telefon zu leicht passiert.
