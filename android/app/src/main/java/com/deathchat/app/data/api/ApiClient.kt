package com.deathchat.app.data.api

import com.deathchat.app.data.local.SessionPreferences
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient(private val sessionPreferences: SessionPreferences) {

    private var currentBaseUrl: String = sessionPreferences.getServerUrl()
    private var retrofit: Retrofit = buildRetrofit(currentBaseUrl)
    private var api: DeathChatApi = retrofit.create(DeathChatApi::class.java)

    fun getApi(): DeathChatApi {
        val configuredUrl = sessionPreferences.getServerUrl()
        if (configuredUrl != currentBaseUrl) {
            currentBaseUrl = configuredUrl
            retrofit = buildRetrofit(currentBaseUrl)
            api = retrofit.create(DeathChatApi::class.java)
        }
        return api
    }

    private fun buildRetrofit(baseUrl: String): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()

            val sessionId = sessionPreferences.getSessionId()
            if (!sessionId.isNullOrBlank()) {
                requestBuilder.header("x-session-id", sessionId)
            }

            requestBuilder.header("Content-Type", "application/json")
            chain.proceed(requestBuilder.build())
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

        val safeUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(safeUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
