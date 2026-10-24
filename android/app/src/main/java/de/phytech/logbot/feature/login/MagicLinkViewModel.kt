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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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

    private var pollJob: Job? = null

    init {
        (step as? MagicLinkStep.Waiting)?.let { startPolling() }
    }

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
                    startPolling()
                },
                onFailure = { error = it.message },
            )
            loading = false
        }
    }

    /** Link aus der Mail auf diesem Gerät geöffnet. */
    fun redeem(link: MagicLinkIntent) {
        pollJob?.cancel()
        step = MagicLinkStep.Redeeming
        error = null
        viewModelScope.launch { apply(repo.redeemMagicLink(link.requestId, link.token)) }
    }

    fun cancel() {
        pollJob?.cancel()
        repo.cancelMagicLink()
        step = MagicLinkStep.Enter
        error = null
    }

    /** Nachfragen, ob die Anmeldung auf einem anderen Gerät bestätigt wurde. */
    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                val pending = (step as? MagicLinkStep.Waiting)?.pending ?: break
                if (pending.isExpired) {
                    repo.cancelMagicLink()
                    error = "Der Link ist abgelaufen. Bitte einen neuen anfordern."
                    step = MagicLinkStep.Enter
                    break
                }
                when (val outcome = repo.redeemMagicLink()) {
                    is MagicLinkOutcome.Pending -> delay(outcome.pollSeconds.coerceIn(2, 30) * 1000L)
                    else -> {
                        apply(outcome)
                        break
                    }
                }
            }
        }
    }

    private fun apply(outcome: MagicLinkOutcome) {
        when (outcome) {
            MagicLinkOutcome.Success -> step = MagicLinkStep.Done
            is MagicLinkOutcome.MfaRequired -> step = MagicLinkStep.Mfa(outcome.mfaToken)
            is MagicLinkOutcome.Failed -> {
                error = outcome.message
                step = repo.pendingMagicLink?.let { MagicLinkStep.Waiting(it) } ?: MagicLinkStep.Enter
                if (step is MagicLinkStep.Waiting) startPolling()
            }
            is MagicLinkOutcome.Pending -> {
                step = repo.pendingMagicLink?.let { MagicLinkStep.Waiting(it) } ?: MagicLinkStep.Enter
                startPolling()
            }
        }
    }

    private companion object {
        val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        const val RESEND_AFTER_MS = 60_000L
    }
}
