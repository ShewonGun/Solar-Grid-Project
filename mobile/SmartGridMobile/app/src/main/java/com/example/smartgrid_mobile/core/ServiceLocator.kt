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
import com.example.smartgrid_mobile.data.local.ReservationCache
import com.example.smartgrid_mobile.data.local.SessionStore
import com.example.smartgrid_mobile.data.local.StationCache
import com.example.smartgrid_mobile.data.location.DeviceLocationProvider
import com.example.smartgrid_mobile.data.remote.ApiClient
import com.example.smartgrid_mobile.data.repository.AuthRepository
import com.example.smartgrid_mobile.data.repository.OperatorRepository
import com.example.smartgrid_mobile.data.repository.ReservationRepository

object ServiceLocator {

    private lateinit var appContext: Context

    /** Local SQLite-backed session cache, created lazily on first use. */
    val sessionStore: SessionStore by lazy { SessionStore(appContext) }

    /** Retrofit implementation of the SmartGrid Web API. */
    private val api by lazy { ApiClient.create(sessionStore) }

    /** Repository shared by the authentication and prosumer view models. */
    val authRepository: AuthRepository by lazy {
        AuthRepository(api, sessionStore, stationCache, reservationCache)
    }

    /** Local SQLite copies of the reference data last served by the Web API. */
    val stationCache: StationCache by lazy { StationCache(appContext) }
    val reservationCache: ReservationCache by lazy { ReservationCache(appContext) }

    /** Repository behind the slot-booking screen, reading through the local caches. */
    val reservationRepository: ReservationRepository by lazy {
        ReservationRepository(api, stationCache, reservationCache)
    }

    /** Repository behind grid operator mode: QR verification and completion. */
    val operatorRepository: OperatorRepository by lazy { OperatorRepository(api) }

    /** Device position source used by the node map; location is always optional. */
    val locationProvider: DeviceLocationProvider by lazy { DeviceLocationProvider(appContext) }

    /** Called once from the Application class before any screen is created. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }
}
