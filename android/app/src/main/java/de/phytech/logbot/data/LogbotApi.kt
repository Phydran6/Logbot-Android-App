/**
 * Datei:        LogbotApi.kt
 * Projekt:      Logbot
 * Paket:        de.phytech.logbot.data
 *
 * Beschreibung:
 * Schmaler HTTP-Client fuer die REST-Schnittstelle des Logbot-Servers.
 *
 * Bewusst ohne Bibliothek: Es sind eine Handvoll GET-Aufrufe gegen genau eine
 * Instanz. HttpURLConnection und org.json liegen im System, das spart rund
 * 1 MB APK und eine Abhaengigkeit, die gepflegt werden muesste.
 *
 * Nebenlaeufigkeit: Aufrufe laufen in einem kleinen Thread-Pool, die Antwort
 * kommt auf dem Hauptthread zurueck. Callbacks werden verworfen, sobald der
 * Aufrufer `cancelled` meldet - so schreibt keine Antwort mehr in eine
 * Oberflaeche, die es nicht mehr gibt.
 */
package de.phytech.logbot.data

import android.net.Uri
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** Ergebnis eines Aufrufs: entweder Nutzdaten oder ein eingeordneter Fehler. */
sealed class ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>()
    data class Err(val error: ApiError) : ApiResult<Nothing>()
}

/**
 * Fehler mit Einordnung statt nacktem Stacktrace.
 *
 * `status` ist der HTTP-Code, 0 wenn die Verbindung gar nicht zustande kam.
 */
data class ApiError(val status: Int, val message: String) {
    /** 401/403: Token abgelaufen oder zurueckgezogen - die App muss neu einrichten. */
    val unauthorized: Boolean get() = status == 401 || status == 403
    /** 404: Endpunkt gibt es auf diesem Serverstand noch nicht. Kein Fehler des Nutzers. */
    val notImplemented: Boolean get() = status == 404
    val offline: Boolean get() = status == 0
}

class LogbotApi(baseUrl: String, private val token: String) {

    private val base = baseUrl.trimEnd('/')

    // --- Endpunkte ----------------------------------------------------------

    fun health(done: (ApiResult<ServerHealth>) -> Unit) =
        get("/api/health/detailed", done) { ServerHealth.from(it) }

    fun filterOptions(done: (ApiResult<FilterOptions>) -> Unit) =
        get("/api/logs/filter-options", done) { FilterOptions.from(it) }

    fun logs(query: LogQuery, done: (ApiResult<LogPage>) -> Unit) =
        get(query.toPath(), done) { LogPage.from(it) }

    /** Einzelner Eintrag samt Rohtext - erst beim Aufklappen der Detailansicht. */
    fun logDetail(id: Long, done: (ApiResult<LogDetail>) -> Unit) =
        get("/api/logs/$id", done) { LogDetail.from(it) }

    /** Optionaler Endpunkt, siehe docs/SERVER-API.md. 404 = Server kann es noch nicht. */
    fun mailStatus(done: (ApiResult<MailStatus>) -> Unit) =
        get("/api/mail/status", done) { MailStatus.from(it) }

    /** Optionaler Endpunkt: stoesst eine Passwort-Reset-Mail ueber Postfix an. */
    fun requestPasswordReset(login: String, done: (ApiResult<String>) -> Unit) {
        val body = JSONObject().put("login", login).toString()
        post("/api/mail/password-reset", body, done) {
            it.optStringOrEmpty("message").ifEmpty { "Reset-Mail wurde in die Warteschlange gelegt." }
        }
    }

    // --- Innereien ----------------------------------------------------------

    private fun <T> get(path: String, done: (ApiResult<T>) -> Unit, parse: (JSONObject) -> T) =
        request("GET", path, null, done, parse)

    private fun <T> post(
        path: String,
        body: String,
        done: (ApiResult<T>) -> Unit,
        parse: (JSONObject) -> T
    ) = request("POST", path, body, done, parse)

