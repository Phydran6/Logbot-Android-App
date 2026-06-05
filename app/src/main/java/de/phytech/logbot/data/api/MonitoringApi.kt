/**
 * Retrofit-Interface für Health- und Statistik-Daten (Dashboard).
 */
package de.phytech.logbot.data.api

import retrofit2.http.GET

interface MonitoringApi {

    @GET("api/health/detailed")
    suspend fun healthDetailed(): HealthDetailedDto

    @GET("api/logs/stats")
    suspend fun logStats(): LogStatsDto
}
