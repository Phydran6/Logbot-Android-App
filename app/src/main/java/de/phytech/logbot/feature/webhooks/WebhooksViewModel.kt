package de.phytech.logbot.feature.webhooks

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.WebhookCreateRequest
import de.phytech.logbot.data.api.WebhookDto
import de.phytech.logbot.data.api.WebhookUpdateRequest
import de.phytech.logbot.data.repository.WebhooksRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WebhooksViewModel @Inject constructor(
    private val repo: WebhooksRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<List<WebhookDto>>>(UiState.Loading)
        private set
    var actionError by mutableStateOf<String?>(null)
        private set

    init {
        refresh()
    }

    fun refresh() {
        state = UiState.Loading
        viewModelScope.launch { state = repo.webhooks() }
    }

    fun create(request: WebhookCreateRequest, onDone: () -> Unit) =
        execute(onDone) { repo.create(request) }

    fun update(id: Int, request: WebhookUpdateRequest, onDone: () -> Unit) =
        execute(onDone) { repo.update(id, request) }

    fun regenerate(id: Int) = execute({}) { repo.regenerateToken(id) }

    fun delete(id: Int) = execute({}) { repo.delete(id) }

    fun callUrl(webhook: WebhookDto): String = repo.callUrl(webhook)

    private fun execute(onDone: () -> Unit, block: suspend () -> UiState<Unit>) {
        viewModelScope.launch {
            when (val r = block()) {
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
