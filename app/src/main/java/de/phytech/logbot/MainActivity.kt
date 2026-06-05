/**
 * Datei:        MainActivity.kt
 * Projekt:      Logbot
 * Paket:        de.phytech.logbot
 *
 * Beschreibung:
 * Single-Activity-Host der nativen App. Setzt das Material-3-Theme und hostet den
 * Root-NavHost (Auth-Graph → Haupt-Graph). Die frühere WebView wurde durch native
 * Compose-Screens ersetzt.
 *
 * Phase 2a: Auth ist noch gestubbt (Setup-Screen springt direkt ins Menü-Gerüst).
 * Phase 2b ergänzt echten Login, Session-Management und Biometrie-Lock.
 */
package de.phytech.logbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import de.phytech.logbot.core.designsystem.theme.LogbotTheme
import de.phytech.logbot.core.navigation.Routes
import de.phytech.logbot.feature.setup.SetupScreen
import de.phytech.logbot.feature.shell.MainShell

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LogbotTheme {
                LogbotAppRoot()
            }
        }
    }
}

@Composable
private fun LogbotAppRoot() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.SETUP) {
        composable(Routes.SETUP) {
            SetupScreen(
                onContinue = {
                    nav.navigate(Routes.MAIN) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.MAIN) {
            MainShell(
                onLogout = {
                    nav.navigate(Routes.SETUP) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
            )
        }
    }
}
