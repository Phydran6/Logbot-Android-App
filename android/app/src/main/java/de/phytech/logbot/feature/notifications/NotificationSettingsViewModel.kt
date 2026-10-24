package de.phytech.logbot.feature.notifications

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.phytech.logbot.core.auth.CredentialStore
import de.phytech.logbot.core.notifications.NotificationScheduler
import de.phytech.logbot.core.notifications.Notifier
import de.phytech.logbot.core.notifications.PushManager
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.repository.NotificationsRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Wie Meldungen das Handy erreichen. */
enum class Delivery { OFF, POLLING, PUSH }

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: CredentialStore,
    private val push: PushManager,
    private val repository: NotificationsRepository,
) : ViewModel() {

    var enabled by mutableStateOf(store.notificationsEnabled)
        private set
    var minSeverity by mutableStateOf(store.notifyMinSeverity)
        private set
    var delivery by mutableStateOf(currentDelivery())
        private set
    var hasDistributor by mutableStateOf(push.distributors(context).isNotEmpty())
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set

    val canPost: Boolean get() = Notifier.canPost(context)

    /** Nach Rückkehr in die App (z.B. ntfy installiert, Berechtigung erteilt) neu einlesen. */
    fun reload() {
        enabled = store.notificationsEnabled
        hasDistributor = push.distributors(context).isNotEmpty()
        delivery = currentDelivery()
    }

    private fun currentDelivery(): Delivery = when {
        !store.notificationsEnabled -> Delivery.OFF
        push.activeDistributor(context) != null && push.isRegisteredWithServer -> Delivery.PUSH
        else -> Delivery.POLLING
    }

    /** Einschalten – die Berechtigung hat der Screen vorher eingeholt. */
    fun enable(activity: Activity) {
        store.notificationsEnabled = true
        enabled = true
        NotificationScheduler.update(context)
        NotificationScheduler.checkNow(context)
        if (hasDistributor) enablePush(activity) else delivery = currentDelivery()
    }

    fun disable() {
        store.notificationsEnabled = false
        enabled = false
        NotificationScheduler.update(context)
        viewModelScope.launch {
            push.disable(context)
            delivery = currentDelivery()
        }
    }

    fun enablePush(activity: Activity) {
        busy = true
        message = null
        viewModelScope.launch {
            val ok = push.enable(activity)
            busy = false
            message = if (ok) {
                "Push wird eingerichtet – das dauert einen Moment."
            } else {
                "Keine Push-App ausgewählt. Die App sieht weiter alle 15 Minuten nach."
            }
            delivery = currentDelivery()
        }
    }

    /** Gefiltert wird auf dem Gerät – Push und Abfrage zeigen nur, was diese Schwelle erreicht. */
    fun chooseMinSeverity(value: String) {
        store.notifyMinSeverity = value
        minSeverity = value
    }

    fun test() {
        busy = true
        message = null
        viewModelScope.launch {
            message = when (val r = repository.testPush()) {
                is UiState.Success -> {
                    val failed = r.data.results.filter { !it.ok }
                    if (failed.isEmpty()) {
                        "Testmeldung verschickt – sie sollte gleich erscheinen."
                    } else {
                        "Nicht zugestellt: " + failed.joinToString("; ") {
                            "${it.deviceName ?: "Gerät"}: ${it.message}"
                        }
                    }
                }
                is UiState.Error -> r.message
                UiState.Loading -> null
            }
            busy = false
        }
    }
}
