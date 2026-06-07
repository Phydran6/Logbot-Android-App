/**
 * Repository für Logs. Liefert aktuell die neueste Seite (Filter/Paging folgen).
 */
package de.phytech.logbot.data.repository

import de.phytech.logbot.core.network.safeApiCall
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.LogDto
import de.phytech.logbot.data.api.LogsApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogsRepository @Inject constructor(
    private val api: LogsApi,
) {
    suspend fun recent(pageSize: Int = 100): UiState<List<LogDto>> =
        safeApiCall { api.list(page = 1, pageSize = pageSize).items }
}
