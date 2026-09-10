/**
 * Datei:        LogsFragment.kt
 * Projekt:      Logbot
 * Paket:        de.phytech.logbot.ui
 *
 * Beschreibung:
 * Native Logansicht: Suche, Schweregrad, Kategorie, Endlos-Nachladen.
 *
 * Warum nicht die Weboberflaeche im WebView? Weil die Tabelle dort fuer einen
 * Bildschirm gebaut ist, der doppelt so breit ist wie ein Telefon. Hier wird
 * gefiltert, was der Server ohnehin filtern kann (siehe LogQuery), und
 * angezeigt, was auf ein Telefon passt.
 */
package de.phytech.logbot.ui

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import de.phytech.logbot.R
import de.phytech.logbot.data.ApiResult
import de.phytech.logbot.data.Credentials
import de.phytech.logbot.data.FilterOption
import de.phytech.logbot.data.FilterOptions
import de.phytech.logbot.data.LogEntry
import de.phytech.logbot.data.LogQuery
import de.phytech.logbot.data.LogbotApi
import de.phytech.logbot.data.grouped

class LogsFragment : Fragment(R.layout.fragment_logs) {

    private var api: LogbotApi? = null
    private lateinit var adapter: LogAdapter
    private lateinit var list: RecyclerView
    private lateinit var refresh: SwipeRefreshLayout
    private lateinit var summary: TextView
    private lateinit var empty: TextView
    private lateinit var severityChips: ChipGroup
    private lateinit var categoryChips: ChipGroup

    private var query = LogQuery()
    private var rows = emptyList<LogRow>()
    private var lastDay = ""
    private var loading = false
    private var reachedEnd = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireContext()
        api = LogbotApi.from(Credentials.instanceUrl(context), Credentials.authToken(context))

        adapter = LogAdapter { entry -> LogDetailSheet.show(parentFragmentManager, entry) }
        list = view.findViewById(R.id.logList)
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter
        list.setHasFixedSize(true)

        refresh = view.findViewById(R.id.refreshLogs)
        summary = view.findViewById(R.id.logSummary)
        empty = view.findViewById(R.id.logEmpty)
        severityChips = view.findViewById(R.id.severityChips)
        categoryChips = view.findViewById(R.id.categoryChips)

        refresh.setOnRefreshListener { reload() }

        view.findViewById<TextInputEditText>(R.id.logSearch).setOnEditorActionListener { field, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEARCH || id == EditorInfo.IME_ACTION_DONE) {
                query = query.copy(search = field.text?.toString().orEmpty(), page = 1)
                reload()
                true
            } else {
                false
            }
        }

        // Nachladen, sobald die letzten fuenf Zeilen sichtbar werden. Ein
        // "Mehr laden"-Knopf waere ein zusaetzlicher Tipp pro Seite.
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(view: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0 || loading || reachedEnd) return
                val manager = view.layoutManager as LinearLayoutManager
                if (manager.findLastVisibleItemPosition() >= adapter.itemCount - 5) loadNextPage()
            }
        })

        loadFilterOptions()
        reload()
    }

    // --- Filter -------------------------------------------------------------

    private fun loadFilterOptions() {
        api?.filterOptions { result ->
            if (!isAdded) return@filterOptions
            val options = (result as? ApiResult.Ok)?.value ?: FilterOptions.EMPTY
            fillChips(severityChips, options.severities, R.string.logs_filter_any_severity) { key ->
                query = query.copy(minSeverity = key, page = 1); reload()
            }
            fillChips(categoryChips, options.categories, R.string.logs_filter_any_category) { key ->
                query = query.copy(category = key, page = 1); reload()
            }
        }
    }

    /**
     * Baut eine Chip-Reihe mit "Alle" an erster Stelle. Der Server liefert die
     * Listen, die App kennt sie also nicht fest - ein neuer Logtyp auf dem
     * Server erscheint hier ohne App-Update.
     */
    private fun fillChips(
        group: ChipGroup,
        options: List<FilterOption>,
        anyLabelRes: Int,
        onPick: (String) -> Unit
    ) {
        group.removeAllViews()
        val entries = listOf(FilterOption("", getString(anyLabelRes))) + options
        entries.forEachIndexed { index, option ->
            val chip = layoutInflater.inflate(R.layout.item_filter_chip, group, false) as Chip
            chip.text = option.label
            chip.isChecked = index == 0
            chip.setOnClickListener {
                if (!chip.isChecked) {
                    chip.isChecked = true   // Abwaehlen soll nichts tun - eine Wahl bleibt immer
                    return@setOnClickListener
                }
                onPick(option.key)
            }
            group.addView(chip)
        }
    }

    // --- Laden --------------------------------------------------------------

    private fun reload() {
        query = query.copy(page = 1)
        rows = emptyList()
        lastDay = ""
        reachedEnd = false
        adapter.submitList(emptyList())
        fetch()
    }

    private fun loadNextPage() {
        query = query.copy(page = query.page + 1)
        fetch()
    }

    private fun fetch() {
        val client = api ?: return
        loading = true
        refresh.isRefreshing = true
        val requested = query
        client.logs(requested) { result ->
            if (!isAdded || view == null) return@logs
            // Antwort einer inzwischen ueberholten Anfrage verwerfen: Wer
            // schnell filtert, soll nicht das Ergebnis von vorhin sehen.
            if (requested != query) return@logs

            loading = false
            refresh.isRefreshing = false
            when (result) {
                is ApiResult.Ok -> append(result.value.items, result.value.total, result.value.hasMore)
                is ApiResult.Err -> {
                    empty.text = result.error.message
                    empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun append(entries: List<LogEntry>, total: Long, hasMore: Boolean) {
        reachedEnd = !hasMore || entries.isEmpty()

        val (newRows, day) = LogAdapter.rowsOf(entries, lastDay)
        lastDay = day
        rows = rows + newRows
        adapter.submitList(rows)

        summary.text = if (query.filtered) {
            getString(R.string.logs_summary_filtered, total.grouped())
        } else {
            getString(R.string.logs_summary_all, total.grouped())
        }

        val nothing = rows.isEmpty()
        empty.setText(R.string.logs_empty)
        empty.visibility = if (nothing) View.VISIBLE else View.GONE
        list.visibility = if (nothing) View.GONE else View.VISIBLE
    }
}
