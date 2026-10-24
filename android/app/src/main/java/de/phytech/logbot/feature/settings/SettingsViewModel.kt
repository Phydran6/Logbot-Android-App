package de.phytech.logbot.feature.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.phytech.logbot.core.auth.CredentialStore
import de.phytech.logbot.core.security.Biometrics
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.repository.AuthRepository
import de.phytech.logbot.data.repository.MobileRepository
import de.phytech.logbot.data.repository.SettingSpec
import de.phytech.logbot.data.repository.SettingsBundle
import de.phytech.logbot.data.repository.SettingsRepository
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: SettingsRepository,
    private val auth: AuthRepository,
    private val mobile: MobileRepository,
    private val credentialStore: CredentialStore,
) : ViewModel() {

    var state by mutableStateOf<UiState<SettingsBundle>>(UiState.Loading)
        private set
    var actionError by mutableStateOf<String?>(null)
        private set
    var saving by mutableStateOf(false)
        private set

    val biometricAvailable: Boolean = Biometrics.isAvailable(context)
    var biometricEnabled by mutableStateOf(credentialStore.biometricEnabled)
        private set

    val serverUrl: String = credentialStore.instanceUrl.orEmpty()
    val serverHost: String = serverUrl.toHttpUrlOrNull()?.host ?: serverUrl
    val serverVersion: String get() = mobile.bootstrap.value?.server?.version.orEmpty()

    private var isAdmin = false

    fun load(admin: Boolean) {
        isAdmin = admin
        state = UiState.Loading
        viewModelScope.launch { state = repo.load(includeDatabase = admin) }
    }

    fun toggleBiometric(enabled: Boolean) {
        if (enabled && !biometricAvailable) return
        credentialStore.biometricEnabled = enabled
        biometricEnabled = enabled
    }

    fun save(spec: SettingSpec, raw: String, onDone: () -> Unit) {
        val value = SettingsRepository.parse(spec, raw)
        if (value == null) {
            actionError = buildString {
                append("Ungültiger Wert für „${spec.label}“.")
                if (spec.min != null && spec.max != null) append(" Erlaubt: ${spec.min} bis ${spec.max}.")
            }
            return
        }
        saving = true
        viewModelScope.launch {
            when (val r = repo.update(spec.key, value)) {
                is UiState.Success -> {
                    onDone()
                    state = repo.load(includeDatabase = isAdmin)
                }
                is UiState.Error -> actionError = r.message
                UiState.Loading -> Unit
            }
            saving = false
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            auth.logout()
            mobile.clear()
            onDone()
        }
    }

    fun forgetServer(onDone: () -> Unit) {
        viewModelScope.launch {
            auth.forgetServer()
            mobile.clear()
            onDone()
        }
    }

    fun dismissActionError() {
        actionError = null
    }
}
