/* ============================================================================
 * File        : MyBookingsScreen.kt
 * Purpose     : Shows the prosumer their upcoming and past energy reservations,
 *               and lets them change or cancel one while the Web API still
 *               allows it (at least 12 hours before the slot starts).
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.booking

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.example.smartgrid_mobile.data.remote.ReservationDto
import com.example.smartgrid_mobile.data.remote.ReservationStatuses
import com.example.smartgrid_mobile.data.remote.ReservationTypes
import com.example.smartgrid_mobile.ui.common.BannerTone
import com.example.smartgrid_mobile.ui.common.FormField
import com.example.smartgrid_mobile.ui.common.InfoRow
import com.example.smartgrid_mobile.ui.common.MessageBanner
import com.example.smartgrid_mobile.ui.common.SectionCard
import com.example.smartgrid_mobile.ui.common.SectionLabel
import com.example.smartgrid_mobile.ui.common.StatusChip
import com.example.smartgrid_mobile.ui.common.formatKWh
import com.example.smartgrid_mobile.ui.common.formatWindow
import com.example.smartgrid_mobile.ui.common.reservationStatusVisuals
import com.example.smartgrid_mobile.ui.common.reservationTypeLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
    viewModel: MyBookingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My bookings", fontWeight = FontWeight.SemiBold) },
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
        ) {
            TabRow(selectedTabIndex = state.tab.ordinal) {
                Tab(
                    selected = state.tab == BookingsTab.UPCOMING,
                    onClick = { viewModel.onTabChange(BookingsTab.UPCOMING) },
                    text = { Text("Upcoming") }
                )
                Tab(
                    selected = state.tab == BookingsTab.HISTORY,
                    onClick = { viewModel.onTabChange(BookingsTab.HISTORY) },
                    text = { Text("History") }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(4.dp))
                MessageBanner(state.errorMessage, BannerTone.ERROR)
                MessageBanner(state.successMessage, BannerTone.SUCCESS)

                when {
                    state.loading -> CenteredBox {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    state.visible.isEmpty() -> CenteredBox {
                        EmptyState(tab = state.tab)
                    }

                    else -> LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(state.visible, key = { it.id }) { reservation ->
                            BookingCard(
                                reservation = reservation,
                                stationName = viewModel.stationName(reservation.stationId),
                                canModify = viewModel.canModify(reservation),
                                lockedNote = viewModel.lockedNote(reservation),
                                working = state.working,
                                onEdit = { viewModel.onEditRequested(reservation) },
                                onCancel = { viewModel.onCancelRequested(reservation) }
                            )
                        }
                    }
                }
            }
        }
    }

    // ---- Cancel confirmation ----------------------------------------------
    state.cancelling?.let { reservation ->
        AlertDialog(
            onDismissRequest = { if (!state.working) viewModel.onCancelDismissed() },
            title = { Text("Cancel this booking?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "${viewModel.stationName(reservation.stationId)}\n" +
                            formatWindow(
                                reservation.reservationStart,
                                reservation.reservationEnd
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                TextButton(onClick = viewModel::confirmCancel, enabled = !state.working) {
                    Text(if (state.working) "Cancelling..." else "Cancel booking")
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
        AlertDialog(
            onDismissRequest = { if (!state.working) viewModel.onEditDismissed() },
            title = { Text("Change booking") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "${viewModel.stationName(reservation.stationId)}\n" +
                            formatWindow(
                                reservation.reservationStart,
                                reservation.reservationEnd
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SectionLabel("Direction")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.editType == ReservationTypes.DROP_OFF,
                            onClick = { viewModel.onEditTypeChange(ReservationTypes.DROP_OFF) },
                            label = { Text("Drop off") }
                        )
                        FilterChip(
                            selected = state.editType == ReservationTypes.CHARGING,
                            onClick = { viewModel.onEditTypeChange(ReservationTypes.CHARGING) },
                            label = { Text("Charge") }
                        )
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
                        Text(
                            text = "This booking is already approved. Saving a change sends " +
                                "it back for approval and revokes the current QR code.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmEdit, enabled = !state.working) {
                    Text(if (state.working) "Saving..." else "Save changes")
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
    onCancel: () -> Unit
) {
    val visuals = reservationStatusVisuals(reservation.status)

    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stationName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            StatusChip(
                text = visuals.label,
                container = visuals.container,
                content = visuals.content
            )
        }

        InfoRow("When", formatWindow(reservation.reservationStart, reservation.reservationEnd))
        InfoRow("Direction", reservationTypeLabel(reservation.type))
        InfoRow("Energy", formatKWh(reservation.energyKWh))

        // The token itself belongs on the QR screen; here it is only a signal
        // that the booking is ready to be shown to an operator.
        if (reservation.status == ReservationStatuses.APPROVED && !reservation.qrToken.isNullOrBlank()) {
            Text(
                text = "Approved - your QR code is ready for the grid operator.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (!reservation.cancellationReason.isNullOrBlank()) {
            InfoRow("Reason", reservation.cancellationReason)
        }

        if (canModify) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit, enabled = !working) { Text("Change") }
                TextButton(onClick = onCancel, enabled = !working) { Text("Cancel booking") }
            }
        } else if (lockedNote != null) {
            Text(
                text = lockedNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Wording for an empty list, which differs between the two tabs. */
@Composable
private fun EmptyState(tab: BookingsTab) {
    val title = if (tab == BookingsTab.UPCOMING) "No upcoming bookings" else "No past bookings"
    val body = if (tab == BookingsTab.UPCOMING) {
        "Reserve an energy slot from the home screen and it will appear here."
    } else {
        "Completed and cancelled bookings are kept here once a slot has passed."
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
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
