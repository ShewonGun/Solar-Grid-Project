/* ============================================================================
 * File        : ApiClient.kt
 * Purpose     : Builds the Retrofit/OkHttp stack used to reach the SmartGrid
 *               Web API, attaching the locally stored bearer token to requests.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.data.remote

import com.example.smartgrid_mobile.BuildConfig
import com.example.smartgrid_mobile.data.local.SessionStore
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    /** Creates the API implementation; the token is read per request, never cached. */
    fun create(sessionStore: SessionStore): SmartGridApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor(sessionStore))
            .addInterceptor(loggingInterceptor())
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SmartGridApi::class.java)
    }

    /** Adds the bearer Authorization header whenever a session exists locally. */
    private fun authInterceptor(sessionStore: SessionStore) = Interceptor { chain ->
        val token = sessionStore.token()
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        chain.proceed(request)
    }

    /** Full request/response logging in debug builds only. */
    private fun loggingInterceptor() = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }
}
