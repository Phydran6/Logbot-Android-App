/**
 * Retrofit-Interface für Einstellungen.
 */
package de.phytech.logbot.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface SettingsApi {

    @GET("api/settings")
    suspend fun settings(): SettingsResponse

    @PUT("api/settings/{key}")
    suspend fun updateSetting(
        @Path("key") key: String,
        @Body request: SettingUpdateRequest,
    ): Response<Unit>

    @GET("api/settings/database")
    suspend fun database(): DatabaseSettingsDto
}
