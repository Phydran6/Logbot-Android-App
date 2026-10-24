/**
 * DTOs der schlanken App-Schnittstelle des Servers (/api/app/…, routes/mobile.py)
 * und der Meldungen (/api/app/notifications, /api/app/push).
 *
 * Die App-Schnittstelle ist für Mobilfunk gebaut: Blättern per Cursor statt
 * Seitenzahl, Logzeilen mit lesbarer Zusammenfassung, ein Aufruf beim Start.
 */
package de.phytech.logbot.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// --- Start ---------------------------------------------------------------

@Serializable
data class BootstrapDto(
    val server: BootstrapServerDto = BootstrapServerDto(),
    val user: BootstrapUserDto = BootstrapUserDto(),
    val filters: BootstrapFiltersDto = BootstrapFiltersDto(),
)

@Serializable
data class BootstrapServerDto(
    val name: String = "LogBot",
    val version: String = "",
    @SerialName("api_level") val apiLevel: Int = 0,
    val capabilities: List<String> = emptyList(),
)

@Serializable
data class BootstrapUserDto(
    val username: String = "",
    val role: String = "user",
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("mfa_enabled") val mfaEnabled: Boolean = false,
)

@Serializable
data class BootstrapFiltersDto(
    val hostnames: List<String> = emptyList(),
    @SerialName("device_types") val deviceTypes: List<String> = emptyList(),
    val levels: List<String> = emptyList(),
)

// --- Übersicht -----------------------------------------------------------

@Serializable
data class SummaryDto(
    @SerialName("logs_total") val logsTotal: Long = 0,
    @SerialName("logs_today") val logsToday: Long = 0,
    @SerialName("by_level_today") val byLevelToday: Map<String, Long> = emptyMap(),
    @SerialName("problems_today") val problemsToday: Long = 0,
    val devices: SummaryDevicesDto = SummaryDevicesDto(),
)

@Serializable
data class SummaryDevicesDto(
    val total: Int = 0,
    val online: Int = 0,
    val offline: Int = 0,
)

// --- Logs ----------------------------------------------------------------

@Serializable
data class HighlightDto(
    val key: String = "",
    val label: String = "",
    val value: String = "",
    val kind: String = "",
)

@Serializable
data class AppLogDto(
    val id: Long = 0,
    val ts: String? = null,
    val host: String? = null,
    val ip: String? = null,
    val level: String? = null,
    val source: String? = null,
    val message: String? = null,
    /** Lesbare Fassung, nur wenn sie sich von [message] unterscheidet. */
    val summary: String? = null,
    val badges: List<HighlightDto> = emptyList(),
    val app: String? = null,
) {
    val displayText: String get() = summary ?: message.orEmpty()
}

@Serializable
data class AppLogPageDto(
    val items: List<AppLogDto> = emptyList(),
    @SerialName("has_more") val hasMore: Boolean = false,
    @SerialName("next_before_id") val nextBeforeId: Long? = null,
    @SerialName("newest_id") val newestId: Long? = null,
)

@Serializable
data class ParsedLogDto(
    val summary: String = "",
    val format: String = "plain",
    val highlights: List<HighlightDto> = emptyList(),
    val fields: Map<String, JsonElement> = emptyMap(),
)

@Serializable
data class AppLogDetailDto(
    val id: Long = 0,
    val ts: String? = null,
    val host: String? = null,
    val ip: String? = null,
    val level: String? = null,
    val source: String? = null,
    val message: String? = null,
    val facility: Int? = null,
    @SerialName("agent_id") val agentId: Int? = null,
    val parsed: ParsedLogDto = ParsedLogDto(),
    val raw: String? = null,
)

// --- Geräte --------------------------------------------------------------

@Serializable
data class DeviceDto(
    val id: Int = 0,
    val host: String = "",
    val ip: String? = null,
    val type: String? = null,
    @SerialName("last_seen") val lastSeen: String? = null,
    val online: Boolean = false,
)

@Serializable
data class DeviceListDto(
    val items: List<DeviceDto> = emptyList(),
    @SerialName("offline_after_seconds") val offlineAfterSeconds: Int = 300,
)

// --- Meldungen und Push --------------------------------------------------

@Serializable
data class NotificationDto(
    val id: Long = 0,
    @SerialName("created_at") val createdAt: String? = null,
    val severity: String = "info",
    val title: String = "",
    val body: String? = null,
    val hostname: String? = null,
    @SerialName("log_id") val logId: Long? = null,
    @SerialName("agent_id") val agentId: Int? = null,
    val read: Boolean = false,
)

@Serializable
data class NotificationPageDto(
    val items: List<NotificationDto> = emptyList(),
    @SerialName("unread_count") val unreadCount: Int = 0,
    @SerialName("has_more") val hasMore: Boolean = false,
    @SerialName("next_before_id") val nextBeforeId: Long? = null,
    @SerialName("newest_id") val newestId: Long? = null,
)

@Serializable
data class NotificationsReadRequest(
    @SerialName("up_to_id") val upToId: Long,
)

@Serializable
data class PushRegisterRequest(
    val endpoint: String,
    val p256dh: String? = null,
    val auth: String? = null,
    @SerialName("device_name") val deviceName: String,
    @SerialName("min_severity") val minSeverity: String? = null,
)

@Serializable
data class PushUnregisterRequest(
    val endpoint: String,
)

@Serializable
data class VapidKeyDto(
    @SerialName("public_key") val publicKey: String? = null,
)

@Serializable
data class PushTestResultDto(
    @SerialName("device_name") val deviceName: String? = null,
    val ok: Boolean = false,
    val message: String = "",
)

@Serializable
data class PushTestDto(
    val results: List<PushTestResultDto> = emptyList(),
)

/** Inhalt eines Push: bewusst minimal, der Rest kommt aus der Meldungsliste. */
@Serializable
data class PushPayloadDto(
    val type: String = "notification",
    val id: Long = 0,
    val severity: String = "info",
    val title: String = "",
)
