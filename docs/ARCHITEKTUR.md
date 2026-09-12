# Architektur

Wie die App aufgebaut ist, und warum so.

[← Doku-Übersicht](README.md) ·
[Server-Schnittstelle](SERVER-API.md) ·
[Sicherheit](SICHERHEIT.md)

---

## Der Grundgedanke

Logbot war ursprünglich eine Vollbild-`WebView` und sonst nichts. Das war
richtig für den Anfang — die Weboberfläche des Servers kann alles, und alles
doppelt zu bauen wäre Unfug.

Es hatte nur einen Haken: **Die drei Dinge, die man am Telefon wirklich
braucht, waren die drei umständlichsten.** Serverzustand ansehen hieß:
Weboberfläche laden, Menü ausklappen, Dashboard antippen, warten. Logs lesen
hieß: eine Tabelle mit acht Spalten auf einem Bildschirm, der für drei Platz
hat.

Also gilt jetzt eine einfache Regel:

> **Was man am Telefon oft braucht, ist nativ. Alles andere bleibt Web.**

Nativ sind Status, Logs und Mail. Die Weboberfläche ist der vierte Bereich und
deckt weiterhin Benutzer, Webhooks, Branding, Updates und alles Seltene ab.

---

## Die Schichten

```
ui/          StatusFragment   LogsFragment   MailFragment   WebFragment
             (Anzeige, kein Wissen über HTTP)
                    │
data/        LogbotApi        Models         Credentials
             (HTTP + JSON)    (Typen)        (Schlüsselverwahrung)
                    │
             Logbot-Server, REST über HTTPS
```

Kein ViewModel, kein Repository-Muster, keine Dependency Injection. Die App
hat vier Ansichten und fünf Endpunkte; jede weitere Schicht wäre Zeremonie
ohne Nutzen. Was an Zustand da ist, lebt im jeweiligen Fragment und ist beim
Wechsel weg — außer dem, was der Rahmen bewusst festhält (siehe unten).

Die iOS-Fassung folgt derselben Aufteilung mit SwiftUI und `async/await`.

---

## Der Rahmen

`MainActivity` legt jeden Bereich **einmal** an und blendet danach nur noch
ein und aus (`FragmentTransaction.hide/show`), statt ihn auszutauschen.

Das hat drei Folgen, alle erwünscht:

1. **Zustand bleibt.** Filter, Scrollposition und der Verlauf der Weboberfläche
   überleben einen Wechsel zwischen den Bereichen.
2. **Keine Übergangsanimation.** Es gibt nichts zu animieren, wenn nichts
   ausgetauscht wird. Ein Tipp auf die Leiste wechselt sofort.
3. **Die `WebView` lädt nicht neu.** Wer in der Weboberfläche drei Ebenen tief
   ist, steht nach einem Abstecher in die Logs wieder dort.

Der Preis: Alle vier Bereiche liegen gleichzeitig im Speicher. Bei vier
schlanken Ansichten ist das der günstigere Handel.

---

## Nebenläufigkeit

Ein Thread-Pool mit drei Threads, Rückmeldung auf dem Hauptthread. Mehr
gleichzeitige Anfragen stellt die App nie — Status, Logs und Mail können
parallel laufen, das war es.

Jede Rückmeldung prüft, ob die Ansicht noch da ist (`isAdded`), bevor sie
etwas schreibt. Die Logliste prüft zusätzlich, ob die Antwort noch zur
aktuellen Anfrage gehört: Wer schnell filtert, soll nicht das Ergebnis von
vorhin sehen.

---

## Anzeige der Logs

Das ist der Teil, an dem die alte Ansicht scheiterte.

| Alte Tabelle | Jetzt |
|:--|:--|
| Acht gleich gewichtete Spalten | Die Nachricht groß, alles andere klein darunter |
| Schweregrad als Textspalte | Farbstreifen am linken Rand |
| Datum in jeder Zeile | Trennzeile beim Tageswechsel |
| Seitenzahlen unten | Nachladen, sobald die letzten fünf Zeilen sichtbar werden |
| Filter in einem Dialog | Chip-Reihen direkt über der Liste |

**Gefiltert wird auf dem Server.** Bei Millionen Zeilen ist alles andere
aussichtslos; `LogQuery` bildet genau die Parameter ab, die `/api/logs`
versteht. Die Filterwerte selbst kommen ebenfalls vom Server
(`/api/logs/filter-options`) — ein neuer Logtyp erscheint in der App ohne
App-Update.

---

## Der Mail-Bereich

Zwei Quellen, bewusst getrennt:

1. **Die Logzeilen** kommen aus `/api/logs?category=mail`. Diese Kategorie
   bündelt serverseitig `postfix`, `dovecot`, `sendmail`, `exim` und
   `opendkim`. Das läuft mit **jedem** Serverstand.
2. **Dienstzustand, Warteschlange und Passwort-Reset** liegen hinter
   `/api/mail/*`. Diese Endpunkte sind neu; ältere Server antworten mit 404.

Bei 404 zeigt der Bereich einen Hinweis statt einer Fehlermeldung, und die
Logzeilen funktionieren weiter. Die App ist dadurch mit jedem Serverstand
benutzbar und wird besser, sobald der Server nachzieht. Der Kontrakt steht in
[SERVER-API.md](SERVER-API.md#optional-apimail).

---

## Breite statt Bruchstellen

Kein eigenes Tablet-Layout. Ab 600 dp und ab 840 dp Breite wächst nur der
Seitenrand (`res/values-w600dp/dimens.xml`, `res/values-w840dp/dimens.xml`).

Eine Textzeile über die volle Breite eines Tablets liest sich schlecht; eine
Spalte mit Luft daneben liest sich gut. Zwei Spalten wären ein zweites Layout
mit eigenen Fehlern.

---

## Farben

Die Oberfläche nimmt die Material-3-Palette des Systems und folgt hell/dunkel
automatisch. Fest sind nur die **Schweregrad-Farben** — sie tragen eine
Bedeutung und dürfen nicht mit dem Systemakzent wandern.

Für dunkle Hintergründe liegen aufgehellte Gegenstücke in
`res/values-night/colors.xml`. Dieselben Werte stehen in `ios/Logbot/Theme.swift`.
