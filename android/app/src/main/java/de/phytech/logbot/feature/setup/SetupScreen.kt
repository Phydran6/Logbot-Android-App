/**
 * Setup-/Verbindungs-Screen: Instanz-URL eingeben (→ Login) oder QR-App-Login scannen
 * (→ direkt angemeldet via Token-Exchange).
 */
package de.phytech.logbot.feature.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.phytech.logbot.core.util.rememberAppVersionName

@Composable
fun SetupScreen(
    onNeedsLogin: () -> Unit,
    onAuthenticated: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    var url by rememberSaveable { mutableStateOf(viewModel.previousUrl) }
    val version = rememberAppVersionName()

    val startScan = rememberQrScanner(
        onResult = { viewModel.handleQr(it, onAuthenticated) },
        onError = viewModel::showError,
    )

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
            Text("Logbot", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Mit deiner Instanz verbinden",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
            )

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Adresse deines Logbot-Servers") },
                placeholder = { Text("logbot.example.de") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { viewModel.connect(url, onNeedsLogin) }),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.connect(url, onNeedsLogin) },
                enabled = !viewModel.loading,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Verbinden") }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { startScan() },
                enabled = !viewModel.loading,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("QR-Code aus der Weboberfläche scannen") }
            Text(
                text = "Den Code zeigt die Weboberfläche unter „App verbinden“ – damit ist man ohne Passwort angemeldet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )

            if (viewModel.loading) {
                Spacer(Modifier.height(20.dp))
                CircularProgressIndicator()
            }
            viewModel.error?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = "v$version",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}
