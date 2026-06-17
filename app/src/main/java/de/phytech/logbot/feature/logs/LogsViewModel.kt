package de.phytech.logbot.feature.logs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.LogDetailDto
import de.phytech.logbot.data.api.LogDto
import de.phytech.logbot.data.repository.LOGS_PAGE_SIZE
import de.phytech.logbot.data.repository.LogFilter
import de.phytech.logbot.data.repository.LogsRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val repo: LogsRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<List<LogDto>>>(UiState.Loading)
        private set
    var loadingMore by mutableStateOf(false)
        private set
    var canLoadMore by mutableStateOf(false)
        private set
    var filter by mutableStateOf(LogFilter())
        private set
    var detail by mutableStateOf<UiState<LogDetailDto>?>(null)
        private set

    private var page = 1
    private val items = mutableListOf<LogDto>()

    init {
        refresh()
    }

    fun refresh() {
        page = 1
        items.clear()
        canLoadMore = false
        loadingMore = false
        state = UiState.Loading
        viewModelScope.launch { loadPage(reset = true) }
    }

    fun applyFilter(newFilter: LogFilter) {
        filter = newFilter
        refresh()
    }

    fun loadMore() {
        if (loadingMore || !canLoadMore) return
        loadingMore = true
        page += 1
        viewModelScope.launch { loadPage(reset = false) }
    }

    private suspend fun loadPage(reset: Boolean) {
        when (val r = repo.page(page, filter)) {
            is UiState.Success -> {
                items.addAll(r.data.items)
                canLoadMore = r.data.items.size >= LOGS_PAGE_SIZE
                state = UiState.Success(items.toList())
                loadingMore = false
            }
            is UiState.Error -> {
                if (reset) {
                    state = UiState.Error(r.message)
                } else {
                    page -= 1
                    loadingMore = false
                }
            }
            else -> loadingMore = false
        }
    }

    fun openDetail(id: Long) {
        detail = UiState.Loading
        viewModelScope.launch { detail = repo.detail(id) }
    }

    fun closeDetail() {
        detail = null
    }
}
