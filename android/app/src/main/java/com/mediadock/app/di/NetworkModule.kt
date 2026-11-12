package com.mediadock.app.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.mediadock.app.core.datastore.PreferencesDataStore
import com.mediadock.app.core.network.ApiConstants
import com.mediadock.app.core.network.AuthInterceptor
import com.mediadock.app.core.network.MediaDockApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(ApiConstants.TIMEOUT_CONNECT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(ApiConstants.TIMEOUT_READ_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(ApiConstants.TIMEOUT_WRITE_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideMediaDockApi(
        okHttpClient: OkHttpClient,
        json: Json,
        preferencesDataStore: PreferencesDataStore
    ): MediaDockApi {
        val initialBaseUrl = runBlocking {
            try {
                preferencesDataStore.preferencesFlow.first().serverUrl
            } catch (_: Exception) {
                ApiConstants.DEFAULT_BASE_URL
            }
        }

        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(initialBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(MediaDockApi::class.java)
    }
}
