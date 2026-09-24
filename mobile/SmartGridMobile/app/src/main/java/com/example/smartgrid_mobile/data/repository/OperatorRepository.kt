/* ============================================================================
 * File        : OperatorRepository.kt
 * Purpose     : Entry point for the grid operator endpoints: verifying a scanned
 *               transaction QR code and finalising the energy transfer behind it.
 *               Whether a token is genuine, unused and in date is decided by the
 *               Web API, never by this app.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.data.repository

import com.example.smartgrid_mobile.data.ApiResult
import com.example.smartgrid_mobile.data.remote.QrTokenRequest
import com.example.smartgrid_mobile.data.remote.ReservationDto
import com.example.smartgrid_mobile.data.remote.SmartGridApi
import com.example.smartgrid_mobile.data.remote.StationDto

class OperatorRepository(private val api: SmartGridApi) {

    /** Checks a scanned token against the service and returns the booking it belongs to. */
    suspend fun verifyQr(qrToken: String): ApiResult<ReservationDto> =
        apiCall({ api.verifyQr(QrTokenRequest(qrToken.trim())) })

    /** Finalises the energy transfer for a token the service has already verified. */
    suspend fun completeTransfer(qrToken: String): ApiResult<ReservationDto> =
        apiCall({ api.completeTransfer(QrTokenRequest(qrToken.trim())) })

    /** Lists the grid nodes, used to name the node on a verified booking. */
    suspend fun stations(): ApiResult<List<StationDto>> =
        apiCall({ api.getStations() })

    /** This operator's own completed transfers, most recent first. */
    suspend fun completedHistory(operatorNic: String): ApiResult<List<ReservationDto>> =
        apiCall({ api.getCompletedByOperator(completedBy = operatorNic) })
}
