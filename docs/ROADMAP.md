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
| **4 – Feinschliff** | Lade-/Fehler-/Leer-Zustände + Retry überall vorhanden. Optional künftig: Offline-Caching, Pull-to-refresh, Cert-Pinning, Agent-Retention-UI, Branding-Logo-Upload. | 🟦 Grundlagen drin |

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
| **iOS nachziehen** | Die SwiftUI-App bildet noch den früheren Stand mit vier Bereichen ab und ruft `/api/mail` auf, das es im Server nicht gibt. Sie übersetzt, ist aber kein Spiegel der Android-Fassung mehr |
| Offline-Caching | Logs und Stammdaten ohne Verbindung anzeigen |
| Pull-to-refresh | Aktualisieren ohne Umweg über das Menü |
| Certificate Pinning | Siehe [SICHERHEIT.md](SICHERHEIT.md) — bewusst offen, Abwägung dort |
| Agent-Retention-UI | Aufbewahrung je Agent einstellen |
| Branding-Logo-Upload | Bisher nur Texte und Farben |
| Logs: Endlos-Scrollen | Statt „Mehr laden" |
