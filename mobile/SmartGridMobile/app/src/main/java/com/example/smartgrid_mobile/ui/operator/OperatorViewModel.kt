/* ============================================================================
 * File        : OperatorViewModel.kt
 * Purpose     : Drives grid operator mode: takes the token read from a prosumer's
 *               QR code, verifies it against the Web API and finalises the energy
 *               transfer. Every decision about the token belongs to the service.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.operator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartgrid_mobile.data.ApiResult
import com.example.smartgrid_mobile.data.remote.ReservationDto
import com.example.smartgrid_mobile.data.remote.StationDto
import com.example.smartgrid_mobile.data.repository.OperatorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where the operator is in the scan, verify and finalise sequence. */
enum class OperatorStage { IDLE, VERIFYING, VERIFIED, COMPLETING, COMPLETED }

data class OperatorUiState(
    val stage: OperatorStage = OperatorStage.IDLE,
    /** The token last read from a QR code, kept so the finalise call can reuse it. */
    val scannedToken: String? = null,
    /** The booking the service returned for that token. */
    val reservation: ReservationDto? = null,
    val stations: List<StationDto> = emptyList(),
    val errorMessage: String? = null
) {
    /** True while a request is in flight, so the buttons can be disabled. */
    val working: Boolean
        get() = stage == OperatorStage.VERIFYING || stage == OperatorStage.COMPLETING
}

class OperatorViewModel(private val repository: OperatorRepository) : ViewModel() {

    private val _state = MutableStateFlow(OperatorUiState())
    val state: StateFlow<OperatorUiState> = _state.asStateFlow()

    init {
        loadStations()
    }

    /**
     * Sends a freshly scanned token to the service. The scanner reports one code
     * only, so a second call while a check is running is ignored.
     */
    fun onQrScanned(token: String) {
        if (_state.value.working) return

        _state.update {
            it.copy(
                stage = OperatorStage.VERIFYING,
                scannedToken = token,
                reservation = null,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            when (val result = repository.verifyQr(token)) {
                is ApiResult.Success -> _state.update {
                    it.copy(stage = OperatorStage.VERIFIED, reservation = result.data)
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(stage = OperatorStage.IDLE, errorMessage = result.message)
                }
            }
        }
    }

    /** Finalises the transfer for the verified token, marking the job as done. */
    fun completeTransfer() {
        val token = _state.value.scannedToken ?: return
        if (_state.value.working) return

        _state.update { it.copy(stage = OperatorStage.COMPLETING, errorMessage = null) }

        viewModelScope.launch {
            when (val result = repository.completeTransfer(token)) {
                is ApiResult.Success -> _state.update {
                    it.copy(stage = OperatorStage.COMPLETED, reservation = result.data)
                }

                // Back to the verified state so the operator can try again.
                is ApiResult.Failure -> _state.update {
                    it.copy(stage = OperatorStage.VERIFIED, errorMessage = result.message)
                }
            }
        }
    }

    /** Clears the current job so the scanner can read the next code. */
    fun reset() {
        _state.update {
            it.copy(
                stage = OperatorStage.IDLE,
                scannedToken = null,
                reservation = null,
                errorMessage = null
            )
        }
    }

    /** Clears the error banner once the screen has shown it. */
    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    /** Resolves a station id to its display name, falling back to the raw id. */
    fun stationName(stationId: String?): String {
        if (stationId.isNullOrBlank()) return "-"
        return _state.value.stations.firstOrNull { it.id == stationId }?.stationName ?: stationId
    }

    /** Node names are cosmetic, so a failure here leaves the raw station id. */
    private fun loadStations() {
        viewModelScope.launch {
            (repository.stations() as? ApiResult.Success)?.let { result ->
                _state.update { it.copy(stations = result.data) }
            }
        }
    }
}
