package de.phytech.logbot.feature.health

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.HealthDetailedDto
import de.phytech.logbot.data.repository.MonitoringRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val repo: MonitoringRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<HealthDetailedDto>>(UiState.Loading)
        private set
    var refreshing by mutableStateOf(false)
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

    /** Leise nachladen (Takt im Screen) – ohne Ladeanzeige, Fehler lassen den alten Stand stehen. */
    fun poll() {
        viewModelScope.launch {
            val r = repo.health()
            if (r is UiState.Success) state = r
        }
    }

    private fun load() {
        viewModelScope.launch {
            val r = repo.health()
            if (r is UiState.Success || state !is UiState.Success) state = r
            refreshing = false
        }
    }
}
