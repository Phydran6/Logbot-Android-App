/**
 * Datei:        WebFragment.kt
 * Projekt:      Logbot
 * Paket:        de.phytech.logbot.ui
 *
 * Beschreibung:
 * Die vollstaendige Weboberflaeche des Servers, gehaertet eingebettet.
 *
 * Die nativen Bereiche decken den Alltag ab (Status, Logs, Mail). Alles
 * andere - Benutzerverwaltung, Webhooks, Branding, Updates - lebt in der
 * Weboberflaeche und wird hier gezeigt, statt es doppelt zu bauen.
 *
 * Haertung, unveraendert uebernommen aus der frueheren MainActivity:
 *   - kein Datei- und Content-Zugriff
 *   - kein gemischtes HTTP/HTTPS
 *   - Navigation nur innerhalb der eingerichteten Instanz, alles andere geht
 *     in den Systembrowser
 *   - 401 auf dem Hauptrahmen loescht die Zugangsdaten und fuehrt ins Setup
 */
package de.phytech.logbot.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import de.phytech.logbot.LogbotBridge
import de.phytech.logbot.R
import de.phytech.logbot.SetupActivity
import de.phytech.logbot.data.Credentials
import org.json.JSONObject

class WebFragment : Fragment(R.layout.fragment_web) {

    private var webView: WebView? = null
    private var instanceUrl: String = ""
    private var authToken: String = ""
    private var sessionExpired = false
    private var pendingPath: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireContext()
        instanceUrl = Credentials.instanceUrl(context).orEmpty()
        authToken = Credentials.authToken(context).orEmpty()
        if (instanceUrl.isBlank() || authToken.isBlank()) return

        val web = view.findViewById<WebView>(R.id.webview)
        webView = web
        configure(web)
        web.loadUrl(instanceUrl + (pendingPath ?: ""), authHeaders())
        pendingPath = null
    }

    override fun onDestroyView() {
        // Die WebView haengt am Fragment-Lebenszyklus; ohne Aufraeumen laeuft
        // JavaScript weiter und haelt den Kontext fest.
        webView?.apply {
            stopLoading()
            loadUrl("about:blank")
            removeJavascriptInterface("LogbotApp")
        }
        webView = null
        super.onDestroyView()
    }

    /** Wird von der Shell aufgerufen, wenn ein anderer Bereich hierher verweist. */
    fun open(path: String) {
        val web = webView
        if (web == null || instanceUrl.isBlank()) {
            pendingPath = path
            return
        }
        web.loadUrl(instanceUrl + path, authHeaders())
    }

    fun canGoBack(): Boolean = webView?.canGoBack() == true

    fun goBack() {
        webView?.goBack()
    }

    private fun authHeaders() = mapOf("Authorization" to "Bearer $authToken")

    private fun configure(web: WebView) {
        val instanceHost = Uri.parse(instanceUrl).host.orEmpty()

        web.addJavascriptInterface(LogbotBridge(requireContext().applicationContext), "LogbotApp")

        with(web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = false
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setSupportZoom(false)
        }

        web.webViewClient = object : WebViewClient() {

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (errorResponse.statusCode != 401 || !request.isForMainFrame) return
                if (sessionExpired || !isAdded) return
                sessionExpired = true
                Credentials.clear(requireContext())
                AlertDialog.Builder(requireContext())
                    .setMessage(getString(R.string.error_session_expired))
                    .setPositiveButton("OK") { _, _ ->
                        startActivity(Intent(requireContext(), SetupActivity::class.java))
                        activity?.finish()
                    }
                    .setCancelable(false)
                    .show()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val requestHost = request.url.host ?: return true  // unbekannt: blockieren
                return if (requestHost == instanceHost || requestHost.endsWith(".$instanceHost")) {
                    view.loadUrl(request.url.toString(), authHeaders())
                    true
                } else {
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                    true
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                // JSONObject.quote() liefert den Wert inklusive Anfuehrungs-
                // zeichen und maskiert alles, was sonst JavaScript waere.
                val safeToken = JSONObject.quote(authToken)
                view.evaluateJavascript(
                    "(function(){try{localStorage.setItem('authToken',$safeToken);}catch(e){}})();",
                    null
                )
            }
        }

        web.webChromeClient = object : WebChromeClient() {

            override fun onJsAlert(
                view: WebView, url: String, message: String, result: JsResult
            ): Boolean {
                if (!isAdded) return false
                AlertDialog.Builder(requireContext())
                    .setMessage(message)
                    .setPositiveButton("OK") { _, _ -> result.confirm() }
                    .setOnCancelListener { result.cancel() }
                    .show()
                return true
            }

            override fun onJsConfirm(
                view: WebView, url: String, message: String, result: JsResult
            ): Boolean {
                if (!isAdded) return false
                AlertDialog.Builder(requireContext())
                    .setMessage(message)
                    .setPositiveButton("OK") { _, _ -> result.confirm() }
                    .setNegativeButton(R.string.action_cancel) { _, _ -> result.cancel() }
                    .setOnCancelListener { result.cancel() }
                    .show()
                return true
            }
        }
    }
}
