/**
 * Holt neue Meldungen vom Server und zeigt die, zu denen es noch keine
 * Benachrichtigung gab. Gemeinsamer Weg für Push (der nur "es gibt etwas
 * Neues" sagt) und die Abfrage im Hintergrund.
 */
package de.phytech.logbot.core.notifications

import android.content.Context
import de.phytech.logbot.core.ui.UiState

object NotificationSync {

    /** @return false, wenn der Server nicht erreichbar war (dann später erneut). */
    suspend fun run(context: Context): Boolean {
        val entry = NotificationEntryPoint.get(context)
        val store = entry.credentialStore()
        if (!store.isLoggedIn || !store.notificationsEnabled) return true

        val since = store.lastNotifiedId
        return when (val result = entry.notificationsRepository().since(since)) {
            is UiState.Success -> {
                val items = result.data.items.filter { it.id > since }.sortedBy { it.id }
                // Erster Lauf nach dem Einschalten: nicht die ganze Vorgeschichte
                // auf einmal aufs Handy schütten, nur ab jetzt Bescheid geben.
                if (since > 0) {
                    items.filter { !it.read && Notifier.atLeast(it.severity, store.notifyMinSeverity) }
                        .takeLast(MAX_PER_RUN)
                        .forEach { Notifier.show(context, it.id, it.severity, it.title, listOfNotNull(it.hostname, it.body).joinToString(" · ")) }
                }
                val newest = maxOf(items.maxOfOrNull { it.id } ?: 0L, result.data.newestId ?: 0L)
                if (newest > since) store.lastNotifiedId = newest
                true
            }
            // 4xx (z.B. Server kennt Meldungen noch nicht): Wiederholen hilft nicht.
            // Netzfehler und 5xx: später erneut.
            is UiState.Error -> result.code != null && result.code < 500
            UiState.Loading -> true
        }
    }

    private const val MAX_PER_RUN = 5
}
