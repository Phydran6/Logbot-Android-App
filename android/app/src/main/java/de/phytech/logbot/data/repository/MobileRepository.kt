/**
 * Repository der App-Schnittstelle: Startdaten (Rolle, Fähigkeiten des Servers,
 * Filterwerte), Übersicht, Logs mit Cursor und Geräte.
 *
 * Die Startdaten werden einmal geholt und als [bootstrap] geteilt: Drawer,
 * Logs-Filter und Chat lesen daraus, statt jeder für sich nachzufragen.
 */
package de.phytech.logbot.data.repository

import de.phytech.logbot.core.network.safeApiCall
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.AppLogDetailDto
import de.phytech.logbot.data.api.AppLogPageDto
import de.phytech.logbot.data.api.BootstrapDto
import de.phytech.logbot.data.api.DeviceListDto
import de.phytech.logbot.data.api.MobileApi
import de.phytech.logbot.data.api.SummaryDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Filterkriterien für die Log-Liste (null = kein Filter). */
data class LogFilter(
    val search: String? = null,
    /** Dieses Level und alles Dringendere (Server-Begriff, z.B. "error"). */
    val minSeverity: String? = null,
    val hostname: String? = null,
    val sinceHours: Int? = null,
) {
    val isEmpty: Boolean get() = search.isNullOrBlank() && minSeverity == null && hostname == null && sinceHours == null
}

const val LOGS_PAGE_SIZE = 50

/** Fähigkeiten, die der Server in /api/app/bootstrap meldet. */
object Capability {
    const val NOTIFICATIONS = "notifications"
    const val PUSH = "push-webpush"
    const val MAGIC_LINK = "magic-link"
    const val AI_CHAT = "ai-chat"
}

@Singleton
class MobileRepository @Inject constructor(
    private val api: MobileApi,
) {
    private val _bootstrap = MutableStateFlow<BootstrapDto?>(null)
    val bootstrap: StateFlow<BootstrapDto?> = _bootstrap.asStateFlow()

    suspend fun refreshBootstrap(): UiState<BootstrapDto> =
        safeApiCall { api.bootstrap() }.also { if (it is UiState.Success) _bootstrap.value = it.data }

    fun hasCapability(name: String): Boolean =
        _bootstrap.value?.server?.capabilities?.contains(name) == true

    fun clear() {
        _bootstrap.value = null
    }

    suspend fun summary(): UiState<SummaryDto> = safeApiCall { api.summary() }

    suspend fun logs(filter: LogFilter, beforeId: Long? = null): UiState<AppLogPageDto> =
        safeApiCall {
            api.logs(
                limit = LOGS_PAGE_SIZE,
                beforeId = beforeId,
                hostname = filter.hostname?.ifBlank { null },
                minSeverity = filter.minSeverity,
                search = filter.search?.trim()?.ifBlank { null },
                sinceHours = filter.sinceHours,
            )
        }

    suspend fun logDetail(id: Long): UiState<AppLogDetailDto> = safeApiCall { api.logDetail(id) }

    suspend fun devices(): UiState<DeviceListDto> = safeApiCall { api.devices() }
}
