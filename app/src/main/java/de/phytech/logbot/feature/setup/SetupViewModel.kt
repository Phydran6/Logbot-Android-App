package de.phytech.logbot.feature.setup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.phytech.logbot.data.repository.AuthRepository
import de.phytech.logbot.data.repository.LoginOutcome
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val repo: AuthRepository,
) : ViewModel() {

    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun showError(message: String?) {
        error = message
    }

    /** URL validieren, speichern und zum Login weitergehen. */
    fun connect(url: String, onNeedsLogin: () -> Unit) {
        val u = url.trim()
        if (!u.startsWith("https://")) {
            error = "URL muss mit https:// beginnen"
            return
        }
        error = null
        repo.setInstanceUrl(u)
        onNeedsLogin()
    }

    /** QR-Inhalt {url, token, type} parsen und Token gegen Access-Token tauschen. */
    fun handleQr(content: String, onAuthenticated: () -> Unit) {
        loading = true
        error = null
        viewModelScope.launch {
            val parsed = runCatching {
                val json = JSONObject(content)
                json.getString("url") to json.getString("token")
            }.getOrNull()

            if (parsed == null || !parsed.first.startsWith("https://") || parsed.second.isBlank()) {
                loading = false
                error = "Ungültiger QR-Code"
                return@launch
            }

            when (val r = repo.exchangeAppToken(parsed.first, parsed.second)) {
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
