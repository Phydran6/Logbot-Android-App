/**
 * Zeigt Meldungen des Servers als Android-Benachrichtigung.
 *
 * Drei Kanäle, damit man am Gerät selbst entscheiden kann, was laut sein darf:
 * kritisch (Ton, über "Nicht stören" hinweg nur wenn der Nutzer es erlaubt),
 * Fehler und Warnungen, sowie Hinweise (still).
 */
package de.phytech.logbot.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import de.phytech.logbot.MainActivity
import de.phytech.logbot.R

object Notifier {
    const val CHANNEL_CRITICAL = "critical"
    const val CHANNEL_PROBLEMS = "problems"
    const val CHANNEL_INFO = "info"

    /** Extra am Intent: welche Meldung angetippt wurde. */
    const val EXTRA_NOTIFICATION_ID = "de.phytech.logbot.NOTIFICATION_ID"

    /** Schweregrade in Server-Begriffen, dringendste zuerst. */
    val SEVERITIES = listOf("emergency", "alert", "critical", "error", "warning", "notice", "info", "debug")

    fun rank(severity: String?): Int = when (severity?.lowercase()) {
        "emerg", "panic" -> 0
        "crit" -> 2
        "err" -> 3
        "warn" -> 4
        "informational" -> 6
        else -> SEVERITIES.indexOf(severity?.lowercase()).takeIf { it >= 0 } ?: 6
    }

    /** Ist [severity] mindestens so dringend wie [threshold]? */
    fun atLeast(severity: String?, threshold: String): Boolean = rank(severity) <= rank(threshold)

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_CRITICAL, "Kritische Meldungen", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Notfall, Alarm und kritische Fehler – sofort ansehen" },
                NotificationChannel(CHANNEL_PROBLEMS, "Fehler und Warnungen", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Fehler und Warnungen, die man sich ansehen sollte" },
                NotificationChannel(CHANNEL_INFO, "Hinweise", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "Alles Weitere, ohne Ton" },
            ),
        )
    }

    fun canPost(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun show(context: Context, id: Long, severity: String, title: String, text: String?) {
        if (!canPost(context)) return
        val channel = when {
            rank(severity) <= rank("critical") -> CHANNEL_CRITICAL
            rank(severity) <= rank("warning") -> CHANNEL_PROBLEMS
            else -> CHANNEL_INFO
        }
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_NOTIFICATION_ID, id)
        val pending = PendingIntent.getActivity(
            context, id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val label = severityLabel(severity)
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_logbot)
            .setContentTitle(title.ifBlank { label })
            .setContentText(text?.takeIf { it.isNotBlank() } ?: label)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text?.takeIf { it.isNotBlank() } ?: label))
            .setCategory(if (channel == CHANNEL_CRITICAL) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_STATUS)
            .setPriority(
                if (channel == CHANNEL_CRITICAL) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT,
            )
            // Inhalt von Logmeldungen gehört nicht auf den Sperrbildschirm.
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                NotificationCompat.Builder(context, channel)
                    .setSmallIcon(R.drawable.ic_stat_logbot)
                    .setContentTitle("Logbot: $label")
                    .build(),
            )
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setGroup("logbot")
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id.hashCode(), notification)
        } catch (_: SecurityException) {
            // Berechtigung zwischen Prüfung und Anzeige entzogen – nichts zu tun.
        }
    }

    fun severityLabel(severity: String?): String = when (rank(severity)) {
        0 -> "Notfall"
        1 -> "Alarm"
        2 -> "Kritisch"
        3 -> "Fehler"
        4 -> "Warnung"
        5 -> "Hinweis"
        7 -> "Debug"
        else -> "Info"
    }
}
