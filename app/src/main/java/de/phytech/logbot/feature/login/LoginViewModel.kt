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
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repo: AuthRepository,
) : ViewModel() {

    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun login(
        username: String,
        password: String,
        onAuthenticated: () -> Unit,
        onMfaRequired: (String) -> Unit,
    ) {
        if (username.isBlank() || password.isBlank()) {
            error = "Bitte Benutzername und Passwort eingeben"
            return
        }
        loading = true
        error = null
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
}
