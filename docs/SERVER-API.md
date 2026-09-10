# Server-Schnittstelle

Welche Endpunkte die App benutzt, was Pflicht ist und was optional.

[← Doku-Übersicht](README.md) ·
[Architektur](ARCHITEKTUR.md) ·
[Logbot-Server](https://github.com/Phydran6/Logbot-Server)

---

## Anmeldung

Jede Anfrage trägt den App-Token als Kopfzeile:

```http
Authorization: Bearer <token>
```

Den Token erzeugt die Weboberfläche unter **App verbinden**
(`POST /api/auth/app-token`). Er entsteht nur aus einer bereits
angemeldeten Web-Sitzung — auch dann, wenn der Server MFA verlangt. Die App
kennt das Passwort nie.

**401 oder 403** heißt: Token abgelaufen oder zurückgezogen. Die App löscht
dann die Zugangsdaten und führt zurück in die Einrichtung.

---

## Pflicht: was jeder Serverstand kann

### `GET /api/health/detailed`

Speist den Status-Bereich. Antwort:

```json
{
  "status": "healthy",
  "version": "2026.08.14.14.00.00",
  "uptime_seconds": 384512.4,
  "cpu_percent": 7.3,
  "memory_percent": 41.8,
  "disk_percent": 63.0,
  "database_connected": true,
  "logs_total": 8123456,
  "logs_last_24h": 91240,
  "agents_total": 7,
  "agents_online": 6
}
```

`status` ist `healthy` oder `degraded`. Die App zeigt „Server läuft" nur,
wenn `status` gesund **und** `database_connected` wahr ist.

### `GET /api/logs`

Speist die Logliste. Genutzte Parameter:

| Parameter | Wofür |
|:--|:--|
| `page`, `page_size` | Seitenweises Nachladen, 50 je Seite |
| `search` | Volltext in der Nachricht |
| `hostname` | Teilstring des Hostnamens |
| `min_severity` | Schweregrad-Gruppe: dieses Level und alles Dringendere |
| `category` | Logtyp, siehe `filter-options` |

Antwort: `{ "items": [...], "total": 0, "page": 1, "page_size": 50 }`.
Ein Eintrag hat `id`, `hostname`, `ip_address`, `timestamp`, `level`,
`source`, `message`.

**Zeitstempel** kommen als ISO-8601 in UTC, teils ohne Zonenangabe
(`2026-05-29T22:30:00.123456`). Die App liest sie als UTC und zeigt sie in
der Gerätezone.

### `GET /api/logs/{id}`

Ein einzelner Eintrag samt `raw_message` und `facility`. Die App holt ihn
erst beim Aufklappen der Detailansicht — der Rohtext ist oft ein Vielfaches
der aufbereiteten Nachricht und würde jede Seite aufblähen.

### `GET /api/logs/filter-options`

Die Auswahllisten für die Filterreihen:

```json
{
  "hostnames": ["web01", "mail01"],
  "severities": [{"key": "error", "label": "Fehler und dringender"}],
  "categories": [{"key": "mail", "label": "Mail"}]
}
```

Die App kennt diese Listen **nicht** fest. Ein neuer Logtyp auf dem Server
erscheint ohne App-Update.

---

## Optional: `/api/mail/*`

Diese beiden Endpunkte gibt es im Server noch nicht. Die App fragt sie an,
behandelt **404 als „kennt der Serverstand noch nicht"** und zeigt einen
Hinweis statt einer Fehlermeldung. Der Mail-Bereich bleibt auch ohne sie
benutzbar: Die Logzeilen kommen aus `/api/logs?category=mail` und laufen
mit jedem Stand.

Dieser Abschnitt ist der Kontrakt — wer die Endpunkte im Server nachrüstet,
findet hier, was die App erwartet.

### `GET /api/mail/status`

Zustand des Mailsystems auf dem Host.

```json
{
  "postfix_running": true,
  "queue_length": 0,
  "deferred_length": 2,
  "last_error": "",
  "hostname": "mail01"
}
```

| Feld | Herkunft auf dem Server |
|:--|:--|
| `postfix_running` | `systemctl is-active postfix` über den vorhandenen Host-Zugriff (`hostexec.py`) |
| `queue_length` | `mailq` bzw. `postqueue -p`, aktive Warteschlange |
| `deferred_length` | dieselbe Quelle, zurückgestellte Nachrichten |
| `last_error` | letzte Fehlerzeile aus dem Mail-Log, oder leer |

Nur für angemeldete Benutzer. Ein kurzer Cache (wie bei
`/api/health/detailed`) ist sinnvoll — die App fragt bei jedem Öffnen des
Bereichs.

### `POST /api/mail/password-reset`

Stößt eine Reset-Mail über Postfix an.

```json
{ "login": "anna" }
```

Antwort **immer 200**, unabhängig davon, ob es das Konto gibt:

```json
{ "message": "Falls das Konto existiert, ist eine Mail unterwegs." }
```

> **Das ist Absicht.** Unterschiedliche Antworten für „gibt es" und „gibt es
> nicht" machen den Endpunkt zu einem Verzeichnis aller Konten. Die App zeigt
> die Antwort wörtlich an und formuliert nichts um.

Sinnvolle Begrenzung auf dem Server: eine Handvoll Anfragen je Stunde und
Absender, sonst wird der Endpunkt zum Mailversender für Fremde. Der Server
bringt dafür bereits einen Limiter mit (`backend/app/limiter.py`).

Solange der Endpunkt fehlt, sagt die App: Ein Administrator kann das Passwort
in der Weboberfläche unter **Benutzer** ändern.

---

## Was die App nicht anfasst

Alles Schreibende außer dem Passwort-Reset. Benutzer anlegen, Webhooks
ändern, Aufbewahrung einstellen, Updates auslösen, Server neu starten — das
bleibt der Weboberfläche vorbehalten. Ein Fehlgriff auf einem Telefon ist zu
leicht passiert.
