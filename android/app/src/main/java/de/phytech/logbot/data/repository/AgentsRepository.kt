/**
 * Repository für Agents.
 */
package de.phytech.logbot.data.repository

import de.phytech.logbot.core.network.safeApiCall
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.AgentDto
import de.phytech.logbot.data.api.AgentsApi
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentsRepository @Inject constructor(
    private val api: AgentsApi,
) {
    suspend fun agents(): UiState<List<AgentDto>> =
        safeApiCall { api.list(pageSize = 200).items }

    suspend fun delete(id: Int): UiState<Unit> =
        safeApiCall {
            val resp = api.delete(id)
            if (!resp.isSuccessful) throw HttpException(resp)
            Unit
        }
}
