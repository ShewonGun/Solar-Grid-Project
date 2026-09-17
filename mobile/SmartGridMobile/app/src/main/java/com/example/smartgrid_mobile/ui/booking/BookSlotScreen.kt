/* ============================================================================
 * File        : BookSlotScreen.kt
 * Purpose     : Lets a prosumer browse the energy slots still open for booking,
 *               filter them by grid node, and reserve one as a drop-off or a
 *               charging session. The Web API decides whether the booking is
 *               allowed; this screen only collects and displays.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.booking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartgrid_mobile.data.remote.ReservationTypes
import com.example.smartgrid_mobile.data.remote.SlotDto
import com.example.smartgrid_mobile.ui.common.BannerTone
import com.example.smartgrid_mobile.ui.common.FormField
import com.example.smartgrid_mobile.ui.common.MessageBanner
import com.example.smartgrid_mobile.ui.common.SectionCard
import com.example.smartgrid_mobile.ui.common.SectionLabel
import com.example.smartgrid_mobile.ui.common.formatKWh
import com.example.smartgrid_mobile.ui.common.formatWindow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSlotScreen(
    viewModel: BookingViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Reserve a slot", fontWeight = FontWeight.SemiBold) },
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
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MessageBanner(state.errorMessage, BannerTone.ERROR)
            MessageBanner(state.successMessage, BannerTone.SUCCESS)

            // ---- Grid node filter ------------------------------------------
            if (state.stations.isNotEmpty()) {
                SectionLabel("Grid node")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        NodeChip(
                            label = "All nodes",
                            selected = state.stationFilter == null,
                            onClick = { viewModel.onStationFilterChange(null) }
                        )
                    }
                    items(state.stations, key = { it.id }) { station ->
                        NodeChip(
                            label = station.stationName ?: station.id,
                            selected = state.stationFilter == station.id,
                            onClick = { viewModel.onStationFilterChange(station.id) }
                        )
                    }
                }
            }

            // ---- Slots -----------------------------------------------------
            when {
                state.loading -> CenteredBox {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }

                state.slots.isEmpty() -> CenteredBox {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No open slots",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Nothing is available to book at this node right now. " +
                                "Slots are published by the grid operators.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    SectionLabel("${state.slots.size} slot(s) open")
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(state.slots, key = { it.id }) { slot ->
                            SlotCard(
                                slot = slot,
                                stationName = viewModel.stationName(slot.stationId),
                                onClick = { viewModel.onSlotSelected(slot) }
                            )
                        }
                    }
                }
            }
        }
    }

    // ---- Booking dialog ----------------------------------------------------
    state.selectedSlot?.let { slot ->
        BookingDialog(
            slot = slot,
            stationName = viewModel.stationName(slot.stationId),
            type = state.type,
            energyKWh = state.energyKWh,
            energyError = state.energyError,
            submitting = state.submitting,
            onTypeChange = viewModel::onTypeChange,
            onEnergyChange = viewModel::onEnergyChange,
            onConfirm = viewModel::submit,
            onDismiss = viewModel::onDialogDismissed
        )
    }
}

/** Filter pill for one grid node, plus the "All nodes" entry. */
@Composable
private fun NodeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

/** One bookable slot: when it runs, how much it holds, and which node it is at. */
@Composable
private fun SlotCard(slot: SlotDto, stationName: String, onClick: () -> Unit) {
    SectionCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Bolt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stationName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatWindow(slot.startTime, slot.endTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Bay ${slot.batterySlotNumber ?: "-"} - holds ${formatKWh(slot.capacityKWh)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Collects the transfer direction and the amount, then confirms the booking. */
@Composable
private fun BookingDialog(
    slot: SlotDto,
    stationName: String,
    type: String,
    energyKWh: String,
    energyError: String?,
    submitting: Boolean,
    onTypeChange: (String) -> Unit,
    onEnergyChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        icon = { Icon(Icons.Default.BatteryChargingFull, contentDescription = null) },
        title = { Text("Book this slot") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "$stationName\n${formatWindow(slot.startTime, slot.endTime)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SectionLabel("Direction")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == ReservationTypes.DROP_OFF,
                        onClick = { onTypeChange(ReservationTypes.DROP_OFF) },
                        label = { Text("Drop off") }
                    )
                    FilterChip(
                        selected = type == ReservationTypes.CHARGING,
                        onClick = { onTypeChange(ReservationTypes.CHARGING) },
                        label = { Text("Charge") }
                    )
                }

                FormField(
                    value = energyKWh,
                    onValueChange = onEnergyChange,
                    label = "Energy (kWh)",
                    enabled = !submitting,
                    isError = energyError != null,
                    supportingText = energyError
                        ?: "This bay holds ${formatKWh(slot.capacityKWh)}.",
                    keyboardType = KeyboardType.Decimal
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !submitting) {
                Text(if (submitting) "Booking..." else "Confirm booking")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) { Text("Cancel") }
        }
    )
}

/** Centres a loading spinner or empty-state message in the remaining space. */
@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
