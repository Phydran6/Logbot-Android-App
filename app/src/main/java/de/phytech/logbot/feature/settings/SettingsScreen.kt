/**
 * Settings-Screen: Server-Einstellungen (Schlüssel/Wert, editierbar) und – falls
 * Admin – Datenbank-Verbindungsinfos.
 */
package de.phytech.logbot.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.DatabaseSettingsDto
import de.phytech.logbot.feature.common.ErrorState
import de.phytech.logbot.feature.common.LoadingState
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

private fun JsonElement.asDisplay(): String =
    (this as? JsonPrimitive)?.content ?: this.toString()

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    var editKey by remember { mutableStateOf<String?>(null) }
    var editValue by remember { mutableStateOf("") }

    when (val s = viewModel.state) {
        is UiState.Loading -> LoadingState()
        is UiState.Error -> ErrorState(s.message, onRetry = viewModel::refresh)
        is UiState.Success -> {
            val bundle = s.data
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text("Einstellungen", style = MaterialTheme.typography.titleMedium)
                }
                val entries = bundle.settings.entries.sortedBy { it.key }
                items(entries, key = { it.key }) { (key, value) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editKey = key
                                editValue = value.asDisplay()
                            },
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(key, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            Text(value.asDisplay(), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                bundle.database?.let { db ->
                    item {
                        Text(
                            "Datenbank",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    item { DatabaseCard(db) }
                }
            }
        }
    }

    editKey?.let { key ->
        AlertDialog(
            onDismissRequest = { editKey = null },
            title = { Text(key) },
            text = {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    label = { Text("Wert") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateSetting(key, editValue) { editKey = null }
                }) { Text("Speichern") }
            },
            dismissButton = { TextButton(onClick = { editKey = null }) { Text("Abbrechen") } },
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
private fun DatabaseCard(db: DatabaseSettingsDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            InfoLine("Host", "${db.host}:${db.port}")
            InfoLine("Datenbank", db.name)
            InfoLine("Benutzer", db.user)
            InfoLine("Passwort", db.password)
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium)
    }
}
