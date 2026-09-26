/**
 * Health-Screen: System-Ressourcen der Instanz (CPU/RAM/Disk/Uptime, DB-Status,
 * Log-/Agent-Zahlen) – Live-Daten von /api/health/detailed.
 */
package de.phytech.logbot.feature.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.HealthDetailedDto
import de.phytech.logbot.feature.common.ErrorState
import de.phytech.logbot.feature.common.LoadingState
import de.phytech.logbot.feature.common.StatCard
import kotlin.math.roundToInt

@Composable
fun HealthScreen(viewModel: HealthViewModel = hiltViewModel()) {
    when (val s = viewModel.state) {
        is UiState.Loading -> LoadingState()
        is UiState.Error -> ErrorState(s.message, onRetry = viewModel::refresh)
        is UiState.Success -> HealthContent(s.data)
    }
}

@Composable
private fun HealthContent(h: HealthDetailedDto) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatCard("Status", h.status.ifBlank { "–" })
        StatCard("Server-Version", h.version.ifBlank { "–" })
        StatCard("Uptime", formatUptime(h.uptimeSeconds))
        StatCard("CPU", "${h.cpuPercent.roundToInt()} %")
        StatCard("Arbeitsspeicher", "${h.memoryPercent.roundToInt()} %")
        StatCard("Festplatte", "${h.diskPercent.roundToInt()} %")
        StatCard("Datenbank", if (h.databaseConnected) "verbunden" else "getrennt")
        StatCard("Logs gesamt", h.logsTotal.toString())
        StatCard("Logs (24 h)", h.logsLast24h.toString())
        StatCard("Agents online", "${h.agentsOnline} / ${h.agentsTotal}")
    }
}

private fun formatUptime(seconds: Double): String {
    val total = seconds.toLong()
    val days = total / 86_400
    val hours = (total % 86_400) / 3_600
    val minutes = (total % 3_600) / 60
    return buildString {
        if (days > 0) append("${days}d ")
        if (days > 0 || hours > 0) append("${hours}h ")
        append("${minutes}m")
    }
}
