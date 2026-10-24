package de.phytech.logbot.feature.shell

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.phytech.logbot.core.notifications.PushManager
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.repository.AuthRepository
import de.phytech.logbot.data.repository.MobileRepository
import de.phytech.logbot.data.repository.NotificationsRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val push: PushManager,
    private val auth: AuthRepository,
    private val mobile: MobileRepository,
    private val notifications: NotificationsRepository,
) : ViewModel() {

    var username by mutableStateOf("")
        private set
    var isAdmin by mutableStateOf(false)
        private set
    var serverName by mutableStateOf("Logbot")
        private set

    val unread: StateFlow<Int> = notifications.unread

    init {
        refresh()
        viewModelScope.launch { push.resyncAfterLogin(context) }
    }

    fun refresh() {
        viewModelScope.launch {
            when (val r = mobile.refreshBootstrap()) {
                is UiState.Success -> {
                    username = r.data.user.username
                    isAdmin = r.data.user.isAdmin || r.data.user.role == "admin"
                    serverName = r.data.server.name.ifBlank { "Logbot" }
                }
                is UiState.Error -> {
                    // Ältere Server ohne /api/app: die Rolle kommt dann aus /api/auth/me.
                    runCatching { auth.fetchUser() }.onSuccess {
                        username = it.username
                        isAdmin = it.role == "admin"
                    }
                }
                UiState.Loading -> Unit
            }
            // Zahl der ungelesenen Meldungen fürs Menü; Fehler hier sind egal.
            notifications.page()
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            auth.logout()
            mobile.clear()
            onDone()
        }
    }
}
