/**
 * Hängt den gespeicherten Bearer-Token an jede Anfrage an (sofern vorhanden)
 * und meldet eine abgelehnte Sitzung (HTTP 401) an den [SessionManager].
 */
package de.phytech.logbot.core.network

import de.phytech.logbot.core.auth.CredentialStore
import de.phytech.logbot.core.auth.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val credentialStore: CredentialStore,
    private val sessionManager: SessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = credentialStore.accessToken
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        val response = chain.proceed(request)
        // Nur wenn ein Token mitging: Ein 401 beim Anmelden selbst heißt
        // "falsches Passwort", nicht "Sitzung abgelaufen".
        if (response.code == 401 && !token.isNullOrBlank() && !isAuthEndpoint(request.url.encodedPath)) {
            sessionManager.onUnauthorized()
        }
        return response
    }

    private fun isAuthEndpoint(path: String): Boolean =
        AUTH_PATHS.any { path.contains(it) }

    private companion object {
        val AUTH_PATHS = listOf(
            "/api/auth/login", "/api/auth/magic-link/", "/api/auth/app-token/", "/api/auth/password/",
        )
    }
}
