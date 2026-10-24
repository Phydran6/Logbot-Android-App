/**
 * Zustand des Logbot-Servers: Prozessor, Arbeitsspeicher und Platte als Balken,
 * Datenbank, Laufzeit, Logaufkommen und Geräte. Aktualisiert sich alle 20
 * Sekunden, solange die Ansicht offen ist.
 */
package de.phytech.logbot.feature.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.core.util.Formats
import de.phytech.logbot.data.api.HealthDetailedDto
import de.phytech.logbot.feature.common.ErrorState
import de.phytech.logbot.feature.common.LoadingState
import de.phytech.logbot.feature.common.OkColor
import de.phytech.logbot.feature.common.Refreshable
import de.phytech.logbot.feature.common.StatCard
import de.phytech.logbot.feature.common.WarningColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlin.math.roundToInt

@Composable
fun HealthScreen(viewModel: HealthViewModel = hiltViewModel()) {
    // Nur nachladen, solange der Screen sichtbar ist – im Hintergrund kein Akku verbrauchen.
    LifecycleResumeEffect(Unit) {
        val scope = MainScope()
        scope.launch {
            while (isActive) {
                delay(20_000)
                viewModel.poll()
            }
        }
        onPauseOrDispose { scope.cancel() }
    }
    when (val s = viewModel.state) {
        is UiState.Loading -> LoadingState()
        is UiState.Error -> ErrorState(s.message, onRetry = viewModel::retry)
        is UiState.Success -> Refreshable(viewModel.refreshing, viewModel::refresh) { HealthContent(s.data) }
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
        StatCard(
            title = "Zustand",
            value = if (h.status == "healthy") "Läuft" else h.status.ifBlank { "unbekannt" },
            hint = "Version ${h.version.ifBlank { "?" }} · seit ${Formats.duration(h.uptimeSeconds)}",
            valueColor = if (h.status == "healthy") OkColor else WarningColor,
        )
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Usage("Prozessor", h.cpuPercent)
                Usage("Arbeitsspeicher", h.memoryPercent)
                Usage("Platte", h.diskPercent, warnAt = 80.0, alarmAt = 90.0)
            }
        }
        StatCard(
            title = "Datenbank",
            value = if (h.databaseConnected) "verbunden" else "getrennt",
            valueColor = if (h.databaseConnected) OkColor else MaterialTheme.colorScheme.error,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Logs (24 h)", Formats.number(h.logsLast24h), Modifier.weight(1f),
                hint = "gesamt etwa ${Formats.number(h.logsTotal)}")
            StatCard("Geräte online", "${h.agentsOnline} / ${h.agentsTotal}", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Usage(label: String, percent: Double, warnAt: Double = 85.0, alarmAt: Double = 95.0) {
    val color = when {
        percent >= alarmAt -> MaterialTheme.colorScheme.error
        percent >= warnAt -> WarningColor
        else -> OkColor
    }
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("${percent.roundToInt()} %", style = MaterialTheme.typography.bodyMedium, color = color)
        }
        LinearProgressIndicator(
            progress = { (percent / 100.0).toFloat().coerceIn(0f, 1f) },
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    }
}
