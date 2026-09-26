/**
 * DTOs für Logs (/api/logs) inkl. Detailansicht.
 */
package de.phytech.logbot.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class LogDto(
    val id: Long = 0,
    val hostname: String? = null,
    @SerialName("ip_address") val ipAddress: String? = null,
    val timestamp: String? = null,
    val level: String? = null,
    val source: String? = null,
    val message: String? = null,
)

@Serializable
data class LogListResponse(
    val items: List<LogDto> = emptyList(),
    val total: Long = 0,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 100,
)

@Serializable
data class LogDetailDto(
    val id: Long = 0,
    val hostname: String? = null,
    @SerialName("ip_address") val ipAddress: String? = null,
    val timestamp: String? = null,
    val level: String? = null,
    val source: String? = null,
    val message: String? = null,
    @SerialName("agent_id") val agentId: Int? = null,
    val facility: Int? = null,
    @SerialName("raw_message") val rawMessage: String? = null,
    @SerialName("extra_data") val extraData: JsonElement? = null,
)
