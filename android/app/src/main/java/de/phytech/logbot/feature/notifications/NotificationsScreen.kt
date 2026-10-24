/**
 * Meldungen des Servers: kritische Fehler, ausgefallene Geräte und alles, was
 * die Regeln auf dem Server als "ansehen" einstufen. Oben die Einstellung, ob
 * und wie das Handy Bescheid gibt.
 */
package de.phytech.logbot.feature.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.core.util.Formats
import de.phytech.logbot.data.api.NotificationDto
import de.phytech.logbot.feature.common.ActionErrorDialog
import de.phytech.logbot.feature.common.EmptyState
import de.phytech.logbot.feature.common.ErrorState
import de.phytech.logbot.feature.common.LoadingState
import de.phytech.logbot.feature.common.Refreshable
import de.phytech.logbot.feature.common.SeverityBadge

@Composable
fun NotificationsScreen(
    onOpenLogs: (host: String?, severity: String?) -> Unit,
    onOpenChat: (host: String?, question: String?) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    when (val s = viewModel.state) {
        UiState.Loading -> LoadingState()
        is UiState.Error -> if (s.code == 404) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                EmptyState(
                    "Dieser Server kennt noch keine Meldungen. Nach einem Update des Logbot-Servers " +
                        "erscheinen sie hier – und auf Wunsch als Benachrichtigung.",
                    modifier = Modifier.padding(top = 48.dp),
                )
            }
        } else {
            ErrorState(s.message, onRetry = viewModel::retry)
        }
        is UiState.Success -> Refreshable(viewModel.refreshing, viewModel::refresh) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item { NotificationSettingsCard() }
                if (s.data.any { !it.read }) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = viewModel::markAllRead) { Text("Alle als gelesen markieren") }
                        }
                    }
                }
                if (s.data.isEmpty()) {
                    item { EmptyState("Keine Meldungen – alles ruhig.", modifier = Modifier.padding(top = 32.dp)) }
                }
                items(s.data, key = { it.id }) { n ->
                    NotificationRow(n, onOpenLogs, onOpenChat)
                }
                if (viewModel.canLoadMore) {
                    item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (viewModel.loadingMore) CircularProgressIndicator()
                            else OutlinedButton(onClick = viewModel::loadMore) { Text("Ältere laden") }
                        }
                    }
                }
            }
        }
    }
    ActionErrorDialog(viewModel.actionError, viewModel::dismissActionError)
}

@Composable
private fun NotificationRow(
    n: NotificationDto,
    onOpenLogs: (String?, String?) -> Unit,
    onOpenChat: (String?, String?) -> Unit,
) {
    var expanded by rememberSaveable(n.id) { mutableStateOf(false) }
    Card(
        onClick = { expanded = !expanded },
        colors = if (n.read) CardDefaults.cardColors() else CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SeverityBadge(n.severity)
                Text(
                    text = listOfNotNull(n.hostname, Formats.relative(n.createdAt)).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                text = n.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (n.read) FontWeight.Normal else FontWeight.SemiBold,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            n.body?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (expanded) {
                Text(Formats.dateTime(n.createdAt), style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp))
                Row {
                    TextButton(onClick = { onOpenLogs(n.hostname, n.severity) }) { Text("Logs ansehen") }
                    TextButton(onClick = {
                        onOpenChat(n.hostname, "Was steckt hinter dieser Meldung, und was muss ich tun?\n\n${n.title}\n${n.body.orEmpty()}")
                    }) { Text("KI fragen") }
                }
            }
        }
    }
}
