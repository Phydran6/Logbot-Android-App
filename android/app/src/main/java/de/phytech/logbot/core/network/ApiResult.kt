/**
 * Wiederverwendbarer, fehlertoleranter API-Aufruf: führt den Block auf dem
 * IO-Dispatcher aus und mappt Erfolg/Exception auf UiState.
 */
package de.phytech.logbot.core.network

import de.phytech.logbot.core.ui.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

suspend fun <T> safeApiCall(block: suspend () -> T): UiState<T> =
    withContext(Dispatchers.IO) {
        try {
            UiState.Success(block())
        } catch (e: Exception) {
            UiState.Error(mapApiError(e))
        }
    }

fun mapApiError(e: Throwable): String = when (e) {
    is HttpException -> when (e.code()) {
        401 -> "Sitzung abgelaufen – bitte neu anmelden"
        403 -> "Keine Berechtigung"
        404 -> "Nicht gefunden"
        else -> "Serverfehler (${e.code()})"
    }
    is IOException -> "Keine Verbindung zur Instanz"
    else -> e.message ?: "Unbekannter Fehler"
}
