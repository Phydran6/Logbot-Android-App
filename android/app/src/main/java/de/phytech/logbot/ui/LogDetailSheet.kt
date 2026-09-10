/**
 * Datei:        LogDetailSheet.kt
 * Projekt:      Logbot
 * Paket:        de.phytech.logbot.ui
 *
 * Beschreibung:
 * Detailansicht eines Logeintrags als Blatt von unten. Zeigt zuerst, was die
 * Liste schon hat, und laedt den Rohtext nach. So ist das Blatt sofort da und
 * fuellt sich, statt auf eine Antwort zu warten.
 */
package de.phytech.logbot.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import de.phytech.logbot.R
import de.phytech.logbot.data.ApiResult
import de.phytech.logbot.data.Credentials
import de.phytech.logbot.data.LogEntry
import de.phytech.logbot.data.LogbotApi
import de.phytech.logbot.data.Timestamps

class LogDetailSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.sheet_log_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val args = requireArguments()
        val id = args.getLong(ARG_ID)
        val message = args.getString(ARG_MESSAGE).orEmpty()
        val level = args.getString(ARG_LEVEL).orEmpty()

        view.findViewById<TextView>(R.id.detailMessage).text =
            message.ifBlank { getString(R.string.logs_no_message) }
        view.findViewById<TextView>(R.id.detailLevel).text = level.ifBlank { "-" }.uppercase()
        view.findViewById<TextView>(R.id.detailTime).text =
            Timestamps.full(args.getString(ARG_TIMESTAMP).orEmpty())
        view.findViewById<TextView>(R.id.detailHost).text = listOf(
            args.getString(ARG_HOST).orEmpty(),
            args.getString(ARG_IP).orEmpty()
        ).filter { it.isNotBlank() }.joinToString("  ·  ").ifEmpty { "-" }
        view.findViewById<TextView>(R.id.detailSource).text =
            args.getString(ARG_SOURCE).orEmpty().ifBlank { "-" }

        val severity = de.phytech.logbot.data.Severity.of(level)
        view.findViewById<TextView>(R.id.detailLevel).setTextColor(
            ContextCompat.getColor(requireContext(), LogAdapter.colorFor(severity))
        )

        view.findViewById<MaterialButton>(R.id.detailCopy).setOnClickListener {
            copy(message)
        }

        loadRaw(view, id)
    }

    /** Rohtext nachladen. Fehlt er oder scheitert der Aufruf, bleibt der Block weg. */
    private fun loadRaw(view: View, id: Long) {
        val context = requireContext()
        val api = LogbotApi.from(Credentials.instanceUrl(context), Credentials.authToken(context))
        if (api == null || id <= 0L) return

        api.logDetail(id) { result ->
            if (!isAdded) return@logDetail
            val raw = (result as? ApiResult.Ok)?.value?.rawMessage.orEmpty()
            if (raw.isBlank()) return@logDetail
            view.findViewById<View>(R.id.detailRawBlock).visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.detailRaw).text = raw
        }
    }

    private fun copy(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Logbot", text))
        Toast.makeText(requireContext(), R.string.logs_copied, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "log-detail"
        private const val ARG_ID = "id"
        private const val ARG_MESSAGE = "message"
        private const val ARG_LEVEL = "level"
        private const val ARG_TIMESTAMP = "timestamp"
        private const val ARG_HOST = "host"
        private const val ARG_IP = "ip"
        private const val ARG_SOURCE = "source"

        fun show(manager: FragmentManager, entry: LogEntry) {
            if (manager.isStateSaved || manager.findFragmentByTag(TAG) != null) return
            LogDetailSheet().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ID, entry.id)
                    putString(ARG_MESSAGE, entry.message)
                    putString(ARG_LEVEL, entry.level)
                    putString(ARG_TIMESTAMP, entry.timestamp)
                    putString(ARG_HOST, entry.hostname)
                    putString(ARG_IP, entry.ipAddress)
                    putString(ARG_SOURCE, entry.source)
                }
            }.show(manager, TAG)
        }
    }
}
