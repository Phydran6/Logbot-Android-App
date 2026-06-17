package de.phytech.logbot.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.repository.SettingsBundle
import de.phytech.logbot.data.repository.SettingsRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<SettingsBundle>>(UiState.Loading)
        private set
    var actionError by mutableStateOf<String?>(null)
        private set

    init {
        refresh()
    }

    fun refresh() {
        state = UiState.Loading
        viewModelScope.launch { state = repo.load() }
    }

    fun updateSetting(key: String, value: String, onDone: () -> Unit) {
        viewModelScope.launch {
            when (val r = repo.update(key, value)) {
                is UiState.Success -> {
                    onDone()
                    refresh()
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
