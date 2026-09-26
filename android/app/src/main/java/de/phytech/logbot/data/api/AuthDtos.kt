/**
 * DTOs für die Auth-Endpoints des Logbot-Servers (kotlinx.serialization).
 * Feldnamen via @SerialName an das Server-JSON (snake_case) angepasst.
 */
package de.phytech.logbot.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
)

/**
 * Antwort von /api/auth/login: entweder ein Token (access_token gesetzt)
 * ODER ein MFA-Hinweis (mfa_required = true, mfa_token gesetzt).
 */
@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("mfa_required") val mfaRequired: Boolean = false,
    @SerialName("mfa_token") val mfaToken: String? = null,
    @SerialName("expires_in_seconds") val expiresInSeconds: Int? = null,
)

@Serializable
data class MfaLoginRequest(
    @SerialName("mfa_token") val mfaToken: String,
    val code: String,
)

@Serializable
data class AppTokenExchangeRequest(
    val token: String,
)

@Serializable
data class UserResponse(
    val id: Int,
    val username: String,
    val email: String? = null,
    val role: String = "user",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
)
