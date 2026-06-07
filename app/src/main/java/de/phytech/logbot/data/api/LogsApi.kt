/**
 * Retrofit-Interface für die Log-Liste. Filter/Paging folgen als Ausbau.
 */
package de.phytech.logbot.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface LogsApi {

    @GET("api/logs")
    suspend fun list(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 100,
    ): LogListResponse
}
