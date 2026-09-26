/**
 * Repository für Benutzerverwaltung.
 */
package de.phytech.logbot.data.repository

import de.phytech.logbot.core.network.safeApiCall
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.UserCreateRequest
import de.phytech.logbot.data.api.UserResponse
import de.phytech.logbot.data.api.UserUpdateRequest
import de.phytech.logbot.data.api.UsersApi
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsersRepository @Inject constructor(
    private val api: UsersApi,
) {
    suspend fun users(): UiState<List<UserResponse>> = safeApiCall { api.list() }

    suspend fun create(request: UserCreateRequest): UiState<Unit> =
        safeApiCall { api.create(request); Unit }

    suspend fun update(id: Int, request: UserUpdateRequest): UiState<Unit> =
        safeApiCall { api.update(id, request); Unit }

    suspend fun delete(id: Int): UiState<Unit> =
        safeApiCall {
            val r = api.delete(id)
            if (!r.isSuccessful) throw HttpException(r)
            Unit
        }

    suspend fun resetMfa(id: Int): UiState<Unit> =
        safeApiCall {
            val r = api.resetMfa(id)
            if (!r.isSuccessful) throw HttpException(r)
            Unit
        }
}
