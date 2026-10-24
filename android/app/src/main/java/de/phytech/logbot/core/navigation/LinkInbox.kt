/**
 * Briefkasten für das, was von außen in die App kommt: ein Anmelde-Link aus
 * der Mail oder das Antippen einer Benachrichtigung. Die Activity legt ab, die
 * Oberfläche holt ab, sobald sie so weit ist (z.B. erst nach dem Entsperren).
 */
package de.phytech.logbot.core.navigation

import android.content.Intent
import de.phytech.logbot.core.notifications.Notifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Anmelde-Link: logbot://magic-login?rid=…&t=… */
data class MagicLinkIntent(val requestId: String, val token: String)

@Singleton
class LinkInbox @Inject constructor() {
    private val _magicLink = MutableStateFlow<MagicLinkIntent?>(null)
    val magicLink: StateFlow<MagicLinkIntent?> = _magicLink.asStateFlow()

    private val _openNotification = MutableStateFlow<Long?>(null)
    val openNotification: StateFlow<Long?> = _openNotification.asStateFlow()

    fun handle(intent: Intent?) {
        intent ?: return
        val data = intent.data
        if (intent.action == Intent.ACTION_VIEW && data?.scheme == "logbot" && data.host == "magic-login") {
            val rid = data.getQueryParameter("rid")
            val token = data.getQueryParameter("t")
            if (!rid.isNullOrBlank() && !token.isNullOrBlank()) {
                _magicLink.value = MagicLinkIntent(rid, token)
            }
        }
        if (intent.hasExtra(Notifier.EXTRA_NOTIFICATION_ID)) {
            _openNotification.value = intent.getLongExtra(Notifier.EXTRA_NOTIFICATION_ID, 0L)
        }
    }

    fun consumeMagicLink(): MagicLinkIntent? = _magicLink.value.also { _magicLink.value = null }

    fun consumeNotification() {
        _openNotification.value = null
    }
}
