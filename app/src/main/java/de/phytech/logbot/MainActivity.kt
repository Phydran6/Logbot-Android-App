/**
 * Datei:        MainActivity.kt
 * Projekt:      Logbot
 * Paket:        de.phytech.logbot
 * Autor:        Phydran6
 * Version:      2.0 – 11.04.2026
 *
 * Beschreibung:
 * Haupt-Activity der App. Prüft beim Start ob Instanz-URL und Auth-Token
 * gespeichert sind. Falls nicht → weiterleiten zu SetupActivity.
 * Bei vorhandenen Zugangsdaten: Vollbild-WebView mit Sicherheits-Hardening laden.
 * Token wird als Authorization-Header und per localStorage übergeben.
 */
package de.phytech.logbot

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Zugangsdaten aus sicherem Speicher lesen
        val prefs = getEncryptedPrefs(this)
        val instanceUrl = prefs.getString(PREF_INSTANCE_URL, null)
        val authToken = prefs.getString(PREF_AUTH_TOKEN, null)

        // Nicht konfiguriert → Setup zeigen
        if (instanceUrl.isNullOrBlank() || authToken.isNullOrBlank()) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webview)

        setupWebView(instanceUrl, authToken)

        // Initiales Laden mit Authorization-Header
        webView.loadUrl(instanceUrl, mapOf("Authorization" to "Bearer $authToken"))

        // Zurück-Navigation: WebView-Verlauf nutzen, sonst App beenden
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack()
                else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupWebView(instanceUrl: String, authToken: String) {
        val instanceHost = Uri.parse(instanceUrl).host ?: ""
        val authHeaders = mapOf("Authorization" to "Bearer $authToken")

        // --- Security Hardening ---
        with(webView.settings) {
            javaScriptEnabled = true          // Pflicht für Logbot-Oberfläche
            domStorageEnabled = true          // localStorage / sessionStorage
            allowFileAccess = false           // Kein Zugriff auf lokale Dateien
            allowContentAccess = false        // Kein Zugriff auf Content-Provider
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = false
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = false
            // Kein gemischtes HTTP/HTTPS erlauben
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            // User-Agent nicht verändern (kein Fingerprinting-Hinweis)
            setSupportZoom(false)
        }

        webView.webViewClient = object : WebViewClient() {

            private var sessionExpired = false

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (errorResponse.statusCode == 401 && request.isForMainFrame && !sessionExpired) {
                    sessionExpired = true
                    getEncryptedPrefs(this@MainActivity).edit()
                        .remove(PREF_INSTANCE_URL)
                        .remove(PREF_AUTH_TOKEN)
                        .apply()
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage(getString(R.string.error_session_expired))
                        .setPositiveButton("OK") { _, _ ->
                            startActivity(Intent(this@MainActivity, SetupActivity::class.java))
                            finish()
                        }
                        .setCancelable(false)
                        .show()
                }
            }

            /**
             * Navigations-Kontrolle: Nur innerhalb der konfigurierten Instanz erlaubt.
             * Externe Links werden im System-Browser geöffnet.
             * Bei internen Navigationen wird der Auth-Header erneut mitgeschickt.
             */
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val requestHost = request.url.host ?: return true // unbekannt → blockieren
                return if (requestHost == instanceHost || requestHost.endsWith(".$instanceHost")) {
                    view.loadUrl(request.url.toString(), authHeaders)
                    true
                } else {
                    // Externe URL sicher im Browser öffnen
                    runCatching {
                        startActivity(Intent(Intent.ACTION_VIEW, request.url))
                    }
                    true
                }
            }

            /**
             * Nach dem Laden: Token per localStorage injizieren für JS-basierte Auth.
             * JSONObject.quote() stellt sicher, dass kein JS injiziert werden kann.
             */
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                val safeToken = JSONObject.quote(authToken) // inkl. Anführungszeichen
                view.evaluateJavascript(
                    "(function(){try{localStorage.setItem('authToken',$safeToken);}catch(e){}})();",
                    null
                )
            }
        }

        // JS-Dialoge nativ darstellen (alert / confirm)
        webView.webChromeClient = object : WebChromeClient() {

            override fun onJsAlert(
                view: WebView, url: String, message: String, result: JsResult
            ): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setMessage(message)
                    .setPositiveButton("OK") { _, _ -> result.confirm() }
                    .setOnCancelListener { result.cancel() }
                    .show()
                return true
            }

            override fun onJsConfirm(
                view: WebView, url: String, message: String, result: JsResult
            ): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setMessage(message)
                    .setPositiveButton("OK") { _, _ -> result.confirm() }
                    .setNegativeButton("Abbrechen") { _, _ -> result.cancel() }
                    .setOnCancelListener { result.cancel() }
                    .show()
                return true
            }
        }
    }

    companion object {
        const val PREF_INSTANCE_URL = "instance_url"
        const val PREF_AUTH_TOKEN = "auth_token"
        private const val PREFS_FILE = "logbot_secure_prefs"

        /**
         * Liefert EncryptedSharedPreferences mit AES-256-GCM-verschlüsselten Werten.
         * Schlüssel werden im Android Keystore gespeichert und verlassen das Gerät nie.
         */
        fun getEncryptedPrefs(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }
}
