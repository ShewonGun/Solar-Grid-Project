/* ============================================================================
 * File        : ReservationRepository.kt
 * Purpose     : Entry point for the energy-trading endpoints: the grid nodes,
 *               the slots a prosumer may still book, and creating a booking.
 *               Every rule (7-day window, slot availability, account status)
 *               is enforced by the Web API, not here.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.data.repository

import com.example.smartgrid_mobile.data.ApiResult
import com.example.smartgrid_mobile.data.local.ReservationCache
import com.example.smartgrid_mobile.data.local.SmartGridDbHelper
import com.example.smartgrid_mobile.data.local.StationCache
import com.example.smartgrid_mobile.data.orCached
import com.example.smartgrid_mobile.data.remote.CancelReservationRequest
import com.example.smartgrid_mobile.data.remote.CreateReservationRequest
import com.example.smartgrid_mobile.data.remote.NearbyStationDto
import com.example.smartgrid_mobile.data.remote.ReservationCountsDto
import com.example.smartgrid_mobile.data.remote.ReservationDto
import com.example.smartgrid_mobile.data.remote.SlotDto
import com.example.smartgrid_mobile.data.remote.SmartGridApi
import com.example.smartgrid_mobile.data.remote.StationDto
import com.example.smartgrid_mobile.data.remote.UpdateReservationRequest

class ReservationRepository(
    private val api: SmartGridApi,
    private val stationCache: StationCache,
    private val reservationCache: ReservationCache
) {

    /**
     * Lists the grid nodes, used to name and filter slots on the booking screen.
     * A successful response refreshes the local copy; a failed one falls back to it.
     */
    suspend fun stations(): ApiResult<List<StationDto>> =
        apiCall({ api.getStations() }, onSuccess = { stationCache.replaceAll(it) })
            .orCached { stationCache.readAll().ifEmpty { null } }

    /** Lists active nodes within [radiusKm] of a point, nearest first, for the map. */
    suspend fun nearbyStations(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): ApiResult<List<NearbyStationDto>> =
        apiCall({ api.getNearbyStations(latitude, longitude, radiusKm) })

    /** Lists the slots open for booking, optionally narrowed to a single station. */
    suspend fun bookableSlots(stationId: String? = null): ApiResult<List<SlotDto>> =
        apiCall({ api.getBookableSlots(stationId) })

    /** Books one slot for the signed-in prosumer. */
    suspend fun createReservation(
        slotId: String,
        type: String,
        energyKWh: Double
    ): ApiResult<ReservationDto> =
        apiCall({ api.createReservation(CreateReservationRequest(slotId, type, energyKWh)) })

    /** Pending and approved-upcoming counts for the prosumer's dashboard. */
    suspend fun dashboardCounts(): ApiResult<ReservationCountsDto> =
        apiCall({ api.getDashboardCounts() })

    /** Reads one booking back from the service, used by the summary screen. */
    suspend fun reservation(id: String): ApiResult<ReservationDto> =
        apiCall(
            { api.getReservation(id) },
            onSuccess = { reservationCache.upsert(SmartGridDbHelper.BUCKET_UPCOMING, it) }
        ).orCached { reservationCache.readById(id) }

    /** Pending and approved bookings that have not ended yet. */
    suspend fun upcoming(): ApiResult<List<ReservationDto>> =
        apiCall(
            { api.getUpcomingReservations() },
            onSuccess = {
                reservationCache.replaceBucket(SmartGridDbHelper.BUCKET_UPCOMING, it)
            }
        ).orCached {
            reservationCache.readBucket(SmartGridDbHelper.BUCKET_UPCOMING).ifEmpty { null }
        }

    /** Completed, cancelled and already-ended bookings. */
    suspend fun history(): ApiResult<List<ReservationDto>> =
        apiCall(
            { api.getReservationHistory() },
            onSuccess = {
                reservationCache.replaceBucket(SmartGridDbHelper.BUCKET_HISTORY, it)
            }
        ).orCached {
            reservationCache.readBucket(SmartGridDbHelper.BUCKET_HISTORY).ifEmpty { null }
        }

    /** Changes the direction and amount of an existing booking. */
    suspend fun updateReservation(
        id: String,
        type: String,
        energyKWh: Double
    ): ApiResult<ReservationDto> =
        apiCall({ api.updateReservation(id, UpdateReservationRequest(type = type, energyKWh = energyKWh)) })

    /** Cancels a booking, optionally recording why. */
    suspend fun cancelReservation(id: String, reason: String?): ApiResult<ReservationDto> =
        apiCall({ api.cancelReservation(id, CancelReservationRequest(reason?.trim()?.ifBlank { null })) })
}
