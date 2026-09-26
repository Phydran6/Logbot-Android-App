/**
 * Repository für Einstellungen (Lesen, Wert ändern, DB-Infos).
 */
package de.phytech.logbot.data.repository

import de.phytech.logbot.core.network.safeApiCall
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.DatabaseSettingsDto
import de.phytech.logbot.data.api.SettingUpdateRequest
import de.phytech.logbot.data.api.SettingsApi
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

data class SettingsBundle(
    val settings: Map<String, JsonElement>,
    val database: DatabaseSettingsDto?,
)

@Singleton
class SettingsRepository @Inject constructor(
    private val api: SettingsApi,
) {
    suspend fun load(): UiState<SettingsBundle> = safeApiCall {
        val settings = api.settings().settings
        // DB-Infos sind Admin-only; bei fehlender Berechtigung ignorieren.
        val db = runCatching { api.database() }.getOrNull()
        SettingsBundle(settings, db)
    }

    suspend fun update(key: String, rawValue: String): UiState<Unit> = safeApiCall {
        val element: JsonElement = rawValue.toLongOrNull()?.let { JsonPrimitive(it) }
            ?: rawValue.toBooleanStrictOrNull()?.let { JsonPrimitive(it) }
            ?: JsonPrimitive(rawValue)
        val r = api.updateSetting(key, SettingUpdateRequest(element))
        if (!r.isSuccessful) throw HttpException(r)
        Unit
    }
}
