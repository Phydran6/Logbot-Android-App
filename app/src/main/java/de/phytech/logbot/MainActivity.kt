/**
 * Datei:        MainActivity.kt
 * Projekt:      Logbot
 * Paket:        de.phytech.logbot
 *
 * Beschreibung:
 * Single-Activity-Host. Setzt das Theme und hostet den Root-NavHost:
 * Auth-Graph (Setup → Login → MFA) bzw. direkt der Haupt-Graph, falls bereits
 * angemeldet (Token im CredentialStore vorhanden).
 */
package de.phytech.logbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import de.phytech.logbot.core.auth.CredentialStore
import de.phytech.logbot.core.designsystem.theme.LogbotTheme
import de.phytech.logbot.core.navigation.Routes
import de.phytech.logbot.feature.login.LoginScreen
import de.phytech.logbot.feature.login.MfaScreen
import de.phytech.logbot.feature.setup.SetupScreen
import de.phytech.logbot.feature.shell.MainShell
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var credentialStore: CredentialStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val start = if (credentialStore.isLoggedIn) Routes.MAIN else Routes.SETUP
        setContent {
            LogbotTheme {
                LogbotAppRoot(startDestination = start)
            }
        }
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
