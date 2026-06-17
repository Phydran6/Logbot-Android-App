/**
 * Retrofit-Interface für Benutzerverwaltung.
 */
package de.phytech.logbot.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface UsersApi {

    @GET("api/users")
    suspend fun list(): List<UserResponse>

    @POST("api/users")
    suspend fun create(@Body request: UserCreateRequest): UserResponse

    @PUT("api/users/{id}")
    suspend fun update(@Path("id") id: Int, @Body request: UserUpdateRequest): UserResponse

    @DELETE("api/users/{id}")
    suspend fun delete(@Path("id") id: Int): Response<Unit>

    @POST("api/users/{id}/mfa/reset")
    suspend fun resetMfa(@Path("id") id: Int): Response<Unit>
}
