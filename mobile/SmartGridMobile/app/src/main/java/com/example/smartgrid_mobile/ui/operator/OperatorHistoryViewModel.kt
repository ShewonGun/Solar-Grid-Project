/* ============================================================================
 * File        : OperatorHistoryViewModel.kt
 * Purpose     : Drives the grid operator's completed-transfers history screen -
 *               loads the reservations this operator has personally scanned and
 *               finalised, most recent first, plus the node names to show
 *               alongside them.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-24
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.operator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartgrid_mobile.data.ApiResult
import com.example.smartgrid_mobile.data.local.SessionStore
import com.example.smartgrid_mobile.data.remote.ReservationDto
import com.example.smartgrid_mobile.data.remote.StationDto
import com.example.smartgrid_mobile.data.repository.OperatorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OperatorHistoryUiState(
    val loading: Boolean = false,
    val completed: List<ReservationDto> = emptyList(),
    val stations: List<StationDto> = emptyList(),
    val errorMessage: String? = null
)

class OperatorHistoryViewModel(
    private val repository: OperatorRepository,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _state = MutableStateFlow(OperatorHistoryUiState())
    val state: StateFlow<OperatorHistoryUiState> = _state.asStateFlow()

    init {
        load()
    }

    /** Reloads this operator's completed transfers and the node names for them. */
    fun load() {
        val operatorNic = sessionStore.session.value?.user?.nic
        if (operatorNic.isNullOrBlank()) {
            _state.update { it.copy(errorMessage = "Sign in again to load your history.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorMessage = null) }

            // Node names are cosmetic, so a failure here leaves the raw station id.
            if (_state.value.stations.isEmpty()) {
                (repository.stations() as? ApiResult.Success)?.let { result ->
                    _state.update { it.copy(stations = result.data) }
                }
            }

            when (val result = repository.completedHistory(operatorNic)) {
                is ApiResult.Success -> _state.update {
                    it.copy(loading = false, completed = result.data)
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
