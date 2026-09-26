/**
 * Verschlüsselter Speicher für Zugangsdaten (Instanz-URL, Access-Token, Biometrie-Flag).
 * Werte liegen AES-256-GCM-verschlüsselt; der Schlüssel im Android Keystore.
 */
package de.phytech.logbot.core.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var instanceUrl: String?
        get() = prefs.getString(KEY_URL, null)
        set(value) = prefs.edit().putString(KEY_URL, value).apply()

    var accessToken: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()

    val hasInstance: Boolean get() = !instanceUrl.isNullOrBlank()
    val isLoggedIn: Boolean get() = !instanceUrl.isNullOrBlank() && !accessToken.isNullOrBlank()

    /** Speichert Instanz-URL und Token in einem Schritt. */
    fun saveSession(url: String, token: String) {
        prefs.edit().putString(KEY_URL, url).putString(KEY_TOKEN, token).apply()
    }

    /** Nur das Token verwerfen (z. B. nach 401) – Instanz-URL bleibt erhalten. */
    fun clearToken() = prefs.edit().remove(KEY_TOKEN).apply()

    /** Alles löschen (vollständiger Reset). */
    fun clearAll() = prefs.edit().clear().apply()

    companion object {
        private const val PREFS_FILE = "logbot_secure_prefs"
        private const val KEY_URL = "instance_url"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_BIOMETRIC = "biometric_enabled"
    }
}
