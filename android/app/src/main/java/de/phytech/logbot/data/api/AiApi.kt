/**
 * Retrofit-Interface für den KI-Assistenten.
 */
package de.phytech.logbot.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AiApi {

    /** Darf dieser Benutzer die KI nutzen, und ist sie eingerichtet? */
    @GET("api/ai/status")
    suspend fun status(): AiStatusDto

    /** Beantwortet die letzte Frage des Verlaufs – der Server sieht selbst in den Logs nach. */
    @POST("api/ai/chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponseDto
}
