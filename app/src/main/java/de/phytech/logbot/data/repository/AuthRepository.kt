/**
 * Repository für Authentifizierung: kapselt AuthApi + CredentialStore und mappt
 * Server-Antworten/Fehler auf ein einfaches Ergebnis-Modell.
 */
package de.phytech.logbot.data.repository

import de.phytech.logbot.core.auth.CredentialStore
import de.phytech.logbot.data.api.AppTokenExchangeRequest
import de.phytech.logbot.data.api.AuthApi
import de.phytech.logbot.data.api.MfaLoginRequest
import de.phytech.logbot.data.api.UserResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LoginOutcome {
    data object Success : LoginOutcome
    data class MfaRequired(val mfaToken: String) : LoginOutcome
    data class Error(val message: String) : LoginOutcome
}

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val credentialStore: CredentialStore,
) {
    /** Instanz-URL speichern (vor dem Login, damit der Host-Interceptor greift). */
    fun setInstanceUrl(url: String) {
        credentialStore.instanceUrl = url.trim().trimEnd('/')
    }

    suspend fun login(username: String, password: String): LoginOutcome =
        withContext(Dispatchers.IO) {
            try {
                val res = api.login(username, password)
                when {
                    res.mfaRequired && !res.mfaToken.isNullOrBlank() ->
                        LoginOutcome.MfaRequired(res.mfaToken)
                    !res.accessToken.isNullOrBlank() -> {
                        credentialStore.accessToken = res.accessToken
                        LoginOutcome.Success
                    }
                    else -> LoginOutcome.Error("Unerwartete Antwort vom Server")
                }
            } catch (e: Exception) {
                LoginOutcome.Error(mapError(e))
            }
        }

    suspend fun loginMfa(mfaToken: String, code: String): LoginOutcome =
        withContext(Dispatchers.IO) {
            try {
                val token = api.loginMfa(MfaLoginRequest(mfaToken, code))
                credentialStore.accessToken = token.accessToken
                LoginOutcome.Success
            } catch (e: Exception) {
                LoginOutcome.Error(mapError(e))
            }
        }

    suspend fun exchangeAppToken(url: String, token: String): LoginOutcome =
        withContext(Dispatchers.IO) {
            try {
                credentialStore.instanceUrl = url.trim().trimEnd('/')
                val res = api.exchangeAppToken(AppTokenExchangeRequest(token))
                credentialStore.accessToken = res.accessToken
                LoginOutcome.Success
            } catch (e: Exception) {
                credentialStore.clearToken()
                LoginOutcome.Error(mapError(e))
            }
        }

    suspend fun fetchUser(): UserResponse = withContext(Dispatchers.IO) { api.me() }

    fun logout() = credentialStore.clearAll()

    private fun mapError(e: Exception): String = when (e) {
        is HttpException -> when (e.code()) {
            401 -> "Falsche Anmeldedaten oder Code"
            403 -> "Benutzer deaktiviert"
            423 -> "Zu viele Fehlversuche – kurz gesperrt"
            else -> "Serverfehler (${e.code()})"
        }
        is IOException -> "Keine Verbindung zur Instanz"
        else -> e.message ?: "Unbekannter Fehler"
    }
}
