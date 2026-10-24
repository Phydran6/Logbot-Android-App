/**
 * Application-Klasse mit Hilt-Setup. Einstiegspunkt für Dependency Injection.
 * Legt beim Start die Benachrichtigungskanäle an und plant – falls gewünscht –
 * die Abfrage neuer Meldungen im Hintergrund.
 */
package de.phytech.logbot

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import de.phytech.logbot.core.notifications.NotificationScheduler
import de.phytech.logbot.core.notifications.Notifier

@HiltAndroidApp
class LogbotApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
        NotificationScheduler.update(this)
    }
}
