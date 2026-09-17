/* ============================================================================
 * File        : ServiceLocator.kt
 * Purpose     : Minimal manual dependency container. Keeps the app free of any
 *               injection framework while giving every screen one shared
 *               repository, API client and SQLite session store.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.core

import android.content.Context
import com.example.smartgrid_mobile.data.local.SessionStore
import com.example.smartgrid_mobile.data.remote.ApiClient
import com.example.smartgrid_mobile.data.repository.AuthRepository
import com.example.smartgrid_mobile.data.repository.ReservationRepository

object ServiceLocator {

    private lateinit var appContext: Context

    /** Local SQLite-backed session cache, created lazily on first use. */
    val sessionStore: SessionStore by lazy { SessionStore(appContext) }

    /** Retrofit implementation of the SmartGrid Web API. */
    private val api by lazy { ApiClient.create(sessionStore) }

    /** Repository shared by the authentication and prosumer view models. */
    val authRepository: AuthRepository by lazy { AuthRepository(api, sessionStore) }

    /** Repository behind the slot-booking screen. */
    val reservationRepository: ReservationRepository by lazy { ReservationRepository(api) }

    /** Called once from the Application class before any screen is created. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }
}
