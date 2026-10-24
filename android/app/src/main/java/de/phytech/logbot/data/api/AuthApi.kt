/**
 * Retrofit-Interface für die Auth-Endpoints. Pfade relativ zur (dynamischen) Base-URL.
 */
package de.phytech.logbot.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    /** Schritt 1: Passwort-Login (form-urlencoded, OAuth2PasswordRequestForm). */
    @FormUrlEncoded
    @POST("api/auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
    ): LoginResponse

    /** Schritt 2 bei aktiver MFA: Pending-Token + TOTP/Backup-Code gegen Access-Token. */
    @POST("api/auth/login/mfa")
    suspend fun loginMfa(@Body request: MfaLoginRequest): TokenResponse

    /** Aktuellen Benutzer (inkl. Rolle) laden. */
    @GET("api/auth/me")
    suspend fun me(): UserResponse

    /** QR-App-Login: einmaligen Token gegen vollwertiges Access-Token tauschen. */
    @POST("api/auth/app-token/exchange")
    suspend fun exchangeAppToken(@Body request: AppTokenExchangeRequest): TokenResponse

    /** Anmelde-Link per Mail anfordern (Antwort immer gleich, egal ob die Adresse bekannt ist). */
    @POST("api/auth/magic-link/request")
    suspend fun requestMagicLink(@Body request: MagicLinkRequest): MagicLinkRequestResponse

    /** Link einlösen – oder nachfragen, ob er auf einem anderen Gerät bestätigt wurde. */
    @POST("api/auth/magic-link/exchange")
    suspend fun exchangeMagicLink(@Body request: MagicLinkExchangeRequest): MagicLinkExchangeResponse

    /** Passwort vergessen: der Server schickt einen Link zum Zurücksetzen (immer 202). */
    @POST("api/auth/password/forgot")
    suspend fun forgotPassword(@Body request: PasswordForgotRequest): Response<Unit>
}
