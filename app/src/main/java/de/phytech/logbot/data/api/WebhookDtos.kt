/**
 * DTOs für Webhooks (/api/webhooks).
 */
package de.phytech.logbot.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WebhookFiltersDto(
    val hostname: String? = null,
    val source: String? = null,
    val level: List<String>? = null,
)

@Serializable
data class WebhookDto(
    val id: Int = 0,
    val name: String = "",
    val description: String? = null,
    val filters: WebhookFiltersDto = WebhookFiltersDto(),
    @SerialName("max_results") val maxResults: Int = 100,
    @SerialName("include_raw") val includeRaw: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true,
    val token: String = "",
    @SerialName("call_count") val callCount: Int = 0,
    @SerialName("last_called_at") val lastCalledAt: String? = null,
)

@Serializable
data class WebhookCreateRequest(
    val name: String,
    val description: String? = null,
    val filters: WebhookFiltersDto = WebhookFiltersDto(),
    @SerialName("max_results") val maxResults: Int = 100,
    @SerialName("include_raw") val includeRaw: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true,
)

@Serializable
data class WebhookUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    val filters: WebhookFiltersDto? = null,
    @SerialName("max_results") val maxResults: Int? = null,
    @SerialName("include_raw") val includeRaw: Boolean? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
)
