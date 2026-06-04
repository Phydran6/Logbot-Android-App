/**
 * Zentrale Routen- und Menü-Definitionen. Das Hauptmenü spiegelt 1:1 die
 * Sidebar des Logbot-Servers (Dashboard, Logs, Agents, Users, Webhooks,
 * Health, Settings, Branding).
 */
package de.phytech.logbot.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Webhook
import androidx.compose.ui.graphics.vector.ImageVector

/** Alle Navigations-Routen der App. */
object Routes {
    // Auth-Graph
    const val SETUP = "setup"

    // Haupt-Graph (Container mit Drawer)
    const val MAIN = "main"

    // Haupt-Menüpunkte (Server-Sidebar)
    const val DASHBOARD = "dashboard"
    const val LOGS = "logs"
    const val AGENTS = "agents"
    const val USERS = "users"
    const val WEBHOOKS = "webhooks"
    const val HEALTH = "health"
    const val SETTINGS = "settings"
    const val BRANDING = "branding"
}

/**
 * Ein Eintrag im Navigations-Drawer.
 * @param adminOnly nur für Admin-Rolle sichtbar (Server bleibt die Autorität).
 */
data class MenuItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val adminOnly: Boolean = false,
)

/** Reihenfolge & Beschriftung wie im Server-Frontend. */
val mainMenuItems: List<MenuItem> = listOf(
    MenuItem(Routes.DASHBOARD, "Dashboard", Icons.Filled.Dashboard),
    MenuItem(Routes.LOGS, "Logs", Icons.Filled.Article),
    MenuItem(Routes.AGENTS, "Agents", Icons.Filled.Devices),
    MenuItem(Routes.USERS, "Users", Icons.Filled.People, adminOnly = true),
    MenuItem(Routes.WEBHOOKS, "Webhooks", Icons.Filled.Webhook),
    MenuItem(Routes.HEALTH, "Health", Icons.Filled.MonitorHeart),
    MenuItem(Routes.SETTINGS, "Einstellungen", Icons.Filled.Settings),
    MenuItem(Routes.BRANDING, "Branding", Icons.Filled.Palette, adminOnly = true),
)
