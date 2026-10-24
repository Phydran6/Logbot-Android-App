/**
 * Einstellungen für Benachrichtigungen aufs Handy – in den Meldungen und in
 * den App-Einstellungen derselbe Baustein.
 */
package de.phytech.logbot.feature.notifications

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import de.phytech.logbot.feature.common.OkColor
import de.phytech.logbot.feature.common.WarningColor

private val THRESHOLDS = listOf("critical" to "Nur Kritisches", "error" to "Fehler und Kritisches", "warning" to "Auch Warnungen")

@Composable
fun NotificationSettingsCard(
    modifier: Modifier = Modifier,
    viewModel: NotificationSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    LifecycleResumeEffect(Unit) {
        viewModel.reload()
        onPauseOrDispose { }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && activity != null) viewModel.enable(activity)
    }
    val turnOn = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !viewModel.canPost) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (activity != null) {
            viewModel.enable(activity)
        }
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Benachrichtigungen aufs Handy", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = when (viewModel.delivery) {
                            Delivery.OFF -> "Aus – Meldungen siehst du nur hier in der App."
                            Delivery.PUSH -> "Sofort per Push."
                            Delivery.POLLING -> "Die App sieht alle 15 Minuten nach."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when (viewModel.delivery) {
                            Delivery.PUSH -> OkColor
                            Delivery.POLLING -> WarningColor
                            Delivery.OFF -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        },
                    )
                }
                Switch(checked = viewModel.enabled, onCheckedChange = { if (it) turnOn() else viewModel.disable() })
            }

            if (viewModel.enabled && !viewModel.canPost) {
                Text(
                    "Android lässt Logbot gerade keine Benachrichtigungen anzeigen.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = { context.openAppNotificationSettings() }) { Text("In den Android-Einstellungen erlauben") }
            }

            if (viewModel.enabled) {
                Text("Melden ab", style = MaterialTheme.typography.labelMedium)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    THRESHOLDS.forEach { (value, label) ->
                        FilterChip(
                            selected = viewModel.minSeverity == value,
                            onClick = { viewModel.chooseMinSeverity(value) },
                            label = { Text(label) },
                        )
                    }
                }

                if (viewModel.delivery != Delivery.PUSH) {
                    if (viewModel.hasDistributor) {
                        OutlinedButton(
                            onClick = { activity?.let(viewModel::enablePush) },
                            enabled = !viewModel.busy,
                        ) { Text("Sofort-Push einrichten") }
                    } else {
                        Text(
                            "Für Meldungen ohne Verzögerung braucht Android eine Push-App. Empfohlen: " +
                                "ntfy (kostenlos, ohne Google-Konto). Nach der Installation hier " +
                                "„Sofort-Push einrichten“ antippen.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        TextButton(onClick = { context.openUrl(NTFY_URL) }) { Text("ntfy installieren") }
                    }
                } else {
                    TextButton(onClick = viewModel::test, enabled = !viewModel.busy) { Text("Testmeldung schicken") }
                }
            }
            viewModel.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

private const val NTFY_URL = "https://play.google.com/store/apps/details?id=io.heckel.ntfy"

fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun Context.openUrl(url: String) {
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun Context.openAppNotificationSettings() {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
    }
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
