/**
 * Setup-/Verbindungs-Screen: Instanz-URL eingeben (→ Login) oder QR-App-Login scannen
 * (→ direkt angemeldet via Token-Exchange).
 */
package de.phytech.logbot.feature.setup

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import de.phytech.logbot.core.util.rememberAppVersionName

@Composable
fun SetupScreen(
    onNeedsLogin: () -> Unit,
    onAuthenticated: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var url by rememberSaveable { mutableStateOf("") }
    val version = rememberAppVersionName()

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { viewModel.handleQr(it, onAuthenticated) }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) scanLauncher.launch(scanOptions())
        else viewModel.showError("Kamera-Berechtigung wird für den QR-Scan benötigt")
    }
    val startScan = {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) scanLauncher.launch(scanOptions())
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

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
                label = { Text("Instanz-URL (https://…)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
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
            ) { Text("QR-Code scannen") }

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

private fun scanOptions() = ScanOptions().apply {
    setPrompt("Logbot QR-Code scannen")
    setBeepEnabled(false)
    setOrientationLocked(false)
    setBarcodeImageEnabled(false)
}
