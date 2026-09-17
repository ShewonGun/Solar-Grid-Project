/* ============================================================================
 * File        : NodesMapScreen.kt
 * Purpose     : Plots the solar grid nodes on a Google Map from the coordinates
 *               held by the Web API, and shows a node's details when its marker
 *               or list row is selected. Location access is optional.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartgrid_mobile.BuildConfig
import com.example.smartgrid_mobile.ui.common.BannerTone
import com.example.smartgrid_mobile.ui.common.CenteredBox
import com.example.smartgrid_mobile.ui.common.EmptyStateBlock
import com.example.smartgrid_mobile.ui.common.IconBadge
import com.example.smartgrid_mobile.ui.common.MessageBanner
import com.example.smartgrid_mobile.ui.common.MetaPill
import com.example.smartgrid_mobile.ui.common.PageHeaderCard
import com.example.smartgrid_mobile.ui.common.SectionLabel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

/** Fallback camera target (Colombo) used until nodes or a device location arrive. */
private val DefaultTarget = LatLng(6.9271, 79.8612)

/** Zoom levels: one for a city overview, a closer one once a node is picked. */
private const val OVERVIEW_ZOOM = 11f
private const val NODE_ZOOM = 14f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodesMapScreen(
    viewModel: NodesMapViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedNode = state.selected
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DefaultTarget, OVERVIEW_ZOOM)
    }

    // Asking on entry keeps the flow simple: granting reloads with distances,
    // refusing leaves the full node list on screen.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.load() }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Centres on the device when known, otherwise on the first node that has
    // coordinates, so the map never opens on an empty ocean.
    LaunchedEffect(state.location, state.plottable.firstOrNull()?.station?.id) {
        val target = state.location?.let { LatLng(it.latitude, it.longitude) }
            ?: state.plottable.firstOrNull()?.let { LatLng(it.station.latitude!!, it.station.longitude!!) }

        if (target != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(target, OVERVIEW_ZOOM)
            )
        }
    }

    // Moves the camera onto whichever node the user picked.
    LaunchedEffect(state.selectedId) {
        val node = state.selected ?: return@LaunchedEffect
        if (!node.hasPosition) return@LaunchedEffect
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(node.station.latitude!!, node.station.longitude!!),
                NODE_ZOOM
            )
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Nearby nodes", fontWeight = FontWeight.SemiBold) },
                // Sits on the page background, matching the rest of the app.
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::load, enabled = !state.loading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MessageBanner(state.errorMessage, BannerTone.ERROR)

            // Without a key the SDK draws an empty grid, which looks like a bug.
            if (!BuildConfig.HAS_MAPS_KEY) {
                MessageBanner(
                    "No Google Maps API key is configured, so the map cannot draw. " +
                        "Add MAPS_API_KEY to local.properties and rebuild.",
                    BannerTone.INFO
                )
            }

            PageHeaderCard(
                icon = Icons.Default.MyLocation,
                title = when {
                    state.loading -> "Loading nodes"
                    state.nodes.isEmpty() -> "No nodes found"
                    state.nodes.size == 1 -> "1 grid node"
                    else -> "${state.nodes.size} grid nodes"
                },
                subtitle = if (state.location != null) {
                    "Nearest to you first"
                } else {
                    "All active nodes"
                },
                footnote = if (state.locationDenied) {
                    "Location is off, so distances are hidden. Turn it on to sort by " +
                        "how near each node is."
                } else {
                    null
                }
            )

            MapPanel(
                state = state,
                cameraPositionState = cameraPositionState,
                onNodeSelected = viewModel::onNodeSelected
            )

            when {
                state.loading -> CenteredBox {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }

                state.nodes.isEmpty() -> CenteredBox {
                    EmptyStateBlock(
                        icon = Icons.Default.LocationOff,
                        title = "No nodes found",
                        body = "No active grid nodes were returned for this area. " +
                            "Backoffice staff register nodes in the web application."
                    )
                }

                selectedNode != null -> NodeDetailsCard(
                    node = selectedNode,
                    onClose = viewModel::onSelectionCleared
                )

                else -> NodeList(
                    nodes = state.nodes,
                    onNodeSelected = viewModel::onNodeSelected
                )
            }
        }
    }
}

/** The map itself, with one marker per node that has coordinates. */
@Composable
private fun MapPanel(
    state: NodesMapUiState,
    cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    onNodeSelected: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            // The blue dot only appears once the permission has been granted.
            properties = MapProperties(isMyLocationEnabled = state.location != null),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)
        ) {
            state.plottable.forEach { node ->
                Marker(
                    state = MarkerState(
                        position = LatLng(node.station.latitude!!, node.station.longitude!!)
                    ),
                    title = node.station.stationName ?: node.station.id,
                    snippet = node.distanceKm?.let { "%.1f km away".format(it) },
                    onClick = {
                        onNodeSelected(node.station.id)
                        // False lets the map show its own info window as well.
                        false
                    }
                )
            }
        }
    }
}

/** Tappable list of every node, shown while no marker is selected. */
@Composable
private fun NodeList(nodes: List<MapNode>, onNodeSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Grid nodes")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(nodes, key = { it.station.id }) { node ->
                NodeRow(node = node, onClick = { onNodeSelected(node.station.id) })
            }
        }
    }
}

/** One node in the list: name, distance when known, and its headline capacity. */
@Composable
private fun NodeRow(node: MapNode, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, colors.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(
                icon = Icons.Default.Bolt,
                container = colors.primaryContainer,
                tint = colors.primary,
                size = 40.dp
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = node.station.stationName ?: node.station.id,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = node.distanceKm?.let { "%.1f km away".format(it) }
                        ?: node.station.operatingSchedule.orEmpty().ifBlank { "Active node" },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

/** Details panel for the selected node, replacing the list while it is open. */
@Composable
private fun NodeDetailsCard(node: MapNode, onClose: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val station = node.station

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Selected node")
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, colors.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        icon = Icons.Default.Bolt,
                        container = colors.primaryContainer,
                        tint = colors.primary,
                        size = 40.dp
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = station.stationName ?: station.id,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = node.distanceKm?.let { "%.1f km away".format(it) }
                                ?: "Distance unavailable",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close details")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetaPill(
                        icon = Icons.Default.Bolt,
                        text = station.capacityKWh?.let { "%.0f kWh".format(it) } ?: "-"
                    )
                    MetaPill(
                        icon = Icons.Default.BatteryChargingFull,
                        text = "${station.totalBatterySlots ?: 0} bays"
                    )
                }

                if (!station.operatingSchedule.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = station.operatingSchedule,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
