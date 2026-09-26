/**
 * Retrofit-Interface für Logs (Liste mit Filter/Paging + Detail).
 */
package de.phytech.logbot.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface LogsApi {

    @GET("api/logs")
    suspend fun list(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("search") search: String? = null,
        @Query("level") level: String? = null,
        @Query("hostname") hostname: String? = null,
        @Query("source") source: String? = null,
    ): LogListResponse

    @GET("api/logs/{id}")
    suspend fun detail(@Path("id") id: Long): LogDetailDto
}
