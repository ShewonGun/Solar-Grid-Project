/* ============================================================================
 * File        : BookingSummaryScreen.kt
 * Purpose     : Confirmation shown after a booking is created, changed or
 *               cancelled. Reads the booking back from the Web API and states
 *               what happens next, so every action ends on a summary.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import com.example.smartgrid_mobile.data.remote.ReservationStatuses
import com.example.smartgrid_mobile.ui.common.BannerTone
import com.example.smartgrid_mobile.ui.common.CenteredBox
import com.example.smartgrid_mobile.ui.common.InfoRow
import com.example.smartgrid_mobile.ui.common.MessageBanner
import com.example.smartgrid_mobile.ui.common.PageHeaderCard
import com.example.smartgrid_mobile.ui.common.PrimaryButton
import com.example.smartgrid_mobile.ui.common.SectionCard
import com.example.smartgrid_mobile.ui.common.SectionLabel
import com.example.smartgrid_mobile.ui.common.StatusChip
import com.example.smartgrid_mobile.ui.common.formatKWh
import com.example.smartgrid_mobile.ui.common.formatWindow
import com.example.smartgrid_mobile.ui.common.reservationStatusVisuals
import com.example.smartgrid_mobile.ui.common.reservationTypeLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingSummaryScreen(
    viewModel: BookingSummaryViewModel,
    reservationId: String,
    action: BookingAction,
    onViewBookings: () -> Unit,
    onShowQr: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val reservation = state.reservation

    LaunchedEffect(reservationId) { viewModel.load(reservationId) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Booking summary", fontWeight = FontWeight.SemiBold) },
                // Sits on the page background, matching the rest of the app.
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            MessageBanner(state.errorMessage, BannerTone.ERROR)

            PageHeaderCard(
                icon = if (action == BookingAction.CANCELLED) {
                    Icons.Default.EventBusy
                } else {
                    Icons.Default.CheckCircle
                },
                title = when (action) {
                    BookingAction.CREATED -> "Booking confirmed"
                    BookingAction.UPDATED -> "Booking updated"
                    BookingAction.CANCELLED -> "Booking cancelled"
                },
                subtitle = reservation
                    ?.let { viewModel.stationName(it.stationId) }
                    ?: "Reading it back from the service",
                footnote = whatHappensNext(action)
            )

            when {
                state.loading -> CenteredBox {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }

                reservation == null -> Text(
                    text = "The booking could not be read back. Open My bookings to " +
                        "check its current state.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel("What the service recorded")
                        SectionCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = formatWindow(
                                        reservation.reservationStart,
                                        reservation.reservationEnd
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                val visuals = reservationStatusVisuals(reservation.status)
                                StatusChip(
                                    text = visuals.label,
                                    container = visuals.container,
                                    content = visuals.content
                                )
                            }

                            InfoRow("Grid node", viewModel.stationName(reservation.stationId))
                            InfoRow("Direction", reservationTypeLabel(reservation.type))
                            InfoRow("Energy", formatKWh(reservation.energyKWh))
                            InfoRow("Booking ref", reservation.id)

                            if (!reservation.cancellationReason.isNullOrBlank()) {
                                InfoRow("Reason", reservation.cancellationReason)
                            }
                        }
                    }

                    // The QR code only exists once an operator has approved it.
                    if (reservation.status == ReservationStatuses.APPROVED &&
                        !reservation.qrToken.isNullOrBlank()
                    ) {
                        PrimaryButton(
                            text = "Show transaction QR",
                            onClick = { onShowQr(reservation.id) }
                        )
                    }

                    PrimaryButton(text = "View my bookings", onClick = onViewBookings)
                    OutlinedButton(
                        onClick = onDone,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back to home")
                    }
                }
            }
        }
    }
}

/** One line explaining what the service does next, which differs per action. */
private fun whatHappensNext(action: BookingAction): String = when (action) {
    BookingAction.CREATED ->
        "It stays pending until a grid operator approves it. The QR code appears then."

    BookingAction.UPDATED ->
        "A changed booking goes back to pending, so it needs approving again and its " +
            "old QR code no longer works."

    BookingAction.CANCELLED ->
        "The slot has been released for other prosumers and this cannot be undone."
}
