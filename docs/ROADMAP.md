# Roadmap – Umbau zur nativen App

Vom WebView-Wrapper zur eigenständigen nativen Android-App (Jetpack Compose), die die
[Logbot-Server](https://github.com/Phydran6/Logbot-Server)-Oberfläche nativ nachbaut.
Umsetzung in Phasen, je ~1 Arbeits-Session, damit der Umfang überschaubar bleibt.

| Phase | Inhalt | Status |
|-------|--------|--------|
| **0 – Discovery** | Ist-Zustand der App + Server-API/Screens analysiert, Machbarkeit bestätigt, Tech-Entscheidung (Android-only Compose) | ✅ erledigt |
| **1 – Architektur & Beschreibung** | [ARCHITEKTUR.md](ARCHITEKTUR.md), Roadmap, README/CHANGELOG auf native Ausrichtung umgebaut. *Kein Produktiv-Code.* | ✅ erledigt |
| **2a – Skeleton** | Build modernisiert (Kotlin + Compose), Material-3-Theme, Single-Activity, Navigation-Drawer als Server-Sidebar mit Platzhalter-Screens. WebView entfernt. | ✅ erledigt |
| **2b – Auth nativ** | Hilt + KSP, Networking-Layer + API-Client, **Auth**: Instanz-URL, Passwort-Login, MFA, QR-App-Login (Token-Exchange), verschlüsselter Token-Speicher; Rollen-Gating + 401-Logout; **Biometrie-/PIN-App-Lock**. | ✅ erledigt |
| **3a – Read-Screens** | Dashboard, Health, Logs (Liste + Filter + Detail + Mehr-laden), Agents | ✅ erledigt |
| **3b – Management-Screens** | Users (CRUD/MFA-Reset), Webhooks (CRUD/Token/URL), Settings (Werte + DB), Branding (Whitelabel), Agents-Löschen | ✅ erledigt |
| **4 – Feinschliff** | Lade-/Fehler-/Leer-Zustände + Retry, Pull-to-refresh, Endlos-Scrollen per Cursor, Fehlertexte vom Server, zentrale 401-Abmeldung | ✅ erledigt |
| **5 – Mehr als ein Spiegel** | Anmeldung per E-Mail-Link, KI-Assistent mit Blick in die Logs, Meldungen mit Push (UnifiedPush) und Hintergrundabfrage, Übersicht mit Urteil | ✅ erledigt (Meldungen brauchen das nächste Server-Release) |

## Funktionsparität – Checkliste

Erhalten bleiben (nach Compose portiert):
- [x] Setup: Instanz-URL eingeben
- [x] Setup: QR-App-Login (mit korrektem Token-Exchange)
- [x] Verschlüsselter Token-Speicher
- [x] Biometrie-/PIN-App-Lock
- [x] 401-Behandlung (Logout/Session-Reset)

Neu nativ (Server-Screens):
- [x] Login + MFA (TOTP/Backup-Code)
- [x] Dashboard
- [x] Logs (Liste, Filter, Detail, Mehr-laden)
- [x] Agents (Liste + Löschen)
- [x] Users (CRUD + MFA-Reset)
- [x] Webhooks (CRUD + Token + Aufruf-URL)
- [x] Health
- [x] Settings (Werte editieren + DB-Infos)
- [x] Branding (Whitelabel bearbeiten)

---

## Offen

| Punkt | Warum |
|:--|:--|
| **iOS nachziehen** | Die SwiftUI-App bildet noch den früheren Stand mit vier Bereichen ab und ruft `/api/mail` auf, das es im Server so nicht gibt. E-Mail-Link, KI-Assistent und Meldungen fehlen dort. Push unter iOS ginge nur über APNs – UnifiedPush gibt es dort nicht |
| Push über Firebase (FCM) | Für Geräte ohne Verteiler-App. Bräuchte ein Firebase-Projekt und einen Server-Schlüssel je Installation – bewusst nicht gebaut, die 15-Minuten-Abfrage deckt den Fall ab |
| Offline-Caching | Logs und Stammdaten ohne Verbindung anzeigen |
| Certificate Pinning | Siehe [SICHERHEIT.md](SICHERHEIT.md) — bewusst offen, Abwägung dort |
| Agent-Retention-UI | Aufbewahrung je Gerät einstellen |
| Branding-Logo-Upload | Bisher nur Texte und Farben |
| Verlauf des KI-Chats speichern | Heute lebt ein Gespräch, solange der Bildschirm offen ist |
