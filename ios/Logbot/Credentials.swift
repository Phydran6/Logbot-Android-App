//
//  Credentials.swift
//  Logbot
//
//  Zugangsdaten der Instanz: Instanz-URL und App-Token.
//
//  Beides liegt im Schluesselbund (kSecClassGenericPassword) mit
//  `ThisDeviceOnly` - die Werte sollen nicht per iCloud-Schluesselbund auf
//  andere Geraete wandern. Das Android-Gegenstueck macht dasselbe mit
//  EncryptedSharedPreferences.
//

import Foundation
import Security

enum Credentials {

    private static let service = "de.phytech.logbot"
    private static let urlKey = "instance_url"
    private static let tokenKey = "auth_token"
    private static let lockKey = "app_lock_enabled"

    // MARK: - Lesen

    static var instanceURL: String? {
        guard let raw = read(urlKey), !raw.isEmpty else { return nil }
        // Ohne abschliessenden Schraegstrich, damit Pfade sauber anhaengen.
        return raw.hasSuffix("/") ? String(raw.dropLast()) : raw
    }

    static var authToken: String? {
        guard let raw = read(tokenKey), !raw.isEmpty else { return nil }
        return raw
    }

    static var isConfigured: Bool { instanceURL != nil && authToken != nil }

    /// Die App-Sperre ist kein Geheimnis - ein Schalterzustand reicht in den Defaults.
    static var appLockEnabled: Bool {
        get { UserDefaults.standard.bool(forKey: lockKey) }
        set { UserDefaults.standard.set(newValue, forKey: lockKey) }
    }

    // MARK: - Schreiben

    static func save(url: String, token: String, appLock: Bool) {
        let trimmed = url.hasSuffix("/") ? String(url.dropLast()) : url
        write(urlKey, trimmed)
        write(tokenKey, token)
        appLockEnabled = appLock
    }

    /// Loescht URL und Token. Die Sperre-Einstellung bleibt bewusst stehen.
    static func clear() {
        delete(urlKey)
        delete(tokenKey)
    }

    // MARK: - Schluesselbund

    private static func query(_ account: String) -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
    }

    private static func read(_ account: String) -> String? {
        var q = query(account)
        q[kSecReturnData as String] = true
        q[kSecMatchLimit as String] = kSecMatchLimitOne

        var item: CFTypeRef?
        guard SecItemCopyMatching(q as CFDictionary, &item) == errSecSuccess,
              let data = item as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    private static func write(_ account: String, _ value: String) {
        // Erst loeschen, dann anlegen: SecItemUpdate braucht Sonderbehandlung
        // fuer den Fall "gibt es noch nicht", und das ist hier nur Ballast.
        delete(account)
        var q = query(account)
        q[kSecValueData as String] = Data(value.utf8)
        q[kSecAttrAccessible as String] = kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        SecItemAdd(q as CFDictionary, nil)
    }

    private static func delete(_ account: String) {
        SecItemDelete(query(account) as CFDictionary)
    }

    // MARK: - Version

    static var appVersion: String {
        let info = Bundle.main.infoDictionary
        let short = info?["CFBundleShortVersionString"] as? String ?? "?"
        let build = info?["CFBundleVersion"] as? String ?? "?"
        return "\(short) (\(build))"
    }
}
