/* ============================================================================
 * File        : NodesMapViewModel.kt
 * Purpose     : Backs the nearby grid nodes map. Asks the Web API for the nodes
 *               around the device when a location is available, and falls back
 *               to the full node list when it is not.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartgrid_mobile.data.ApiResult
import com.example.smartgrid_mobile.data.location.DeviceLocation
import com.example.smartgrid_mobile.data.map
import com.example.smartgrid_mobile.data.location.DeviceLocationProvider
import com.example.smartgrid_mobile.data.remote.StationDto
import com.example.smartgrid_mobile.data.repository.ReservationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One node on the map, with its distance when the device location is known. */
data class MapNode(
    val station: StationDto,
    val distanceKm: Double?
) {
    /** True when the node carries usable coordinates to plot. */
    val hasPosition: Boolean
        get() = station.latitude != null && station.longitude != null
}

data class NodesMapUiState(
    val loading: Boolean = false,
    val nodes: List<MapNode> = emptyList(),
    val location: DeviceLocation? = null,
    val locationDenied: Boolean = false,
    val selectedId: String? = null,
    val errorMessage: String? = null
) {
    /** Only nodes with coordinates can be drawn; the rest stay in the list below. */
    val plottable: List<MapNode>
        get() = nodes.filter { it.hasPosition }

    /** The node whose details panel is open, or null when none is selected. */
    val selected: MapNode?
        get() = nodes.firstOrNull { it.station.id == selectedId }
}

class NodesMapViewModel(
    private val repository: ReservationRepository,
    private val locationProvider: DeviceLocationProvider
) : ViewModel() {

    private companion object {
        /** Search radius for the nearby lookup, wide enough to cover a city. */
        const val SEARCH_RADIUS_KM = 25.0
    }

    private val _state = MutableStateFlow(NodesMapUiState())
    val state: StateFlow<NodesMapUiState> = _state.asStateFlow()

    init {
        load()
    }

    /**
     * Loads the nodes. With a location the API sorts them by distance; without
     * one the full active list is shown instead, so the map is never empty just
     * because the permission was refused.
     */
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorMessage = null) }

            val location = locationProvider.current()
            val result = if (location != null) {
                repository.nearbyStations(
                    location.latitude,
                    location.longitude,
                    SEARCH_RADIUS_KM
                ).map { list -> list.map { MapNode(it.station, it.distanceKm) } }
            } else {
                repository.stations().map { list -> list.map { MapNode(it, null) } }
            }

            when (result) {
                is ApiResult.Success -> _state.update { current ->
                    current.copy(
                        loading = false,
                        nodes = result.data,
                        location = location,
                        locationDenied = !locationProvider.hasPermission()
                    )
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(
                        loading = false,
                        errorMessage = result.message,
                        location = location,
                        locationDenied = !locationProvider.hasPermission()
                    )
                }
            }
        }
    }

    /** Opens the details panel for a node tapped on the map or in the list. */
    fun onNodeSelected(stationId: String) {
        _state.update { it.copy(selectedId = stationId) }
    }

    /** Closes the details panel. */
    fun onSelectionCleared() {
        _state.update { it.copy(selectedId = null) }
    }

    /** Clears the error banner once the screen has shown it. */
    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
