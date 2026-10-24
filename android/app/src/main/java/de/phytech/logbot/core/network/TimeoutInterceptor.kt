/**
 * Längere Wartezeit für die KI: Eine Chat-Antwort braucht mehrere Runden mit
 * dem Anbieter und dauert schnell eine Minute. Alle anderen Aufrufe behalten
 * die kurze Grenze, damit ein hängender Server nicht minutenlang blockiert.
 */
package de.phytech.logbot.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeoutInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val path = chain.request().url.encodedPath
        return if (path.contains("/api/ai/")) {
            chain.withReadTimeout(AI_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .withWriteTimeout(AI_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .proceed(chain.request())
        } else {
            chain.proceed(chain.request())
        }
    }

    private companion object {
        const val AI_TIMEOUT_SECONDS = 300
    }
}
