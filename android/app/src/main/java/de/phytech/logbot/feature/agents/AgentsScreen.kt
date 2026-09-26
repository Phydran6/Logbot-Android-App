/**
 * Agents-Screen: Geräteliste der Instanz mit Online-Status, Typ, letzter Aktivität
 * und Log-Anzahl. Einträge können gelöscht werden (mit Bestätigung).
 */
package de.phytech.logbot.feature.agents

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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.AgentDto
import de.phytech.logbot.feature.common.EmptyState
import de.phytech.logbot.feature.common.ErrorState
import de.phytech.logbot.feature.common.LoadingState

@Composable
fun AgentsScreen(viewModel: AgentsViewModel = hiltViewModel()) {
    var toDelete by remember { mutableStateOf<AgentDto?>(null) }

    when (val s = viewModel.state) {
        is UiState.Loading -> LoadingState()
        is UiState.Error -> ErrorState(s.message, onRetry = viewModel::refresh)
        is UiState.Success -> {
            val agents = s.data
            if (agents.isEmpty()) {
                EmptyState("Keine Agents registriert")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(agents, key = { it.id }) { agent ->
                        AgentRow(agent, onDelete = { toDelete = agent })
                    }
                }
            }
        }
    }

    toDelete?.let { agent ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Agent löschen?") },
            text = { Text("${agent.hostname} und zugehörige Logs werden entfernt.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(agent.id)
                    toDelete = null
                }) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Abbrechen") }
            },
        )
    }

    viewModel.actionError?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::dismissActionError,
            confirmButton = { TextButton(onClick = viewModel::dismissActionError) { Text("OK") } },
            title = { Text("Fehler") },
            text = { Text(msg) },
        )
    }
}

@Composable
private fun AgentRow(agent: AgentDto, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val online = agent.isOnline == true
            Surface(
                color = if (online) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                shape = CircleShape,
                modifier = Modifier.size(10.dp),
            ) {}
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(agent.hostname.ifBlank { "—" }, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = buildString {
                        append(agent.deviceType.ifBlank { "unbekannt" })
                        agent.ipAddress?.let { append("  •  $it") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Text(
                    text = buildString {
                        append(if (online) "online" else "offline")
                        agent.logCount?.let { append("  •  $it Logs") }
                        agent.lastSeen?.let { append("  •  ${it.take(19).replace('T', ' ')}") }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Löschen")
            }
        }
    }
}
