/**
 * Liest den aktuellen versionName der App aus dem PackageManager (vom Build gesetzt,
 * Git-basiertes Auto-Versioning). Bewusst über PackageManager statt BuildConfig,
 * damit kein BuildConfig-Feature nötig ist.
 */
package de.phytech.logbot.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberAppVersionName(): String {
    val context = LocalContext.current
    return remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
        }.getOrDefault("?")
    }
}
