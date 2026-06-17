/**
 * Logs-Screen: Live-Liste mit Filter (Suche/Level/Host/Source), Detailansicht beim
 * Antippen und seitenweisem Nachladen.
 */
package de.phytech.logbot.feature.logs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.LogDetailDto
import de.phytech.logbot.data.api.LogDto
import de.phytech.logbot.data.repository.LogFilter
import de.phytech.logbot.feature.common.EmptyState
import de.phytech.logbot.feature.common.ErrorState
import de.phytech.logbot.feature.common.LoadingState

@Composable
fun LogsScreen(viewModel: LogsViewModel = hiltViewModel()) {
    when (val s = viewModel.state) {
        is UiState.Loading -> LoadingState()
        is UiState.Error -> ErrorState(s.message, onRetry = viewModel::refresh)
        is UiState.Success -> {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterCard(
                        current = viewModel.filter,
                        onApply = viewModel::applyFilter,
                    )
                }
                if (s.data.isEmpty()) {
                    item { EmptyState("Keine Logs gefunden") }
                }
                items(s.data, key = { it.id }) { log ->
                    LogRow(log, onClick = { viewModel.openDetail(log.id) })
                }
                if (viewModel.canLoadMore) {
                    item {
                        if (viewModel.loadingMore) {
                            Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            OutlinedButton(
                                onClick = viewModel::loadMore,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Mehr laden") }
                        }
                    }
                }
            }
        }
    }

    viewModel.detail?.let { detailState ->
        LogDetailDialog(detailState, onDismiss = viewModel::closeDetail)
    }
}

@Composable
private fun FilterCard(current: LogFilter, onApply: (LogFilter) -> Unit) {
    var search by remember(current) { mutableStateOf(current.search ?: "") }
    var level by remember(current) { mutableStateOf(current.level ?: "") }
    var hostname by remember(current) { mutableStateOf(current.hostname ?: "") }
    var source by remember(current) { mutableStateOf(current.source ?: "") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(search, { search = it }, label = { Text("Suche") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(level, { level = it }, label = { Text("Level") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(hostname, { hostname = it }, label = { Text("Host") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(source, { source = it }, label = { Text("Source") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onApply(LogFilter()) },
                    modifier = Modifier.weight(1f),
                ) { Text("Zurücksetzen") }
                OutlinedButton(
                    onClick = { onApply(LogFilter(search = search, level = level, hostname = hostname, source = source)) },
                    modifier = Modifier.weight(1f),
                ) { Text("Filtern") }
            }
        }
    }
}

@Composable
private fun LogRow(log: LogDto, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LevelBadge(log.level)
                Text(
                    text = formatTimestamp(log.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            Text(
                text = log.hostname ?: "—",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = log.message ?: "",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun LogDetailDialog(state: UiState<LogDetailDto>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schließen") } },
        title = { Text("Log-Detail") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                when (state) {
                    is UiState.Loading -> CircularProgressIndicator()
                    is UiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
                    is UiState.Success -> {
                        val d = state.data
                        DetailLine("Zeit", formatTimestamp(d.timestamp))
                        DetailLine("Level", d.level ?: "—")
                        DetailLine("Host", d.hostname ?: "—")
                        DetailLine("IP", d.ipAddress ?: "—")
                        DetailLine("Source", d.source ?: "—")
                        DetailLine("Facility", d.facility?.toString() ?: "—")
                        DetailLine("Agent-ID", d.agentId?.toString() ?: "—")
                        DetailLine("Nachricht", d.rawMessage ?: d.message ?: "—")
                        d.extraData?.let { DetailLine("Extra", it.toString()) }
                    }
                }
            }
        },
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LevelBadge(level: String?) {
    val lvl = (level ?: "").lowercase()
    val color = when {
        lvl.contains("err") || lvl.contains("crit") || lvl.contains("alert") || lvl.contains("emerg") ->
            MaterialTheme.colorScheme.error
        lvl.contains("warn") -> Color(0xFFE08600)
        lvl.contains("info") || lvl.contains("notice") -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    }
    Surface(color = color.copy(alpha = 0.15f), contentColor = color, shape = RoundedCornerShape(4.dp)) {
        Text(
            text = (level ?: "—").uppercase(),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

private fun formatTimestamp(ts: String?): String {
    if (ts.isNullOrBlank()) return ""
    return ts.take(19).replace('T', ' ')
}
