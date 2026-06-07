package de.phytech.logbot.feature.shell

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
import de.phytech.logbot.data.api.UserResponse
import de.phytech.logbot.data.repository.AuthRepository
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repo: AuthRepository,
    private val credentialStore: CredentialStore,
    @ApplicationContext appContext: Context,
) : ViewModel() {

    var user by mutableStateOf<UserResponse?>(null)
        private set
    var sessionExpired by mutableStateOf(false)
        private set

    val isAdmin: Boolean get() = user?.role == "admin"

    /** Geräte-Sperre verfügbar (Biometrie oder PIN/Muster eingerichtet)? */
    val biometricAvailable: Boolean = Biometrics.isAvailable(appContext)

    var biometricEnabled by mutableStateOf(credentialStore.biometricEnabled)
        private set

    fun toggleBiometric(enabled: Boolean) {
        if (enabled && !biometricAvailable) return
        credentialStore.biometricEnabled = enabled
        biometricEnabled = enabled
    }

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                user = repo.fetchUser()
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    repo.logout()
                    sessionExpired = true
                }
            } catch (_: Exception) {
                // Netzwerkfehler hier ignorieren; UI bleibt nutzbar, Retry möglich.
            }
        }
    }

    fun logout() = repo.logout()
}
