/**
 * Datei:        MailFragment.kt
 * Projekt:      Logbot
 * Paket:        de.phytech.logbot.ui
 *
 * Beschreibung:
 * Zugang zum Mailsystem des Servers (Postfix): Dienstzustand, Warteschlange,
 * Passwort-Reset per Mail und die letzten Mail-Logzeilen.
 *
 * Zwei Quellen, bewusst getrennt:
 *
 *   1. Die Logzeilen kommen aus GET /api/logs?category=mail. Diese Kategorie
 *      buendelt serverseitig postfix, dovecot, sendmail, exim und opendkim -
 *      das laeuft mit jedem Serverstand.
 *   2. Dienstzustand, Warteschlange und Reset-Mail liegen hinter /api/mail/*.
 *      Diese Endpunkte sind neu; aeltere Server antworten mit 404. Dann zeigt
 *      der Bereich einen Hinweis statt einer Fehlermeldung und der Rest der
 *      Ansicht bleibt benutzbar.
 *
 * Der Kontrakt der /api/mail-Endpunkte steht in docs/SERVER-API.md.
 */
package de.phytech.logbot.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import de.phytech.logbot.R
import de.phytech.logbot.data.ApiResult
import de.phytech.logbot.data.Credentials
import de.phytech.logbot.data.LogQuery
import de.phytech.logbot.data.LogbotApi
import de.phytech.logbot.data.MailStatus

class MailFragment : Fragment(R.layout.fragment_mail) {

    private var api: LogbotApi? = null
    private lateinit var adapter: LogAdapter
    private lateinit var refresh: SwipeRefreshLayout
    private lateinit var stateText: TextView
    private lateinit var queueText: TextView
    private lateinit var notice: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireContext()
        api = LogbotApi.from(Credentials.instanceUrl(context), Credentials.authToken(context))

        refresh = view.findViewById(R.id.refreshMail)
        stateText = view.findViewById(R.id.mailState)
        queueText = view.findViewById(R.id.mailQueue)
        notice = view.findViewById(R.id.mailNotice)

        adapter = LogAdapter { entry -> LogDetailSheet.show(parentFragmentManager, entry) }
        val list = view.findViewById<RecyclerView>(R.id.mailLogList)
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter
        // Die Liste steckt in einer scrollenden Seite: feste Hoehe, kein
        // eigenes Scrollen, sonst kaempfen zwei Scroller gegeneinander.
        list.isNestedScrollingEnabled = false

        view.findViewById<MaterialButton>(R.id.mailResetPassword).setOnClickListener {
            askForLogin()
        }
        view.findViewById<MaterialButton>(R.id.mailOpenWeb).setOnClickListener {
            (activity as? WebHost)?.openWeb("/settings")
        }

        refresh.setOnRefreshListener { load() }
        load()
    }

    private fun load() {
        val client = api ?: return
        refresh.isRefreshing = true

        client.mailStatus { result ->
            if (!isAdded) return@mailStatus
            refresh.isRefreshing = false
            when (result) {
                is ApiResult.Ok -> renderStatus(result.value)
                is ApiResult.Err -> {
                    notice.setText(
                        if (result.error.notImplemented) R.string.mail_not_supported
                        else R.string.mail_status_failed
                    )
                    notice.visibility = View.VISIBLE
                    view?.findViewById<View>(R.id.mailStatusBlock)?.visibility = View.GONE
                }
            }
        }

        client.logs(LogQuery(page = 1, pageSize = 40, category = "mail")) { result ->
            if (!isAdded) return@logs
            val entries = (result as? ApiResult.Ok)?.value?.items.orEmpty()
            adapter.submitList(LogAdapter.rowsOf(entries).first)
            view?.findViewById<TextView>(R.id.mailLogEmpty)?.visibility =
                if (entries.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun renderStatus(status: MailStatus) {
        notice.visibility = View.GONE
        view?.findViewById<View>(R.id.mailStatusBlock)?.visibility = View.VISIBLE

        stateText.setText(
            if (status.postfixRunning) R.string.mail_postfix_running
            else R.string.mail_postfix_stopped
        )
        queueText.text = getString(
            R.string.mail_queue_value,
            status.queueLength,
            status.deferredLength
        )
        if (status.lastError.isNotBlank()) {
            notice.text = status.lastError
            notice.visibility = View.VISIBLE
        }
    }

    /**
     * Passwort-Reset: Benutzername oder Mailadresse abfragen und den Server
     * eine Mail schicken lassen. Der Server sagt aus Prinzip nicht, ob es das
     * Konto gibt - sonst waere der Dialog ein Verzeichnis aller Konten.
     */
    private fun askForLogin() {
        val context = requireContext()
        val view = layoutInflater.inflate(R.layout.dialog_password_reset, null)
        val field = view.findViewById<TextInputEditText>(R.id.resetLogin)

        AlertDialog.Builder(context)
            .setTitle(R.string.mail_reset_title)
            .setView(view)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.mail_reset_send) { _, _ ->
                val login = field.text?.toString()?.trim().orEmpty()
                if (login.isEmpty()) return@setPositiveButton
                sendReset(login)
            }
            .show()
    }

    private fun sendReset(login: String) {
        api?.requestPasswordReset(login) { result ->
            if (!isAdded) return@requestPasswordReset
            val message = when (result) {
                is ApiResult.Ok -> result.value
                is ApiResult.Err ->
                    if (result.error.notImplemented) getString(R.string.mail_reset_not_supported)
                    else result.error.message
            }
            AlertDialog.Builder(requireContext())
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }
}

/** Wird von der MainActivity erfuellt: Wechsel in die Weboberflaeche mit Pfad. */
interface WebHost {
    fun openWeb(path: String)
}
