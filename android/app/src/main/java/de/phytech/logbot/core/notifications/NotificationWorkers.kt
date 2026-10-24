/**
 * Hintergrundarbeit für Meldungen:
 *
 * - [NotificationCheckWorker] fragt alle 15 Minuten nach neuen Meldungen. Das
 *   ist der Weg ohne Push-Verteiler – nicht sofort, aber ohne jede Einrichtung.
 *   Mit Push läuft er trotzdem mit: Fällt der Verteiler aus, kommt die
 *   Meldung so wenigstens mit Verzug an.
 * - [PushRegisterWorker] meldet einen neuen Push-Endpunkt beim Server an und
 *   wiederholt das, bis es klappt (Netz weg, Server kurz nicht erreichbar).
 */
package de.phytech.logbot.core.notifications

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import de.phytech.logbot.core.ui.UiState
import java.util.concurrent.TimeUnit

class NotificationCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        if (NotificationSync.run(applicationContext)) Result.success() else Result.retry()
}

class PushRegisterWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val endpoint = inputData.getString(KEY_ENDPOINT) ?: return Result.failure()
        val entry = NotificationEntryPoint.get(applicationContext)
        if (!entry.credentialStore().isLoggedIn) return Result.success()
        val result = entry.notificationsRepository().registerPush(
            endpoint, inputData.getString(KEY_P256DH), inputData.getString(KEY_AUTH),
        )
        return when {
            result is UiState.Success -> Result.success()
            result is UiState.Error && result.code != null && result.code in 400..499 -> Result.failure()
            runAttemptCount >= 8 -> Result.failure()
            else -> Result.retry()
        }
    }

    companion object {
        const val KEY_ENDPOINT = "endpoint"
        const val KEY_P256DH = "p256dh"
        const val KEY_AUTH = "auth"
    }
}

object NotificationScheduler {
    private const val PERIODIC = "logbot-notification-check"
    private const val REGISTER = "logbot-push-register"

    private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Plant die Abfrage ein oder aus – je nach Anmeldung und Einstellung. */
    fun update(context: Context) {
        val store = NotificationEntryPoint.get(context).credentialStore()
        val work = WorkManager.getInstance(context)
        if (store.isLoggedIn && store.notificationsEnabled) {
            work.enqueueUniquePeriodicWork(
                PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<NotificationCheckWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(network)
                    .build(),
            )
        } else {
            work.cancelUniqueWork(PERIODIC)
        }
    }

    /** Einmal sofort nachsehen (z.B. nach dem Einschalten oder einem Push ohne Inhalt). */
    fun checkNow(context: Context) {
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<NotificationCheckWorker>().setConstraints(network).build(),
        )
    }

    fun registerEndpoint(context: Context, endpoint: String, p256dh: String?, auth: String?) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            REGISTER,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<PushRegisterWorker>()
                .setConstraints(network)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .setInputData(
                    workDataOf(
                        PushRegisterWorker.KEY_ENDPOINT to endpoint,
                        PushRegisterWorker.KEY_P256DH to p256dh,
                        PushRegisterWorker.KEY_AUTH to auth,
                    ),
                )
                .build(),
        )
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(PERIODIC)
            cancelUniqueWork(REGISTER)
        }
    }
}
