/**
 * DTOs für Health- und Statistik-Endpoints des Logbot-Servers.
 */
package de.phytech.logbot.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthDetailedDto(
    val status: String = "",
    val version: String = "",
    @SerialName("uptime_seconds") val uptimeSeconds: Double = 0.0,
    @SerialName("cpu_percent") val cpuPercent: Double = 0.0,
    @SerialName("memory_percent") val memoryPercent: Double = 0.0,
    @SerialName("disk_percent") val diskPercent: Double = 0.0,
    @SerialName("database_connected") val databaseConnected: Boolean = false,
    @SerialName("logs_total") val logsTotal: Long = 0,
    @SerialName("logs_last_24h") val logsLast24h: Long = 0,
    @SerialName("agents_total") val agentsTotal: Int = 0,
    @SerialName("agents_online") val agentsOnline: Int = 0,
)

@Serializable
data class LogStatsDto(
    @SerialName("total_logs") val totalLogs: Long = 0,
    @SerialName("logs_today") val logsToday: Long = 0,
    @SerialName("logs_by_level") val logsByLevel: Map<String, Int> = emptyMap(),
    @SerialName("logs_by_source") val logsBySource: Map<String, Int> = emptyMap(),
    @SerialName("unique_hosts") val uniqueHosts: Int = 0,
)
