package de.phytech.logbot.feature.notifications

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.NotificationDto
import de.phytech.logbot.data.repository.NotificationsRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repo: NotificationsRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<List<NotificationDto>>>(UiState.Loading)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var loadingMore by mutableStateOf(false)
        private set
    var canLoadMore by mutableStateOf(false)
        private set
    var actionError by mutableStateOf<String?>(null)
        private set

    private var nextBeforeId: Long? = null
    private val items = mutableListOf<NotificationDto>()

    init {
        load(reset = true)
    }

    fun refresh() {
        refreshing = true
        load(reset = true)
    }

    fun retry() {
        state = UiState.Loading
        load(reset = true)
    }

    fun loadMore() {
        if (loadingMore || !canLoadMore) return
        loadingMore = true
        load(reset = false)
    }

    private fun load(reset: Boolean) {
        viewModelScope.launch {
            when (val r = repo.page(if (reset) null else nextBeforeId)) {
                is UiState.Success -> {
                    if (reset) items.clear()
                    items.addAll(r.data.items.filter { n -> items.none { it.id == n.id } })
                    nextBeforeId = r.data.nextBeforeId
                    canLoadMore = r.data.hasMore && r.data.nextBeforeId != null
                    state = UiState.Success(items.toList())
                }
                is UiState.Error -> if (state !is UiState.Success) state = r else actionError = r.message
                UiState.Loading -> Unit
            }
            refreshing = false
            loadingMore = false
        }
    }

    fun markAllRead() {
        val newest = items.maxOfOrNull { it.id } ?: return
        viewModelScope.launch {
            when (val r = repo.markAllRead(newest)) {
                is UiState.Success -> {
                    for (i in items.indices) items[i] = items[i].copy(read = true)
                    state = UiState.Success(items.toList())
                }
                is UiState.Error -> actionError = r.message
                UiState.Loading -> Unit
            }
        }
    }

    fun dismissActionError() {
        actionError = null
    }
}
