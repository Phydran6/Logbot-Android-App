package de.phytech.logbot.feature.agents

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.DeviceListDto
import de.phytech.logbot.data.repository.AgentsRepository
import de.phytech.logbot.data.repository.MobileRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val mobile: MobileRepository,
    private val agents: AgentsRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<DeviceListDto>>(UiState.Loading)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var onlyOffline by mutableStateOf(false)
    var query by mutableStateOf("")
    var actionError by mutableStateOf<String?>(null)
        private set

    init {
        load()
    }

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
            when (val r = mobile.devices()) {
                is UiState.Success -> state = r
                // Beim Aktualisieren die alte Liste stehen lassen und nur den Fehler zeigen.
                is UiState.Error -> if (state is UiState.Success) actionError = r.message else state = r
                UiState.Loading -> Unit
            }
            refreshing = false
        }
    }

    fun delete(id: Int, purgeLogs: Boolean) {
        viewModelScope.launch {
            when (val r = agents.delete(id, purgeLogs)) {
                is UiState.Success -> load()
                is UiState.Error -> actionError = r.message
                UiState.Loading -> Unit
            }
        }
    }

    fun dismissActionError() {
        actionError = null
    }
}
