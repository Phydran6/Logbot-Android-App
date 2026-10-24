package de.phytech.logbot.feature.setup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.repository.AuthRepository
import de.phytech.logbot.data.repository.LoginOutcome
import de.phytech.logbot.data.repository.MonitoringRepository
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val repo: AuthRepository,
    private val monitoring: MonitoringRepository,
) : ViewModel() {

    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    /** Zuletzt benutzte Adresse zum Vorausfüllen. */
    val previousUrl: String = repo.instanceUrl.orEmpty()

    fun showError(message: String?) {
        error = message
    }

    /**
     * URL prüfen, speichern und zum Login weitergehen. Vorher wird nachgesehen,
     * ob unter der Adresse wirklich ein Logbot-Server antwortet – sonst merkt
     * man einen Tippfehler erst beim Anmelden an einer unklaren Meldung.
     */
    fun connect(input: String, onNeedsLogin: () -> Unit) {
        val url = normalize(input) ?: run {
            error = "Bitte eine gültige Adresse eingeben, z.B. https://logbot.example.de"
            return
        }
        if (!url.startsWith("https://")) {
            error = "Nur https ist erlaubt – Zugangsdaten gehen nicht unverschlüsselt über das Netz."
            return
        }
        loading = true
        error = null
        viewModelScope.launch {
            repo.setInstanceUrl(url)
            when (val r = monitoring.ping()) {
                is UiState.Success -> {
                    loading = false
                    if (r.data.status.isBlank()) {
                        error = "Unter dieser Adresse antwortet kein Logbot-Server."
                    } else {
                        onNeedsLogin()
                    }
                }
                is UiState.Error -> {
                    loading = false
                    error = if (r.code == 404) "Unter dieser Adresse antwortet kein Logbot-Server." else r.message
                }
                UiState.Loading -> Unit
            }
        }
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

            if (parsed == null || parsed.second.isBlank()) {
                loading = false
                error = "Das ist kein Logbot-Anmeldecode. Er steht in der Weboberfläche unter „App verbinden“."
                return@launch
            }
            if (!parsed.first.startsWith("https://")) {
                loading = false
                error = "Der QR-Code nennt als Adresse ${parsed.first}. Die App verbindet sich nur über " +
                    "https – auf dem Server unter Meldungen → Kanäle → E-Mail die öffentliche " +
                    "https-Adresse eintragen und den Code neu erzeugen."
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
                is LoginOutcome.MfaRequired -> {
                    loading = false
                    error = "Unerwartete Antwort"
                }
            }
        }
    }

    private fun normalize(input: String): String? {
        val trimmed = input.trim().trimEnd('/')
        if (trimmed.isBlank()) return null
        val withScheme = if ("://" in trimmed) trimmed else "https://$trimmed"
        val parsed = withScheme.toHttpUrlOrNull() ?: return null
        return parsed.toString().trimEnd('/')
    }
}
