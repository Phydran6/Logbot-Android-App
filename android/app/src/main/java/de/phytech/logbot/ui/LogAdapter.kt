/**
 * Datei:        LogAdapter.kt
 * Projekt:      Logbot
 * Paket:        de.phytech.logbot.ui
 *
 * Beschreibung:
 * Listendarstellung der Logeintraege.
 *
 * Die alte Ansicht war eine Tabelle im WebView: viele Spalten, alles gleich
 * gewichtet, auf einem Telefon nicht zu lesen. Hier steht die Nachricht gross
 * und alles Beiwerk klein darunter, der Schweregrad ist ein Farbstreifen am
 * Rand statt einer weiteren Spalte. Tageswechsel bekommen eine Trennzeile,
 * damit man beim Scrollen weiss, wo man ist.
 */
package de.phytech.logbot.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.phytech.logbot.R
import de.phytech.logbot.data.LogEntry
import de.phytech.logbot.data.Severity
import de.phytech.logbot.data.Timestamps

/** Ein Listeneintrag ist entweder eine Datums-Trennzeile oder ein Logeintrag. */
sealed class LogRow {
    data class Day(val label: String, val key: String) : LogRow()
    data class Entry(val log: LogEntry) : LogRow()
}

class LogAdapter(
    private val onClick: (LogEntry) -> Unit
) : ListAdapter<LogRow, RecyclerView.ViewHolder>(DIFF) {

    override fun getItemViewType(position: Int): Int =
        if (getItem(position) is LogRow.Day) TYPE_DAY else TYPE_ENTRY

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_DAY) {
            DayHolder(inflater.inflate(R.layout.item_log_day, parent, false))
        } else {
            EntryHolder(inflater.inflate(R.layout.item_log, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is LogRow.Day -> (holder as DayHolder).bind(row)
            is LogRow.Entry -> (holder as EntryHolder).bind(row.log, onClick)
        }
    }

    private class DayHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val label: TextView = view.findViewById(R.id.dayLabel)
        fun bind(row: LogRow.Day) {
            label.text = row.label
        }
    }

    private class EntryHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val stripe: View = view.findViewById(R.id.severityStripe)
        private val message: TextView = view.findViewById(R.id.logMessage)
        private val meta: TextView = view.findViewById(R.id.logMeta)
        private val level: TextView = view.findViewById(R.id.logLevel)

        fun bind(log: LogEntry, onClick: (LogEntry) -> Unit) {
            message.text = log.message.ifBlank { itemView.context.getString(R.string.logs_no_message) }

            // Eine Zeile Beiwerk: Uhrzeit, Host, Quelle. Fehlende Werte fallen
            // raus, statt als leerer Zwischenraum stehen zu bleiben.
            meta.text = listOf(
                Timestamps.time(log.timestamp),
                log.hostname,
                log.source
            ).filter { it.isNotBlank() }.joinToString("  ·  ")

            level.text = log.level.ifBlank { "-" }.uppercase()

            val color = ContextCompat.getColor(itemView.context, colorFor(log.severity))
            stripe.setBackgroundColor(color)
            level.setTextColor(color)

            itemView.setOnClickListener { onClick(log) }
        }
    }

    companion object {
        private const val TYPE_DAY = 0
        private const val TYPE_ENTRY = 1

        fun colorFor(severity: Severity): Int = when (severity) {
            Severity.ERROR -> R.color.severity_error
            Severity.WARN -> R.color.severity_warn
            Severity.INFO -> R.color.severity_info
            Severity.DEBUG -> R.color.severity_debug
        }

        /**
         * Baut aus einer flachen Liste die Reihenfolge mit Trennzeilen.
         * `lastDay` traegt den Tag der vorigen Seite, damit beim Nachladen
         * keine zweite Trennzeile fuer denselben Tag entsteht.
         */
        fun rowsOf(entries: List<LogEntry>, startDay: String = ""): Pair<List<LogRow>, String> {
            val rows = ArrayList<LogRow>(entries.size + 4)
            var day = startDay
            for (entry in entries) {
                val key = Timestamps.dayKey(entry.timestamp)
                if (key != day) {
                    rows.add(LogRow.Day(Timestamps.day(entry.timestamp), key))
                    day = key
                }
                rows.add(LogRow.Entry(entry))
            }
            return rows to day
        }

        private val DIFF = object : DiffUtil.ItemCallback<LogRow>() {
            override fun areItemsTheSame(old: LogRow, new: LogRow): Boolean = when {
                old is LogRow.Day && new is LogRow.Day -> old.key == new.key
                old is LogRow.Entry && new is LogRow.Entry -> old.log.id == new.log.id
                else -> false
            }

            override fun areContentsTheSame(old: LogRow, new: LogRow): Boolean = old == new
        }
    }
}
