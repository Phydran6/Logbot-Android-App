/**
 * Haupt-Hülle der App nach der Anmeldung: ein Navigations-Drawer, der die
 * Server-Sidebar abbildet, plus ein verschachtelter NavHost für die Inhalte.
 *
 * Phase 2a: Inhalte sind Platzhalter, `isAdmin` ist gestubbt (true). In Phase 2b
 * kommt die Rolle aus der Session, in Phase 3 die echten Screens.
 */
package de.phytech.logbot.feature.shell

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Webhook
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
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.phytech.logbot.core.navigation.Routes
import de.phytech.logbot.core.navigation.mainMenuItems
import de.phytech.logbot.core.util.rememberAppVersionName
import de.phytech.logbot.feature.common.PlaceholderScreen
import de.phytech.logbot.feature.dashboard.DashboardScreen
import de.phytech.logbot.feature.health.HealthScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(
    onLoggedOut: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val isAdmin = viewModel.isAdmin

    LaunchedEffect(viewModel.sessionExpired) {
        if (viewModel.sessionExpired) onLoggedOut()
    }

    val items = remember(isAdmin) { mainMenuItems.filter { !it.adminOnly || isAdmin } }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.DASHBOARD
    val currentItem = mainMenuItems.firstOrNull { it.route == currentRoute }
    val version = rememberAppVersionName()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Logbot",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                )
                Spacer(Modifier.height(8.dp))
                items.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                        selected = item.route == currentRoute,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (item.route != currentRoute) {
                                navController.navigate(item.route) {
                                    popUpTo(Routes.DASHBOARD) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
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
                        viewModel.logout()
                        onLoggedOut()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "v$version",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                )
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
            NavHost(
                navController = navController,
                startDestination = Routes.DASHBOARD,
                modifier = Modifier.padding(padding),
            ) {
                composable(Routes.DASHBOARD) { DashboardScreen() }
                composable(Routes.LOGS) { PlaceholderScreen("Logs", Icons.Filled.Article) }
                composable(Routes.AGENTS) { PlaceholderScreen("Agents", Icons.Filled.Devices) }
                composable(Routes.USERS) { PlaceholderScreen("Users", Icons.Filled.People) }
                composable(Routes.WEBHOOKS) { PlaceholderScreen("Webhooks", Icons.Filled.Webhook) }
                composable(Routes.HEALTH) { HealthScreen() }
                composable(Routes.SETTINGS) { PlaceholderScreen("Einstellungen", Icons.Filled.Settings) }
                composable(Routes.BRANDING) { PlaceholderScreen("Branding", Icons.Filled.Palette) }
            }
        }
    }
}
