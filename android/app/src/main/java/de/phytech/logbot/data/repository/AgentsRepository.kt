/**
 * Repository für Geräte-Verwaltung (Löschen ist Administratoren vorbehalten).
 */
package de.phytech.logbot.data.repository

import de.phytech.logbot.core.network.requireSuccess
import de.phytech.logbot.core.network.safeApiCall
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.AgentsApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentsRepository @Inject constructor(
    private val api: AgentsApi,
) {
    suspend fun delete(id: Int, purgeLogs: Boolean): UiState<Unit> =
        safeApiCall { api.delete(id, purgeLogs).requireSuccess() }
}
