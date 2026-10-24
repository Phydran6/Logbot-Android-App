/**
 * Repository für Server-Einstellungen (Lesen, Wert ändern, DB-Infos).
 *
 * Bearbeitet werden nur einfache Werte (Zahl, Ja/Nein, Text). Verschachtelte
 * Einstellungen (KI, Mail, LDAP …) pflegen eigene Bereiche des Servers mit
 * eigener Prüfung – ein Überschreiben von hier aus würde sie zerstören.
 */
package de.phytech.logbot.data.repository

import de.phytech.logbot.core.network.requireSuccess
import de.phytech.logbot.core.network.safeApiCall
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.DatabaseSettingsDto
import de.phytech.logbot.data.api.SettingUpdateRequest
import de.phytech.logbot.data.api.SettingsApi
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

data class SettingsBundle(
    val settings: Map<String, JsonElement>,
    val database: DatabaseSettingsDto?,
)

/** Art eines bekannten Werts – bestimmt Eingabefeld und Prüfung. */
enum class SettingKind { INT, BOOL, TEXT }

data class SettingSpec(
    val key: String,
    val label: String,
    val description: String,
    val kind: SettingKind,
    val unit: String? = null,
    val min: Long? = null,
    val max: Long? = null,
)

/** Die Einstellungen, die die Weboberfläche unter "Allgemein" anbietet – mit Erklärung. */
val KNOWN_SETTINGS = listOf(
    SettingSpec("log_retention_days", "Logs aufbewahren", "Ältere Logs löscht der Server automatisch.",
        SettingKind.INT, unit = "Tage", min = 1, max = 3650),
    SettingSpec("agent_offline_timeout", "Gerät gilt als offline nach", "Ohne neue Logs in dieser Zeit.",
        SettingKind.INT, unit = "Sekunden", min = 30, max = 86_400),
    SettingSpec("max_logs_per_request", "Höchstzahl Logs je Abruf", "Grenze für Abrufe über die API.",
        SettingKind.INT, min = 1, max = 100_000),
    SettingSpec("enable_auto_discovery", "Geräte automatisch erkennen", "Neue Absender werden selbst angelegt.",
        SettingKind.BOOL),
    SettingSpec("magic_link_enabled", "Anmeldung per E-Mail-Link", "Erlaubt die Anmeldung in der App ohne Passwort.",
        SettingKind.BOOL),
    SettingSpec("app_name", "Name der Instanz", "Wird in Oberfläche und Mails angezeigt.", SettingKind.TEXT),
    SettingSpec("timezone", "Zeitzone", "Z.B. Europe/Berlin.", SettingKind.TEXT),
)

@Singleton
class SettingsRepository @Inject constructor(
    private val api: SettingsApi,
) {
    suspend fun load(includeDatabase: Boolean): UiState<SettingsBundle> = safeApiCall {
        val settings = api.settings().settings
        // DB-Infos sind Administratoren vorbehalten.
        val db = if (includeDatabase) runCatching { api.database() }.getOrNull() else null
        SettingsBundle(settings, db)
    }

    suspend fun update(key: String, value: JsonElement): UiState<Unit> = safeApiCall {
        api.updateSetting(key, SettingUpdateRequest(value)).requireSuccess()
    }

    companion object {
        /** Eingabe in den passenden JSON-Wert umwandeln – oder null bei ungültiger Eingabe. */
        fun parse(spec: SettingSpec, raw: String): JsonElement? = when (spec.kind) {
            SettingKind.INT -> raw.trim().toLongOrNull()
                ?.takeIf { (spec.min == null || it >= spec.min) && (spec.max == null || it <= spec.max) }
                ?.let { JsonPrimitive(it) }
            SettingKind.BOOL -> raw.toBooleanStrictOrNull()?.let { JsonPrimitive(it) }
            SettingKind.TEXT -> JsonPrimitive(raw.trim())
        }
    }
}
