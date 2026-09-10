# Technische Dokumentation

Fünf Seiten. Jede beantwortet eine Frage.

[← Zurück zur Übersicht](../README.md)

---

| Seite | Beantwortet |
|:--|:--|
| [ARCHITEKTUR.md](ARCHITEKTUR.md) | Wie ist die App aufgebaut, und warum so? |
| [SERVER-API.md](SERVER-API.md) | Welche Endpunkte nutzt die App, was ist Pflicht, was optional? |
| [RELEASE.md](RELEASE.md) | Wie kommt eine neue Version heraus? |
| [STORE.md](STORE.md) | Was ist zu tun, damit die App in Play Store und App Store landet? |
| [SICHERHEIT.md](SICHERHEIT.md) | Was schützt die App, was nicht — und was offen ist |
| [CHANGELOG.md](CHANGELOG.md) | Was hat sich wann geändert? |

---

## Kurz gefasst

Logbot ist eine Client-App zu einem selbst betriebenen Server. Sie speichert
nichts eigenes außer den Zugangsdaten zu **einer** Instanz, hat keine
Benutzerkonten, keine Cloud und keine Telemetrie.

```
┌──────────────────┐   HTTPS + Bearer-Token   ┌─────────────────────┐
│  Logbot-App      │ ───────────────────────► │  Logbot-Server      │
│  Android / iOS   │                          │  FastAPI + Postgres │
└──────────────────┘ ◄─────────────────────── └─────────────────────┘
        │                    JSON                        │
        │                                                │
   Zugangsdaten                                    Agenten, Syslog,
   verschlüsselt                                   Postfix, Webhooks
   auf dem Gerät
```

Der Server liegt in einem eigenen Repository:
[Phydran6/Logbot-Server](https://github.com/Phydran6/Logbot-Server).

---

## Nachbarverzeichnisse

[`android/`](../android/README.md) ·
[`ios/`](../ios/README.md) ·
[`scripts/`](../scripts/README.md) ·
[`.github/`](../.github/README.md)
