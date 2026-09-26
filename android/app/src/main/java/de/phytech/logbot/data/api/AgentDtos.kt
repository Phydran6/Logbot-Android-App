/**
 * DTOs für Agents (/api/agents).
 */
package de.phytech.logbot.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AgentDto(
    val id: Int = 0,
    val hostname: String = "",
    @SerialName("ip_address") val ipAddress: String? = null,
    @SerialName("device_type") val deviceType: String = "",
    @SerialName("last_seen") val lastSeen: String? = null,
    @SerialName("is_online") val isOnline: Boolean? = null,
    @SerialName("log_count") val logCount: Long? = null,
    @SerialName("retention_days") val retentionDays: Int? = null,
    @SerialName("retention_max_logs") val retentionMaxLogs: Int? = null,
)

@Serializable
data class AgentListResponse(
    val items: List<AgentDto> = emptyList(),
    val total: Long = 0,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 50,
)
