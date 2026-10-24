/**
 * Ersetzt Schema/Host/Port jeder Anfrage durch die konfigurierte Instanz-URL.
 * Dadurch kann Retrofit mit einer Platzhalter-Base-URL gebaut werden und die echte
 * Instanz erst zur Laufzeit (nach Setup) wirken.
 *
 * Läuft der Server unter einem Unterpfad (https://host/logbot), wird der Pfad
 * vorangestellt – vorher ging er verloren und jede Anfrage lief ins Leere.
 */
package de.phytech.logbot.core.network

import de.phytech.logbot.core.auth.CredentialStore
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HostSelectionInterceptor @Inject constructor(
    private val credentialStore: CredentialStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val instance = credentialStore.instanceUrl?.toHttpUrlOrNull()
            ?: return chain.proceed(request)
        val prefix = instance.encodedPath.trimEnd('/')
        val newUrl = request.url.newBuilder()
            .scheme(instance.scheme)
            .host(instance.host)
            .port(instance.port)
            .apply { if (prefix.isNotEmpty()) encodedPath(prefix + request.url.encodedPath) }
            .build()
        return chain.proceed(request.newBuilder().url(newUrl).build())
    }
}
