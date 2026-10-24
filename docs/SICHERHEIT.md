# Sicherheit

Was die App schützt, was nicht — und was offen ist.

[← Doku-Übersicht](README.md) ·
[Server-Schnittstelle](SERVER-API.md) ·
[Fehler melden](https://github.com/Phydran6/Logbot-Android-App/issues)

---

## Was geschützt ist

| | |
|:--|:--|
| **Zugangsdaten** | Android: `EncryptedSharedPreferences`, AES-256-GCM, Schlüssel im Android Keystore, vom Backup ausgeschlossen. Lässt sich der Speicher nicht mehr entschlüsseln (z.B. nach einer Wiederherstellung), legt die App ihn neu an, statt abzustürzen. iOS: Schlüsselbund mit `WhenUnlockedThisDeviceOnly` |
| **Nur https** | Die Einrichtung lehnt `http://` ab, das Manifest verbietet unverschlüsselten Verkehr (`usesCleartextTraffic=false`) |
| **Anmelde-Link per Mail** | Der Link allein meldet niemanden an: Eingelöst wird er nur zusammen mit einem Geheimnis (PKCE-`code_verifier`), das beim Anfordern auf dem Handy entsteht und das Gerät erst beim Einlösen verlässt. Der Server speichert vom Link nur einen Abdruck, er gilt 15 Minuten und einmal. Am Rechner geöffnet, muss ein Code eingegeben werden, den nur die anfordernde App zeigt – er steht nicht in der Mail. Ein Aufruf des Links durch Mailprogramme oder Virenscanner löst nichts aus. Ist MFA eingeschaltet, folgt der zweite Faktor wie beim Passwort |
| **App-Sperre** | Optional: Fingerabdruck, Gesicht oder Gerätecode beim Start und nach mehr als einer Minute im Hintergrund. Bei Abbruch schließt die App, ohne dass ein Token entschlüsselt wurde |
| **Abgelaufene Sitzung** | Jede Anfrage, die der Server mit 401 ablehnt, verwirft den Token und führt zur Anmeldung |
| **Benachrichtigungen** | Der Server schickt per Push nur ID, Schweregrad und Titel, nach RFC 8291 verschlüsselt – der Push-Dienst (z.B. ntfy) sieht keinen Inhalt. Unverschlüsselte Push-Nachrichten zeigt die App nicht an, sondern fragt beim eigenen Server nach. Auf dem Sperrbildschirm steht nur „Logbot: Kritisch“, nicht der Inhalt |
| **KI** | Die App schickt nur die Frage; welche Logzeilen an die KI gehen, entscheidet der Server, gedeckelt durch die dort eingestellte Höchstzahl. Unter jeder Antwort steht, wie viele Zeilen von welchen Geräten gesendet wurden. Ist die KI auf dem Server aus, verlässt nichts den Server |
| **Kein Protokoll im Release** | Das Netzwerkprotokoll läuft nur im Debug-Build, der `Authorization`-Kopf ist auch dort geschwärzt |
| **Keine Telemetrie** | Keine Analytik, keine Werbe-IDs, keine Absturzberichte an Dritte. Die App spricht mit der eingetragenen Instanz – und, nur wenn Push eingeschaltet ist, über die gewählte Verteiler-App |

---

## Was die App bewusst nicht tut

**Den Server umbauen.** Neustart, Updates, Netzwerk, Aufbewahrung ausführen,
KI-, Mail-, LDAP- oder Archiv-Anbindung einrichten — das bleibt der
Weboberfläche vorbehalten. Ein Fehlgriff auf einem Telefon ist zu leicht
passiert. In den Einstellungen lassen sich nur einfache Werte ändern, und nur
von Administratoren.

**Rechte vortäuschen.** Was nur Administratoren dürfen, blendet die App aus –
geprüft wird es trotzdem auf dem Server.

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
| Android | `POST_NOTIFICATIONS` (optional) | Benachrichtigungen bei Meldungen, wird erst beim Einschalten erfragt |
| Android | `WAKE_LOCK` (über UnifiedPush) | Push zustellen, während das Gerät schläft |
| iOS | Face ID | App-Sperre |

Mehr fragt die App nicht an.

---

## Etwas gefunden?

Bitte **kein öffentliches Issue** für Sicherheitslücken. Kurze Nachricht über
die Kontaktmöglichkeiten des Repository-Eigentümers, dann wird es zeitnah
behandelt.
