/**
 * Retrofit-Interface für Agents (Liste + Löschen).
 */
package de.phytech.logbot.data.api

import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AgentsApi {

    @GET("api/agents")
    suspend fun list(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 200,
    ): AgentListResponse

    @DELETE("api/agents/{id}")
    suspend fun delete(@Path("id") id: Int): Response<Unit>
}
