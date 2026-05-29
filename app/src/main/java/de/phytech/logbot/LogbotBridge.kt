/**
 * JavaScript-Bruecke zwischen WebView (Logbot-Server-UI) und nativer App.
 * Wird in MainActivity per addJavascriptInterface unter dem Namen 'LogbotApp'
 * registriert und ist damit aus der Web-UI als window.LogbotApp erreichbar.
 *
 * Nur Methoden mit @JavascriptInterface sind aus JS aufrufbar (API 17+).
 *
 * Geplanter Web-UI-Code (auf der Settings-Seite):
 *   if (window.LogbotApp && LogbotApp.isBiometricAvailable()) {
 *       toggle.checked = LogbotApp.isBiometricEnabled();
 *       toggle.onchange = () => LogbotApp.setBiometricEnabled(toggle.checked);
 *   }
 */
package de.phytech.logbot

import android.content.Context
import android.webkit.JavascriptInterface
import androidx.biometric.BiometricManager

class LogbotBridge(private val appContext: Context) {

    private val prefs get() = MainActivity.getEncryptedPrefs(appContext)

    /** True, wenn das Geraet biometrische Sensoren oder PIN/Pattern enrolled hat. */
    @JavascriptInterface
    fun isBiometricAvailable(): Boolean {
        val bm = BiometricManager.from(appContext)
        val allowed = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return bm.canAuthenticate(allowed) == BiometricManager.BIOMETRIC_SUCCESS
    }

    @JavascriptInterface
    fun isBiometricEnabled(): Boolean =
        prefs.getBoolean(MainActivity.PREF_BIOMETRIC_ENABLED, false)

    /**
     * Aktiviert/deaktiviert den App-Lock per Biometrie/PIN.
     * Aktivieren scheitert, wenn das Geraet keine Biometrie/PIN eingerichtet hat.
     * @return true wenn der Wert geaendert/bestaetigt wurde.
     */
    @JavascriptInterface
    fun setBiometricEnabled(enabled: Boolean): Boolean {
        if (enabled && !isBiometricAvailable()) return false
        prefs.edit().putBoolean(MainActivity.PREF_BIOMETRIC_ENABLED, enabled).apply()
        return true
    }
}
