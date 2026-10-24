/**
 * Geräte: alles, was Logs an den Server liefert, mit Online-Zustand. Antippen
 * öffnet die Aktionen – Logs des Geräts, die KI zum Gerät befragen und (nur
 * Administratoren) das Gerät entfernen.
 */
package de.phytech.logbot.feature.agents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.core.util.Formats
import de.phytech.logbot.data.api.DeviceDto
import de.phytech.logbot.feature.common.ActionErrorDialog
import de.phytech.logbot.feature.common.EmptyState
import de.phytech.logbot.feature.common.ErrorState
import de.phytech.logbot.feature.common.LoadingState
import de.phytech.logbot.feature.common.OkColor
import de.phytech.logbot.feature.common.Refreshable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    isAdmin: Boolean,
    onOpenLogs: (host: String?, severity: String?) -> Unit,
    onOpenChat: (host: String?, question: String?) -> Unit,
    viewModel: DevicesViewModel = hiltViewModel(),
) {
    var selected by remember { mutableStateOf<DeviceDto?>(null) }
    var toDelete by remember { mutableStateOf<DeviceDto?>(null) }

    when (val s = viewModel.state) {
        is UiState.Loading -> LoadingState()
        is UiState.Error -> ErrorState(s.message, onRetry = viewModel::retry)
        is UiState.Success -> {
            val all = s.data.items
            val shown = all
                .filter { !viewModel.onlyOffline || !it.online }
                .filter {
                    viewModel.query.isBlank() || listOfNotNull(it.host, it.ip, it.type)
                        .any { v -> v.contains(viewModel.query.trim(), ignoreCase = true) }
                }
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = viewModel.query,
                        onValueChange = { viewModel.query = it },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        placeholder = { Text("Name, IP oder Typ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        FilterChip(
                            selected = !viewModel.onlyOffline,
                            onClick = { viewModel.onlyOffline = false },
                            label = { Text("Alle (${all.size})") },
                        )
                        FilterChip(
                            selected = viewModel.onlyOffline,
                            onClick = { viewModel.onlyOffline = true },
                            label = { Text("Offline (${all.count { !it.online }})") },
                        )
                    }
                }
                Refreshable(viewModel.refreshing, viewModel::refresh) {
                    if (shown.isEmpty()) {
                        EmptyState(
                            when {
                                all.isEmpty() -> "Noch keine Geräte. Sobald ein Agent oder Syslog Daten schickt, erscheint es hier."
                                viewModel.onlyOffline -> "Alle Geräte sind online."
                                else -> "Kein Gerät passt zur Suche."
                            },
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(shown, key = { it.id }) { device ->
                                DeviceRow(device) { selected = device }
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { device ->
        ModalBottomSheet(onDismissRequest = { selected = null }) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Text(device.host, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp))
                Text(
                    listOfNotNull(device.type, device.ip, "zuletzt ${Formats.relative(device.lastSeen)}").joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                ListItem(
                    headlineContent = { Text("Logs dieses Geräts") },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null) },
                    modifier = Modifier.clickable { selected = null; onOpenLogs(device.host, null) },
                )
                ListItem(
                    headlineContent = { Text("KI fragen: Ist alles in Ordnung?") },
                    supportingContent = { Text("Die KI sieht in die Logs von ${device.host}") },
                    leadingContent = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
                    modifier = Modifier.clickable {
                        selected = null
                        onOpenChat(device.host, "Ist mit ${device.host} alles in Ordnung?")
                    },
                )
                if (isAdmin) {
                    ListItem(
                        headlineContent = { Text("Gerät entfernen", color = MaterialTheme.colorScheme.error) },
                        leadingContent = {
                            Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        modifier = Modifier.clickable { selected = null; toDelete = device },
                    )
                }
            }
        }
    }

    toDelete?.let { device ->
        var purge by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("${device.host} entfernen?") },
            text = {
                Column {
                    Text("Das Gerät verschwindet aus der Liste. Schickt es weiter Logs, taucht es wieder auf.")
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Checkbox(checked = purge, onCheckedChange = { purge = it })
                        Text("Auch alle Logs des Geräts löschen")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(device.id, purge)
                    toDelete = null
                }) { Text("Entfernen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Abbrechen") } },
        )
    }

    ActionErrorDialog(viewModel.actionError, viewModel::dismissActionError)
}

@Composable
private fun DeviceRow(device: DeviceDto, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = if (device.online) OkColor else MaterialTheme.colorScheme.error,
                shape = CircleShape,
                modifier = Modifier.size(10.dp),
            ) {}
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(device.host.ifBlank { "—" }, style = MaterialTheme.typography.titleSmall)
                Text(
                    listOfNotNull(device.type?.takeIf { it.isNotBlank() }, device.ip).joinToString(" · ").ifBlank { "unbekannt" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            Text(
                text = if (device.online) "online" else "offline\n${Formats.relative(device.lastSeen)}",
                style = MaterialTheme.typography.labelSmall,
                color = if (device.online) OkColor else MaterialTheme.colorScheme.error,
            )
        }
    }
}
