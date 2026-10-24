/**
 * Repository für Meldungen: Liste vom Server, gelesen markieren und die
 * Anmeldung eines Push-Endpunkts (UnifiedPush) beim Server.
 */
package de.phytech.logbot.data.repository

import de.phytech.logbot.core.auth.CredentialStore
import de.phytech.logbot.core.network.requireSuccess
import de.phytech.logbot.core.network.safeApiCall
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.MobileApi
import de.phytech.logbot.data.api.NotificationPageDto
import de.phytech.logbot.data.api.NotificationsReadRequest
import de.phytech.logbot.data.api.PushRegisterRequest
import de.phytech.logbot.data.api.PushTestDto
import de.phytech.logbot.data.api.PushUnregisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationsRepository @Inject constructor(
    private val api: MobileApi,
    private val credentialStore: CredentialStore,
) {
    private val _unread = MutableStateFlow(0)

    /** Ungelesene Meldungen – für das Abzeichen im Menü. */
    val unread: StateFlow<Int> = _unread.asStateFlow()

    suspend fun page(beforeId: Long? = null): UiState<NotificationPageDto> =
        safeApiCall { api.notifications(beforeId = beforeId) }
            .also { if (it is UiState.Success) _unread.value = it.data.unreadCount }

    /** Neue Meldungen seit [sinceId] – für Hintergrundabfrage und Push. */
    suspend fun since(sinceId: Long): UiState<NotificationPageDto> =
        safeApiCall { api.notifications(sinceId = sinceId, limit = 20) }
            .also { if (it is UiState.Success) _unread.value = it.data.unreadCount }

    suspend fun markAllRead(upToId: Long): UiState<Unit> =
        safeApiCall { api.markRead(NotificationsReadRequest(upToId)).requireSuccess() }
            .also { if (it is UiState.Success) _unread.value = 0 }

    suspend fun vapidKey(): String? =
        (safeApiCall { api.vapidKey() } as? UiState.Success)?.data?.publicKey?.takeIf { it.isNotBlank() }

    suspend fun registerPush(endpoint: String, p256dh: String?, auth: String?): UiState<Unit> =
        safeApiCall {
            api.registerPush(
                PushRegisterRequest(
                    endpoint = endpoint,
                    p256dh = p256dh,
                    auth = auth,
                    deviceName = AuthRepository.deviceName(),
                    minSeverity = credentialStore.notifyMinSeverity,
                ),
            ).requireSuccess()
        }.also { if (it is UiState.Success) credentialStore.pushEndpoint = endpoint }

    suspend fun testPush(): UiState<PushTestDto> = safeApiCall { api.testPush() }

    /** Push-Endpunkt beim Server abmelden. Fehler sind hier egal – der Server räumt tote Endpunkte selbst ab. */
    suspend fun disablePushOnServer() {
        val endpoint = credentialStore.pushEndpoint ?: return
        if (credentialStore.accessToken != null) {
            safeApiCall { api.unregisterPush(PushUnregisterRequest(endpoint)).requireSuccess() }
        }
        credentialStore.pushEndpoint = null
    }
}
