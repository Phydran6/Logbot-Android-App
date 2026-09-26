/**
 * Retrofit-Interface für Branding.
 */
package de.phytech.logbot.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface BrandingApi {

    @GET("api/branding/config")
    suspend fun config(): BrandingConfigDto

    @PUT("api/branding/config")
    suspend fun updateConfig(@Body config: BrandingConfigDto): BrandingConfigDto

    @POST("api/branding/reset")
    suspend fun reset(): BrandingConfigDto
}
