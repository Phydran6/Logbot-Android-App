/**
 * Datei:        StatusFragment.kt
 * Projekt:      Logbot
 * Paket:        de.phytech.logbot.ui
 *
 * Beschreibung:
 * Zustand des Servers auf einen Blick: erreichbar, Auslastung, Datenbank,
 * Agenten, Logaufkommen. Quelle ist GET /api/health/detailed.
 *
 * Aktualisiert sich alle 20 Sekunden, aber nur solange die Ansicht sichtbar
 * ist - im Hintergrund fragt nichts nach. Zusaetzlich Ziehen zum Neuladen.
 */
package de.phytech.logbot.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.progressindicator.LinearProgressIndicator
import de.phytech.logbot.R
import de.phytech.logbot.data.ApiResult
import de.phytech.logbot.data.Credentials
import de.phytech.logbot.data.LogbotApi
import de.phytech.logbot.data.ServerHealth
import de.phytech.logbot.data.Timestamps
import de.phytech.logbot.data.grouped
import kotlin.math.roundToInt

class StatusFragment : Fragment(R.layout.fragment_status) {

    private var api: LogbotApi? = null
    private val ticker = Handler(Looper.getMainLooper())

    private lateinit var refresh: SwipeRefreshLayout
    private lateinit var stateDot: View
    private lateinit var stateTitle: TextView
    private lateinit var stateDetail: TextView
    private lateinit var host: TextView

    private val tick = object : Runnable {
        override fun run() {
            load(showSpinner = false)
            ticker.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        refresh = view.findViewById(R.id.refreshStatus)
        stateDot = view.findViewById(R.id.stateDot)
        stateTitle = view.findViewById(R.id.stateTitle)
        stateDetail = view.findViewById(R.id.stateDetail)
        host = view.findViewById(R.id.stateHost)

        refresh.setOnRefreshListener { load(showSpinner = true) }

        val context = requireContext()
        api = LogbotApi.from(Credentials.instanceUrl(context), Credentials.authToken(context))
        host.text = Credentials.instanceUrl(context).orEmpty()
    }

    override fun onResume() {
        super.onResume()
        ticker.post(tick)
    }

    override fun onPause() {
        super.onPause()
        ticker.removeCallbacks(tick)
    }

    private fun load(showSpinner: Boolean) {
        val client = api ?: return
        if (showSpinner) refresh.isRefreshing = true
        client.health { result ->
            if (!isAdded || view == null) return@health
            refresh.isRefreshing = false
            when (result) {
                is ApiResult.Ok -> render(result.value)
                is ApiResult.Err -> renderError(result.error.message, result.error.unauthorized)
            }
        }
    }

    private fun render(health: ServerHealth) {
        val view = view ?: return
        val ok = health.healthy && health.databaseConnected

        stateDot.setBackgroundResource(if (ok) R.drawable.dot_ok else R.drawable.dot_warn)
        stateTitle.setText(if (ok) R.string.status_reachable else R.string.status_degraded)
        stateDetail.text = getString(
            R.string.status_detail,
            health.version,
            Timestamps.uptime(health.uptimeSeconds)
        )

        gauge(view, R.id.cpuBar, R.id.cpuValue, health.cpuPercent)
        gauge(view, R.id.memBar, R.id.memValue, health.memoryPercent)
        gauge(view, R.id.diskBar, R.id.diskValue, health.diskPercent)

        view.findViewById<TextView>(R.id.dbValue).setText(
            if (health.databaseConnected) R.string.status_db_ok else R.string.status_db_down
        )
        view.findViewById<TextView>(R.id.agentsValue).text =
            getString(R.string.status_agents_value, health.agentsOnline, health.agentsTotal)
        view.findViewById<TextView>(R.id.logs24Value).text = health.logsLast24h.grouped()
        view.findViewById<TextView>(R.id.logsTotalValue).text = health.logsTotal.grouped()

        view.findViewById<View>(R.id.statusContent).visibility = View.VISIBLE
        view.findViewById<TextView>(R.id.statusError).visibility = View.GONE
    }

    /**
     * Balken und Prozentzahl. Ab 75 % gelb, ab 90 % rot - die Schwellen sind
     * bewusst grosszuegig: Ein Logserver darf Platte und RAM auslasten, ohne
     * dass die Ansicht sofort Alarm schlaegt.
     */
    private fun gauge(root: View, barId: Int, valueId: Int, percent: Double) {
        val value = percent.coerceIn(0.0, 100.0)
        val bar = root.findViewById<LinearProgressIndicator>(barId)
        bar.setProgressCompat(value.roundToInt(), true)

        val color = when {
            value >= 90 -> R.color.severity_error
            value >= 75 -> R.color.severity_warn
            else -> R.color.severity_ok
        }
        bar.setIndicatorColor(ContextCompat.getColor(requireContext(), color))
        root.findViewById<TextView>(valueId).text =
            getString(R.string.status_percent, value.roundToInt())
    }

    private fun renderError(message: String, unauthorized: Boolean) {
        val view = view ?: return
        stateDot.setBackgroundResource(R.drawable.dot_error)
        stateTitle.setText(R.string.status_unreachable)
        stateDetail.text = message

        val error = view.findViewById<TextView>(R.id.statusError)
        error.text = getString(
            if (unauthorized) R.string.status_error_auth else R.string.status_error_generic
        )
        error.visibility = View.VISIBLE
        view.findViewById<View>(R.id.statusContent).visibility = View.GONE
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 20_000L
    }
}
