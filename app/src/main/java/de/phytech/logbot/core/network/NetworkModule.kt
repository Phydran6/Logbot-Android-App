/**
 * Hilt-Modul für die Netzwerk-Schicht.
 *
 * Phase 2b-1: stellt OkHttpClient und Retrofit bereit (Toolchain-Verdrahtung).
 * Die Base-URL ist hier noch ein Platzhalter; in 2b-2 wird sie dynamisch aus dem
 * CredentialStore gelesen (konfigurierte Instanz) und ein Converter + Auth-Interceptor
 * ergänzt.
 */
package de.phytech.logbot.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            // Platzhalter – wird in 2b-2 durch die konfigurierte Instanz-URL ersetzt.
            .baseUrl("https://localhost/")
            .client(client)
            .build()
}
