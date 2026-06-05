/**
 * Hilt-Modul für die Netzwerk-Schicht: Json, OkHttp (mit Host-Selection + Auth +
 * Logging), Retrofit (kotlinx.serialization-Converter) und die Auth-API.
 *
 * Die Base-URL ist ein Platzhalter; der HostSelectionInterceptor setzt zur Laufzeit
 * die konfigurierte Instanz ein.
 */
package de.phytech.logbot.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.phytech.logbot.data.api.AuthApi
import de.phytech.logbot.data.api.MonitoringApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        hostSelection: HostSelectionInterceptor,
        auth: AuthInterceptor,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
        }
        return OkHttpClient.Builder()
            .addInterceptor(hostSelection)
            .addInterceptor(auth)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://localhost/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideMonitoringApi(retrofit: Retrofit): MonitoringApi =
        retrofit.create(MonitoringApi::class.java)
}
