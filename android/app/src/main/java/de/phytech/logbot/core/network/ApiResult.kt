/**
 * Wiederverwendbarer, fehlertoleranter API-Aufruf: führt den Block auf dem
 * IO-Dispatcher aus und mappt Erfolg/Exception auf UiState.
 *
 * Fehlertexte kommen, wo möglich, vom Server: FastAPI schickt im Feld `detail`
 * einen Satz, der sagt, was los ist ("Die KI-Auswertung ist nicht eingerichtet …").
 * Das ist für den Nutzer wertvoller als "Serverfehler (409)".
 */
package de.phytech.logbot.core.network

import de.phytech.logbot.core.ui.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

suspend fun <T> safeApiCall(block: suspend () -> T): UiState<T> =
    withContext(Dispatchers.IO) {
        try {
            UiState.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UiState.Error(mapApiError(e), (e as? HttpException)?.code())
        }
    }

/** Wirft für eine Antwort ohne Inhalt (204) eine [HttpException], wenn sie fehlschlug. */
fun retrofit2.Response<*>.requireSuccess() {
    if (!isSuccessful) throw HttpException(this)
}

fun mapApiError(e: Throwable): String = when (e) {
    is HttpException -> serverDetail(e) ?: when (e.code()) {
        401 -> "Sitzung abgelaufen – bitte neu anmelden"
        403 -> "Keine Berechtigung"
        404 -> "Nicht gefunden – ist der Server aktuell?"
        429 -> "Zu viele Anfragen – bitte kurz warten"
        502, 503, 504 -> "Der Server ist gerade nicht erreichbar (${e.code()})"
        else -> "Serverfehler (${e.code()})"
    }
    is SocketTimeoutException -> "Zeitüberschreitung – der Server antwortet nicht"
    is UnknownHostException -> "Server nicht gefunden – Adresse und Netz prüfen"
    is SSLException -> "Sichere Verbindung fehlgeschlagen (Zertifikat?)"
    is IOException -> "Keine Verbindung zur Instanz"
    else -> e.message ?: "Unbekannter Fehler"
}

private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

/** Liest `detail` aus einer FastAPI-Fehlerantwort (Text oder Liste von Prüffehlern). */
private fun serverDetail(e: HttpException): String? = runCatching {
    val body = e.response()?.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: return null
    when (val detail = lenientJson.parseToJsonElement(body).jsonObject["detail"]) {
        is JsonPrimitive -> detail.contentOrNull
        is JsonArray -> detail.mapNotNull { item ->
            (item as? JsonObject)?.get("msg")?.jsonPrimitive?.contentOrNull
        }.joinToString("\n").ifBlank { null }
        else -> null
    }
}.getOrNull()?.take(400)
