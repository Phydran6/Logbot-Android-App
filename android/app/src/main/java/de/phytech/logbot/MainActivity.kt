/**
 * Datei:        MainActivity.kt
 * Projekt:      Logbot
 * Paket:        de.phytech.logbot
 *
 * Beschreibung:
 * Rahmen der App: App-Lock, Titelleiste, vier Bereiche in einer Leiste unten.
 *
 * Frueher war das hier eine Vollbild-WebView und sonst nichts. Wer den
 * Serverzustand sehen wollte, musste sich durch das Menue der Weboberflaeche
 * tippen - auf einem Telefon mehrere Schritte fuer eine Zahl. Jetzt liegen
 * Status, Logs und Mail je einen Tipp entfernt, die Weboberflaeche bleibt als
 * vierter Bereich fuer alles Uebrige.
 *
 * Bereiche werden angelegt und danach nur noch ein- und ausgeblendet, nicht
 * ausgetauscht. Das haelt Scrollposition, Filter und den Web-Verlauf beim
 * Wechseln - und spart die Uebergangsanimation, die sonst bei jedem Tipp
 * laufen wuerde.
 */
package de.phytech.logbot

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import de.phytech.logbot.data.Credentials
import de.phytech.logbot.ui.LogsFragment
import de.phytech.logbot.ui.MailFragment
import de.phytech.logbot.ui.StatusFragment
import de.phytech.logbot.ui.WebFragment
import de.phytech.logbot.ui.WebHost

class MainActivity : AppCompatActivity(), WebHost {

    private lateinit var bottomNav: BottomNavigationView
    private var currentTag: String = TAG_STATUS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Credentials.isConfigured(this)) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        // App-Lock: bei aktivierter Biometrie zuerst entsperren lassen. Bei
        // Abbruch schliesst die App, ohne dass ein Token entschluesselt wurde.
        if (Credentials.biometricEnabled(this)) {
            promptBiometric(onSuccess = { buildUi(savedInstanceState) }, onFailure = { finish() })
        } else {
            buildUi(savedInstanceState)
        }
    }

    private fun promptBiometric(onSuccess: () -> Unit, onFailure: () -> Unit) {
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_prompt_title))
            .setSubtitle(getString(R.string.biometric_prompt_subtitle))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) =
                    onSuccess()

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) =
                    onFailure()
            }
        ).authenticate(info)
    }

    private fun buildUi(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        bottomNav = findViewById(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            show(tagFor(item.itemId))
            true
        }
        // Zweiter Tipp auf denselben Bereich: nichts tun statt neu aufzubauen.
        bottomNav.setOnItemReselectedListener { }

        currentTag = savedInstanceState?.getString(STATE_TAG) ?: TAG_STATUS
        show(currentTag)
        bottomNav.selectedItemId = itemFor(currentTag)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val web = supportFragmentManager.findFragmentByTag(TAG_WEB) as? WebFragment
                when {
                    currentTag == TAG_WEB && web?.canGoBack() == true -> web.goBack()
                    currentTag != TAG_STATUS -> {
                        show(TAG_STATUS)
                        bottomNav.selectedItemId = R.id.nav_status
                    }
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_TAG, currentTag)
    }

    // --- Bereiche -----------------------------------------------------------

    private fun show(tag: String) {
        val manager = supportFragmentManager
        val transaction = manager.beginTransaction().setReorderingAllowed(true)

        var target = manager.findFragmentByTag(tag)
        if (target == null) {
            target = create(tag)
            transaction.add(R.id.contentFrame, target, tag)
        }
        for (existing in manager.fragments) {
            val existingTag = existing.tag ?: continue
            if (existing !== target && existingTag in ALL_TAGS) transaction.hide(existing)
        }
        transaction.show(target).commit()

        currentTag = tag
        setTitle(titleFor(tag))
    }

    private fun create(tag: String): Fragment = when (tag) {
        TAG_LOGS -> LogsFragment()
        TAG_MAIL -> MailFragment()
        TAG_WEB -> WebFragment()
        else -> StatusFragment()
    }

    private fun tagFor(itemId: Int): String = when (itemId) {
        R.id.nav_logs -> TAG_LOGS
        R.id.nav_mail -> TAG_MAIL
        R.id.nav_web -> TAG_WEB
        else -> TAG_STATUS
    }

    private fun itemFor(tag: String): Int = when (tag) {
        TAG_LOGS -> R.id.nav_logs
        TAG_MAIL -> R.id.nav_mail
        TAG_WEB -> R.id.nav_web
        else -> R.id.nav_status
    }

    private fun titleFor(tag: String): Int = when (tag) {
        TAG_LOGS -> R.string.nav_logs
        TAG_MAIL -> R.string.nav_mail
        TAG_WEB -> R.string.nav_web
        else -> R.string.nav_status
    }

    /** Aus MailFragment: in die Weboberflaeche wechseln und dort einen Pfad oeffnen. */
    override fun openWeb(path: String) {
        show(TAG_WEB)
        bottomNav.selectedItemId = R.id.nav_web
        (supportFragmentManager.findFragmentByTag(TAG_WEB) as? WebFragment)?.open(path)
    }

    // --- Menue --------------------------------------------------------------

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean = when (item.itemId) {
        R.id.action_biometric -> {
            toggleBiometric()
            true
        }
        R.id.action_disconnect -> {
            confirmDisconnect()
            true
        }
        R.id.action_about -> {
            showAbout()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: android.view.Menu): Boolean {
        menu.findItem(R.id.action_biometric)?.isChecked = Credentials.biometricEnabled(this)
        return super.onPrepareOptionsMenu(menu)
    }

    private fun toggleBiometric() {
        val available = BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS

        if (!available) {
            AlertDialog.Builder(this)
                .setMessage(R.string.biometric_unavailable)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }
        Credentials.setBiometricEnabled(this, !Credentials.biometricEnabled(this))
        invalidateOptionsMenu()
    }

    private fun confirmDisconnect() {
        AlertDialog.Builder(this)
            .setTitle(R.string.action_disconnect)
            .setMessage(R.string.disconnect_confirm)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_disconnect) { _, _ ->
                Credentials.clear(this)
                startActivity(Intent(this, SetupActivity::class.java))
                finish()
            }
            .show()
    }

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(
                getString(
                    R.string.about_body,
                    Credentials.appVersionName(this),
                    Credentials.instanceUrl(this).orEmpty()
                )
            )
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    companion object {
        private const val STATE_TAG = "current_tag"
        private const val TAG_STATUS = "status"
        private const val TAG_LOGS = "logs"
        private const val TAG_MAIL = "mail"
        private const val TAG_WEB = "web"
        private val ALL_TAGS = setOf(TAG_STATUS, TAG_LOGS, TAG_MAIL, TAG_WEB)
    }
}
