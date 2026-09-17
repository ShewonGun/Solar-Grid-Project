/* ============================================================================
 * File        : BookingSummaryViewModel.kt
 * Purpose     : Backs the summary shown after a booking is created, changed or
 *               cancelled. Reads the booking back from the Web API so the screen
 *               confirms what the service actually stored, not what was sent.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartgrid_mobile.data.ApiResult
import com.example.smartgrid_mobile.data.remote.ReservationDto
import com.example.smartgrid_mobile.data.remote.StationDto
import com.example.smartgrid_mobile.data.repository.ReservationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The action the summary is confirming. */
enum class BookingAction { CREATED, UPDATED, CANCELLED }

data class BookingSummaryUiState(
    val loading: Boolean = false,
    val reservation: ReservationDto? = null,
    val stations: List<StationDto> = emptyList(),
    val errorMessage: String? = null
)

class BookingSummaryViewModel(private val repository: ReservationRepository) : ViewModel() {

    private val _state = MutableStateFlow(BookingSummaryUiState())
    val state: StateFlow<BookingSummaryUiState> = _state.asStateFlow()

    /** Loads the booking behind the summary, along with the node names. */
    fun load(reservationId: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorMessage = null) }

            // Node names are cosmetic, so a failure here leaves the raw station id.
            if (_state.value.stations.isEmpty()) {
                (repository.stations() as? ApiResult.Success)?.let { result ->
                    _state.update { it.copy(stations = result.data) }
                }
            }

            when (val result = repository.reservation(reservationId)) {
                is ApiResult.Success -> _state.update {
                    it.copy(loading = false, reservation = result.data)
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, errorMessage = result.message)
                }
            }
        }
    }

    /** Resolves a station id to its display name, falling back to the raw id. */
    fun stationName(stationId: String?): String {
        if (stationId.isNullOrBlank()) return "-"
        return _state.value.stations.firstOrNull { it.id == stationId }?.stationName ?: stationId
    }
}
