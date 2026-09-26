/**
 * Webhooks-Screen: Liste mit Aufruf-URL (kopierbar), Anlegen/Bearbeiten (Name,
 * Beschreibung, Filter, Limit, Optionen), Token neu erzeugen, Löschen.
 */
package de.phytech.logbot.feature.webhooks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.WebhookCreateRequest
import de.phytech.logbot.data.api.WebhookDto
import de.phytech.logbot.data.api.WebhookFiltersDto
import de.phytech.logbot.data.api.WebhookUpdateRequest
import de.phytech.logbot.feature.common.EmptyState
import de.phytech.logbot.feature.common.ErrorState
import de.phytech.logbot.feature.common.LoadingState

@Composable
fun WebhooksScreen(viewModel: WebhooksViewModel = hiltViewModel()) {
    var showForm by remember { mutableStateOf(false) }
    var formItem by remember { mutableStateOf<WebhookDto?>(null) }
    var toDelete by remember { mutableStateOf<WebhookDto?>(null) }
    val clipboard = LocalClipboardManager.current

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
                    Button(
                        onClick = { formItem = null; showForm = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Webhook anlegen") }
                }
                if (s.data.isEmpty()) {
                    item { EmptyState("Keine Webhooks") }
                }
                items(s.data, key = { it.id }) { webhook ->
                    WebhookRow(
                        webhook = webhook,
                        url = viewModel.callUrl(webhook),
                        onCopy = { clipboard.setText(AnnotatedString(viewModel.callUrl(webhook))) },
                        onEdit = { formItem = webhook; showForm = true },
                        onRegenerate = { viewModel.regenerate(webhook.id) },
                        onDelete = { toDelete = webhook },
                    )
                }
            }
        }
    }

    if (showForm) {
        WebhookFormDialog(
            existing = formItem,
            onDismiss = { showForm = false },
            onSubmitCreate = { req -> viewModel.create(req) { showForm = false } },
            onSubmitUpdate = { req -> viewModel.update(formItem!!.id, req) { showForm = false } },
        )
    }

    toDelete?.let { webhook ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Webhook löschen?") },
            text = { Text("${webhook.name} wird entfernt.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(webhook.id); toDelete = null }) { Text("Löschen") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Abbrechen") } },
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
private fun WebhookRow(
    webhook: WebhookDto,
    url: String,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(webhook.name.ifBlank { "—" }, style = MaterialTheme.typography.titleSmall)
            webhook.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            Text(
                text = buildString {
                    append(if (webhook.isActive) "aktiv" else "inaktiv")
                    append("  •  ${webhook.callCount} Aufrufe")
                    append("  •  max ${webhook.maxResults}")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                modifier = Modifier.padding(top = 6.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCopy) { Text("URL kopieren") }
                TextButton(onClick = onRegenerate) { Text("Token neu") }
                TextButton(onClick = onEdit) { Text("Bearbeiten") }
                TextButton(onClick = onDelete) { Text("Löschen") }
            }
        }
    }
}

@Composable
private fun WebhookFormDialog(
    existing: WebhookDto?,
    onDismiss: () -> Unit,
    onSubmitCreate: (WebhookCreateRequest) -> Unit,
    onSubmitUpdate: (WebhookUpdateRequest) -> Unit,
) {
    val isCreate = existing == null
    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var description by remember(existing) { mutableStateOf(existing?.description ?: "") }
    var maxResults by remember(existing) { mutableStateOf((existing?.maxResults ?: 100).toString()) }
    var includeRaw by remember(existing) { mutableStateOf(existing?.includeRaw ?: false) }
    var active by remember(existing) { mutableStateOf(existing?.isActive ?: true) }
    var hostname by remember(existing) { mutableStateOf(existing?.filters?.hostname ?: "") }
    var source by remember(existing) { mutableStateOf(existing?.filters?.source ?: "") }
    var levels by remember(existing) { mutableStateOf(existing?.filters?.level?.joinToString(", ") ?: "") }

    fun buildFilters() = WebhookFiltersDto(
        hostname = hostname.trim().ifBlank { null },
        source = source.trim().ifBlank { null },
        level = levels.split(",").map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { null },
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isCreate) "Webhook anlegen" else "Webhook bearbeiten") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Beschreibung (optional)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    maxResults, { maxResults = it.filter { c -> c.isDigit() } },
                    label = { Text("Max. Treffer") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Filter", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(hostname, { hostname = it }, label = { Text("Hostname") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(source, { source = it }, label = { Text("Source") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(levels, { levels = it }, label = { Text("Level (kommagetrennt)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rohnachricht einschließen", modifier = Modifier.weight(1f))
                    Switch(checked = includeRaw, onCheckedChange = { includeRaw = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Aktiv", modifier = Modifier.weight(1f))
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val max = maxResults.toIntOrNull() ?: 100
                if (isCreate) {
                    onSubmitCreate(
                        WebhookCreateRequest(
                            name = name.trim(),
                            description = description.trim().ifBlank { null },
                            filters = buildFilters(),
                            maxResults = max,
                            includeRaw = includeRaw,
                            isActive = active,
                        ),
                    )
                } else {
                    onSubmitUpdate(
                        WebhookUpdateRequest(
                            name = name.trim(),
                            description = description.trim().ifBlank { null },
                            filters = buildFilters(),
                            maxResults = max,
                            includeRaw = includeRaw,
                            isActive = active,
                        ),
                    )
                }
            }) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
