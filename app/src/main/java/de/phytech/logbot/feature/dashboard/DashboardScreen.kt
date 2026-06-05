/**
 * Dashboard: Log-Statistiken der Instanz (Gesamt/heute, Hosts, Verteilung nach Level)
 * – Live-Daten von /api/logs/stats.
 */
package de.phytech.logbot.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.LogStatsDto
import de.phytech.logbot.feature.common.ErrorState
import de.phytech.logbot.feature.common.LoadingState
import de.phytech.logbot.feature.common.StatCard

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    when (val s = viewModel.state) {
        is UiState.Loading -> LoadingState()
        is UiState.Error -> ErrorState(s.message, onRetry = viewModel::refresh)
        is UiState.Success -> DashboardContent(s.data)
    }
}

@Composable
private fun DashboardContent(stats: LogStatsDto) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatCard("Logs gesamt", stats.totalLogs.toString())
        StatCard("Logs heute", stats.logsToday.toString())
        StatCard("Eindeutige Hosts", stats.uniqueHosts.toString())

        if (stats.logsByLevel.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Logs nach Level (heute)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    stats.logsByLevel.entries
                        .sortedByDescending { it.value }
                        .forEach { (level, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(level, style = MaterialTheme.typography.bodyLarge)
                                Text(count.toString(), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                }
            }
        }
    }
}
