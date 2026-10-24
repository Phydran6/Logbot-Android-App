/**
 * Empfängt Push über UnifiedPush. Der Verteiler (z.B. die ntfy-App) hält die
 * Verbindung zum Push-Server; Logbot selbst braucht dafür weder Dauerverbindung
 * noch Google-Dienste.
 *
 * Der Server schickt nur das Nötigste (ID, Schweregrad, Titel) und verschlüsselt
 * nach RFC 8291 – der Push-Server sieht keinen Inhalt.
 */
package de.phytech.logbot.core.notifications

import android.util.Log
import de.phytech.logbot.data.api.PushPayloadDto
import kotlinx.serialization.json.Json
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage

class LogbotPushService : PushService() {

    override fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        NotificationScheduler.registerEndpoint(
            applicationContext, endpoint.url, endpoint.pubKeySet?.pubKey, endpoint.pubKeySet?.auth,
        )
    }

    override fun onMessage(message: PushMessage, instance: String) {
        val store = NotificationEntryPoint.get(applicationContext).credentialStore()
        if (!store.isLoggedIn || !store.notificationsEnabled) return
        val payload = if (message.decrypted) {
            runCatching { json.decodeFromString<PushPayloadDto>(message.content.decodeToString()) }.getOrNull()
        } else {
            null
        }
        if (payload == null) {
            // Unverschlüsselt oder unlesbar: nicht anzeigen, was nicht vom
            // eigenen Server stammen muss – stattdessen dort nachsehen.
            NotificationScheduler.checkNow(applicationContext)
            return
        }
        if (payload.type == "test") {
            Notifier.show(applicationContext, -1, payload.severity, payload.title.ifBlank { "Testmeldung" },
                "Push kommt an – Logbot meldet sich, wenn etwas los ist.")
            return
        }
        if (payload.id <= store.lastNotifiedId) return
        if (Notifier.atLeast(payload.severity, store.notifyMinSeverity)) {
            Notifier.show(applicationContext, payload.id, payload.severity, payload.title, null)
        }
        store.lastNotifiedId = payload.id
    }

    override fun onRegistrationFailed(reason: FailedReason, instance: String) {
        Log.w(TAG, "Push-Anmeldung beim Verteiler fehlgeschlagen: $reason")
    }

    override fun onUnregistered(instance: String) {
        NotificationEntryPoint.get(applicationContext).credentialStore().pushEndpoint = null
    }

    private companion object {
        const val TAG = "LogbotPush"
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    }
}
