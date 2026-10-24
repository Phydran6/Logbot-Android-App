/**
 * Zentrale Stelle für "die Sitzung ist vorbei". Jede Anfrage, die mit HTTP 401
 * zurückkommt, landet hier – egal aus welchem Screen. Der Token wird verworfen
 * und die Oberfläche kehrt zur Anmeldung zurück.
 *
 * Vorher kannte nur der Drawer-Start den Fall; jeder andere Screen zeigte
 * "Sitzung abgelaufen" mit einem Wiederholen-Knopf, der nie helfen konnte.
 */
package de.phytech.logbot.core.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val credentialStore: CredentialStore,
) {
    private val _expired = MutableStateFlow(false)

    /** true, sobald der Server den Token abgelehnt hat – bis zur nächsten Anmeldung. */
    val expired: StateFlow<Boolean> = _expired.asStateFlow()

    /** Vom Netzwerk-Interceptor bei 401 auf eine angemeldete Anfrage gerufen. */
    fun onUnauthorized() {
        if (credentialStore.accessToken != null) credentialStore.clearToken()
        _expired.value = true
    }

    /** Nach erfolgreicher Anmeldung. */
    fun onLoggedIn() {
        _expired.value = false
    }
}
