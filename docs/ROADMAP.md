# Roadmap – Umbau zur nativen App

Vom WebView-Wrapper zur eigenständigen nativen Android-App (Jetpack Compose), die die
[Logbot-Server](https://github.com/Phydran6/Logbot-Server)-Oberfläche nativ nachbaut.
Umsetzung in Phasen, je ~1 Arbeits-Session, damit der Umfang überschaubar bleibt.

| Phase | Inhalt | Status |
|-------|--------|--------|
| **0 – Discovery** | Ist-Zustand der App + Server-API/Screens analysiert, Machbarkeit bestätigt, Tech-Entscheidung (Android-only Compose) | ✅ erledigt |
| **1 – Architektur & Beschreibung** | [ARCHITECTURE.md](ARCHITECTURE.md), Roadmap, README/CHANGELOG auf native Ausrichtung umgebaut. *Kein Produktiv-Code.* | ✅ erledigt |
| **2a – Skeleton** | Build modernisiert (Kotlin + Compose), Material-3-Theme, Single-Activity, Navigation-Drawer als Server-Sidebar mit Platzhalter-Screens. WebView entfernt. | ✅ erledigt |
| **2b – Auth nativ** | Hilt + KSP, Networking-Layer + API-Client, **Auth**: Instanz-URL, Passwort-Login, MFA, QR-App-Login (Token-Exchange), verschlüsselter Token-Speicher; Rollen-Gating + 401-Logout. *(Biometrie-Lock noch offen.)* | 🟦 Login steht |
| **3a – Read-Screens** | Dashboard (Stats/Charts), Logs (Paging + Filter + Detail), Health, Agents-Liste | offen |
| **3b – Management-Screens** | Users, Webhooks, Agents-Aktionen (Retention/Decommission), Settings, Branding | offen |
| **4 – Feinschliff** | Performance, Offline/Caching, Pull-to-refresh, Leer-/Fehlerzustände, Polishing, ggf. Cert-Pinning | offen |

## Funktionsparität – Checkliste

Erhalten bleiben (nach Compose portiert):
- [x] Setup: Instanz-URL eingeben
- [x] Setup: QR-App-Login (mit korrektem Token-Exchange)
- [x] Verschlüsselter Token-Speicher
- [ ] Biometrie-/PIN-App-Lock
- [x] 401-Behandlung (Logout/Session-Reset)

Neu nativ (Server-Screens):
- [x] Login + MFA (TOTP/Backup-Code)
- [ ] Dashboard
- [ ] Logs (Liste, Filter, Detail)
- [ ] Agents
- [ ] Users
- [ ] Webhooks
- [ ] Health
- [ ] Settings
- [ ] Branding
