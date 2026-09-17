/* ============================================================================
 * File        : MyBookingsViewModel.kt
 * Purpose     : Backs the "My bookings" screen. Loads the prosumer's upcoming
 *               and past reservations, and drives the edit and cancel actions.
 *               The 12-hour notice rule is decided by the Web API; this class
 *               only reads the start time to choose what to offer on screen.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartgrid_mobile.data.ApiResult
import com.example.smartgrid_mobile.data.remote.ReservationDto
import com.example.smartgrid_mobile.data.remote.ReservationStatuses
import com.example.smartgrid_mobile.data.remote.StationDto
import com.example.smartgrid_mobile.data.repository.ReservationRepository
import com.example.smartgrid_mobile.ui.common.hoursUntil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The two lists the screen switches between. */
enum class BookingsTab { UPCOMING, HISTORY }

data class MyBookingsUiState(
    val tab: BookingsTab = BookingsTab.UPCOMING,
    val loading: Boolean = false,
    val working: Boolean = false,
    val upcoming: List<ReservationDto> = emptyList(),
    val history: List<ReservationDto> = emptyList(),
    val stations: List<StationDto> = emptyList(),
    /** True when the lists came from SQLite because the service was unreachable. */
    val showingCached: Boolean = false,
    /** Free-text search over node name, status, direction and booking reference. */
    val query: String = "",
    /** Null means every status; otherwise only bookings in that status. */
    val statusFilter: String? = null,
    /** Booking the last completed action applies to, handed to the summary screen. */
    val completedActionId: String? = null,
    val completedAction: String? = null,
    /** The booking the cancel dialog is open for, or null when it is closed. */
    val cancelling: ReservationDto? = null,
    val cancelReason: String = "",
    /** The booking the edit dialog is open for, or null when it is closed. */
    val editing: ReservationDto? = null,
    val editType: String = "",
    val editEnergyKWh: String = "",
    val editEnergyError: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    /** Every booking on the selected tab, before the search and status filters. */
    val tabBookings: List<ReservationDto>
        get() = if (tab == BookingsTab.UPCOMING) upcoming else history

    /** The bookings actually listed: the tab, narrowed by status and search text. */
    val visible: List<ReservationDto>
        get() {
            val needle = query.trim().lowercase()
            return tabBookings.filter { reservation ->
                val matchesStatus = statusFilter == null || reservation.status == statusFilter
                matchesStatus && (needle.isEmpty() || reservation.matches(needle))
            }
        }

    /** True when a filter is hiding bookings that the tab does hold. */
    val filtered: Boolean
        get() = query.isNotBlank() || statusFilter != null

    /** Matches the search text against the fields a prosumer would search by. */
    private fun ReservationDto.matches(needle: String): Boolean {
        val stationName = stations.firstOrNull { it.id == stationId }?.stationName
        return listOfNotNull(stationName, stationId, status, type, id)
            .any { it.lowercase().contains(needle) }
    }
}

class MyBookingsViewModel(private val repository: ReservationRepository) : ViewModel() {

    private companion object {
        /** Mirrors EnergyReservationService.MinNoticeHours, for display only. */
        const val MIN_NOTICE_HOURS = 12.0
    }

    private val _state = MutableStateFlow(MyBookingsUiState())
    val state: StateFlow<MyBookingsUiState> = _state.asStateFlow()

    init {
        load()
    }

