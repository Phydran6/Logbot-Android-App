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
class MfaViewModel @Inject constructor(
    private val repo: AuthRepository,
) : ViewModel() {

    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun verify(mfaToken: String, code: String, onAuthenticated: () -> Unit) {
        if (code.isBlank()) {
            error = "Bitte Code eingeben"
            return
        }
        loading = true
        error = null
        viewModelScope.launch {
            when (val r = repo.loginMfa(mfaToken, code.trim())) {
                is LoginOutcome.Success -> {
                    loading = false
                    onAuthenticated()
                }
                is LoginOutcome.Error -> {
                    loading = false
                    error = r.message
                }
                else -> {
                    loading = false
                    error = "Unerwartete Antwort"
                }
            }
        }
    }
}
