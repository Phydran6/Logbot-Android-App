/**
 * Datei:        MainActivity.kt
 * Projekt:      Logbot
 * Paket:        de.phytech.logbot
 *
 * Beschreibung:
 * Single-Activity-Host (AppCompatActivity, damit BiometricPrompt nutzbar ist).
 * Beim Start: optionaler Biometrie-/PIN-Lock, dann der Root-NavHost
 * (Auth-Graph Setup → Login → MFA bzw. direkt der Haupt-Graph, falls angemeldet).
 */
package de.phytech.logbot

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import de.phytech.logbot.core.auth.CredentialStore
import de.phytech.logbot.core.designsystem.theme.LogbotTheme
import de.phytech.logbot.core.navigation.Routes
import de.phytech.logbot.core.security.Biometrics
import de.phytech.logbot.feature.lock.LockScreen
import de.phytech.logbot.feature.login.LoginScreen
import de.phytech.logbot.feature.login.MfaScreen
import de.phytech.logbot.feature.setup.SetupScreen
import de.phytech.logbot.feature.shell.MainShell
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var credentialStore: CredentialStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val start = if (credentialStore.isLoggedIn) Routes.MAIN else Routes.SETUP
        val needsLock = credentialStore.isLoggedIn && credentialStore.biometricEnabled
        setContent {
            LogbotTheme {
                var unlocked by rememberSaveable { mutableStateOf(!needsLock) }
                if (unlocked) {
                    LogbotAppRoot(startDestination = start)
                } else {
                    LockScreen(onUnlock = { authenticate { unlocked = true } })
                    LaunchedEffect(Unit) { authenticate { unlocked = true } }
                }
            }
        }
    }

    /** Geräte-Authentifizierung (Biometrie/PIN). Bei Abbruch wird die App geschlossen. */
    private fun authenticate(onSuccess: () -> Unit) {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    finish()
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
}

@Composable
private fun LogbotAppRoot(startDestination: String) {
    val nav = rememberNavController()

    val toMain: () -> Unit = {
        nav.navigate(Routes.MAIN) {
            popUpTo(Routes.SETUP) { inclusive = true }
            launchSingleTop = true
        }
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
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            route = "${Routes.MFA}/{mfaToken}",
            arguments = listOf(navArgument("mfaToken") { type = NavType.StringType }),
        ) { entry ->
            MfaScreen(
                mfaToken = entry.arguments?.getString("mfaToken").orEmpty(),
                onAuthenticated = toMain,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.MAIN) {
            MainShell(
                onLoggedOut = {
                    nav.navigate(Routes.SETUP) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
