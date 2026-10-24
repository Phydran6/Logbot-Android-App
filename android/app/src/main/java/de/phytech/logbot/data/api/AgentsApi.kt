/**
 * Retrofit-Interface für die Geräte-Verwaltung. Die Liste kommt aus der
 * App-Schnittstelle (MobileApi.devices), hier bleibt nur das Löschen.
 */
package de.phytech.logbot.data.api

import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Query

interface AgentsApi {

    /** [purgeLogs]: die Logs des Geräts mit löschen (sonst bleiben sie erhalten). */
    @DELETE("api/agents/{id}")
    suspend fun delete(@Path("id") id: Int, @Query("purge_logs") purgeLogs: Boolean = false): Response<Unit>
}
