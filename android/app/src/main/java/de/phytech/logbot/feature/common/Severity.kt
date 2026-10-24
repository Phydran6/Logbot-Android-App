/**
 * Darstellung von Schweregraden: Farbe und deutsche Bezeichnung, einheitlich in
 * Logs, Meldungen und Übersicht.
 */
package de.phytech.logbot.feature.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.phytech.logbot.core.notifications.Notifier

val WarningColor = Color(0xFFE08600)
val OkColor = Color(0xFF22A55A)

@Composable
fun severityColor(level: String?): Color = when (Notifier.rank(level)) {
    0, 1, 2, 3 -> MaterialTheme.colorScheme.error
    4 -> WarningColor
    5, 6 -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
}

@Composable
fun SeverityBadge(level: String?, modifier: Modifier = Modifier) {
    val color = severityColor(level)
    Surface(
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier,
    ) {
        Text(
            text = (level?.takeIf { it.isNotBlank() } ?: "—").uppercase(),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/** Auswahl für Filter: Server-Begriff und Beschriftung. */
val SEVERITY_FILTERS: List<Pair<String?, String>> = listOf(
    null to "Alle",
    "critical" to "Kritisch",
    "error" to "Fehler+",
    "warning" to "Warnungen+",
)
