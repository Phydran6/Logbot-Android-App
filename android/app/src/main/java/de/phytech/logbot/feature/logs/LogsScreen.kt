/**
 * Logs: lesbare Liste, neueste zuerst, mit Endlos-Scrollen. Filter nach
 * Schweregrad, Gerät, Zeitraum und Suchbegriff. Antippen öffnet die Details
 * mit zerlegten Feldern, der Rohzeile und "KI fragen".
 */
package de.phytech.logbot.feature.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.core.util.Formats
import de.phytech.logbot.data.api.AppLogDetailDto
import de.phytech.logbot.data.api.AppLogDto
import de.phytech.logbot.feature.common.EmptyState
import de.phytech.logbot.feature.common.ErrorState
import de.phytech.logbot.feature.common.LoadingState
import de.phytech.logbot.feature.common.Refreshable
import de.phytech.logbot.feature.common.SEVERITY_FILTERS
import de.phytech.logbot.feature.common.SeverityBadge
import de.phytech.logbot.feature.common.severityColor
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Composable
fun LogsScreen(
    onAskAi: (host: String?, question: String?) -> Unit,
    viewModel: LogsViewModel = hiltViewModel(),
) {
    Column(Modifier.fillMaxSize()) {
        FilterBar(viewModel)
        when (val s = viewModel.state) {
            is UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(s.message, onRetry = viewModel::retry)
            is UiState.Success -> Refreshable(viewModel.refreshing, viewModel::refresh) {
                LogList(viewModel, s.data)
            }
        }
    }

    viewModel.detail?.let { detailState ->
        LogDetailSheet(detailState, onDismiss = viewModel::closeDetail, onAskAi = { log ->
            viewModel.closeDetail()
            onAskAi(
                log.host,
                "Was bedeutet diese Logzeile von ${log.host ?: "diesem Gerät"} " +
                    "(${Formats.dateTime(log.ts)}), und muss ich etwas tun?\n\n" +
                    (log.raw ?: log.message).orEmpty().take(1500),
            )
        })
    }
}

@Composable
private fun FilterBar(viewModel: LogsViewModel) {
    var search by rememberSaveable { mutableStateOf(viewModel.filter.search.orEmpty()) }
    var hostMenu by remember { mutableStateOf(false) }
    var timeMenu by remember { mutableStateOf(false) }
    val filter = viewModel.filter

    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = search,
            onValueChange = {
                search = it
                viewModel.setSearch(it)
            },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (search.isNotEmpty()) {
                    IconButton(onClick = { search = ""; viewModel.setSearch("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Suche leeren")
                    }
                }
            },
            placeholder = { Text("In Logs suchen …") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SEVERITY_FILTERS.forEach { (value, label) ->
                FilterChip(
                    selected = filter.minSeverity == value,
                    onClick = { viewModel.setSeverity(value) },
                    label = { Text(label) },
                )
            }
            Box {
                FilterChip(
                    selected = filter.hostname != null,
                    onClick = { hostMenu = true },
                    leadingIcon = { Icon(Icons.Filled.Devices, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text(filter.hostname ?: "Gerät") },
                )
                DropdownMenu(expanded = hostMenu, onDismissRequest = { hostMenu = false }) {
                    DropdownMenuItem(text = { Text("Alle Geräte") }, onClick = { viewModel.setHost(null); hostMenu = false })
                    viewModel.knownHosts.forEach { host ->
                        DropdownMenuItem(text = { Text(host) }, onClick = { viewModel.setHost(host); hostMenu = false })
                    }
                }
            }
            Box {
                FilterChip(
                    selected = filter.sinceHours != null,
                    onClick = { timeMenu = true },
                    leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text(TIME_RANGES.firstOrNull { it.first == filter.sinceHours }?.second ?: "Zeitraum") },
                )
                DropdownMenu(expanded = timeMenu, onDismissRequest = { timeMenu = false }) {
                    TIME_RANGES.forEach { (hours, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { viewModel.setSinceHours(hours); timeMenu = false })
                    }
                }
            }
        }
    }
}

