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

    init {
        refresh()
    }

    fun refresh() {
        state = UiState.Loading
        viewModelScope.launch {
            state = repo.health()
        }
    }
}
