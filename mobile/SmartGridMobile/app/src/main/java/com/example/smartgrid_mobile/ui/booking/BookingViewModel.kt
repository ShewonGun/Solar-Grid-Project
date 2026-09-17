/* ============================================================================
 * File        : BookingViewModel.kt
 * Purpose     : Backs the slot-booking screen. Loads the grid nodes and the
 *               slots still open for booking, holds the station filter and the
 *               booking form, and posts the reservation. Only the shape of the
 *               input is checked here - the 7-day window, slot availability and
 *               account rules are decided by the Web API.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartgrid_mobile.data.ApiResult
import com.example.smartgrid_mobile.data.remote.ReservationTypes
import com.example.smartgrid_mobile.data.remote.SlotDto
import com.example.smartgrid_mobile.data.remote.StationDto
import com.example.smartgrid_mobile.data.repository.ReservationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookingUiState(
    val loading: Boolean = false,
    val submitting: Boolean = false,
    val stations: List<StationDto> = emptyList(),
    val slots: List<SlotDto> = emptyList(),
    /** Null means "every node"; otherwise the station the list is filtered to. */
    val stationFilter: String? = null,
    /** The slot the booking dialog is open for, or null when it is closed. */
    val selectedSlot: SlotDto? = null,
    val type: String = ReservationTypes.DROP_OFF,
    val energyKWh: String = "",
    val energyError: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class BookingViewModel(private val repository: ReservationRepository) : ViewModel() {

    private val _state = MutableStateFlow(BookingUiState())
    val state: StateFlow<BookingUiState> = _state.asStateFlow()

    init {
        load()
    }

    /** Loads the grid nodes once and the bookable slots for the current filter. */
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorMessage = null) }

            // The node list only names and filters the slots, so a failure here
            // is not fatal - the slots still render with their raw station id.
            if (_state.value.stations.isEmpty()) {
                (repository.stations() as? ApiResult.Success)?.let { result ->
                    _state.update { it.copy(stations = result.data) }
                }
            }

            when (val result = repository.bookableSlots(_state.value.stationFilter)) {
                is ApiResult.Success -> _state.update {
                    it.copy(loading = false, slots = result.data.sortedBy { slot -> slot.startTime })
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, slots = emptyList(), errorMessage = result.message)
                }
            }
        }
    }

    /** Switches the station filter and reloads; null means every node. */
    fun onStationFilterChange(stationId: String?) {
        if (_state.value.stationFilter == stationId) return
        _state.update { it.copy(stationFilter = stationId) }
        load()
    }

    /** Opens the booking dialog for a slot, pre-filling the slot's full capacity. */
    fun onSlotSelected(slot: SlotDto) {
        _state.update {
            it.copy(
                selectedSlot = slot,
                type = ReservationTypes.DROP_OFF,
                energyKWh = slot.capacityKWh?.let { capacity -> trimCapacity(capacity) }.orEmpty(),
                energyError = null,
                errorMessage = null
            )
        }
    }

    /** Closes the booking dialog without booking. */
    fun onDialogDismissed() {
        _state.update { it.copy(selectedSlot = null, energyError = null) }
    }

    /** Switches between dropping energy off and drawing it out. */
    fun onTypeChange(type: String) {
        _state.update { it.copy(type = type) }
    }

    /** Updates the energy amount and clears its error. */
    fun onEnergyChange(value: String) {
        _state.update { it.copy(energyKWh = value, energyError = null) }
    }

    /** Clears the banners once the screen has shown them. */
    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, successMessage = null) }
    }

    /** Validates the amount, books the slot and refreshes the list on success. */
    fun submit() {
        val current = _state.value
        val slot = current.selectedSlot ?: return

        val amount = current.energyKWh.trim().toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _state.update { it.copy(energyError = "Enter an amount greater than 0 kWh.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(submitting = true, energyError = null, errorMessage = null) }

            when (val result = repository.createReservation(slot.id, current.type, amount)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            submitting = false,
                            selectedSlot = null,
                            successMessage = "Slot booked. It stays pending until a grid " +
                                "operator approves it, and the QR code appears then."
                        )
                    }
                    // The slot is now Reserved, so pull a fresh list.
                    load()
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(submitting = false, errorMessage = result.message)
                }
            }
        }
    }

    /** Looks up a readable node name for a slot, falling back to the raw id. */
    fun stationName(stationId: String): String =
        _state.value.stations.firstOrNull { it.id == stationId }?.stationName
            ?: stationId

    /** Drops the trailing ".0" so the pre-filled amount reads as a plain number. */
    private fun trimCapacity(capacity: Double): String =
        if (capacity % 1.0 == 0.0) capacity.toInt().toString() else capacity.toString()
}