    /** Reloads both lists, plus the node names on the first pass. */
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorMessage = null) }

            // Node names are cosmetic, so a failure here leaves the raw station id.
            if (_state.value.stations.isEmpty()) {
                (repository.stations() as? ApiResult.Success)?.let { result ->
                    _state.update { it.copy(stations = result.data) }
                }
            }

            val upcoming = repository.upcoming()
            val history = repository.history()

            _state.update { current ->
                current.copy(
                    loading = false,
                    upcoming = (upcoming as? ApiResult.Success)?.data ?: current.upcoming,
                    history = (history as? ApiResult.Success)?.data ?: current.history,
                    // Either list falling back to SQLite means the screen is stale.
                    showingCached = (upcoming as? ApiResult.Success)?.fromCache == true ||
                        (history as? ApiResult.Success)?.fromCache == true,
                    errorMessage = (upcoming as? ApiResult.Failure)?.message
                        ?: (history as? ApiResult.Failure)?.message
                )
            }
        }
    }

    /** Switches between the upcoming and history lists, clearing the status filter. */
    fun onTabChange(tab: BookingsTab) {
        _state.update {
            it.copy(
                tab = tab,
                // The two tabs hold different statuses, so keeping one would
                // silently empty the list.
                statusFilter = null,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    /** Updates the free-text search over the listed bookings. */
    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
    }

    /** Narrows the list to one status, or clears the filter when given null. */
    fun onStatusFilterChange(status: String?) {
        _state.update { it.copy(statusFilter = status) }
    }

    /** Clears both the search text and the status filter. */
    fun clearFilters() {
        _state.update { it.copy(query = "", statusFilter = null) }
    }

    /** Clears the pending summary hand-off once the screen has navigated. */
    fun onSummaryShown() {
        _state.update { it.copy(completedActionId = null, completedAction = null) }
    }

    /** Clears the banners once the screen has shown them. */
    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, successMessage = null) }
    }

    // ---- Cancelling --------------------------------------------------------

    /** Opens the cancel confirmation for a booking. */
    fun onCancelRequested(reservation: ReservationDto) {
        _state.update { it.copy(cancelling = reservation, cancelReason = "", errorMessage = null) }
    }

    /** Updates the optional reason recorded against the cancellation. */
    fun onCancelReasonChange(value: String) {
        _state.update { it.copy(cancelReason = value) }
    }

    /** Closes the cancel dialog without cancelling. */
    fun onCancelDismissed() {
        _state.update { it.copy(cancelling = null, cancelReason = "") }
    }

    /** Cancels the booking; the API rejects it if under 12 hours' notice. */
    fun confirmCancel() {
        val reservation = _state.value.cancelling ?: return
        val reason = _state.value.cancelReason

        viewModelScope.launch {
            _state.update { it.copy(working = true, errorMessage = null) }

            when (val result = repository.cancelReservation(reservation.id, reason)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            working = false,
                            cancelling = null,
                            cancelReason = "",
                            completedActionId = result.data.id,
                            completedAction = BookingAction.CANCELLED.name
                        )
                    }
                    load()
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(working = false, cancelling = null, errorMessage = result.message)
                }
            }
        }
    }

    // ---- Editing -----------------------------------------------------------

    /** Opens the edit dialog pre-filled with the booking's current values. */
    fun onEditRequested(reservation: ReservationDto) {
        _state.update {
            it.copy(
                editing = reservation,
                editType = reservation.type.orEmpty(),
                editEnergyKWh = reservation.energyKWh?.let(::trimAmount).orEmpty(),
                editEnergyError = null,
                errorMessage = null
            )
        }
    }

    /** Switches the edited booking between dropping off and charging. */
    fun onEditTypeChange(type: String) {
        _state.update { it.copy(editType = type) }
    }

    /** Updates the edited amount and clears its error. */
    fun onEditEnergyChange(value: String) {
        _state.update { it.copy(editEnergyKWh = value, editEnergyError = null) }
    }

    /** Closes the edit dialog without saving. */
    fun onEditDismissed() {
        _state.update { it.copy(editing = null, editEnergyError = null) }
    }

    /** Saves the edited direction and amount; the API re-checks every rule. */
    fun confirmEdit() {
        val current = _state.value
        val reservation = current.editing ?: return

        val amount = current.editEnergyKWh.trim().toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _state.update { it.copy(editEnergyError = "Enter an amount greater than 0 kWh.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(working = true, editEnergyError = null, errorMessage = null) }

            when (val result = repository.updateReservation(reservation.id, current.editType, amount)) {
                is ApiResult.Success -> {
                    // The summary screen explains that a changed booking goes back
                    // to Pending and loses its QR code.
                    _state.update {
                        it.copy(
                            working = false,
                            editing = null,
                            completedActionId = result.data.id,
                            completedAction = BookingAction.UPDATED.name
                        )
                    }
                    load()
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(working = false, editing = null, errorMessage = result.message)
                }
            }
        }
    }

    // ---- Display helpers ---------------------------------------------------

    /** Looks up a readable node name, falling back to the raw id. */
    fun stationName(stationId: String?): String {
        if (stationId.isNullOrBlank()) return "-"
        return _state.value.stations.firstOrNull { it.id == stationId }?.stationName ?: stationId
    }

    /**
     * True when the screen should offer edit and cancel: the booking is still
     * open and starts far enough ahead. The API makes the real decision, so a
     * stale clock only means a rejected request with a clear message.
     */
    fun canModify(reservation: ReservationDto): Boolean {
        val open = reservation.status == ReservationStatuses.PENDING ||
            reservation.status == ReservationStatuses.APPROVED
        val notice = hoursUntil(reservation.reservationStart) ?: return false
        return open && notice >= MIN_NOTICE_HOURS
    }

    /** Explains why a booking can no longer be changed, or null when it can. */
    fun lockedNote(reservation: ReservationDto): String? {
        if (canModify(reservation)) return null

        val open = reservation.status == ReservationStatuses.PENDING ||
            reservation.status == ReservationStatuses.APPROVED
        if (!open) return null

        return "Starts in under ${MIN_NOTICE_HOURS.toInt()} hours, so it can no longer be " +
            "changed or cancelled."
    }

    /** Drops the trailing ".0" so a whole amount reads as a plain number. */
    private fun trimAmount(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}
