package de.phytech.logbot.feature.logs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.AppLogDetailDto
import de.phytech.logbot.data.api.AppLogDto
import de.phytech.logbot.data.repository.LogFilter
import de.phytech.logbot.data.repository.MobileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val repo: MobileRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    var state by mutableStateOf<UiState<List<AppLogDto>>>(UiState.Loading)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var loadingMore by mutableStateOf(false)
        private set
    var loadMoreError by mutableStateOf<String?>(null)
        private set
    var canLoadMore by mutableStateOf(false)
        private set
    var filter by mutableStateOf(
        LogFilter(
            hostname = savedState.get<String>("host")?.ifBlank { null },
            minSeverity = savedState.get<String>("severity")?.ifBlank { null },
        ),
    )
        private set
    var detail by mutableStateOf<UiState<AppLogDetailDto>?>(null)
        private set

    val knownHosts: List<String> get() = repo.bootstrap.value?.filters?.hostnames.orEmpty()

    private var nextBeforeId: Long? = null
    private val items = mutableListOf<AppLogDto>()
    private var loadJob: Job? = null
    private var searchJob: Job? = null

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

    fun setSeverity(value: String?) = applyFilter(filter.copy(minSeverity = value))
    fun setHost(value: String?) = applyFilter(filter.copy(hostname = value?.ifBlank { null }))
    fun setSinceHours(value: Int?) = applyFilter(filter.copy(sinceHours = value))

    /** Suche mit kurzer Verzögerung – nicht bei jedem Tastendruck eine Anfrage. */
    fun setSearch(value: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400)
            applyFilter(filter.copy(search = value.ifBlank { null }))
        }
    }

    fun clearFilter() = applyFilter(LogFilter())

    private fun applyFilter(newFilter: LogFilter) {
        if (newFilter == filter) return
        filter = newFilter
        state = UiState.Loading
        load(reset = true)
    }

    fun loadMore() {
        if (loadingMore || !canLoadMore || loadJob?.isActive == true) return
        loadingMore = true
        loadMoreError = null
        load(reset = false)
    }

    private fun load(reset: Boolean) {
        if (reset) loadJob?.cancel()
        val cursor = if (reset) null else nextBeforeId
        val requested = filter
        loadJob = viewModelScope.launch {
            when (val r = repo.logs(requested, cursor)) {
                is UiState.Success -> {
                    if (reset) items.clear()
                    // Doppelte vermeiden, falls sich beim Nachladen etwas überschneidet.
                    val known = items.mapTo(HashSet()) { it.id }
                    items.addAll(r.data.items.filter { it.id !in known })
                    nextBeforeId = r.data.nextBeforeId
                    canLoadMore = r.data.hasMore && r.data.nextBeforeId != null
                    state = UiState.Success(items.toList())
                }
                is UiState.Error -> {
                    if (reset && state !is UiState.Success) state = r
                    if (!reset) loadMoreError = r.message
                    if (reset && state is UiState.Success) loadMoreError = r.message
                }
                UiState.Loading -> Unit
            }
            loadingMore = false
            refreshing = false
        }
    }

    fun openDetail(id: Long) {
        detail = UiState.Loading
        viewModelScope.launch { detail = repo.logDetail(id) }
    }

    fun closeDetail() {
        detail = null
    }
}
