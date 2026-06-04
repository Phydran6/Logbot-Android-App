# 📱 Logbot Android

**Nativer Android-Client für einen selbst gehosteten
[Logbot-Server](https://github.com/Phydran6/Logbot-Server).**

Logbot Android ist **kein** eigenständiges Log-Werkzeug, sondern die mobile
Oberfläche zu *deiner* Logbot-Instanz: Die App verbindet sich mit dem Server,
holt die Daten über dessen REST-API und bildet die Server-Oberfläche
(Dashboard, Logs, Agents, Users, Webhooks, Health, Settings, Branding) nativ ab.

---

## 🚧 Status: Umbau auf native App

Die App wird gerade vom bisherigen **WebView-Wrapper** zu einer **eigenständigen
nativen App** (Jetpack Compose) umgebaut. Grund: Die WebView-Lösung war einfach,
aber nicht praxistauglich genug — die native App ist schneller und
benutzerfreundlicher, bei gleicher Funktionsabdeckung.

- Aktuelle Phase: **Phase 1 – Architektur & Beschreibung**
- Bauplan: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- Fahrplan: [docs/ROADMAP.md](docs/ROADMAP.md)

> Solange der Umbau läuft, kann der gebaute Stand noch Teile der alten
> WebView-Variante enthalten. Die README beschreibt das **Zielbild** der App.

---

## ✨ Funktionen

**Verbindung & Anmeldung**
- Verbindung zur eigenen Instanz über **URL + Login** oder **QR-App-Login**
  (kurzlebiger Einmal-Token, der serverseitig gegen ein Zugangstoken getauscht wird)
- **MFA/2FA** (TOTP oder Backup-Code), passend zum Server
- **Biometrie-/PIN-App-Lock** beim Start
- **Verschlüsselter Token-Speicher** (Android Keystore)

**Oberfläche (spiegelt den Server)**
- 📊 **Dashboard** mit Statistiken
- 🔍 **Logs** mit Filter, Suche und Detailansicht (paginiert, für große Datenmengen)
- 🖥️ **Agents** – Geräte, Online-Status, Retention
- 👥 **Users** – Benutzer- und Rollenverwaltung (Admin)
- 🔗 **Webhooks** – Verwaltung und Aufruf-Statistik
- ❤️ **Health** – System-Ressourcen (CPU/RAM/Disk/Uptime)
- ⚙️ **Settings** & 🎨 **Branding** (Whitelabel, Dark/Light)

---

## 🛠️ Technologie

- **Kotlin** + **Jetpack Compose** + **Material 3**
- Architektur: **MVVM + Repository**, Single-Activity, Navigation-Compose
- **Hilt** (DI), **Retrofit/OkHttp** + kotlinx.serialization (REST), **Coroutines/Flow**
- **Paging 3** für die Log-Liste
- Gradle Build System

Details im [Architektur-Dokument](docs/ARCHITECTURE.md).

---

## 🚀 Voraussetzungen

- Eine erreichbare **Logbot-Server-Instanz** (HTTPS) – siehe
  [Logbot-Server](https://github.com/Phydran6/Logbot-Server)
- Android-Gerät mit **Android 7.0+ (API 24)**

---

## ▶️ Einrichtung in der App

1. App starten → **Setup-Screen**
2. Entweder:
   - **Instanz-URL** (`https://…`) eingeben und mit Benutzer/Passwort anmelden
     (ggf. MFA-Code), **oder**
   - **QR-Code scannen** (im Server-Web-UI im Benutzer-Bearbeiten-Modal erzeugbar)
3. Optional **Biometrie-Lock** aktivieren
4. Loslegen – Navigation über das Menü (gleiche Punkte wie im Server)

---

## 🧪 Build

```bash
# Debug-Build
./gradlew assembleDebug

# Release-Build (benötigt signing.properties, siehe unten)
./gradlew assembleRelease
```

Release-Signing über eine gitignorete `signing.properties` (`storeFile`,
`storePassword`, `keyAlias`, `keyPassword`). Fehlt sie, wird die Release-Signing-Config
einfach nicht registriert (Debug-Builds funktionieren trotzdem).

---

## 🔒 Sicherheitshinweis – Certificate Pinning

Die App implementiert **kein Certificate Pinning**. Sie prüft, dass die HTTPS-Verbindung
ein gültiges Zertifikat verwendet, verifiziert aber nicht das *spezifische* Zertifikat
deiner Instanz. In einem nicht vertrauenswürdigen Netzwerk könnte ein Angreifer
theoretisch per Man-in-the-Middle ein eigenes gültiges Zertifikat einschleusen und den
Token abfangen.

Das Projekt ist primär für den Eigeneinsatz in kontrollierten Umgebungen (eigenes
Netzwerk, VPN) gebaut. Certificate Pinning brächte zudem operativen Aufwand (neue
App-Version bei jedem Server-Zertifikatswechsel). Pull Requests dazu sind willkommen.

---

## 🤝 Mitwirken

Pull Requests sind willkommen. Bitte vorab ein Issue für größere Änderungen erstellen.

## 📄 Lizenz

MIT-Lizenz – siehe [LICENSE](LICENSE).

## 👤 Autor

Phydran6
