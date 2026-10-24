/**
 * Repository für Authentifizierung: kapselt AuthApi + CredentialStore und mappt
 * Server-Antworten/Fehler auf ein einfaches Ergebnis-Modell.
 *
 * Drei Wege hinein: Passwort (+ MFA), QR-Code aus der Weboberfläche und der
 * Anmelde-Link per Mail. Beim Link entsteht auf dem Gerät ein Geheimnis
 * (PKCE-`code_verifier`); der Server kennt nur dessen Abdruck. Wer die Mail
 * abfängt, kann den Link deshalb nicht einlösen.
 */
package de.phytech.logbot.data.repository

import android.content.Context
import android.os.Build
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import de.phytech.logbot.core.auth.CredentialStore
import de.phytech.logbot.core.auth.PendingMagicLink
import de.phytech.logbot.core.auth.SessionManager
import de.phytech.logbot.core.network.mapApiError
import de.phytech.logbot.core.notifications.NotificationScheduler
import de.phytech.logbot.data.api.AppTokenExchangeRequest
import de.phytech.logbot.data.api.AuthApi
import de.phytech.logbot.data.api.MagicLinkExchangeRequest
import de.phytech.logbot.data.api.MagicLinkRequest
import de.phytech.logbot.data.api.MfaLoginRequest
import de.phytech.logbot.data.api.PasswordForgotRequest
import de.phytech.logbot.data.api.UserResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LoginOutcome {
    data object Success : LoginOutcome
    data class MfaRequired(val mfaToken: String) : LoginOutcome
    data class Error(val message: String) : LoginOutcome
}

/** Stand einer Anmeldung per E-Mail-Link. */
sealed interface MagicLinkOutcome {
    /** Noch nicht bestätigt – in [pollSeconds] wieder nachfragen. */
    data class Pending(val pollSeconds: Int) : MagicLinkOutcome
    data object Success : MagicLinkOutcome
    data class MfaRequired(val mfaToken: String) : MagicLinkOutcome

