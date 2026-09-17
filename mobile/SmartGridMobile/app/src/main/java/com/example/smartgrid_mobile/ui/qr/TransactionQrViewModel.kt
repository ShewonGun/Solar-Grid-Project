/* ============================================================================
 * File        : TransactionQrViewModel.kt
 * Purpose     : Backs the transaction QR screen. Loads the prosumer's upcoming
 *               bookings, keeps only the approved ones the Web API has issued a
 *               QR token for, and tracks which of them is on screen.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.qr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartgrid_mobile.data.ApiResult
import com.example.smartgrid_mobile.data.remote.ReservationDto
import com.example.smartgrid_mobile.data.remote.ReservationStatuses
import com.example.smartgrid_mobile.data.remote.StationDto
import com.example.smartgrid_mobile.data.repository.ReservationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionQrUiState(
    val loading: Boolean = false,
    /** Approved bookings that carry a QR token, newest slot first. */
    val codes: List<ReservationDto> = emptyList(),
    val stations: List<StationDto> = emptyList(),
    val selectedId: String? = null,
    val errorMessage: String? = null
) {
    /** The booking whose QR code is on screen, or null when there is none. */
    val selected: ReservationDto?
        get() = codes.firstOrNull { it.id == selectedId } ?: codes.firstOrNull()
}

class TransactionQrViewModel(private val repository: ReservationRepository) : ViewModel() {

    private val _state = MutableStateFlow(TransactionQrUiState())
    val state: StateFlow<TransactionQrUiState> = _state.asStateFlow()

    /** Booking asked for by the caller, applied once the list has arrived. */
    private var requestedId: String? = null

    init {
        load()
    }

    /**
     * Remembers the booking the caller opened the screen for. The list may not
     * have loaded yet, so the choice is applied again after every load.
     */
    fun preselect(reservationId: String?) {
        if (reservationId.isNullOrBlank()) return
        requestedId = reservationId
        applyRequestedSelection()
    }

    /** Switches the screen to another approved booking. */
    fun onSelect(reservationId: String) {
        _state.update { it.copy(selectedId = reservationId) }
    }

    /** Reloads the approved bookings, plus the node names on the first pass. */
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorMessage = null) }

            // Node names are cosmetic, so a failure here leaves the raw station id.
            if (_state.value.stations.isEmpty()) {
                (repository.stations() as? ApiResult.Success)?.let { result ->
                    _state.update { it.copy(stations = result.data) }
                }
            }

            when (val result = repository.upcoming()) {
                is ApiResult.Success -> _state.update { current ->
                    current.copy(loading = false, codes = withQrCode(result.data))
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, errorMessage = result.message)
                }
            }

            applyRequestedSelection()
        }
    }

    /** Resolves a station id to its display name, falling back to the raw id. */
    fun stationName(stationId: String?): String {
        if (stationId.isNullOrBlank()) return "-"
        return _state.value.stations.firstOrNull { it.id == stationId }?.stationName ?: stationId
    }

    /** Keeps only bookings the service has approved and issued a token for. */
    private fun withQrCode(reservations: List<ReservationDto>): List<ReservationDto> =
        reservations.filter {
            it.status == ReservationStatuses.APPROVED && !it.qrToken.isNullOrBlank()
        }

    /** Selects the requested booking once it is present in the loaded list. */
    private fun applyRequestedSelection() {
        val id = requestedId ?: return
        _state.update { current ->
            if (current.codes.any { it.id == id }) current.copy(selectedId = id) else current
        }
    }
}