    private fun <T> request(
        method: String,
        path: String,
        body: String?,
        done: (ApiResult<T>) -> Unit,
        parse: (JSONObject) -> T
    ) {
        pool.execute {
            val result: ApiResult<T> = try {
                ApiResult.Ok(parse(JSONObject(execute(method, path, body))))
            } catch (e: ApiException) {
                ApiResult.Err(ApiError(e.status, e.message ?: "Unbekannter Fehler"))
            } catch (e: IOException) {
                ApiResult.Err(ApiError(0, e.message ?: "Server nicht erreichbar"))
            } catch (e: Exception) {
                ApiResult.Err(ApiError(-1, "Antwort nicht lesbar: ${e.message}"))
            }
            main.post { done(result) }
        }
    }

    private fun execute(method: String, path: String, body: String?): String {
        val connection = URL(base + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }

            val status = connection.responseCode
            if (status in 200..299) {
                return connection.inputStream.bufferedReader().use { it.readText() }
            }

            // Fehlertext des Servers mitnehmen, aber nur wenn er kurz und
            // lesbar ist. FastAPI liefert {"detail": "..."} - alles andere
            // (HTML-Fehlerseiten eines Reverse-Proxy) wuerde nur verwirren.
            val detail = runCatching {
                val raw = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                JSONObject(raw).optStringOrEmpty("detail")
            }.getOrDefault("")
            throw ApiException(status, detail.ifEmpty { describe(status) })
        } finally {
            connection.disconnect()
        }
    }

    private fun describe(status: Int): String = when (status) {
        401, 403 -> "Zugang abgelehnt - Token ungueltig oder abgelaufen"
        404 -> "Dieser Serverstand kennt den Endpunkt noch nicht"
        429 -> "Zu viele Anfragen - kurz warten"
        in 500..599 -> "Der Server meldet einen internen Fehler ($status)"
        else -> "Unerwartete Antwort ($status)"
    }

    private class ApiException(val status: Int, message: String) : Exception(message)

    companion object {
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 20_000

        // Drei Threads reichen: Status, Logs und Mail koennen parallel laufen,
        // mehr gleichzeitige Anfragen stellt die App nie.
        private val pool = Executors.newFixedThreadPool(3)
        private val main = Handler(Looper.getMainLooper())

        /** Baut den Client aus den gespeicherten Zugangsdaten, oder null. */
        fun from(url: String?, token: String?): LogbotApi? =
            if (url.isNullOrBlank() || token.isNullOrBlank()) null else LogbotApi(url, token)
    }
}

/**
 * Filter der Log-Ansicht.
 *
 * Leere Werte werden weggelassen - der Server behandelt einen gesetzten,
 * leeren Parameter sonst als Suche nach dem leeren String.
 */
data class LogQuery(
    val page: Int = 1,
    val pageSize: Int = 50,
    val search: String = "",
    val hostname: String = "",
    val minSeverity: String = "",
    val category: String = ""
) {
    fun toPath(): String {
        val builder = Uri.parse("/api/logs").buildUpon()
            .appendQueryParameter("page", page.toString())
            .appendQueryParameter("page_size", pageSize.toString())
        if (search.isNotBlank()) builder.appendQueryParameter("search", search.trim())
        if (hostname.isNotBlank()) builder.appendQueryParameter("hostname", hostname.trim())
        if (minSeverity.isNotBlank()) builder.appendQueryParameter("min_severity", minSeverity)
        if (category.isNotBlank()) builder.appendQueryParameter("category", category)
        return builder.build().toString()
    }

    /** True, wenn die Liste gerade gefiltert ist - fuer den "Filter zuruecksetzen"-Knopf. */
    val filtered: Boolean
        get() = search.isNotBlank() || hostname.isNotBlank() ||
            minSeverity.isNotBlank() || category.isNotBlank()
}
