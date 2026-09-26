/**
 * Users-Screen (Admin): Benutzerliste mit Anlegen, Bearbeiten (Rolle/aktiv/Passwort),
 * MFA-Reset und Löschen.
 */
package de.phytech.logbot.feature.users

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.UserCreateRequest
import de.phytech.logbot.data.api.UserResponse
import de.phytech.logbot.data.api.UserUpdateRequest
import de.phytech.logbot.feature.common.EmptyState
import de.phytech.logbot.feature.common.ErrorState
import de.phytech.logbot.feature.common.LoadingState

@Composable
fun UsersScreen(viewModel: UsersViewModel = hiltViewModel()) {
    var showForm by remember { mutableStateOf(false) }
    var formUser by remember { mutableStateOf<UserResponse?>(null) }
    var toDelete by remember { mutableStateOf<UserResponse?>(null) }

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
                        onClick = { formUser = null; showForm = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Benutzer anlegen") }
                }
                if (s.data.isEmpty()) {
                    item { EmptyState("Keine Benutzer") }
                }
                items(s.data, key = { it.id }) { user ->
                    UserRow(
                        user = user,
                        onEdit = { formUser = user; showForm = true },
                        onDelete = { toDelete = user },
                    )
                }
            }
        }
    }

    if (showForm) {
        UserFormDialog(
            existing = formUser,
            onResetMfa = formUser?.let { u -> { viewModel.resetMfa(u.id) {} } },
            onDismiss = { showForm = false },
            onSubmitCreate = { req -> viewModel.create(req) { showForm = false } },
            onSubmitUpdate = { req -> viewModel.update(formUser!!.id, req) { showForm = false } },
        )
    }

    toDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Benutzer löschen?") },
            text = { Text("${user.username} wird dauerhaft entfernt.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(user.id); toDelete = null }) { Text("Löschen") }
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
private fun UserRow(user: UserResponse, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(user.username, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = buildString {
                        append(if (user.role == "admin") "Administrator" else "Benutzer")
                        if (!user.isActive) append("  •  deaktiviert")
                        user.email?.takeIf { it.isNotBlank() }?.let { append("  •  $it") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "Bearbeiten") }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "Löschen") }
        }
    }
}

@Composable
private fun UserFormDialog(
    existing: UserResponse?,
    onResetMfa: (() -> Unit)?,
    onDismiss: () -> Unit,
    onSubmitCreate: (UserCreateRequest) -> Unit,
    onSubmitUpdate: (UserUpdateRequest) -> Unit,
) {
    val isCreate = existing == null
    var username by remember(existing) { mutableStateOf(existing?.username ?: "") }
    var email by remember(existing) { mutableStateOf(existing?.email ?: "") }
    var password by remember(existing) { mutableStateOf("") }
    var admin by remember(existing) { mutableStateOf(existing?.role == "admin") }
    var active by remember(existing) { mutableStateOf(existing?.isActive ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isCreate) "Benutzer anlegen" else "Benutzer bearbeiten") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (isCreate) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Benutzername") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-Mail (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (isCreate) "Passwort" else "Neues Passwort (optional)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Administrator", modifier = Modifier.weight(1f))
                    Switch(checked = admin, onCheckedChange = { admin = it })
                }
                if (!isCreate) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Aktiv", modifier = Modifier.weight(1f))
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                    if (onResetMfa != null) {
                        OutlinedButton(onClick = onResetMfa, modifier = Modifier.fillMaxWidth()) {
                            Text("MFA zurücksetzen")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (isCreate) {
                    onSubmitCreate(
                        UserCreateRequest(
                            username = username.trim(),
                            email = email.trim().ifBlank { null },
                            role = if (admin) "admin" else "user",
                            password = password,
                        ),
                    )
                } else {
                    onSubmitUpdate(
                        UserUpdateRequest(
                            email = email.trim().ifBlank { null },
                            role = if (admin) "admin" else "user",
                            isActive = active,
                            password = password.ifBlank { null },
                        ),
                    )
                }
            }) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
