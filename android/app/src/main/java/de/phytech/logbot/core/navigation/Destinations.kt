/**
 * Zentrale Routen- und Menü-Definitionen. Das Hauptmenü folgt der Sidebar des
 * Logbot-Servers und ergänzt, was am Handy zuerst gebraucht wird: Meldungen
 * und den KI-Assistenten.
 */
package de.phytech.logbot.core.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Webhook
import androidx.compose.ui.graphics.vector.ImageVector

/** Alle Navigations-Routen der App. */
object Routes {
    // Auth-Graph
    const val SETUP = "setup"
    const val LOGIN = "login"
    const val MAGIC_LINK = "magic-link"
    const val MFA = "mfa" // erwartet Argument: mfa/{mfaToken}

    // Haupt-Graph (Container mit Drawer)
    const val MAIN = "main"

    // Haupt-Menüpunkte
    const val DASHBOARD = "dashboard"
    const val NOTIFICATIONS = "notifications"
    const val CHAT = "chat"
    const val LOGS = "logs"
    const val DEVICES = "devices"
    const val USERS = "users"
    const val WEBHOOKS = "webhooks"
    const val HEALTH = "health"
    const val SETTINGS = "settings"
    const val BRANDING = "branding"

    // Mit optionalen Argumenten – so registriert im NavHost
    const val CHAT_PATTERN = "$CHAT?host={host}&q={q}"
    const val LOGS_PATTERN = "$LOGS?host={host}&severity={severity}"

    /** Chat, optional an ein Gerät gebunden und mit vorbereiteter Frage. */
    fun chat(host: String? = null, question: String? = null): String =
        "$CHAT?host=${enc(host)}&q=${enc(question)}"

    /** Logs, optional vorgefiltert. */
    fun logs(host: String? = null, severity: String? = null): String =
        "$LOGS?host=${enc(host)}&severity=${enc(severity)}"

    private fun enc(value: String?): String = Uri.encode(value.orEmpty())

    /** Basisroute ohne Argumente (für Markierung im Drawer). */
    fun base(route: String?): String? = route?.substringBefore('?')
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

val mainMenuItems: List<MenuItem> = listOf(
    MenuItem(Routes.DASHBOARD, "Übersicht", Icons.Filled.Dashboard),
    MenuItem(Routes.NOTIFICATIONS, "Meldungen", Icons.Filled.Notifications),
    MenuItem(Routes.CHAT, "KI-Assistent", Icons.Filled.AutoAwesome),
    MenuItem(Routes.LOGS, "Logs", Icons.AutoMirrored.Filled.Article),
    MenuItem(Routes.DEVICES, "Geräte", Icons.Filled.Devices),
    MenuItem(Routes.HEALTH, "Server-Zustand", Icons.Filled.MonitorHeart),
    MenuItem(Routes.USERS, "Benutzer", Icons.Filled.People, adminOnly = true),
    MenuItem(Routes.WEBHOOKS, "Webhooks", Icons.Filled.Webhook, adminOnly = true),
    MenuItem(Routes.SETTINGS, "Einstellungen", Icons.Filled.Settings),
    MenuItem(Routes.BRANDING, "Branding", Icons.Filled.Palette, adminOnly = true),
)
