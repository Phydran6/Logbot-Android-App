/**
 * Retrofit-Interface für Webhooks.
 */
package de.phytech.logbot.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface WebhooksApi {

    @GET("api/webhooks")
    suspend fun list(): List<WebhookDto>

    @POST("api/webhooks")
    suspend fun create(@Body request: WebhookCreateRequest): WebhookDto

    @PUT("api/webhooks/{id}")
    suspend fun update(@Path("id") id: Int, @Body request: WebhookUpdateRequest): WebhookDto

    @POST("api/webhooks/{id}/regenerate-token")
    suspend fun regenerateToken(@Path("id") id: Int): WebhookDto

    @DELETE("api/webhooks/{id}")
    suspend fun delete(@Path("id") id: Int): Response<Unit>
}
