/**
 * DTOs für die KI (/api/ai/status, /api/ai/chat).
 */
package de.phytech.logbot.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AiStatusDto(
    val available: Boolean = false,
    val allowed: Boolean = false,
    val provider: String = "off",
    @SerialName("provider_label") val providerLabel: String = "",
    @SerialName("max_logs") val maxLogs: Int = 0,
)

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String,
)

@Serializable
data class ChatRequest(
    val messages: List<ChatMessageDto>,
    val hostname: String? = null,
)

@Serializable
data class ToolUseDto(
    val tool: String = "",
    val args: Map<String, JsonElement> = emptyMap(),
)

@Serializable
data class ChatResponseDto(
    val answer: String = "",
    val model: String = "",
    val provider: String = "",
    @SerialName("provider_label") val providerLabel: String = "",
    val mode: String = "",
    @SerialName("logs_sent") val logsSent: Int = 0,
    val hosts: List<String> = emptyList(),
    @SerialName("tools_used") val toolsUsed: List<ToolUseDto> = emptyList(),
    @SerialName("duration_seconds") val durationSeconds: Double = 0.0,
)
