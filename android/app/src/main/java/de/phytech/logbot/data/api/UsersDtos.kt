/**
 * Request-DTOs für Benutzerverwaltung (UserResponse liegt in AuthDtos).
 */
package de.phytech.logbot.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserCreateRequest(
    val username: String,
    val email: String? = null,
    val role: String = "user",
    val password: String,
)

@Serializable
data class UserUpdateRequest(
    val email: String? = null,
    val role: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    val password: String? = null,
)
