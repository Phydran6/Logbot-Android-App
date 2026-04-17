/**
 * Datei:        SetupActivity.kt
 * Projekt:      Logbot
 * Paket:        de.phytech.logbot
 * Autor:        Phydran6
 * Version:      2.0 – 11.04.2026
 *
 * Beschreibung:
 * Setup-Screen der App. Wird angezeigt wenn noch keine Instanz-URL und kein
 * Auth-Token gespeichert sind. Der Nutzer kann entweder:
 *   a) URL + Token manuell eingeben und auf "Verbinden" tippen
 *   b) Einen QR-Code scannen der {"url":"...","token":"..."} als JSON enthält
 *
 * Nach erfolgreicher Konfiguration werden die Zugangsdaten verschlüsselt gespeichert
 * und die MainActivity gestartet.
 */
package de.phytech.logbot

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import org.json.JSONException
import org.json.JSONObject

class SetupActivity : AppCompatActivity() {

    private lateinit var tilUrl: TextInputLayout
    private lateinit var etUrl: TextInputEditText
    private lateinit var tilToken: TextInputLayout
    private lateinit var etToken: TextInputEditText
    private lateinit var btnConnect: MaterialButton
    private lateinit var btnScanQr: MaterialButton

    // ZXing QR-Scanner Launcher (Activity Result API)
    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { handleQrResult(it) }
    }

    // Kamera-Berechtigung anfragen
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startQrScan()
        else Toast.makeText(
            this, getString(R.string.error_camera_permission), Toast.LENGTH_SHORT
        ).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        tilUrl     = findViewById(R.id.tilInstanceUrl)
        etUrl      = findViewById(R.id.etInstanceUrl)
        tilToken   = findViewById(R.id.tilAuthToken)
        etToken    = findViewById(R.id.etAuthToken)
        btnConnect = findViewById(R.id.btnConnect)
        btnScanQr  = findViewById(R.id.btnScanQr)

        btnConnect.setOnClickListener { handleConnect() }
        btnScanQr.setOnClickListener  { requestCameraAndScan() }
    }

    // --- Manueller Login ---

    private fun handleConnect() {
        val url   = etUrl.text?.toString()?.trim()   ?: ""
        val token = etToken.text?.toString()?.trim() ?: ""

        tilUrl.error   = null
        tilToken.error = null
        var valid = true

        if (url.isBlank()) {
            tilUrl.error = getString(R.string.error_url_required)
            valid = false
        } else if (!url.startsWith("https://")) {
            tilUrl.error = getString(R.string.error_url_invalid)
            valid = false
        }

        if (token.isBlank()) {
            tilToken.error = getString(R.string.error_token_required)
            valid = false
        }

        if (valid) saveAndLaunch(url, token)
    }

    // --- QR-Code Flow ---

    /**
     * Prüft Kamera-Berechtigung. Fragt an falls nötig, startet sonst direkt den Scanner.
     */
    private fun requestCameraAndScan() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> startQrScan()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startQrScan() {
        val options = ScanOptions()
            .setPrompt(getString(R.string.qr_scan_prompt))
            .setBeepEnabled(false)
            .setOrientationLocked(false)
            .setBarcodeImageEnabled(false)
        barcodeLauncher.launch(options)
    }

    /**
     * Wertet den QR-Code aus.
     * Erwartetes Format: {"url":"https://meine-instanz.de","token":"abc123"}
     */
    private fun handleQrResult(content: String) {
        try {
            val json  = JSONObject(content)
            val url   = json.getString("url").trim()
            val token = json.getString("token").trim()

            if (url.startsWith("https://") && token.isNotBlank()) {
                saveAndLaunch(url, token)
            } else {
                showQrError()
            }
        } catch (e: JSONException) {
            showQrError()
        }
    }

    private fun showQrError() {
        Toast.makeText(this, getString(R.string.error_qr_invalid), Toast.LENGTH_LONG).show()
    }

    // --- Speichern & Starten ---

    /**
     * Speichert URL und Token verschlüsselt und startet die MainActivity.
     */
    private fun saveAndLaunch(url: String, token: String) {
        MainActivity.getEncryptedPrefs(this).edit()
            .putString(MainActivity.PREF_INSTANCE_URL, url)
            .putString(MainActivity.PREF_AUTH_TOKEN, token)
            .apply()

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
