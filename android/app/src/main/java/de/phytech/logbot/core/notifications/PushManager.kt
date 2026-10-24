/**
 * Ein- und Ausschalten von Push über UnifiedPush.
 *
 * Push braucht eine Verteiler-App auf dem Gerät (empfohlen: ntfy). Ist keine
 * installiert, bleibt die Abfrage alle 15 Minuten – das sagt die Oberfläche
 * auch so, statt still nichts zu tun.
 */
package de.phytech.logbot.core.notifications

import android.app.Activity
import android.content.Context
import de.phytech.logbot.core.auth.CredentialStore
import de.phytech.logbot.data.repository.NotificationsRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import org.unifiedpush.android.connector.UnifiedPush
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class PushManager @Inject constructor(
    private val credentialStore: CredentialStore,
    private val repository: NotificationsRepository,
) {
    /** Installierte Verteiler-Apps (ohne Logbot selbst). */
    fun distributors(context: Context): List<String> =
        runCatching { UnifiedPush.getDistributors(context).filter { it != context.packageName } }
            .getOrDefault(emptyList())

    /** Verteiler, der die Anmeldung schon bestätigt hat – null, wenn Push nicht läuft. */
    fun activeDistributor(context: Context): String? =
        runCatching { UnifiedPush.getAckDistributor(context) }.getOrNull()

    val isRegisteredWithServer: Boolean get() = credentialStore.pushEndpoint != null

    /**
     * Wählt den Verteiler (bei mehreren fragt Android) und meldet sich an.
     * Der Endpunkt kommt danach asynchron im [LogbotPushService] an.
     * @return false, wenn kein Verteiler gewählt wurde.
     */
    suspend fun enable(activity: Activity): Boolean {
        val vapid = repository.vapidKey()
        val picked = suspendCancellableCoroutine { cont ->
            runCatching {
                UnifiedPush.tryUseCurrentOrDefaultDistributor(activity) { ok -> if (cont.isActive) cont.resume(ok) }
            }.onFailure { if (cont.isActive) cont.resume(false) }
        }
        if (!picked) return false
        return runCatching {
            UnifiedPush.register(activity, INSTANCE, "Logbot – Meldungen vom Server", vapid)
        }.isSuccess
    }

    /**
     * Nach erneuter Anmeldung: Beim Abmelden wurde der Endpunkt beim Server
     * abgemeldet, beim Verteiler aber nicht. Erneut registrieren – der
     * Verteiler meldet den Endpunkt dann wieder, und der Server erfährt ihn.
     */
    suspend fun resyncAfterLogin(context: Context) {
        if (!credentialStore.notificationsEnabled || credentialStore.pushEndpoint != null) return
        if (activeDistributor(context) == null) return
        val vapid = repository.vapidKey()
        runCatching { UnifiedPush.register(context, INSTANCE, "Logbot – Meldungen vom Server", vapid) }
    }

    suspend fun disable(context: Context) {
        runCatching { UnifiedPush.unregister(context, INSTANCE) }
        repository.disablePushOnServer()
    }

    private companion object {
        const val INSTANCE = "default"
    }
}
