/**
 * Repository für Health- und Statistik-Daten. Mappt Erfolg/Fehler auf UiState.
 */
package de.phytech.logbot.data.repository

import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.HealthDetailedDto
import de.phytech.logbot.data.api.LogStatsDto
import de.phytech.logbot.data.api.MonitoringApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitoringRepository @Inject constructor(
    private val api: MonitoringApi,
) {
    suspend fun health(): UiState<HealthDetailedDto> = call { api.healthDetailed() }
    suspend fun stats(): UiState<LogStatsDto> = call { api.logStats() }

    private suspend fun <T> call(block: suspend () -> T): UiState<T> =
        withContext(Dispatchers.IO) {
            try {
                UiState.Success(block())
            } catch (e: Exception) {
                UiState.Error(mapError(e))
            }
        }

    private fun mapError(e: Exception): String = when (e) {
        is HttpException -> when (e.code()) {
            401 -> "Sitzung abgelaufen – bitte neu anmelden"
            403 -> "Keine Berechtigung"
            else -> "Serverfehler (${e.code()})"
        }
        is IOException -> "Keine Verbindung zur Instanz"
        else -> e.message ?: "Unbekannter Fehler"
    }
}
