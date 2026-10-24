/**
 * Übersicht: auf einen Blick, ob alles läuft – Urteil oben, darunter Geräte,
 * Probleme heute, offene Meldungen, Zustand des Servers und Schnellfragen an
 * die KI. Jede Kachel führt zur passenden Detailansicht.
 */
package de.phytech.logbot.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.core.util.Formats
import de.phytech.logbot.feature.common.ErrorState
import de.phytech.logbot.feature.common.LoadingState
import de.phytech.logbot.feature.common.OkColor
import de.phytech.logbot.feature.common.Refreshable
import de.phytech.logbot.feature.common.SeverityBadge
import de.phytech.logbot.feature.common.StatCard
import de.phytech.logbot.feature.common.WarningColor
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    onOpenLogs: (host: String?, severity: String?) -> Unit,
    onOpenChat: (host: String?, question: String?) -> Unit,
    onOpenDevices: () -> Unit,
    onOpenNotifications: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    when (val s = viewModel.state) {
        is UiState.Loading -> LoadingState()
        is UiState.Error -> ErrorState(s.message, onRetry = viewModel::retry)
        is UiState.Success -> Refreshable(viewModel.refreshing, viewModel::refresh) {
            DashboardContent(s.data, onOpenLogs, onOpenChat, onOpenDevices, onOpenNotifications)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DashboardContent(
    data: DashboardData,
    onOpenLogs: (String?, String?) -> Unit,
    onOpenChat: (String?, String?) -> Unit,
    onOpenDevices: () -> Unit,
    onOpenNotifications: () -> Unit,
) {
    val summary = data.summary
    val health = data.health
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Verdict(data)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = "Geräte online",
                value = "${summary.devices.online} / ${summary.devices.total}",
                hint = if (summary.devices.offline > 0) "${summary.devices.offline} offline" else "alle erreichbar",
                valueColor = if (summary.devices.offline > 0) WarningColor else OkColor,
                onClick = onOpenDevices,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                title = "Probleme heute",
                value = Formats.number(summary.problemsToday),
                hint = "Fehler und dringender",
                valueColor = if (summary.problemsToday > 0) MaterialTheme.colorScheme.error else OkColor,
                onClick = { onOpenLogs(null, "error") },
                modifier = Modifier.weight(1f),
            )
        }

        data.notifications?.takeIf { it.isNotEmpty() }?.let { items ->
            Card(onClick = onOpenNotifications, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Offene Meldungen", style = MaterialTheme.typography.titleSmall)
                    items.forEach { n ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SeverityBadge(n.severity)
                            Text(
                                text = listOfNotNull(n.hostname, n.title).joinToString(": "),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp).weight(1f),
                            )
                            Text(Formats.relative(n.createdAt), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        if (data.aiAllowed) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("KI fragen", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp))
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QUICK_QUESTIONS.forEach { q ->
                            AssistChip(onClick = { onOpenChat(null, q) }, label = { Text(q) })
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Logs heute", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${Formats.number(summary.logsToday)} Zeilen · gesamt etwa ${Formats.number(summary.logsTotal)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                summary.byLevelToday.entries.sortedBy { de.phytech.logbot.core.notifications.Notifier.rank(it.key) }
                    .forEach { (level, count) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SeverityBadge(level)
                            Text(Formats.number(count), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                TextButton(onClick = { onOpenLogs(null, null) }) { Text("Alle Logs ansehen") }
            }
        }

        health?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Logbot-Server", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "CPU ${it.cpuPercent.roundToInt()} % · RAM ${it.memoryPercent.roundToInt()} % · " +
                            "Platte ${it.diskPercent.roundToInt()} %",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Datenbank ${if (it.databaseConnected) "verbunden" else "GETRENNT"} · läuft seit " +
                            Formats.duration(it.uptimeSeconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (it.databaseConnected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

/** Ein Satz, der die Lage zusammenfasst – mit derselben Logik wie die Kacheln. */
@Composable
private fun Verdict(data: DashboardData) {
    val s = data.summary
    val diskFull = (data.health?.diskPercent ?: 0.0) >= 90
    val dbDown = data.health?.databaseConnected == false
    val critical = (s.byLevelToday["emergency"] ?: 0L) + (s.byLevelToday["alert"] ?: 0L) + (s.byLevelToday["critical"] ?: 0L)
    val (icon, color, text) = when {
        dbDown -> Triple(Icons.Filled.Error, MaterialTheme.colorScheme.error, "Die Datenbank des Servers ist nicht erreichbar.")
        critical > 0 -> Triple(Icons.Filled.Error, MaterialTheme.colorScheme.error, "$critical kritische Meldungen heute – bitte ansehen.")
        diskFull -> Triple(Icons.Filled.Warning, WarningColor, "Die Platte des Servers ist fast voll.")
        s.devices.offline > 0 -> Triple(Icons.Filled.Warning, WarningColor, "${s.devices.offline} Gerät(e) melden sich nicht.")
        s.problemsToday > 0 -> Triple(Icons.Filled.Warning, WarningColor, "${s.problemsToday} Fehler heute, nichts Kritisches.")
        else -> Triple(Icons.Filled.CheckCircle, OkColor, "Alles in Ordnung.")
    }
    VerdictCard(icon, color, text)
}

@Composable
private fun VerdictCard(icon: ImageVector, color: androidx.compose.ui.graphics.Color, text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

private val QUICK_QUESTIONS = listOf(
    "Ist im WLAN alles gut?",
    "Gab es heute kritische Fehler?",
    "Welche Geräte sind offline und warum?",
    "Gab es verdächtige Anmeldeversuche?",
)
