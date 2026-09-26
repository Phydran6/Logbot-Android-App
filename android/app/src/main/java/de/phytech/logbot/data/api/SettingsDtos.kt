/**
 * DTOs für Einstellungen (/api/settings). Werte sind beliebiges JSON (JsonElement).
 */
package de.phytech.logbot.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SettingsResponse(
    val settings: Map<String, JsonElement> = emptyMap(),
)

@Serializable
data class SettingUpdateRequest(
    val value: JsonElement,
)

@Serializable
data class DatabaseSettingsDto(
    val host: String = "",
    val port: Int = 0,
    val user: String = "",
    val name: String = "",
    val password: String = "",
)

@Serializable
data class RetentionResultDto(
    @SerialName("logs_to_delete") val logsToDelete: Long? = null,
    @SerialName("deleted_count") val deletedCount: Long? = null,
    @SerialName("oldest_log_date") val oldestLogDate: String? = null,
    val message: String? = null,
)
