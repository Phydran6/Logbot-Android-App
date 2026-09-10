# Sicherheit

Was die App schützt, was nicht — und was offen ist.

[← Doku-Übersicht](README.md) ·
[Server-Schnittstelle](SERVER-API.md) ·
[Fehler melden](https://github.com/Phydran6/Logbot-Android-App/issues)

---

## Was geschützt ist

| | |
|:--|:--|
| **Zugangsdaten** | Android: `EncryptedSharedPreferences`, AES-256-GCM, Schlüssel im Android Keystore. iOS: Schlüsselbund mit `WhenUnlockedThisDeviceOnly` — die Werte wandern nicht per iCloud auf andere Geräte |
| **Nur https** | Die Einrichtung lehnt `http://` ab. Die eingebettete Weboberfläche läuft mit `MIXED_CONTENT_NEVER_ALLOW`, iOS mit `NSAllowsArbitraryLoads = false` |
| **App-Sperre** | Optional: Fingerabdruck, Gesicht oder Gerätecode beim Start. Bei Abbruch schließt die App, **ohne** dass ein Token entschlüsselt wurde |
| **Navigationsgrenze** | Die `WebView` folgt nur der eingerichteten Instanz und ihren Unterdomänen. Alles andere geht in den Systembrowser |
| **Kein Dateizugriff** | `allowFileAccess`, `allowContentAccess` und beide `…FromFileURLs` stehen auf `false` |
| **Token-Injektion** | Der Token wird per `JSONObject.quote()` bzw. `JSONSerialization` maskiert, bevor er in JavaScript landet |
| **Abgelaufene Sitzung** | Eine 401 auf dem Hauptrahmen löscht die Zugangsdaten und führt zurück in die Einrichtung |
| **Keine Telemetrie** | Keine Analytik, keine Werbe-IDs, keine Absturzberichte an Dritte. Die App spricht ausschließlich mit der eingetragenen Instanz |

---

## Was die App bewusst nicht tut

**Schreiben.** Außer dem Passwort-Reset löst die App keine Änderung am Server
aus. Benutzer anlegen, Aufbewahrung einstellen, Updates auslösen, neu starten
— das bleibt der Weboberfläche vorbehalten. Ein Fehlgriff auf einem Telefon
ist zu leicht passiert.

**Passwörter kennen.** Die App bekommt nie ein Passwort zu sehen. Der
App-Token entsteht nur aus einer bereits angemeldeten Web-Sitzung — auch dann,
wenn der Server MFA verlangt.

---

## Offen: Certificate Pinning

Die App prüft, dass die https-Verbindung ein **gültiges** Zertifikat
verwendet, aber nicht, ob es das Zertifikat **deiner** Instanz ist. Wer im
selben Netz ein eigenes gültiges Zertifikat einschleusen kann
(Man-in-the-Middle), kann den Token abfangen.

**Warum nicht umgesetzt:** In einer kontrollierten Umgebung — eigenes Netz,
VPN — ist der Gewinn gering, der Aufwand dauerhaft: Bei jedem
Zertifikatswechsel des Servers müsste eine neue App-Version veröffentlicht
werden, sonst sperrt sich die App selbst aus.

**Was das für dich heißt:** Wer die App in einem öffentlichen oder nicht
vertrauenswürdigen Netz benutzt, sollte das Risiko kennen. Pull Requests zu
diesem Thema sind ausdrücklich willkommen.

---

## Signaturschlüssel

Der Upload-Keystore und `signing.properties` gehören **nicht** ins
Repository. Beide stehen in [`.gitignore`](../.gitignore), die CI erzeugt sie
zur Laufzeit aus Secrets.

> **Hinweis zur Historie:** In der Vergangenheit lag eine PKCS#12-Datei mit
> dem Alias `logbot` in der Wurzel des Repositorys. Sie ist inzwischen
> entfernt — aber ein einmal gepushter Schlüssel bleibt in der Git-Historie
> und ist damit als kompromittiert zu behandeln, auch wenn er
> passwortgeschützt ist. Wer ihn noch für Release-Builds verwendet, sollte
> ihn ersetzen:
>
> - Für **Play-Store-Uploads**: einen neuen Upload-Key erzeugen und in der
>   Play Console unter *App-Integrität → Upload-Schlüssel zurücksetzen*
>   austauschen. Der App-Signaturschlüssel bei Google bleibt davon unberührt,
>   installierte Apps aktualisieren sich weiter.
> - Für **direkt verteilte APKs**: Ein Schlüsselwechsel bricht die
>   Update-Kette — Nutzer müssen die App einmal neu installieren.
>
> Zusätzlich lässt sich die Datei mit `git filter-repo` aus der Historie
> entfernen. Das schreibt die Historie um; alle Klone müssen danach neu
> geholt werden, und der GitLab-Spiegel wird beim nächsten Lauf ohnehin
> überschrieben (`--force`).

---

## Berechtigungen

| Plattform | Berechtigung | Wofür |
|:--|:--|:--|
| Android | `INTERNET` | Zugriff auf die eigene Instanz |
| Android | `CAMERA` (optional) | QR-Code bei der Einrichtung |
| iOS | Face ID | App-Sperre |

Mehr fragt die App nicht an.

---

## Etwas gefunden?

Bitte **kein öffentliches Issue** für Sicherheitslücken. Kurze Nachricht über
die Kontaktmöglichkeiten des Repository-Eigentümers, dann wird es zeitnah
behandelt.
