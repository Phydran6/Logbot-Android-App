/**
 * Einstellungen: oben das, was nur dieses Handy betrifft (App-Sperre,
 * Benachrichtigungen, Server, Abmelden), darunter die Einstellungen des
 * Servers – lesbar für alle, änderbar für Administratoren.
 */
package de.phytech.logbot.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.repository.KNOWN_SETTINGS
import de.phytech.logbot.data.repository.SettingKind
import de.phytech.logbot.data.repository.SettingSpec
import de.phytech.logbot.feature.common.ActionErrorDialog
import de.phytech.logbot.feature.common.ErrorState
import de.phytech.logbot.feature.notifications.NotificationSettingsCard
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

private fun JsonElement?.asDisplay(spec: SettingSpec): String {
    val primitive = this as? JsonPrimitive ?: return "—"
    return when (spec.kind) {
        SettingKind.BOOL -> when (primitive.booleanOrNull ?: primitive.content.toBooleanStrictOrNull()) {
            true -> "an"
            false -> "aus"
            null -> primitive.content
        }
        else -> primitive.content.ifBlank { "—" } + (spec.unit?.let { " $it" } ?: "")
    }
}

@Composable
fun SettingsScreen(
    isAdmin: Boolean,
    onLoggedOut: () -> Unit,
    onServerForgotten: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    LaunchedEffect(isAdmin) { viewModel.load(isAdmin) }
    var editing by remember { mutableStateOf<SettingSpec?>(null) }
    var confirmForget by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text("Diese App", style = MaterialTheme.typography.titleMedium) }
        if (viewModel.biometricAvailable) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("App-Sperre", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Beim Öffnen und nach einer Minute im Hintergrund nach Fingerabdruck, Gesicht oder PIN fragen.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(checked = viewModel.biometricEnabled, onCheckedChange = viewModel::toggleBiometric)
                    }
                }
            }
        }
        item { NotificationSettingsCard() }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Server", style = MaterialTheme.typography.titleSmall)
                    Text(viewModel.serverHost, style = MaterialTheme.typography.bodyMedium)
                    if (viewModel.serverVersion.isNotBlank()) {
                        Text("Version ${viewModel.serverVersion}", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedButton(onClick = { viewModel.logout(onLoggedOut) }) { Text("Abmelden") }
                        TextButton(onClick = { confirmForget = true }) { Text("Server wechseln") }
                    }
                }
            }
        }

        item {
            Text("Server-Einstellungen", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
            if (!isAdmin) {
                Text(
                    "Ändern dürfen nur Administratoren.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
        when (val s = viewModel.state) {
            UiState.Loading -> item { CircularProgressIndicator(Modifier.padding(16.dp)) }
            is UiState.Error -> item { ErrorState(s.message, onRetry = { viewModel.load(isAdmin) }) }
            is UiState.Success -> {
                val present = KNOWN_SETTINGS.filter { it.key in s.data.settings || it.kind == SettingKind.BOOL }
                items(present, key = { it.key }) { spec ->
                    val value = s.data.settings[spec.key]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isAdmin) Modifier.clickable { editing = spec } else Modifier),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(spec.label, style = MaterialTheme.typography.titleSmall)
                                Text(spec.description, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                            }
                            Text(
                                if (value == null && spec.kind == SettingKind.BOOL && spec.key == "magic_link_enabled") "an"
                                else value.asDisplay(spec),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }
                item {
                    Text(
                        "KI, Mail, Anmeldung über LDAP/SSO, Archiv und Netzwerk haben eigene Bereiche in der " +
                            "Weboberfläche – dort werden sie mit Prüfung eingerichtet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                s.data.database?.let { db ->
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Datenbank", style = MaterialTheme.typography.titleSmall)
                                Text("${db.host}:${db.port} · ${db.name} · Benutzer ${db.user}",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { spec ->
        val current = (viewModel.state as? UiState.Success)?.data?.settings?.get(spec.key)
        EditDialog(spec, current, saving = viewModel.saving, onDismiss = { editing = null }) { raw ->
            viewModel.save(spec, raw) { editing = null }
        }
    }

    if (confirmForget) {
        AlertDialog(
            onDismissRequest = { confirmForget = false },
            title = { Text("Server wechseln?") },
            text = { Text("Du wirst abgemeldet, und die App vergisst diesen Server samt Einstellungen.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmForget = false
                    viewModel.forgetServer(onServerForgotten)
                }) { Text("Wechseln") }
            },
            dismissButton = { TextButton(onClick = { confirmForget = false }) { Text("Abbrechen") } },
        )
    }

    ActionErrorDialog(viewModel.actionError, viewModel::dismissActionError)
}

@Composable
private fun EditDialog(
    spec: SettingSpec,
    current: JsonElement?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val initial = (current as? JsonPrimitive)?.content.orEmpty()
    var text by remember { mutableStateOf(initial) }
    var flag by remember {
        mutableStateOf((current as? JsonPrimitive)?.booleanOrNull ?: (spec.key == "magic_link_enabled"))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(spec.label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(spec.description, style = MaterialTheme.typography.bodySmall)
                when (spec.kind) {
                    SettingKind.BOOL -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (flag) "an" else "aus", modifier = Modifier.weight(1f))
                        Switch(checked = flag, onCheckedChange = { flag = it })
                    }
                    else -> OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text(spec.unit ?: "Wert") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (spec.kind == SettingKind.INT) KeyboardType.Number else KeyboardType.Text,
                        ),
                        supportingText = if (spec.min != null && spec.max != null) {
                            { Text("${spec.min} bis ${spec.max}") }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(if (spec.kind == SettingKind.BOOL) flag.toString() else text) }, enabled = !saving) {
                Text("Speichern")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
