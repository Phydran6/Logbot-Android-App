package de.phytech.logbot.feature.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.phytech.logbot.core.auth.PendingMagicLink
import de.phytech.logbot.core.navigation.MagicLinkIntent
import de.phytech.logbot.data.repository.AuthRepository
import de.phytech.logbot.data.repository.MagicLinkOutcome
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Was der Screen gerade zeigt. */
sealed interface MagicLinkStep {
    data object Enter : MagicLinkStep
    data class Waiting(val pending: PendingMagicLink) : MagicLinkStep
    data object Redeeming : MagicLinkStep
    data object Done : MagicLinkStep
    data class Mfa(val token: String) : MagicLinkStep
}

@HiltViewModel
class MagicLinkViewModel @Inject constructor(
    private val repo: AuthRepository,
) : ViewModel() {

    var step by mutableStateOf<MagicLinkStep>(
        repo.pendingMagicLink?.let { MagicLinkStep.Waiting(it) } ?: MagicLinkStep.Enter,
    )
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    /** Wann frühestens ein neuer Link angefordert werden darf (gegen Mail-Fluten). */
    var resendAtMillis by mutableStateOf(0L)
        private set

    val suggestedEmail: String = repo.pendingMagicLink?.email
        ?: repo.lastLogin?.takeIf { '@' in it }.orEmpty()

    fun request(email: String) {
        val address = email.trim()
        if (!EMAIL.matches(address)) {
            error = "Bitte eine gültige E-Mail-Adresse eingeben"
            return
        }
        loading = true
        error = null
        viewModelScope.launch {
            repo.requestMagicLink(address).fold(
                onSuccess = {
                    step = MagicLinkStep.Waiting(it)
                    resendAtMillis = System.currentTimeMillis() + RESEND_AFTER_MS
                },
                onFailure = { error = it.message },
            )
            loading = false
        }
    }

    /** Link aus der Mail angetippt – die App ist darüber geöffnet worden. */
    fun redeem(link: MagicLinkIntent) {
        step = MagicLinkStep.Redeeming
        error = null
        viewModelScope.launch {
            when (val outcome = repo.redeemMagicLink(link.requestId, link.token)) {
                MagicLinkOutcome.Success -> step = MagicLinkStep.Done
                is MagicLinkOutcome.MfaRequired -> step = MagicLinkStep.Mfa(outcome.mfaToken)
                is MagicLinkOutcome.Failed -> {
                    error = outcome.message
                    // Ist die Anfrage noch offen (z.B. nur kurz kein Netz), einfach den
                    // Link noch einmal antippen; sonst einen neuen anfordern.
                    step = repo.pendingMagicLink?.let { MagicLinkStep.Waiting(it) } ?: MagicLinkStep.Enter
                }
            }
        }
    }

    fun cancel() {
        repo.cancelMagicLink()
        step = MagicLinkStep.Enter
        error = null
    }

    /** Die Frist ist um: neu anfordern lassen. */
    fun expired() {
        repo.cancelMagicLink()
        error = "Der Link ist abgelaufen. Bitte einen neuen anfordern."
        step = MagicLinkStep.Enter
    }

    private companion object {
        val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        const val RESEND_AFTER_MS = 60_000L
    }
}