private val TIME_RANGES = listOf(null to "Gesamter Zeitraum", 1 to "Letzte Stunde", 24 to "Letzte 24 h", 168 to "Letzte 7 Tage")

@Composable
private fun LogList(viewModel: LogsViewModel, logs: List<AppLogDto>) {
    val listState = rememberLazyListState()
    // Endlos-Scrollen: nachladen, sobald das Ende in Sicht kommt.
    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 10
        }
    }
    LaunchedEffect(nearEnd, logs.size) {
        if (nearEnd && viewModel.loadMoreError == null) viewModel.loadMore()
    }

    if (logs.isEmpty()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            EmptyState(if (viewModel.filter.isEmpty) "Noch keine Logs" else "Keine Logs zu diesem Filter")
            if (!viewModel.filter.isEmpty) {
                TextButton(onClick = viewModel::clearFilter, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Filter zurücksetzen")
                }
            }
        }
        return
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(logs, key = { it.id }) { log ->
            LogRow(log, onClick = { viewModel.openDetail(log.id) })
        }
        item {
            Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                when {
                    viewModel.loadingMore -> CircularProgressIndicator()
                    viewModel.loadMoreError != null -> OutlinedButton(onClick = viewModel::loadMore) {
                        Text("Nachladen fehlgeschlagen – erneut versuchen")
                    }
                    !viewModel.canLoadMore -> Text(
                        "Ende der Liste",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

@Composable
private fun LogRow(log: AppLogDto, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            // Farbstreifen am Rand: Schweregrad auf einen Blick beim Scrollen.
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(severityColor(log.level)),
            )
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = log.displayText.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    SeverityBadge(log.level)
                    Text(
                        text = listOfNotNull(Formats.short(log.ts), log.host, log.app ?: log.source)
                            .filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun LogDetailSheet(state: UiState<AppLogDetailDto>, onDismiss: () -> Unit, onAskAi: (AppLogDetailDto) -> Unit) {
    val clipboard = LocalClipboardManager.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (state) {
                is UiState.Loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is UiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
                is UiState.Success -> {
                    val d = state.data
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SeverityBadge(d.level)
                        Text(Formats.dateTime(d.ts), style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(start = 8.dp))
                    }
                    SelectionContainer {
                        Text(d.parsed.summary.ifBlank { d.message.orEmpty() }, style = MaterialTheme.typography.bodyLarge)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        d.host?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                        d.ip?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                        d.source?.takeIf { it.isNotBlank() }?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                        d.parsed.highlights.forEach { h ->
                            AssistChip(onClick = {}, label = { Text("${h.label}: ${h.value}") })
                        }
                    }
                    OutlinedButton(onClick = { onAskAi(d) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("KI fragen, was das bedeutet", modifier = Modifier.padding(start = 8.dp))
                    }
                    if (d.parsed.fields.isNotEmpty()) {
                        HorizontalDivider()
                        Text("Felder", style = MaterialTheme.typography.titleSmall)
                        d.parsed.fields.forEach { (key, value) ->
                            Row {
                                Text(key, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.4f),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                                Text((value as? JsonPrimitive)?.contentOrNull ?: value.toString(),
                                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.6f))
                            }
                        }
                    }
                    d.raw?.takeIf { it.isNotBlank() }?.let { raw ->
                        HorizontalDivider()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Rohzeile", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            IconButton(onClick = { clipboard.setText(AnnotatedString(raw)) }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Rohzeile kopieren")
                            }
                        }
                        SelectionContainer {
                            Text(raw, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(
                        listOfNotNull(
                            "ID ${d.id}",
                            d.facility?.let { "Facility $it" },
                            d.agentId?.let { "Gerät-ID $it" },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 24.dp),
                    )
                }
            }
        }
    }
}
