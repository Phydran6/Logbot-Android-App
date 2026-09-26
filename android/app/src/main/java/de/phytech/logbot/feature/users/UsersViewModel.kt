package de.phytech.logbot.feature.users

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.UserCreateRequest
import de.phytech.logbot.data.api.UserResponse
import de.phytech.logbot.data.api.UserUpdateRequest
import de.phytech.logbot.data.repository.UsersRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val repo: UsersRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<List<UserResponse>>>(UiState.Loading)
        private set
    var actionError by mutableStateOf<String?>(null)
        private set
    var working by mutableStateOf(false)
        private set

    init {
        refresh()
    }

    fun refresh() {
        state = UiState.Loading
        viewModelScope.launch { state = repo.users() }
    }

    fun create(request: UserCreateRequest, onDone: () -> Unit) =
        execute(onDone) { repo.create(request) }

    fun update(id: Int, request: UserUpdateRequest, onDone: () -> Unit) =
        execute(onDone) { repo.update(id, request) }

    fun delete(id: Int) = execute({}) { repo.delete(id) }

    fun resetMfa(id: Int, onDone: () -> Unit) = execute(onDone) { repo.resetMfa(id) }

    private fun execute(onDone: () -> Unit, block: suspend () -> UiState<Unit>) {
        working = true
        viewModelScope.launch {
            when (val r = block()) {
                is UiState.Success -> {
                    working = false
                    onDone()
                    refresh()
                }
                is UiState.Error -> {
                    working = false
                    actionError = r.message
                }
                else -> working = false
            }
        }
    }

    fun dismissActionError() {
        actionError = null
    }
}
