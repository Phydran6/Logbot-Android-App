# .github/ — die Abläufe

Vier Abläufe. Einer läuft ständig, einer bei jedem Tag, zwei auf Zuruf.

[← Zurück zur Übersicht](../README.md) ·
[Release-Prozess](../docs/RELEASE.md) ·
[Veröffentlichen](../docs/STORE.md)

---

| Ablauf | Wann | Was passiert |
|:--|:--|:--|
| [`ci.yml`](workflows/ci.yml) | jeder Push, jeder Pull Request | Android: Tests, Lint, Debug-APK (30 Tage als Artefakt). iOS: unsigniert übersetzen |
| [`release.yml`](workflows/release.yml) | Tag `v*.*.*` | Signiertes AAB + APK, IPA, GitHub-Release mit Changelog-Notizen |
| [`deploy.yml`](workflows/deploy.yml) | nach erfolgreichem Release, oder von Hand | AAB → Play-Track `internal`, IPA → TestFlight |
| [`ios-signing.yml`](workflows/ios-signing.yml) | einmalig, von Hand | Erzeugt Apple-Zertifikat und -Profil im match-Repository |
| [`mirror-to-gitlab.yml`](workflows/mirror-to-gitlab.yml) | Push auf `main`, Tags | Spiegelt nach GitLab, damit F-Droid eine Quelle hat |

---

## Ohne Secrets bleibt alles grün

Jeder Schritt, der ein Store-Geheimnis braucht, prüft es vorher und
überspringt sich mit einer Warnung, wenn es fehlt:

- Kein `ANDROID_KEYSTORE_BASE64` → Release baut mit dem Debug-Key weiter, die
  Warnung sagt, dass Play so ein Paket nicht annimmt
- Apple-Zugang unvollständig → iOS wird unsigniert gebaut und als
  `*-ios-unsigniert.ipa` beigelegt; das Release erscheint trotzdem
- Kein `PLAY_SERVICE_ACCOUNT_JSON` → Deploy überspringt Android

Welche Secrets es gibt und woher sie kommen, steht in
[docs/RELEASE.md](../docs/RELEASE.md#github-secrets).

---

## Der übliche Weg

```bash
# 1. Abschnitt in docs/CHANGELOG.md anlegen und prüfen
bash scripts/extract_changelog.sh 1.1.0

# 2. Taggen
git tag v1.1.0 && git push origin v1.1.0
```

Danach läuft `release.yml`, und wenn es durch ist, `deploy.yml`. Von Hand ist
nichts mehr zu tun — außer den Entwurf in der Play Console freizugeben.
