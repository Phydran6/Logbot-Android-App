package de.phytech.logbot.feature.branding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.BrandingConfigDto
import de.phytech.logbot.data.repository.BrandingRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrandingViewModel @Inject constructor(
    private val repo: BrandingRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<BrandingConfigDto>>(UiState.Loading)
        private set
    var actionError by mutableStateOf<String?>(null)
        private set
    var savedTick by mutableStateOf(0)
        private set

    init {
        refresh()
    }

    fun refresh() {
        state = UiState.Loading
        viewModelScope.launch { state = repo.load() }
    }

    fun save(config: BrandingConfigDto) {
        viewModelScope.launch {
            when (val r = repo.save(config)) {
                is UiState.Success -> {
                    state = UiState.Success(r.data)
                    savedTick++
                }
                is UiState.Error -> actionError = r.message
                else -> {}
            }
        }
    }

    fun reset() {
        viewModelScope.launch {
            when (val r = repo.reset()) {
                is UiState.Success -> {
                    state = UiState.Success(r.data)
                    savedTick++
                }
                is UiState.Error -> actionError = r.message
                else -> {}
            }
        }
    }

    fun dismissActionError() {
        actionError = null
    }
}
