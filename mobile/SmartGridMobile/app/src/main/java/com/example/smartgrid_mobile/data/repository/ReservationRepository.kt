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
import com.example.smartgrid_mobile.data.remote.CancelReservationRequest
import com.example.smartgrid_mobile.data.remote.CreateReservationRequest
import com.example.smartgrid_mobile.data.remote.ReservationDto
import com.example.smartgrid_mobile.data.remote.SlotDto
import com.example.smartgrid_mobile.data.remote.SmartGridApi
import com.example.smartgrid_mobile.data.remote.StationDto
import com.example.smartgrid_mobile.data.remote.UpdateReservationRequest

class ReservationRepository(private val api: SmartGridApi) {

    /** Lists the grid nodes, used to name and filter slots on the booking screen. */
    suspend fun stations(): ApiResult<List<StationDto>> =
        apiCall({ api.getStations() })

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

    /** Pending and approved bookings that have not ended yet. */
    suspend fun upcoming(): ApiResult<List<ReservationDto>> =
        apiCall({ api.getUpcomingReservations() })

    /** Completed, cancelled and already-ended bookings. */
    suspend fun history(): ApiResult<List<ReservationDto>> =
        apiCall({ api.getReservationHistory() })

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
