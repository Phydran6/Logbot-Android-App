/**
 * QR-Scan für die App-Anmeldung – mit Kamera-Berechtigung, wiederverwendbar in
 * Einrichtung und Anmeldung.
 */
package de.phytech.logbot.feature.setup

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/** Liefert eine Funktion, die den Scan startet; das Ergebnis geht an [onResult]. */
@Composable
fun rememberQrScanner(onResult: (String) -> Unit, onError: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let(onResult)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) scanLauncher.launch(scanOptions()) else onError("Für den QR-Scan braucht die App die Kamera.")
    }
    return {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) scanLauncher.launch(scanOptions()) else permissionLauncher.launch(Manifest.permission.CAMERA)
    }
}

private fun scanOptions() = ScanOptions().apply {
    setPrompt("QR-Code aus „App verbinden“ scannen")
    setBeepEnabled(false)
    setOrientationLocked(false)
    setBarcodeImageEnabled(false)
}
