package de.phytech.logbot.feature.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.AiStatusDto
import de.phytech.logbot.data.api.ChatMessageDto
import de.phytech.logbot.data.api.ChatResponseDto
import de.phytech.logbot.data.repository.AiRepository
import de.phytech.logbot.data.repository.MobileRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Eine Nachricht im Gespräch; [meta] nur bei Antworten der KI. */
data class ChatEntry(
    val fromUser: Boolean,
    val text: String,
    val meta: ChatResponseDto? = null,
    val failed: Boolean = false,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repo: AiRepository,
    mobile: MobileRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    /** Gerät, auf das sich das Gespräch beschränkt (leer = alles). */
    var host by mutableStateOf(savedState.get<String>("host").orEmpty())
        private set

    val knownHosts: List<String> = mobile.bootstrap.value?.filters?.hostnames.orEmpty()

    val entries = mutableStateListOf<ChatEntry>()
    var sending by mutableStateOf(false)
        private set
    var status by mutableStateOf<UiState<AiStatusDto>>(UiState.Loading)
        private set

    init {
        viewModelScope.launch {
            status = repo.status()
            // Vorbereitete Frage (z.B. aus der Übersicht) genau einmal abschicken.
            val question = savedState.get<String>("q").orEmpty()
            if (question.isNotBlank() && savedState.get<Boolean>(ASKED) != true &&
                (status as? UiState.Success)?.data?.allowed == true
            ) {
                savedState[ASKED] = true
                send(question)
            }
        }
    }

    fun retryStatus() {
        status = UiState.Loading
        viewModelScope.launch { status = repo.status() }
    }

    fun selectHost(value: String) {
        if (value == host) return
        host = value
        savedState["host"] = value
        entries.clear()
    }

    fun newConversation() {
        entries.clear()
    }

    fun send(text: String) {
        val question = text.trim()
        if (question.isEmpty() || sending) return
        entries.add(ChatEntry(fromUser = true, text = question))
        ask()
    }

    /** Letzte Frage noch einmal schicken (nach einem Fehler). */
    fun retry() {
        if (entries.lastOrNull()?.failed == true) entries.removeAt(entries.lastIndex)
        ask()
    }

    private fun ask() {
        sending = true
        // Fehlversuche gehen nicht mit: die KI soll nur echte Antworten als Verlauf sehen.
        val history = entries.filter { !it.failed }.map {
            ChatMessageDto(role = if (it.fromUser) "user" else "assistant", content = it.text)
        }
        viewModelScope.launch {
            when (val r = repo.chat(history, host.ifBlank { null })) {
                is UiState.Success -> entries.add(ChatEntry(fromUser = false, text = r.data.answer, meta = r.data))
                is UiState.Error -> entries.add(ChatEntry(fromUser = false, text = r.message, failed = true))
                UiState.Loading -> Unit
            }
            sending = false
        }
    }

    private companion object {
        const val ASKED = "asked"
    }
}
