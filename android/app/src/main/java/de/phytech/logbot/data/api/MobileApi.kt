/**
 * Retrofit-Interface der App-Schnittstelle (/api/app/…): Start, Übersicht, Logs
 * mit Cursor, Geräte, Meldungen und Push-Anmeldung.
 */
package de.phytech.logbot.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MobileApi {

    @GET("api/app/bootstrap")
    suspend fun bootstrap(): BootstrapDto

    @GET("api/app/summary")
    suspend fun summary(): SummaryDto

    @GET("api/app/logs")
    suspend fun logs(
        @Query("limit") limit: Int = 50,
        @Query("before_id") beforeId: Long? = null,
        @Query("hostname") hostname: String? = null,
        @Query("min_severity") minSeverity: String? = null,
        @Query("source") source: String? = null,
        @Query("search") search: String? = null,
        @Query("since_hours") sinceHours: Int? = null,
    ): AppLogPageDto

    @GET("api/app/logs/{id}")
    suspend fun logDetail(@Path("id") id: Long): AppLogDetailDto

    @GET("api/app/devices")
    suspend fun devices(
        @Query("limit") limit: Int = 500,
        @Query("only_offline") onlyOffline: Boolean = false,
    ): DeviceListDto

    @GET("api/app/notifications")
    suspend fun notifications(
        @Query("limit") limit: Int = 50,
        @Query("before_id") beforeId: Long? = null,
        @Query("since_id") sinceId: Long? = null,
    ): NotificationPageDto

    @POST("api/app/notifications/read")
    suspend fun markRead(@Body request: NotificationsReadRequest): Response<Unit>

    @GET("api/app/push/vapid")
    suspend fun vapidKey(): VapidKeyDto

    @POST("api/app/push")
    suspend fun registerPush(@Body request: PushRegisterRequest): Response<Unit>

    @HTTP(method = "DELETE", path = "api/app/push", hasBody = true)
    suspend fun unregisterPush(@Body request: PushUnregisterRequest): Response<Unit>

    /** Testmeldung an alle Geräte des Kontos; Ergebnis je Gerät. */
    @POST("api/app/push/test")
    suspend fun testPush(): PushTestDto
}
