/**
 * Datei:        Credentials.kt
 * Projekt:      Logbot
 * Paket:        de.phytech.logbot.data
 *
 * Beschreibung:
 * Einziger Zugriffspunkt auf die gespeicherten Zugangsdaten. Werte liegen in
 * EncryptedSharedPreferences (AES-256-GCM), der Schluessel im Android Keystore
 * und damit nie auf der Platte im Klartext.
 *
 * Frueher lagen diese Helfer in der MainActivity. Sie werden inzwischen aus
 * vier Richtungen gebraucht (Setup, Shell, JS-Bruecke, API-Client) - eine
 * Activity ist dafuer der falsche Ort.
 */
package de.phytech.logbot.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object Credentials {

    const val PREF_INSTANCE_URL = "instance_url"
    const val PREF_AUTH_TOKEN = "auth_token"
    const val PREF_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val PREFS_FILE = "logbot_secure_prefs"

    fun prefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** Instanz-URL ohne abschliessenden Schraegstrich, damit Pfade sauber anhaengen. */
    fun instanceUrl(context: Context): String? =
        prefs(context).getString(PREF_INSTANCE_URL, null)?.trimEnd('/')

    fun authToken(context: Context): String? = prefs(context).getString(PREF_AUTH_TOKEN, null)

    fun isConfigured(context: Context): Boolean =
        !instanceUrl(context).isNullOrBlank() && !authToken(context).isNullOrBlank()

    fun biometricEnabled(context: Context): Boolean =
        prefs(context).getBoolean(PREF_BIOMETRIC_ENABLED, false)

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(PREF_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun save(context: Context, url: String, token: String, biometric: Boolean) {
        prefs(context).edit()
            .putString(PREF_INSTANCE_URL, url.trimEnd('/'))
            .putString(PREF_AUTH_TOKEN, token)
            .putBoolean(PREF_BIOMETRIC_ENABLED, biometric)
            .apply()
    }

    /** Loescht URL und Token. Die Biometrie-Einstellung bleibt bewusst stehen. */
    fun clear(context: Context) {
        prefs(context).edit()
            .remove(PREF_INSTANCE_URL)
            .remove(PREF_AUTH_TOKEN)
            .apply()
    }

    /** versionName aus dem PackageInfo. Faellt auf '?' zurueck. */
    fun appVersionName(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    } catch (_: Exception) {
        "?"
    }
}
