/**
 * Repository für den Zustand des Servers (Health) und die Log-Statistik.
 */
package de.phytech.logbot.data.repository

import de.phytech.logbot.core.network.safeApiCall
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.HealthDetailedDto
import de.phytech.logbot.data.api.HealthDto
import de.phytech.logbot.data.api.LogStatsDto
import de.phytech.logbot.data.api.MonitoringApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitoringRepository @Inject constructor(
    private val api: MonitoringApi,
) {
    /** Ohne Anmeldung: antwortet unter der eingestellten Adresse ein Logbot-Server? */
    suspend fun ping(): UiState<HealthDto> = safeApiCall { api.health() }

    suspend fun health(): UiState<HealthDetailedDto> = safeApiCall { api.healthDetailed() }
    suspend fun stats(): UiState<LogStatsDto> = safeApiCall { api.logStats() }
}
