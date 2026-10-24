/**
 * Zeit- und Zahlenformate. Der Server liefert Zeitstempel als ISO-8601 in UTC,
 * oft ohne Zonenangabe ("2026-05-29T22:30:00.123456"); angezeigt wird in der
 * Zeitzone des Geräts. SimpleDateFormat statt java.time, weil minSdk 24 ohne
 * Desugaring kein java.time kennt.
 */
package de.phytech.logbot.core.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Formats {
    private val utc = TimeZone.getTimeZone("UTC")

    /** Millisekunden seit 1970 – oder null, wenn der Wert nicht zu lesen ist. */
    fun parseServerTime(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        // Bruchteile auf Millisekunden kürzen, Zone abtrennen.
        val match = ISO.matchEntire(value.trim()) ?: return null
        val (base, fraction, zone) = match.destructured
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = utc }
        val millis = runCatching { parser.parse(base.replace(' ', 'T'))?.time }.getOrNull() ?: return null
        val ms = fraction.take(3).padEnd(3, '0').toIntOrNull() ?: 0
        return millis + ms - zoneOffsetMillis(zone)
    }

    private fun zoneOffsetMillis(zone: String): Long {
        if (zone.isEmpty() || zone == "Z") return 0
        val sign = if (zone.startsWith("-")) -1 else 1
        val digits = zone.drop(1).replace(":", "")
        val hours = digits.take(2).toLongOrNull() ?: 0
        val minutes = digits.drop(2).take(2).toLongOrNull() ?: 0
        return sign * (hours * 3_600_000 + minutes * 60_000)
    }

    fun dateTime(value: String?): String =
        parseServerTime(value)?.let { SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY).format(Date(it)) }
            ?: value.orEmpty()

    /** Kurz für Listen: heute nur die Uhrzeit, sonst Datum und Uhrzeit. */
    fun short(value: String?): String {
        val millis = parseServerTime(value) ?: return value.orEmpty()
        val today = SimpleDateFormat("yyyyMMdd", Locale.GERMANY)
        val pattern = if (today.format(Date(millis)) == today.format(Date())) "HH:mm:ss" else "dd.MM. HH:mm"
        return SimpleDateFormat(pattern, Locale.GERMANY).format(Date(millis))
    }

    /** "gerade eben", "vor 5 min", "vor 3 h", "vor 2 Tagen". */
    fun relative(value: String?, now: Long = System.currentTimeMillis()): String {
        val millis = parseServerTime(value) ?: return "unbekannt"
        val seconds = ((now - millis) / 1000).coerceAtLeast(0)
        return when {
            seconds < 60 -> "gerade eben"
            seconds < 3600 -> "vor ${seconds / 60} min"
            seconds < 86_400 -> "vor ${seconds / 3600} h"
            seconds < 2 * 86_400 -> "gestern"
            else -> "vor ${seconds / 86_400} Tagen"
        }
    }

    fun number(value: Number): String = NumberFormat.getIntegerInstance(Locale.GERMANY).format(value)

    fun duration(seconds: Double): String {
        val total = seconds.toLong()
        val days = total / 86_400
        val hours = (total % 86_400) / 3_600
        val minutes = (total % 3_600) / 60
        return buildString {
            if (days > 0) append("${days} d ")
            if (days > 0 || hours > 0) append("${hours} h ")
            append("${minutes} min")
        }
    }

    private val ISO = Regex("""^(\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}:\d{2})(?:\.(\d+))?(Z|[+-]\d{2}:?\d{2})?$""")
}
