/**
 * Anmeldung: per E-Mail-Link (ohne Passwort) oder mit Passwort. Bei aktiver
 * MFA leitet der Server auf den MFA-Schritt um.
 */
package de.phytech.logbot.feature.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit,
    onMfaRequired: (String) -> Unit,
    onMagicLink: () -> Unit,
    onChangeServer: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    var username by rememberSaveable { mutableStateOf(viewModel.lastLogin) }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var showForgot by rememberSaveable { mutableStateOf(false) }
    val submit = { viewModel.login(username, password, onAuthenticated, onMfaRequired) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Anmelden", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = viewModel.serverHost,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            )

            if (viewModel.hasPendingMagicLink) {
                Button(onClick = onMagicLink, modifier = Modifier.fillMaxWidth()) {
                    Text("Anmeldung per Mail fortsetzen")
                }
                Spacer(Modifier.height(8.dp))
            }
            FilledTonalButton(onClick = onMagicLink, enabled = !viewModel.loading, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Anmelde-Link per E-Mail")
            }
            Text(
                text = "Ohne Passwort: Adresse eingeben, Link in der Mail antippen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 20.dp)) {
                HorizontalDivider(Modifier.weight(1f))
                Text(
                    "oder mit Passwort",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                HorizontalDivider(Modifier.weight(1f))
            }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Benutzername oder E-Mail") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentType = ContentType.Username },
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Passwort") },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showPassword) "Passwort verbergen" else "Passwort anzeigen",
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentType = ContentType.Password },
            )

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = submit,
                enabled = !viewModel.loading,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Anmelden") }
            TextButton(onClick = { showForgot = true }, enabled = !viewModel.loading) { Text("Passwort vergessen?") }

            if (viewModel.loading) {
                Spacer(Modifier.height(12.dp))
                CircularProgressIndicator()
            }
            viewModel.error?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
            viewModel.info?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onChangeServer, enabled = !viewModel.loading) { Text("Anderen Server verwenden") }
        }
    }

    if (showForgot) {
        var login by rememberSaveable { mutableStateOf(username) }
        AlertDialog(
            onDismissRequest = { showForgot = false },
            title = { Text("Passwort zurücksetzen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Der Server schickt einen Link zum Zurücksetzen an die hinterlegte E-Mail-Adresse.")
                    OutlinedTextField(
                        value = login,
                        onValueChange = { login = it },
                        label = { Text("Benutzername oder E-Mail") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.forgotPassword(login)
                    showForgot = false
                }) { Text("Link anfordern") }
            },
            dismissButton = { TextButton(onClick = { showForgot = false }) { Text("Abbrechen") } },
        )
    }
}
