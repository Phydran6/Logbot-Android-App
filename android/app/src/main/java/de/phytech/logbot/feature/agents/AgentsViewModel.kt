package de.phytech.logbot.feature.agents

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.AgentDto
import de.phytech.logbot.data.repository.AgentsRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AgentsViewModel @Inject constructor(
    private val repo: AgentsRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<List<AgentDto>>>(UiState.Loading)
        private set
    var actionError by mutableStateOf<String?>(null)
        private set

    init {
        refresh()
    }

    fun refresh() {
        state = UiState.Loading
        viewModelScope.launch { state = repo.agents() }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            when (val r = repo.delete(id)) {
                is UiState.Success -> refresh()
                is UiState.Error -> actionError = r.message
                else -> {}
            }
        }
    }

    fun dismissActionError() {
        actionError = null
    }
}
