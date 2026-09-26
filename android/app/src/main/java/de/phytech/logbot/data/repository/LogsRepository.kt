/**
 * Repository für Logs: gefilterte, seitenweise Liste + Detail.
 */
package de.phytech.logbot.data.repository

import de.phytech.logbot.core.network.safeApiCall
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.LogDetailDto
import de.phytech.logbot.data.api.LogListResponse
import de.phytech.logbot.data.api.LogsApi
import javax.inject.Inject
import javax.inject.Singleton

/** Filterkriterien für die Log-Liste (null = kein Filter). */
data class LogFilter(
    val search: String? = null,
    val level: String? = null,
    val hostname: String? = null,
    val source: String? = null,
)

const val LOGS_PAGE_SIZE = 50

@Singleton
class LogsRepository @Inject constructor(
    private val api: LogsApi,
) {
    suspend fun page(page: Int, filter: LogFilter): UiState<LogListResponse> =
        safeApiCall {
            api.list(
                page = page,
                pageSize = LOGS_PAGE_SIZE,
                search = filter.search?.ifBlank { null },
                level = filter.level?.ifBlank { null },
                hostname = filter.hostname?.ifBlank { null },
                source = filter.source?.ifBlank { null },
            )
        }

    suspend fun detail(id: Long): UiState<LogDetailDto> = safeApiCall { api.detail(id) }
}
