/* ============================================================================
 * File        : VerifiedTransferScreen.kt
 * Purpose     : Shows the booking the Web API returned for a scanned QR code so
 *               the grid operator can check it against the prosumer in front of
 *               them, then finalise the energy transfer as done.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.operator

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartgrid_mobile.data.remote.ReservationTypes
import com.example.smartgrid_mobile.ui.common.BannerTone
import com.example.smartgrid_mobile.ui.common.IconBadge
import com.example.smartgrid_mobile.ui.common.InfoRow
import com.example.smartgrid_mobile.ui.common.MessageBanner
import com.example.smartgrid_mobile.ui.common.MetaPill
import com.example.smartgrid_mobile.ui.common.PageHeaderCard
import com.example.smartgrid_mobile.ui.common.PrimaryButton
import com.example.smartgrid_mobile.ui.common.SectionCard
import com.example.smartgrid_mobile.ui.common.SectionLabel
import com.example.smartgrid_mobile.ui.common.StatusChip
import com.example.smartgrid_mobile.ui.common.formatKWh
import com.example.smartgrid_mobile.ui.common.formatWindow
import com.example.smartgrid_mobile.ui.common.reservationStatusVisuals
import com.example.smartgrid_mobile.ui.common.reservationTypeLabel
import com.example.smartgrid_mobile.ui.theme.successColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifiedTransferScreen(
    viewModel: OperatorViewModel,
    onScanAnother: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val reservation = state.reservation
    val completed = state.stage == OperatorStage.COMPLETED
    val success = successColors()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (completed) "Transfer done" else "Verified code",
                        fontWeight = FontWeight.SemiBold
                    )
                },
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
                icon = if (completed) Icons.Default.CheckCircle else Icons.Default.VerifiedUser,
                title = if (completed) "Transfer finalised" else "Code verified",
                subtitle = reservation?.let { viewModel.stationName(it.stationId) } ?: "-",
                footnote = if (completed) {
                    "The booking is now marked as completed for the prosumer."
                } else {
                    "Check these details against the prosumer before finalising."
                }
            )

            if (reservation == null) {
                // Only reachable if the screen outlives its job, e.g. after a reset.
                Text(
                    text = "This job is no longer loaded. Scan the code again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // ---- Who and what ------------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Booking")
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(
                                icon = if (reservation.type == ReservationTypes.CHARGING) {
                                    Icons.Default.BatteryChargingFull
                                } else {
                                    Icons.Default.Upload
                                },
                                container = MaterialTheme.colorScheme.primaryContainer,
                                tint = MaterialTheme.colorScheme.primary,
                                size = 40.dp
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reservationTypeLabel(reservation.type),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = formatWindow(
                                        reservation.reservationStart,
                                        reservation.reservationEnd
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val visuals = reservationStatusVisuals(reservation.status)
                            StatusChip(
                                text = visuals.label,
                                container = visuals.container,
                                content = visuals.content
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetaPill(
                                icon = Icons.Default.BatteryChargingFull,
                                text = formatKWh(reservation.energyKWh)
                            )
                        }

                        InfoRow("Prosumer NIC", reservation.prosumerNic)
                        InfoRow("Grid node", viewModel.stationName(reservation.stationId))
                        InfoRow("Booking ref", reservation.id)
                    }
                }

                // ---- Actions -----------------------------------------------
                if (completed) {
                    Surface(
                        color = success.container,
                        contentColor = success.onContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Energy transfer recorded against this booking.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    PrimaryButton(text = "Scan another code", onClick = onScanAnother)
                    OutlinedButton(
                        onClick = onDone,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back to operator home")
                    }
                } else {
                    PrimaryButton(
                        text = "Finalise transfer",
                        onClick = viewModel::completeTransfer,
                        loading = state.stage == OperatorStage.COMPLETING
                    )
                    OutlinedButton(
                        onClick = onScanAnother,
                        enabled = !state.working,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel and scan again")
                    }
                }
            }
        }
    }
}
