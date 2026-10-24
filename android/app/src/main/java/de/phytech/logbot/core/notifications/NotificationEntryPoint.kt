/**
 * Zugriff auf die Hilt-Singletons aus Klassen, die Android selbst anlegt
 * (Push-Service, WorkManager-Worker) – ohne zusätzliche hilt-work-Abhängigkeit.
 */
package de.phytech.logbot.core.notifications

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import de.phytech.logbot.core.auth.CredentialStore
import de.phytech.logbot.data.repository.NotificationsRepository

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationEntryPoint {
    fun credentialStore(): CredentialStore
    fun notificationsRepository(): NotificationsRepository

    companion object {
        fun get(context: Context): NotificationEntryPoint =
            EntryPointAccessors.fromApplication(context.applicationContext, NotificationEntryPoint::class.java)
    }
}
