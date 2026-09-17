/* ============================================================================
 * File        : MyBookingsScreen.kt
 * Purpose     : Shows the prosumer their upcoming and past energy reservations,
 *               and lets them change or cancel one while the Web API still
 *               allows it (at least 12 hours before the slot starts).
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.booking

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartgrid_mobile.data.remote.ReservationDto
import com.example.smartgrid_mobile.data.remote.ReservationStatuses
import com.example.smartgrid_mobile.data.remote.ReservationTypes
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
import com.example.smartgrid_mobile.ui.common.StatusChip
import com.example.smartgrid_mobile.ui.common.formatKWh
import com.example.smartgrid_mobile.ui.common.formatWindow
import com.example.smartgrid_mobile.ui.common.reservationStatusVisuals
import com.example.smartgrid_mobile.ui.common.reservationTypeLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
    viewModel: MyBookingsViewModel,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
    onShowQr: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val upcoming = state.tab == BookingsTab.UPCOMING

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My bookings", fontWeight = FontWeight.SemiBold) },
                // Sits on the page background, matching the other prosumer tabs.
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
            PageHeaderCard(
                icon = if (upcoming) Icons.AutoMirrored.Filled.EventNote else Icons.Default.History,
                title = when {
                    state.loading -> "Loading bookings"
                    state.visible.size == 1 -> "1 booking"
                    else -> "${state.visible.size} bookings"
                },
                subtitle = if (upcoming) "Pending and approved" else "Completed and cancelled",
                // The 12-hour rule is enforced by the API; showing it explains the
                // cards that offer no actions.
                footnote = if (upcoming) {
                    "Changes and cancellations close 12 hours before a slot starts."
                } else {
                    null
                }
            )

            // ---- Tabs ------------------------------------------------------
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillChip(
                    label = "Upcoming",
                    selected = upcoming,
                    onClick = { viewModel.onTabChange(BookingsTab.UPCOMING) },
                    modifier = Modifier.weight(1f)
                )
                PillChip(
                    label = "History",
                    selected = !upcoming,
                    onClick = { viewModel.onTabChange(BookingsTab.HISTORY) },
                    modifier = Modifier.weight(1f)
                )
            }

            // ---- Bookings --------------------------------------------------
            when {
                state.loading -> CenteredBox {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp)
                    )
                }

                state.visible.isEmpty() -> CenteredBox {
                    EmptyStateBlock(
                        icon = Icons.Default.EventBusy,
                        title = if (upcoming) "No upcoming bookings" else "No past bookings",
                        body = if (upcoming) {
                            "Reserve an energy slot from the Book tab and it will appear here."
                        } else {
                            "Completed and cancelled bookings are kept here once a slot has passed."
                        }
                    )
                }

                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(state.visible, key = { it.id }) { reservation ->
                        BookingCard(
                            reservation = reservation,
                            stationName = viewModel.stationName(reservation.stationId),
                            canModify = viewModel.canModify(reservation),
                            lockedNote = viewModel.lockedNote(reservation),
                            working = state.working,
                            onEdit = { viewModel.onEditRequested(reservation) },
                            onCancel = { viewModel.onCancelRequested(reservation) },
                            onShowQr = { onShowQr(reservation.id) }
                        )
                    }
                }
            }
        }
    }

    // ---- Cancel confirmation ----------------------------------------------
    state.cancelling?.let { reservation ->
        val colors = MaterialTheme.colorScheme
        AlertDialog(
            onDismissRequest = { if (!state.working) viewModel.onCancelDismissed() },
            shape = RoundedCornerShape(24.dp),
            containerColor = colors.surface,
            icon = {
                IconBadge(
                    icon = Icons.Default.EventBusy,
                    container = colors.errorContainer,
                    tint = colors.error,
                    size = 48.dp
                )
            },
            title = {
                Text(
                    text = "Cancel this booking?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ReservationSummary(
                        stationName = viewModel.stationName(reservation.stationId),
                        window = formatWindow(
                            reservation.reservationStart,
                            reservation.reservationEnd
                        ),
                        detail = "${reservationTypeLabel(reservation.type)} - " +
                            formatKWh(reservation.energyKWh)
                    )
                    Text(
                        text = "The slot is released for other prosumers and this cannot " +
                            "be undone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    FormField(
                        value = state.cancelReason,
                        onValueChange = viewModel::onCancelReasonChange,
                        label = "Reason (optional)",
                        enabled = !state.working,
                        singleLine = false
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmCancel,
                    enabled = !state.working,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = colors.onError
                    )
                ) {
                    if (state.working) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = colors.onError,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text("Cancel booking", fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onCancelDismissed, enabled = !state.working) {
                    Text("Keep it")
                }
            }
        )
    }

    // ---- Edit dialog -------------------------------------------------------
    state.editing?.let { reservation ->
        val colors = MaterialTheme.colorScheme
        AlertDialog(
            onDismissRequest = { if (!state.working) viewModel.onEditDismissed() },
            shape = RoundedCornerShape(24.dp),
            containerColor = colors.surface,
            icon = {
                IconBadge(
                    icon = Icons.AutoMirrored.Filled.EventNote,
                    container = colors.primaryContainer,
                    tint = colors.primary,
                    size = 48.dp
                )
            },
            title = {
                Text(
                    text = "Change booking",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ReservationSummary(
                        stationName = viewModel.stationName(reservation.stationId),
                        window = formatWindow(
                            reservation.reservationStart,
                            reservation.reservationEnd
                        ),
                        detail = "Currently ${reservationTypeLabel(reservation.type).lowercase()}" +
                            " - ${formatKWh(reservation.energyKWh)}"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel("Direction")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SelectableOption(
                                icon = Icons.Default.Upload,
                                label = "Drop off",
                                selected = state.editType == ReservationTypes.DROP_OFF,
                                enabled = !state.working,
                                onClick = {
                                    viewModel.onEditTypeChange(ReservationTypes.DROP_OFF)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            SelectableOption(
                                icon = Icons.Default.BatteryChargingFull,
                                label = "Charge",
                                selected = state.editType == ReservationTypes.CHARGING,
                                enabled = !state.working,
                                onClick = {
                                    viewModel.onEditTypeChange(ReservationTypes.CHARGING)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    FormField(
                        value = state.editEnergyKWh,
                        onValueChange = viewModel::onEditEnergyChange,
                        label = "Energy (kWh)",
                        enabled = !state.working,
                        isError = state.editEnergyError != null,
                        supportingText = state.editEnergyError
                            ?: "Must fit within the slot's capacity.",
                        keyboardType = KeyboardType.Decimal
                    )

                    // Changing an approved booking costs the prosumer its approval,
                    // so the trade-off is stated before they commit to it.
                    if (reservation.status == ReservationStatuses.APPROVED) {
                        Surface(
                            color = colors.errorContainer,
                            contentColor = colors.onErrorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "This booking is already approved. Saving a change " +
                                    "sends it back for approval and revokes the current QR code.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmEdit,
                    enabled = !state.working,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (state.working) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = colors.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text("Save changes", fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onEditDismissed, enabled = !state.working) {
                    Text("Cancel")
                }
            }
        )
    }
}

/** One reservation, with its actions when the API would still accept them. */
@Composable
private fun BookingCard(
    reservation: ReservationDto,
    stationName: String,
    canModify: Boolean,
    lockedNote: String?,
    working: Boolean,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onShowQr: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val visuals = reservationStatusVisuals(reservation.status)
    val approved = reservation.status == ReservationStatuses.APPROVED

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
                        text = stationName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatWindow(
                            reservation.reservationStart,
                            reservation.reservationEnd
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
                StatusChip(
                    text = visuals.label,
                    container = visuals.container,
                    content = visuals.content
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaPill(
                    icon = if (reservation.type == ReservationTypes.CHARGING) {
                        Icons.Default.BatteryChargingFull
                    } else {
                        Icons.Default.Upload
                    },
                    text = reservationTypeLabel(reservation.type)
                )
                MetaPill(icon = Icons.Default.Bolt, text = formatKWh(reservation.energyKWh))
            }

            // The code itself lives on the QR screen; this card only opens it.
            if (approved && !reservation.qrToken.isNullOrBlank()) {
                OutlinedButton(
                    onClick = onShowQr,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, colors.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.QrCode2,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Show transaction QR", fontWeight = FontWeight.SemiBold)
                }
            }

            if (!reservation.cancellationReason.isNullOrBlank()) {
                Text(
                    text = "Reason: ${reservation.cancellationReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            if (canModify) {
                HorizontalDivider(color = colors.outline)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onEdit,
                        enabled = !working,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Change", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !working,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, colors.outline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                }
            } else if (lockedNote != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = lockedNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Tinted recap of the reservation a dialog is about to act on. */
@Composable
private fun ReservationSummary(stationName: String, window: String, detail: String) {
    val colors = MaterialTheme.colorScheme
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
                Text(text = window, style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(text = detail, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
