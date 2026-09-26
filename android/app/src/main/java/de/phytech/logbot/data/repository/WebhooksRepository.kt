/**
 * Repository für Webhooks.
 */
package de.phytech.logbot.data.repository

import de.phytech.logbot.core.auth.CredentialStore
import de.phytech.logbot.core.network.safeApiCall
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.WebhookCreateRequest
import de.phytech.logbot.data.api.WebhookDto
import de.phytech.logbot.data.api.WebhookUpdateRequest
import de.phytech.logbot.data.api.WebhooksApi
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebhooksRepository @Inject constructor(
    private val api: WebhooksApi,
    private val credentialStore: CredentialStore,
) {
    suspend fun webhooks(): UiState<List<WebhookDto>> = safeApiCall { api.list() }

    suspend fun create(request: WebhookCreateRequest): UiState<Unit> =
        safeApiCall { api.create(request); Unit }

    suspend fun update(id: Int, request: WebhookUpdateRequest): UiState<Unit> =
        safeApiCall { api.update(id, request); Unit }

    suspend fun regenerateToken(id: Int): UiState<Unit> =
        safeApiCall { api.regenerateToken(id); Unit }

    suspend fun delete(id: Int): UiState<Unit> =
        safeApiCall {
            val r = api.delete(id)
            if (!r.isSuccessful) throw HttpException(r)
            Unit
        }

    /** Öffentliche Aufruf-URL eines Webhooks (Instanz-URL + Endpoint + Token). */
    fun callUrl(webhook: WebhookDto): String {
        val base = credentialStore.instanceUrl?.trimEnd('/') ?: ""
        return "$base/api/webhook/${webhook.id}/call?token=${webhook.token}"
    }
}
