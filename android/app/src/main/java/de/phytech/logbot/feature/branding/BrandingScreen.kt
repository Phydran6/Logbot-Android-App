/**
 * Branding-Screen (Admin): Whitelabel-Konfiguration bearbeiten (Texte, Marken-
 * farben, Theme). Nicht bearbeitete Felder (Farbschemata, Logo) bleiben erhalten.
 */
package de.phytech.logbot.feature.branding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.BrandingConfigDto
import de.phytech.logbot.feature.common.ErrorState
import de.phytech.logbot.feature.common.LoadingState

@Composable
fun BrandingScreen(viewModel: BrandingViewModel = hiltViewModel()) {
    when (val s = viewModel.state) {
        is UiState.Loading -> LoadingState()
        is UiState.Error -> ErrorState(s.message, onRetry = viewModel::refresh)
        is UiState.Success -> BrandingForm(
            loaded = s.data,
            onSave = viewModel::save,
            onReset = viewModel::reset,
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
private fun BrandingForm(
    loaded: BrandingConfigDto,
    onSave: (BrandingConfigDto) -> Unit,
    onReset: () -> Unit,
) {
    var companyName by remember(loaded) { mutableStateOf(loaded.companyName) }
    var tagline by remember(loaded) { mutableStateOf(loaded.tagline) }
    var footerText by remember(loaded) { mutableStateOf(loaded.footerText) }
    var supportEmail by remember(loaded) { mutableStateOf(loaded.supportEmail) }
    var primary by remember(loaded) { mutableStateOf(loaded.primaryColor) }
    var secondary by remember(loaded) { mutableStateOf(loaded.secondaryColor) }
    var accent by remember(loaded) { mutableStateOf(loaded.accentColor) }
    var success by remember(loaded) { mutableStateOf(loaded.successColor) }
    var warning by remember(loaded) { mutableStateOf(loaded.warningColor) }
    var danger by remember(loaded) { mutableStateOf(loaded.dangerColor) }
    var darkDefault by remember(loaded) { mutableStateOf(loaded.defaultTheme == "dark") }
    var allowToggle by remember(loaded) { mutableStateOf(loaded.allowThemeToggle) }
    var customCss by remember(loaded) { mutableStateOf(loaded.customCss) }
    var showReset by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Allgemein", style = MaterialTheme.typography.titleMedium)
        Field("Firmenname", companyName) { companyName = it }
        Field("Tagline", tagline) { tagline = it }
        Field("Footer-Text", footerText) { footerText = it }
        Field("Support-E-Mail", supportEmail) { supportEmail = it }

        Text("Markenfarben (Hex)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Field("Primär", primary) { primary = it }
        Field("Sekundär", secondary) { secondary = it }
        Field("Akzent", accent) { accent = it }
        Field("Erfolg", success) { success = it }
        Field("Warnung", warning) { warning = it }
        Field("Gefahr", danger) { danger = it }

        Text("Theme", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        SwitchRow("Standard: Dark Mode", darkDefault) { darkDefault = it }
        SwitchRow("Theme-Umschalten erlauben", allowToggle) { allowToggle = it }

        Field("Custom CSS", customCss, singleLine = false) { customCss = it }

        Button(
            onClick = {
                onSave(
                    loaded.copy(
                        companyName = companyName,
                        tagline = tagline,
                        footerText = footerText,
                        supportEmail = supportEmail,
                        primaryColor = primary,
                        secondaryColor = secondary,
                        accentColor = accent,
                        successColor = success,
                        warningColor = warning,
                        dangerColor = danger,
                        defaultTheme = if (darkDefault) "dark" else "light",
                        allowThemeToggle = allowToggle,
                        customCss = customCss,
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("Speichern") }

        OutlinedButton(onClick = { showReset = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Auf Standard zurücksetzen")
        }
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Branding zurücksetzen?") },
            text = { Text("Alle Branding-Einstellungen werden auf die Standardwerte gesetzt.") },
            confirmButton = { TextButton(onClick = { onReset(); showReset = false }) { Text("Zurücksetzen") } },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text("Abbrechen") } },
        )
    }
}

@Composable
private fun Field(label: String, value: String, singleLine: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