    /** Abgelaufen, benutzt oder fremd – ein neuer Link muss her. */
    data class Failed(val message: String) : MagicLinkOutcome
}

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: AuthApi,
    private val credentialStore: CredentialStore,
    private val sessionManager: SessionManager,
    private val notifications: NotificationsRepository,
) {
    val instanceUrl: String? get() = credentialStore.instanceUrl
    val lastLogin: String? get() = credentialStore.lastLogin

    /** Instanz-URL speichern (vor dem Login, damit der Host-Interceptor greift). */
    fun setInstanceUrl(url: String) {
        val normalized = normalizeUrl(url)
        if (normalized != credentialStore.instanceUrl) {
            // Anderer Server: nichts vom alten mitnehmen.
            credentialStore.clearToken()
            credentialStore.pendingMagicLink = null
        }
        credentialStore.instanceUrl = normalized
    }

    suspend fun login(username: String, password: String): LoginOutcome =
        withContext(Dispatchers.IO) {
            try {
                val res = api.login(username, password)
                credentialStore.lastLogin = username
                when {
                    res.mfaRequired && !res.mfaToken.isNullOrBlank() ->
                        LoginOutcome.MfaRequired(res.mfaToken)
                    !res.accessToken.isNullOrBlank() -> {
                        loggedIn(res.accessToken)
                        LoginOutcome.Success
                    }
                    else -> LoginOutcome.Error("Unerwartete Antwort vom Server")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LoginOutcome.Error(mapError(e))
            }
        }

    suspend fun loginMfa(mfaToken: String, code: String): LoginOutcome =
        withContext(Dispatchers.IO) {
            try {
                val token = api.loginMfa(MfaLoginRequest(mfaToken, code.replace(" ", "")))
                loggedIn(token.accessToken)
                LoginOutcome.Success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LoginOutcome.Error(mapError(e))
            }
        }

    suspend fun exchangeAppToken(url: String, token: String): LoginOutcome =
        withContext(Dispatchers.IO) {
            try {
                setInstanceUrl(url)
                val res = api.exchangeAppToken(AppTokenExchangeRequest(token))
                loggedIn(res.accessToken)
                LoginOutcome.Success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                credentialStore.clearToken()
                LoginOutcome.Error(mapError(e))
            }
        }

    // --- Anmeldung per E-Mail-Link ----------------------------------------

    /** Die laufende Anfrage, falls es eine gibt und sie zum eingestellten Server gehört. */
    val pendingMagicLink: PendingMagicLink?
        get() = credentialStore.pendingMagicLink
            ?.takeIf { !it.isExpired && it.instanceUrl == credentialStore.instanceUrl }

    /** Fordert den Link an. Gibt die laufende Anfrage zurück oder eine Fehlermeldung. */
    suspend fun requestMagicLink(email: String): Result<PendingMagicLink> =
        withContext(Dispatchers.IO) {
            val url = credentialStore.instanceUrl
                ?: return@withContext Result.failure(IllegalStateException("Keine Instanz eingestellt"))
            try {
                val verifier = randomVerifier()
                val res = api.requestMagicLink(
                    MagicLinkRequest(
                        email = email.trim(),
                        codeChallenge = challengeOf(verifier),
                        deviceName = deviceName(),
                    ),
                )
                val pending = PendingMagicLink(
                    instanceUrl = url,
                    requestId = res.requestId,
                    codeVerifier = verifier,
                    matchCode = res.matchCode,
                    email = email.trim(),
                    expiresAtMillis = System.currentTimeMillis() + res.expiresInSeconds * 1000L,
                )
                credentialStore.pendingMagicLink = pending
                credentialStore.lastLogin = email.trim()
                Result.success(pending)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(IllegalStateException(mapApiError(e)))
            }
        }

    /**
     * Löst die laufende Anfrage ein. Mit [linkToken] (Link auf diesem Gerät
     * geöffnet) sofort, ohne nur nachfragen, ob anderswo bestätigt wurde.
     */
    suspend fun redeemMagicLink(requestId: String? = null, linkToken: String? = null): MagicLinkOutcome =
        withContext(Dispatchers.IO) {
            val pending = credentialStore.pendingMagicLink
            if (pending == null || pending.isExpired) {
                credentialStore.pendingMagicLink = null
                return@withContext MagicLinkOutcome.Failed(
                    "Zu diesem Link gibt es auf diesem Gerät keine offene Anmeldung. " +
                        "Bitte in der App einen neuen Link anfordern.",
                )
            }
            if (requestId != null && requestId != pending.requestId) {
                return@withContext MagicLinkOutcome.Failed(
                    "Das ist ein älterer Link. Bitte den Link aus der neuesten Mail öffnen.",
                )
            }
            // Der Link gehört zu dem Server, bei dem er angefordert wurde.
            credentialStore.instanceUrl = pending.instanceUrl
            try {
                val res = api.exchangeMagicLink(
                    MagicLinkExchangeRequest(pending.requestId, pending.codeVerifier, linkToken),
                )
                when (res.status) {
                    "ok" -> {
                        credentialStore.pendingMagicLink = null
                        val token = res.accessToken
                            ?: return@withContext MagicLinkOutcome.Failed("Unerwartete Antwort vom Server")
                        loggedIn(token)
                        MagicLinkOutcome.Success
                    }
                    "mfa_required" -> {
                        credentialStore.pendingMagicLink = null
                        MagicLinkOutcome.MfaRequired(res.mfaToken.orEmpty())
                    }
                    else -> MagicLinkOutcome.Pending(res.pollIntervalSeconds ?: 3)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                if (e.code() == 410 || e.code() == 401) credentialStore.pendingMagicLink = null
                MagicLinkOutcome.Failed(mapApiError(e))
            } catch (e: IOException) {
                // Kurz kein Netz: weiter nachfragen statt aufzugeben.
                MagicLinkOutcome.Pending(5)
            } catch (e: Exception) {
                MagicLinkOutcome.Failed(mapApiError(e))
            }
        }

    fun cancelMagicLink() {
        credentialStore.pendingMagicLink = null
    }

    /** Passwort vergessen – der Server antwortet bewusst immer gleich. */
    suspend fun forgotPassword(login: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val r = api.forgotPassword(PasswordForgotRequest(login.trim()))
            if (!r.isSuccessful) throw HttpException(r)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(IllegalStateException(mapApiError(e)))
        }
    }

    suspend fun fetchUser(): UserResponse = withContext(Dispatchers.IO) { api.me() }

    /** Abmelden: Push beim Server abmelden (soweit erreichbar), dann den Token verwerfen. */
    suspend fun logout() {
        notifications.disablePushOnServer()
        credentialStore.clearToken()
        NotificationScheduler.cancelAll(context)
    }

    /** Server wechseln: alles vergessen. */
    suspend fun forgetServer() {
        notifications.disablePushOnServer()
        credentialStore.clearAll()
        NotificationScheduler.cancelAll(context)
    }

    private fun loggedIn(token: String) {
        credentialStore.accessToken = token
        sessionManager.onLoggedIn()
        // Benachrichtigungen waren vielleicht schon eingeschaltet – Abfrage wieder einplanen.
        NotificationScheduler.update(context)
    }

    private fun mapError(e: Exception): String = when (e) {
        is HttpException -> when (e.code()) {
            401 -> "Falsche Anmeldedaten oder Code"
            403 -> "Benutzer deaktiviert"
            423 -> "Zu viele Fehlversuche – kurz gesperrt"
            else -> mapApiError(e)
        }
        else -> mapApiError(e)
    }

    companion object {
        /** https erzwingen, Leerraum und abschließenden Schrägstrich weg. */
        fun normalizeUrl(url: String): String = url.trim().trimEnd('/')

        private fun randomVerifier(): String {
            val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
            return base64Url(bytes)
        }

        /** PKCE S256: base64url(sha256(verifier)). */
        fun challengeOf(verifier: String): String =
            base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))

        private fun base64Url(bytes: ByteArray): String =
            Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

        fun deviceName(): String {
            val maker = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
            val model = Build.MODEL.orEmpty()
            return (if (model.startsWith(maker, ignoreCase = true)) model else "$maker $model")
                .trim().take(80).ifBlank { "Android-Gerät" }
        }
    }
}
