# scripts/ — Hilfsskripte

Fünf Skripte. Keins davon braucht man im Alltag; sie laufen in der CI und
sagen im Fehlerfall, **woran** es liegt.

[← Zurück zur Übersicht](../README.md) ·
[Release-Prozess](../docs/RELEASE.md) ·
[Abläufe](../.github/README.md)

---

| Skript | Wozu | Wer ruft es auf |
|:--|:--|:--|
| `ensure_ios_project.sh` | Erzeugt `ios/Logbot.xcodeproj` aus `project.yml` | CI und Mensch, vor jedem iOS-Build |
| `check_apple_secrets.sh` | Prüft die sechs Apple-Secrets einzeln | `release.yml`, `deploy.yml`, `ios-signing.yml` |
| `check_match_repo.sh` | Prüft Zugriff und Inhalt des Zertifikats-Repositorys | dieselben |
| `run_fastlane.sh` | Ruft eine fastlane-Lane auf und deutet Fehler | dieselben |
| `extract_changelog.sh` | Schneidet einen Versionsabschnitt aus `docs/CHANGELOG.md` | `release.yml` |

---

## Warum diese Prüfungen

Die iOS-Kette scheitert sonst tief in fastlane, mit Meldungen, in denen der
eigentliche Grund nicht vorkommt.

**Beispiel:** Fehlt `MATCH_GIT_TOKEN`, fragt git nach einem Passwort und
bricht ab mit `could not read Password`. Das liest sich wie ein falsches
`MATCH_PASSWORD` — und ist etwas völlig anderes. `check_apple_secrets.sh`
sagt stattdessen, welches der sechs Secrets fehlt, bevor irgendetwas gebaut
wird.

`run_fastlane.sh` schreibt die Ausgabe mit und fasst einen Fehler als
**Annotation** zusammen. Das ist kein Schmuck: Die Protokolle eines
Actions-Laufs sind nur nach Anmeldung lesbar, Annotationen stehen auf der
Zusammenfassungsseite.

---

## Vor dem Taggen prüfen

```bash
bash scripts/extract_changelog.sh 1.0.0
```

Findet das Skript nichts, bekommt das Release einen Platzhalter statt der
Notizen. Also erst den Abschnitt in [`docs/CHANGELOG.md`](../docs/CHANGELOG.md)
anlegen, dann taggen.
