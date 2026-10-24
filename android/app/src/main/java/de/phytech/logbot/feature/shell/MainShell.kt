/**
 * Haupt-Hülle der App nach der Anmeldung: ein Navigations-Drawer, der die
 * Server-Sidebar abbildet, plus ein verschachtelter NavHost für die Inhalte.
 * Admin-Einträge blendet der Drawer anhand der Rolle aus – der Server prüft
 * jede Aktion trotzdem selbst.
 */
package de.phytech.logbot.feature.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Badge
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.phytech.logbot.core.navigation.LinkInbox
import de.phytech.logbot.core.navigation.Routes
import de.phytech.logbot.core.navigation.mainMenuItems
import de.phytech.logbot.core.util.rememberAppVersionName
import de.phytech.logbot.feature.agents.DevicesScreen
import de.phytech.logbot.feature.branding.BrandingScreen
import de.phytech.logbot.feature.chat.ChatScreen
import de.phytech.logbot.feature.dashboard.DashboardScreen
import de.phytech.logbot.feature.health.HealthScreen
import de.phytech.logbot.feature.logs.LogsScreen
import de.phytech.logbot.feature.notifications.NotificationsScreen
import de.phytech.logbot.feature.settings.SettingsScreen
import de.phytech.logbot.feature.users.UsersScreen
import de.phytech.logbot.feature.webhooks.WebhooksScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(
    linkInbox: LinkInbox,
    onLoggedOut: () -> Unit,
    onServerForgotten: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val unread by viewModel.unread.collectAsState()
    val openNotification by linkInbox.openNotification.collectAsState()

    val isAdmin = viewModel.isAdmin
    val items = remember(isAdmin) { mainMenuItems.filter { !it.adminOnly || isAdmin } }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = Routes.base(backStackEntry?.destination?.route) ?: Routes.DASHBOARD
    val currentItem = mainMenuItems.firstOrNull { it.route == currentRoute }
    val version = rememberAppVersionName()

    // Benachrichtigung angetippt: zu den Meldungen.
    LaunchedEffect(openNotification) {
        if (openNotification != null) {
            linkInbox.consumeNotification()
            navController.openTopLevel(Routes.NOTIFICATIONS)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = viewModel.serverName,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 28.dp),
                    )
                    if (viewModel.username.isNotBlank()) {
                        Text(
                            text = viewModel.username + if (isAdmin) " · Administrator" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    items.forEach { item ->
                        NavigationDrawerItem(
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(item.label) },
                            badge = if (item.route == Routes.NOTIFICATIONS && unread > 0) {
                                { Badge { Text(if (unread > 99) "99+" else unread.toString()) } }
                            } else {
                                null
                            },
                            selected = item.route == currentRoute,
                            onClick = {
                                scope.launch { drawerState.close() }
                                if (item.route != currentRoute) navController.openTopLevel(item.route)
                            },
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 28.dp))
                    Spacer(Modifier.height(8.dp))
                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                        label = { Text("Abmelden") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.logout(onLoggedOut)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    Text(
                        text = "App v$version",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                    )
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentItem?.label ?: "Logbot") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menü öffnen")
                        }
                    },
                )
            },
        ) { padding ->
            val openLogs: (String?, String?) -> Unit = { host, severity ->
                navController.navigate(Routes.logs(host, severity))
            }
            val openChat: (String?, String?) -> Unit = { host, question ->
                navController.navigate(Routes.chat(host, question))
            }
            NavHost(
                navController = navController,
                startDestination = Routes.DASHBOARD,
                modifier = Modifier.padding(padding),
            ) {
                composable(Routes.DASHBOARD) {
                    DashboardScreen(
                        onOpenLogs = openLogs,
                        onOpenChat = openChat,
                        onOpenDevices = { navController.openTopLevel(Routes.DEVICES) },
                        onOpenNotifications = { navController.openTopLevel(Routes.NOTIFICATIONS) },
                    )
                }
                composable(Routes.NOTIFICATIONS) {
                    NotificationsScreen(onOpenLogs = openLogs, onOpenChat = openChat)
                }
                composable(
                    Routes.CHAT_PATTERN,
                    arguments = listOf(
                        navArgument("host") { type = NavType.StringType; defaultValue = "" },
                        navArgument("q") { type = NavType.StringType; defaultValue = "" },
                    ),
                ) { ChatScreen() }
                composable(
                    Routes.LOGS_PATTERN,
                    arguments = listOf(
                        navArgument("host") { type = NavType.StringType; defaultValue = "" },
                        navArgument("severity") { type = NavType.StringType; defaultValue = "" },
                    ),
                ) { LogsScreen(onAskAi = openChat) }
                composable(Routes.DEVICES) {
                    DevicesScreen(isAdmin = isAdmin, onOpenLogs = openLogs, onOpenChat = openChat)
                }
                composable(Routes.USERS) { UsersScreen() }
                composable(Routes.WEBHOOKS) { WebhooksScreen() }
                composable(Routes.HEALTH) { HealthScreen() }
                composable(Routes.SETTINGS) {
                    SettingsScreen(isAdmin = isAdmin, onLoggedOut = onLoggedOut, onServerForgotten = onServerForgotten)
                }
                composable(Routes.BRANDING) { BrandingScreen() }
            }
        }
    }
}

/** Hauptmenüpunkt öffnen: Verlauf bis zur Übersicht kappen, Zustand je Punkt behalten. */
private fun NavHostController.openTopLevel(route: String) {
    val target = when (route) {
        Routes.CHAT -> Routes.chat()
        Routes.LOGS -> Routes.logs()
        else -> route
    }
    navigate(target) {
        popUpTo(Routes.DASHBOARD) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
