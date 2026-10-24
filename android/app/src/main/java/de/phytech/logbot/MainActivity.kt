/**
 * Datei:        MainActivity.kt
 * Projekt:      Logbot
 * Paket:        de.phytech.logbot
 *
 * Beschreibung:
 * Single-Activity-Host (AppCompatActivity, damit BiometricPrompt nutzbar ist).
 * Beim Start und nach längerer Zeit im Hintergrund: optionaler Biometrie-/PIN-
 * Lock, dann der Root-NavHost (Auth-Graph Setup → Login → MFA bzw. direkt der
 * Haupt-Graph, falls angemeldet). Anmelde-Links und angetippte Meldungen
 * kommen über den [LinkInbox] herein.
 */
package de.phytech.logbot

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import de.phytech.logbot.core.auth.CredentialStore
import de.phytech.logbot.core.auth.SessionManager
import de.phytech.logbot.core.designsystem.theme.LogbotTheme
import de.phytech.logbot.core.navigation.LinkInbox
import de.phytech.logbot.core.navigation.Routes
import de.phytech.logbot.core.security.Biometrics
import de.phytech.logbot.feature.lock.LockScreen
import de.phytech.logbot.feature.login.LoginScreen
import de.phytech.logbot.feature.login.MagicLinkScreen
import de.phytech.logbot.feature.login.MfaScreen
import de.phytech.logbot.feature.setup.SetupScreen
import de.phytech.logbot.feature.shell.MainShell
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var credentialStore: CredentialStore

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var linkInbox: LinkInbox

    /** Gesperrt, bis Biometrie/PIN bestätigt ist. */
    private var locked by mutableStateOf(false)
    private var backgroundSince = 0L
    private var promptShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        locked = savedInstanceState?.getBoolean(STATE_LOCKED) ?: needsLock()
        linkInbox.handle(intent)

        val start = when {
            credentialStore.isLoggedIn -> Routes.MAIN
            credentialStore.hasInstance -> Routes.LOGIN
            else -> Routes.SETUP
        }
        setContent {
            LogbotTheme {
                if (locked) {
                    LockScreen(onUnlock = ::authenticate)
                    LaunchedEffect(Unit) { authenticate() }
                } else {
                    LogbotAppRoot(startDestination = start, linkInbox = linkInbox, sessionManager = sessionManager)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        linkInbox.handle(intent)
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) backgroundSince = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        // Wer die App eine Weile verlassen hat, muss sich neu ausweisen –
        // vorher galt die Sperre nur beim Kaltstart.
        if (backgroundSince > 0 && needsLock() &&
            SystemClock.elapsedRealtime() - backgroundSince > RELOCK_AFTER_MS
        ) {
            locked = true
        }
        backgroundSince = 0
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_LOCKED, locked)
    }

    private fun needsLock(): Boolean =
        credentialStore.isLoggedIn && credentialStore.biometricEnabled && Biometrics.isAvailable(this)

    /** Geräte-Authentifizierung (Biometrie/PIN). Bei Abbruch bleibt die App gesperrt. */
    private fun authenticate() {
        if (promptShowing) return
        promptShowing = true
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    promptShowing = false
                    locked = false
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    promptShowing = false
                    // Bewusst abgebrochen: App schließen. Andere Fehler (z.B. zu
                    // viele Versuche) zeigen und den Sperrbildschirm stehen lassen.
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        finish()
                    } else if (errorCode != BiometricPrompt.ERROR_CANCELED) {
                        Toast.makeText(this@MainActivity, errString, Toast.LENGTH_LONG).show()
                    }
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Logbot entsperren")
            .setSubtitle("Mit Fingerabdruck, Gesicht oder Geräte-PIN bestätigen")
            .setAllowedAuthenticators(Biometrics.ALLOWED)
            .build()
        prompt.authenticate(info)
    }

    private companion object {
        const val STATE_LOCKED = "locked"
        const val RELOCK_AFTER_MS = 60_000L
    }
}

@Composable
private fun LogbotAppRoot(startDestination: String, linkInbox: LinkInbox, sessionManager: SessionManager) {
    val nav = rememberNavController()
    val magicLink by linkInbox.magicLink.collectAsState()
    val expired by sessionManager.expired.collectAsState()

    val toMain: () -> Unit = {
        nav.navigate(Routes.MAIN) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }
    val toLogin: () -> Unit = {
        nav.navigate(Routes.LOGIN) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }
    val toSetup: () -> Unit = {
        nav.navigate(Routes.SETUP) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    // Anmelde-Link aus der Mail: zum Einlöse-Screen, egal wo man gerade ist.
    LaunchedEffect(magicLink) {
        if (magicLink != null) nav.openMagicLink()
    }
    // Sitzung vom Server beendet: zurück zur Anmeldung.
    LaunchedEffect(expired) {
        if (expired && nav.currentDestination?.route == Routes.MAIN) toLogin()
    }

    NavHost(navController = nav, startDestination = startDestination) {
        composable(Routes.SETUP) {
            SetupScreen(
                onNeedsLogin = { nav.navigate(Routes.LOGIN) },
                onAuthenticated = toMain,
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onAuthenticated = toMain,
                onMfaRequired = { token -> nav.navigate("${Routes.MFA}/$token") },
                onMagicLink = { nav.navigate(Routes.MAGIC_LINK) },
                onChangeServer = toSetup,
            )
        }
        composable(Routes.MAGIC_LINK) {
            MagicLinkScreen(
                linkInbox = linkInbox,
                onAuthenticated = toMain,
                onMfaRequired = { token ->
                    nav.navigate("${Routes.MFA}/$token") { popUpTo(Routes.MAGIC_LINK) { inclusive = true } }
                },
                onBack = { if (!nav.popBackStack()) toLogin() },
            )
        }
        composable(
            route = "${Routes.MFA}/{mfaToken}",
            arguments = listOf(navArgument("mfaToken") { type = NavType.StringType }),
        ) { entry ->
            MfaScreen(
                mfaToken = entry.arguments?.getString("mfaToken").orEmpty(),
                onAuthenticated = toMain,
                onBack = { if (!nav.popBackStack()) toLogin() },
            )
        }
        composable(Routes.MAIN) {
            MainShell(
                linkInbox = linkInbox,
                onLoggedOut = toLogin,
                onServerForgotten = toSetup,
            )
        }
    }
}

private fun NavHostController.openMagicLink() {
    if (currentDestination?.route == Routes.MAGIC_LINK) return
    navigate(Routes.MAGIC_LINK) { launchSingleTop = true }
}
