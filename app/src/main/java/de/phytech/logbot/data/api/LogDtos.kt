/**
 * DTOs für die Log-Liste (/api/logs).
 */
package de.phytech.logbot.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
