/**
 * Repository für Branding.
 */
package de.phytech.logbot.data.repository

import de.phytech.logbot.core.network.safeApiCall
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.BrandingApi
import de.phytech.logbot.data.api.BrandingConfigDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrandingRepository @Inject constructor(
    private val api: BrandingApi,
) {
    suspend fun load(): UiState<BrandingConfigDto> = safeApiCall { api.config() }

    suspend fun save(config: BrandingConfigDto): UiState<BrandingConfigDto> =
        safeApiCall { api.updateConfig(config) }

    suspend fun reset(): UiState<BrandingConfigDto> = safeApiCall { api.reset() }
}
