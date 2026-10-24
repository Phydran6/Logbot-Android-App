/**
 * Verschlüsselter Speicher für Zugangsdaten und die wenigen Zustände, die einen
 * Neustart der App überleben müssen (Instanz-URL, Access-Token, App-Sperre,
 * laufende Anmeldung per E-Mail-Link, Meldungs-Einstellungen).
 * Werte liegen AES-256-GCM-verschlüsselt; der Schlüssel im Android Keystore.
 */
package de.phytech.logbot.core.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Eine angeforderte, noch nicht eingelöste Anmeldung per E-Mail-Link. */
data class PendingMagicLink(
    val instanceUrl: String,
    val requestId: String,
    val codeVerifier: String,
    val matchCode: String,
    val email: String,
    val expiresAtMillis: Long,
) {
    val isExpired: Boolean get() = System.currentTimeMillis() > expiresAtMillis
}

@Singleton
class CredentialStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences by lazy { openPrefs() }

    /**
     * Öffnet die verschlüsselten Einstellungen. Lassen sie sich nicht mehr
     * entschlüsseln (Keystore-Schlüssel nach Geräte-Wiederherstellung weg,
     * Datei beschädigt), wird neu angelegt: Lieber einmal neu anmelden als eine
     * App, die bei jedem Start abstürzt.
     */
    private fun openPrefs(): SharedPreferences = try {
        create()
    } catch (e: Exception) {
        Log.w(TAG, "Verschlüsselte Einstellungen unlesbar – werden neu angelegt", e)
        context.deleteSharedPreferences(PREFS_FILE)
        create()
    }

    private fun create(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var instanceUrl: String?
        get() = prefs.getString(KEY_URL, null)
        set(value) = prefs.edit { putString(KEY_URL, value) }

    var accessToken: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit { putString(KEY_TOKEN, value) }

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, false)
        set(value) = prefs.edit { putBoolean(KEY_BIOMETRIC, value) }

    /** Zuletzt benutzter Anmeldename bzw. E-Mail – nur zum Vorausfüllen. */
    var lastLogin: String?
        get() = prefs.getString(KEY_LAST_LOGIN, null)
        set(value) = prefs.edit { putString(KEY_LAST_LOGIN, value) }

    val hasInstance: Boolean get() = !instanceUrl.isNullOrBlank()
    val isLoggedIn: Boolean get() = !instanceUrl.isNullOrBlank() && !accessToken.isNullOrBlank()

    // --- Anmeldung per E-Mail-Link ------------------------------------------

    var pendingMagicLink: PendingMagicLink?
        get() {
            val requestId = prefs.getString(KEY_ML_REQUEST, null) ?: return null
            return PendingMagicLink(
                instanceUrl = prefs.getString(KEY_ML_URL, null) ?: return null,
                requestId = requestId,
                codeVerifier = prefs.getString(KEY_ML_VERIFIER, null) ?: return null,
                matchCode = prefs.getString(KEY_ML_CODE, null).orEmpty(),
                email = prefs.getString(KEY_ML_EMAIL, null).orEmpty(),
                expiresAtMillis = prefs.getLong(KEY_ML_EXPIRES, 0L),
            )
        }
        set(value) = prefs.edit {
            if (value == null) {
                listOf(KEY_ML_URL, KEY_ML_REQUEST, KEY_ML_VERIFIER, KEY_ML_CODE, KEY_ML_EMAIL, KEY_ML_EXPIRES)
                    .forEach { remove(it) }
            } else {
                putString(KEY_ML_URL, value.instanceUrl)
                putString(KEY_ML_REQUEST, value.requestId)
                putString(KEY_ML_VERIFIER, value.codeVerifier)
                putString(KEY_ML_CODE, value.matchCode)
                putString(KEY_ML_EMAIL, value.email)
                putLong(KEY_ML_EXPIRES, value.expiresAtMillis)
            }
        }

    // --- Meldungen ---------------------------------------------------------

    /** Benachrichtigungen auf diesem Gerät gewünscht? */
    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY, false)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFY, value) }

    /** Ab welchem Schweregrad das Handy Bescheid gibt (Server-Begriffe). */
    var notifyMinSeverity: String
        get() = prefs.getString(KEY_NOTIFY_SEVERITY, null) ?: "error"
        set(value) = prefs.edit { putString(KEY_NOTIFY_SEVERITY, value) }

    /** Höchste Meldungs-ID, zu der schon eine Benachrichtigung kam – gegen Doppelte. */
    var lastNotifiedId: Long
        get() = prefs.getLong(KEY_LAST_NOTIFIED, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_NOTIFIED, value) }

    /** Beim Server angemeldeter Push-Endpunkt (zum Abmelden beim Logout). */
    var pushEndpoint: String?
        get() = prefs.getString(KEY_PUSH_ENDPOINT, null)
        set(value) = prefs.edit { putString(KEY_PUSH_ENDPOINT, value) }

    /** Speichert Instanz-URL und Token in einem Schritt. */
    fun saveSession(url: String, token: String) {
        prefs.edit { putString(KEY_URL, url).putString(KEY_TOKEN, token) }
    }

    /**
     * Abmelden: Token und alles, was am Konto hängt, verwerfen. Instanz-URL,
     * App-Sperre und der letzte Anmeldename bleiben – wer sich neu anmeldet,
     * muss die Adresse nicht wieder eintippen.
     */
    fun clearToken() = prefs.edit {
        remove(KEY_TOKEN)
        remove(KEY_PUSH_ENDPOINT)
        remove(KEY_LAST_NOTIFIED)
    }

    /** Alles löschen (Server wechseln). */
    fun clearAll() = prefs.edit { clear() }

    companion object {
        private const val TAG = "CredentialStore"
        private const val PREFS_FILE = "logbot_secure_prefs"
        private const val KEY_URL = "instance_url"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_BIOMETRIC = "biometric_enabled"
        private const val KEY_LAST_LOGIN = "last_login"
        private const val KEY_ML_URL = "ml_url"
        private const val KEY_ML_REQUEST = "ml_request"
        private const val KEY_ML_VERIFIER = "ml_verifier"
        private const val KEY_ML_CODE = "ml_code"
        private const val KEY_ML_EMAIL = "ml_email"
        private const val KEY_ML_EXPIRES = "ml_expires"
        private const val KEY_NOTIFY = "notify_enabled"
        private const val KEY_NOTIFY_SEVERITY = "notify_min_severity"
        private const val KEY_LAST_NOTIFIED = "last_notified_id"
        private const val KEY_PUSH_ENDPOINT = "push_endpoint"
    }
}
