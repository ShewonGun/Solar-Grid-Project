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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartgrid_mobile.data.remote.ReservationTypes
import com.example.smartgrid_mobile.data.remote.SlotDto
import com.example.smartgrid_mobile.ui.common.BannerTone
import com.example.smartgrid_mobile.ui.common.CenteredBox
import com.example.smartgrid_mobile.ui.common.EmptyStateBlock
import com.example.smartgrid_mobile.ui.common.FormField
import com.example.smartgrid_mobile.ui.common.IconBadge
import com.example.smartgrid_mobile.ui.common.MessageBanner
import com.example.smartgrid_mobile.ui.common.MetaPill
import com.example.smartgrid_mobile.ui.common.PageHeaderCard
import com.example.smartgrid_mobile.ui.common.PillChip
import com.example.smartgrid_mobile.ui.common.SectionLabel
import com.example.smartgrid_mobile.ui.common.SelectableOption
import com.example.smartgrid_mobile.ui.common.formatKWh
import com.example.smartgrid_mobile.ui.common.formatWindow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSlotScreen(
    viewModel: BookingViewModel,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
    onBooked: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // A successful booking ends on the summary screen.
    LaunchedEffect(state.bookedReservationId) {
        val id = state.bookedReservationId ?: return@LaunchedEffect
        viewModel.onSummaryShown()
        onBooked(id)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Reserve a slot", fontWeight = FontWeight.SemiBold) },
                // Sits on the page background, matching the home screen.
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = viewModel::load, enabled = !state.loading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        bottomBar = bottomBar
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MessageBanner(state.errorMessage, BannerTone.ERROR)
            MessageBanner(state.successMessage, BannerTone.SUCCESS)

            // ---- Header ----------------------------------------------------
            SlotsHeader(
                count = state.slots.size,
                loading = state.loading,
                nodeName = state.stationFilter?.let { viewModel.stationName(it) }
            )

            // ---- Grid node filter ------------------------------------------
            if (state.stations.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Grid node")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            PillChip(
                                label = "All nodes",
                                selected = state.stationFilter == null,
                                onClick = { viewModel.onStationFilterChange(null) }
                            )
                        }
                        items(state.stations, key = { it.id }) { station ->
                            PillChip(
                                label = station.stationName ?: station.id,
                                selected = state.stationFilter == station.id,
                                onClick = { viewModel.onStationFilterChange(station.id) }
                            )
                        }
                    }
                }
            }

            // ---- Slots -----------------------------------------------------
            when {
                state.loading -> CenteredBox {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }

                state.slots.isEmpty() -> CenteredBox {
                    EmptyStateBlock(
                        icon = Icons.Default.EventBusy,
                        title = "No open slots",
                        body = "Nothing is available to book at this node right now. " +
                            "Slots are published by the grid operators."
                    )
                }

                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
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

/** Brand-tinted header carrying the slot count and the booking window rule. */
@Composable
private fun SlotsHeader(count: Int, loading: Boolean, nodeName: String?) {
    PageHeaderCard(
        icon = Icons.Default.Bolt,
        title = when {
            loading -> "Loading slots"
            count == 0 -> "No open slots"
            count == 1 -> "1 slot open"
            else -> "$count slots open"
        },
        subtitle = nodeName?.let { "At $it" } ?: "Across every grid node",
        // The API enforces this; saying it up front avoids a rejected booking.
        footnote = "Reservations must start within the next 7 days."
    )
}

/** One bookable slot: when it runs, how much it holds, and which node it is at. */
@Composable
private fun SlotCard(slot: SlotDto, stationName: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, colors.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                        text = stationName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatWindow(slot.startTime, slot.endTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaPill(
                    icon = Icons.Default.BatteryChargingFull,
                    text = "Bay ${slot.batterySlotNumber ?: "-"}"
                )
                MetaPill(
                    icon = Icons.Default.Bolt,
                    text = "Holds ${formatKWh(slot.capacityKWh)}"
                )
            }
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
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        shape = RoundedCornerShape(24.dp),
        containerColor = colors.surface,
        icon = {
            IconBadge(
                icon = Icons.Default.BatteryChargingFull,
                container = colors.primaryContainer,
                tint = colors.primary,
                size = 48.dp
            )
        },
        title = {
            Text(
                text = "Book this slot",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // ---- What is being booked --------------------------------
                Surface(
                    color = colors.secondaryContainer,
                    contentColor = colors.onSecondaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stationName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = formatWindow(slot.startTime, slot.endTime),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Bay ${slot.batterySlotNumber ?: "-"} - " +
                                    "holds ${formatKWh(slot.capacityKWh)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // ---- Direction -------------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Direction")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SelectableOption(
                            icon = Icons.Default.Upload,
                            label = "Drop off",
                            selected = type == ReservationTypes.DROP_OFF,
                            enabled = !submitting,
                            onClick = { onTypeChange(ReservationTypes.DROP_OFF) },
                            modifier = Modifier.weight(1f)
                        )
                        SelectableOption(
                            icon = Icons.Default.BatteryChargingFull,
                            label = "Charge",
                            selected = type == ReservationTypes.CHARGING,
                            enabled = !submitting,
                            onClick = { onTypeChange(ReservationTypes.CHARGING) },
                            modifier = Modifier.weight(1f)
                        )
                    }
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
            Button(
                onClick = onConfirm,
                enabled = !submitting,
                shape = RoundedCornerShape(10.dp)
            ) {
                if (submitting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = colors.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text("Confirm booking", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) { Text("Cancel") }
        }
    )
}

