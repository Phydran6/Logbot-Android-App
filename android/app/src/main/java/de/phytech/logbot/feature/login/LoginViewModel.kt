package de.phytech.logbot.feature.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.phytech.logbot.data.repository.AuthRepository
import de.phytech.logbot.data.repository.LoginOutcome
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repo: AuthRepository,
) : ViewModel() {

    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    /** Hinweis nach "Passwort vergessen". */
    var info by mutableStateOf<String?>(null)
        private set

    val serverHost: String = repo.instanceUrl?.toHttpUrlOrNull()?.host ?: repo.instanceUrl.orEmpty()
    val lastLogin: String = repo.lastLogin.orEmpty()

    /** Läuft noch eine Anmeldung per Mail, die man fortsetzen kann? */
    val hasPendingMagicLink: Boolean get() = repo.pendingMagicLink != null

    fun login(
        username: String,
        password: String,
        onAuthenticated: () -> Unit,
        onMfaRequired: (String) -> Unit,
    ) {
        if (username.isBlank() || password.isBlank()) {
            error = "Bitte Benutzername oder E-Mail und Passwort eingeben"
            return
        }
        loading = true
        error = null
        info = null
        viewModelScope.launch {
            when (val r = repo.login(username.trim(), password)) {
                is LoginOutcome.Success -> {
                    loading = false
                    onAuthenticated()
                }
                is LoginOutcome.MfaRequired -> {
                    loading = false
                    onMfaRequired(r.mfaToken)
                }
                is LoginOutcome.Error -> {
                    loading = false
                    error = r.message
                }
            }
        }
    }

    fun forgotPassword(login: String) {
        if (login.isBlank()) {
            error = "Bitte Benutzername oder E-Mail eingeben"
            return
        }
        loading = true
        error = null
        viewModelScope.launch {
            repo.forgotPassword(login).fold(
                onSuccess = {
                    info = "Falls es zu „${login.trim()}“ ein Konto mit E-Mail-Adresse gibt, ist eine Mail " +
                        "zum Zurücksetzen unterwegs."
                },
                onFailure = { error = it.message },
            )
            loading = false
        }
    }
}
