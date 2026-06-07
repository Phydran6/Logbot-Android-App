/**
 * Hilfen rund um die Geräte-Authentifizierung (Biometrie oder Geräte-PIN/Muster).
 */
package de.phytech.logbot.core.security

import android.content.Context
import androidx.biometric.BiometricManager

object Biometrics {
    /** Erlaubt starke Biometrie ODER die Geräte-Anmeldung (PIN/Muster/Passwort). */
    val ALLOWED =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    /** True, wenn das Gerät entsperrbar ist (Biometrie oder PIN/Muster eingerichtet). */
    fun isAvailable(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(ALLOWED) ==
            BiometricManager.BIOMETRIC_SUCCESS
}
