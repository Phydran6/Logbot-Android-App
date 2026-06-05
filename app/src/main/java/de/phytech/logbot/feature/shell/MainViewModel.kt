package de.phytech.logbot.feature.shell

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.phytech.logbot.data.api.UserResponse
import de.phytech.logbot.data.repository.AuthRepository
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repo: AuthRepository,
) : ViewModel() {

    var user by mutableStateOf<UserResponse?>(null)
        private set
    var sessionExpired by mutableStateOf(false)
        private set

    val isAdmin: Boolean get() = user?.role == "admin"

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
