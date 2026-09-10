/**
 * Datei:        Models.kt
 * Projekt:      Logbot
 * Paket:        de.phytech.logbot.data
 *
 * Beschreibung:
 * Datentypen der Server-Antworten, die die App nativ darstellt. Jeder Typ
 * bringt seinen eigenen Parser mit, damit die Zuordnung von JSON-Feld zu
 * Kotlin-Feld an genau einer Stelle steht.
 *
 * Welche Endpunkte dahinterstehen und was optional ist: docs/SERVER-API.md
 */
package de.phytech.logbot.data

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Antwort von GET /api/health/detailed. */
data class ServerHealth(
    val status: String,
    val version: String,
    val uptimeSeconds: Double,
    val cpuPercent: Double,
    val memoryPercent: Double,
    val diskPercent: Double,
    val databaseConnected: Boolean,
    val logsTotal: Long,
    val logsLast24h: Long,
    val agentsTotal: Int,
    val agentsOnline: Int
) {
    val healthy: Boolean get() = status.equals("healthy", ignoreCase = true)

    companion object {
        fun from(json: JSONObject) = ServerHealth(
            status = json.optString("status", "unknown"),
            version = json.optString("version", "?"),
            uptimeSeconds = json.optDouble("uptime_seconds", 0.0),
            cpuPercent = json.optDouble("cpu_percent", 0.0),
            memoryPercent = json.optDouble("memory_percent", 0.0),
            diskPercent = json.optDouble("disk_percent", 0.0),
            databaseConnected = json.optBoolean("database_connected", false),
            logsTotal = json.optLong("logs_total", 0L),
            logsLast24h = json.optLong("logs_last_24h", 0L),
            agentsTotal = json.optInt("agents_total", 0),
            agentsOnline = json.optInt("agents_online", 0)
        )
    }
}

/** Ein Log-Eintrag aus GET /api/logs. */
data class LogEntry(
    val id: Long,
    val hostname: String,
    val ipAddress: String,
    val timestamp: String,
    val level: String,
    val source: String,
    val message: String
) {
    /** Gruppe fuer die Farbgebung: error, warn, info, debug. */
    val severity: Severity get() = Severity.of(level)

    companion object {
        fun from(json: JSONObject) = LogEntry(
            id = json.optLong("id", 0L),
            hostname = json.optStringOrEmpty("hostname"),
            ipAddress = json.optStringOrEmpty("ip_address"),
            timestamp = json.optStringOrEmpty("timestamp"),
            source = json.optStringOrEmpty("source"),
            level = json.optStringOrEmpty("level"),
            message = json.optStringOrEmpty("message")
        )
    }
}

/**
 * Einzelner Eintrag aus GET /api/logs/{id}.
 *
 * Der Rohtext steht nur hier, nicht in der Liste - er ist oft ein Vielfaches
 * der aufbereiteten Nachricht und wuerde jede Seite unnoetig aufblaehen.
 */
data class LogDetail(
    val entry: LogEntry,
    val facility: Int,
    val rawMessage: String
) {
    companion object {
        fun from(json: JSONObject) = LogDetail(
            entry = LogEntry.from(json),
            facility = json.optInt("facility", -1),
            rawMessage = json.optStringOrEmpty("raw_message")
        )
    }
}

/** Seite aus GET /api/logs. */
data class LogPage(val items: List<LogEntry>, val total: Long, val page: Int, val pageSize: Int) {
    val hasMore: Boolean get() = page.toLong() * pageSize < total

    companion object {
        fun from(json: JSONObject): LogPage {
            val array = json.optJSONArray("items") ?: JSONArray()
            val items = ArrayList<LogEntry>(array.length())
            for (i in 0 until array.length()) {
                array.optJSONObject(i)?.let { items.add(LogEntry.from(it)) }
            }
            return LogPage(
                items = items,
                total = json.optLong("total", items.size.toLong()),
                page = json.optInt("page", 1),
                pageSize = json.optInt("page_size", items.size.coerceAtLeast(1))
            )
        }
    }
}

/**
 * Schweregrad-Gruppen. Der Server kennt syslog-Level in vielen Schreibweisen
 * (`err`, `error`, `ERR`), die Anzeige braucht nur vier Toepfe.
 */
enum class Severity {
    ERROR, WARN, INFO, DEBUG;

    companion object {
        fun of(level: String): Severity = when (level.lowercase(Locale.ROOT)) {
            "emerg", "emergency", "alert", "crit", "critical", "err", "error", "fatal" -> ERROR
            "warn", "warning", "notice" -> WARN
            "debug", "trace" -> DEBUG
            else -> INFO
        }
    }
}

/** Ein Filterwert samt Beschriftung, wie ihn GET /api/logs/filter-options liefert. */
data class FilterOption(val key: String, val label: String)

