/**
 * Nativer Setup-/Einstiegs-Screen.
 *
 * Phase 2a: nur Gerüst — der Button springt direkt ins Menü-Gerüst, damit die
 * Navigation testbar ist. Phase 2b ersetzt das durch echte Instanz-URL-Eingabe,
 * Passwort-/MFA-Login und QR-App-Login (Token-Exchange gegen den Server).
 */
package de.phytech.logbot.feature.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SetupScreen(
    onContinue: () -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Logbot",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Native App – Aufbau läuft (Phase 2a)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 40.dp),
            )
            Text(
                text = "Anmeldung an deine Instanz (URL, Login, MFA, QR) folgt in Phase 2b.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp),
            )
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Menü-Gerüst ansehen")
            }
        }
    }
}
