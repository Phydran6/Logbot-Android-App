/**
 * Repository für den KI-Assistenten.
 */
package de.phytech.logbot.data.repository

import de.phytech.logbot.core.network.safeApiCall
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.AiApi
import de.phytech.logbot.data.api.AiStatusDto
import de.phytech.logbot.data.api.ChatMessageDto
import de.phytech.logbot.data.api.ChatRequest
import de.phytech.logbot.data.api.ChatResponseDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepository @Inject constructor(
    private val api: AiApi,
) {
    suspend fun status(): UiState<AiStatusDto> = safeApiCall { api.status() }

    suspend fun chat(history: List<ChatMessageDto>, hostname: String?): UiState<ChatResponseDto> =
        safeApiCall { api.chat(ChatRequest(messages = history.takeLast(MAX_HISTORY), hostname = hostname)) }

    private companion object {
        // Der Server nimmt höchstens 40 Nachrichten; mehr Verlauf hilft der KI ohnehin kaum.
        const val MAX_HISTORY = 20
    }
}