/** Auswahllisten fuer die Filterleiste. */
data class FilterOptions(
    val severities: List<FilterOption>,
    val categories: List<FilterOption>,
    val hostnames: List<String>
) {
    companion object {
        fun from(json: JSONObject) = FilterOptions(
            severities = json.optOptionList("severities"),
            categories = json.optOptionList("categories"),
            hostnames = json.optStringList("hostnames")
        )

        val EMPTY = FilterOptions(emptyList(), emptyList(), emptyList())
    }
}

/**
 * Zustand des Mailsystems auf dem Server (GET /api/mail/status).
 *
 * Optionaler Endpunkt: Aeltere Server-Staende kennen ihn nicht und antworten
 * mit 404. Die App zeigt dann einen Hinweis statt einer Fehlermeldung.
 */
data class MailStatus(
    val postfixRunning: Boolean,
    val queueLength: Int,
    val deferredLength: Int,
    val lastError: String,
    val hostname: String
) {
    companion object {
        fun from(json: JSONObject) = MailStatus(
            postfixRunning = json.optBoolean("postfix_running", false),
            queueLength = json.optInt("queue_length", 0),
            deferredLength = json.optInt("deferred_length", 0),
            lastError = json.optStringOrEmpty("last_error"),
            hostname = json.optStringOrEmpty("hostname")
        )
    }
}

/** org.json liefert fuer JSON-null den String "null". Das will hier niemand sehen. */
internal fun JSONObject.optStringOrEmpty(key: String): String =
    if (isNull(key)) "" else optString(key, "")

private fun JSONObject.optOptionList(key: String): List<FilterOption> {
    val array = optJSONArray(key) ?: return emptyList()
    val out = ArrayList<FilterOption>(array.length())
    for (i in 0 until array.length()) {
        val entry = array.optJSONObject(i) ?: continue
        val k = entry.optStringOrEmpty("key")
        if (k.isNotEmpty()) out.add(FilterOption(k, entry.optStringOrEmpty("label").ifEmpty { k }))
    }
    return out
}

private fun JSONObject.optStringList(key: String): List<String> {
    val array = optJSONArray(key) ?: return emptyList()
    val out = ArrayList<String>(array.length())
    for (i in 0 until array.length()) {
        if (array.isNull(i)) continue
        val value = array.optString(i, "")
        if (value.isNotEmpty()) out.add(value)
    }
    return out
}

/**
 * Zeitstempel-Aufbereitung.
 *
 * Der Server liefert ISO-8601 in UTC, teils ohne Zonenangabe
 * (`2026-05-29T22:30:00.123456`). Ohne Zone laesst sich nichts umrechnen -
 * deshalb wird der Wert als UTC gelesen und in der Geraetezone ausgegeben.
 */
object Timestamps {

    private val isoUtc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun parse(raw: String): Date? {
        if (raw.length < 19) return null
        return try {
            synchronized(isoUtc) { isoUtc.parse(raw.substring(0, 19)) }
        } catch (_: Exception) {
            null
        }
    }

    /** `14:03:22` - fuer die Listenzeile, wo die Sekunde zaehlt und das Datum nicht. */
    fun time(raw: String): String = format(raw, "HH:mm:ss")

    /** `29.05.2026, 14:03:22` - fuer die Detailansicht. */
    fun full(raw: String): String = format(raw, "dd.MM.yyyy, HH:mm:ss")

    /** `29.05.` - Trennzeile, sobald in der Liste ein neuer Tag beginnt. */
    fun day(raw: String): String = format(raw, "EEEE, dd.MM.yyyy")

    /** Sortierschluessel je Kalendertag, um Trennzeilen zu setzen. */
    fun dayKey(raw: String): String = if (raw.length >= 10) raw.substring(0, 10) else raw

    private fun format(raw: String, pattern: String): String {
        val date = parse(raw) ?: return raw
        return SimpleDateFormat(pattern, Locale.GERMANY).format(date)
    }

    /** `3 T 04:12 h` - Laufzeit in einer Zeile, ohne Nachkommastellen. */
    fun uptime(seconds: Double): String {
        val total = seconds.toLong().coerceAtLeast(0L)
        val days = total / 86_400
        val hours = (total % 86_400) / 3_600
        val minutes = (total % 3_600) / 60
        return if (days > 0) {
            String.format(Locale.GERMANY, "%d T %02d:%02d h", days, hours, minutes)
        } else {
            String.format(Locale.GERMANY, "%02d:%02d h", hours, minutes)
        }
    }
}

/** Grosse Zahlen lesbar machen: 8123456 wird zu "8.123.456". */
fun Long.grouped(): String = String.format(Locale.GERMANY, "%,d", this)
