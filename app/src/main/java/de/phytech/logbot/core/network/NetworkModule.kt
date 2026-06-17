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
import de.phytech.logbot.data.api.AgentsApi
import de.phytech.logbot.data.api.AuthApi
import de.phytech.logbot.data.api.BrandingApi
import de.phytech.logbot.data.api.LogsApi
import de.phytech.logbot.data.api.MonitoringApi
import de.phytech.logbot.data.api.SettingsApi
import de.phytech.logbot.data.api.UsersApi
import de.phytech.logbot.data.api.WebhooksApi
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

    @Provides
    @Singleton
    fun provideLogsApi(retrofit: Retrofit): LogsApi = retrofit.create(LogsApi::class.java)

    @Provides
    @Singleton
    fun provideAgentsApi(retrofit: Retrofit): AgentsApi = retrofit.create(AgentsApi::class.java)

    @Provides
    @Singleton
    fun provideUsersApi(retrofit: Retrofit): UsersApi = retrofit.create(UsersApi::class.java)

    @Provides
    @Singleton
    fun provideWebhooksApi(retrofit: Retrofit): WebhooksApi =
        retrofit.create(WebhooksApi::class.java)

    @Provides
    @Singleton
    fun provideSettingsApi(retrofit: Retrofit): SettingsApi =
        retrofit.create(SettingsApi::class.java)

    @Provides
    @Singleton
    fun provideBrandingApi(retrofit: Retrofit): BrandingApi =
        retrofit.create(BrandingApi::class.java)
}
