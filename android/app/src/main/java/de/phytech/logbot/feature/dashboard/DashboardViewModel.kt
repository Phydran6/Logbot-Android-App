package de.phytech.logbot.feature.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.core.ui.dataOrNull
import de.phytech.logbot.data.api.HealthDetailedDto
import de.phytech.logbot.data.api.NotificationDto
import de.phytech.logbot.data.api.SummaryDto
import de.phytech.logbot.data.repository.AiRepository
import de.phytech.logbot.data.repository.MobileRepository
import de.phytech.logbot.data.repository.MonitoringRepository
import de.phytech.logbot.data.repository.NotificationsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardData(
    val summary: SummaryDto,
    val health: HealthDetailedDto?,
    /** Neueste ungelesene Meldungen; null = Server kennt keine Meldungen. */
    val notifications: List<NotificationDto>?,
    val aiAllowed: Boolean,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val mobile: MobileRepository,
    private val monitoring: MonitoringRepository,
    private val notifications: NotificationsRepository,
    private val ai: AiRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<DashboardData>>(UiState.Loading)
        private set
    var refreshing by mutableStateOf(false)
        private set

    init {
        load()
    }

    /** Ziehen zum Aktualisieren: Inhalt stehen lassen, nur neu holen. */
    fun refresh() {
        refreshing = true
        load()
    }

    fun retry() {
        state = UiState.Loading
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val summary = async { mobile.summary() }
            val health = async { monitoring.health() }
            val inbox = async { notifications.page() }
            val aiStatus = async { ai.status() }
            state = when (val s = summary.await()) {
                is UiState.Success -> UiState.Success(
                    DashboardData(
                        summary = s.data,
                        health = health.await().dataOrNull,
                        notifications = inbox.await().dataOrNull?.items?.filter { !it.read }?.take(3),
                        aiAllowed = aiStatus.await().dataOrNull?.allowed == true,
                    ),
                )
                is UiState.Error -> if (state is UiState.Success) state.also { refreshing = false } else s
                UiState.Loading -> state
            }
            refreshing = false
        }
    }
}
