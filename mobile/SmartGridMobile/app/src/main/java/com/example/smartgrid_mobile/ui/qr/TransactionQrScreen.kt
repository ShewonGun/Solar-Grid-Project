/* ============================================================================
 * File        : TransactionQrScreen.kt
 * Purpose     : Shows the secure transaction QR code for an approved booking so
 *               the grid operator can scan it at the node. The token comes from
 *               the Web API; this screen only renders and explains it.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.qr

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.QrCode2
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartgrid_mobile.data.remote.ReservationDto
import com.example.smartgrid_mobile.ui.common.BannerTone
import com.example.smartgrid_mobile.ui.common.CenteredBox
import com.example.smartgrid_mobile.ui.common.EmptyStateBlock
import com.example.smartgrid_mobile.ui.common.InfoRow
import com.example.smartgrid_mobile.ui.common.MessageBanner
import com.example.smartgrid_mobile.ui.common.PageHeaderCard
import com.example.smartgrid_mobile.ui.common.PillChip
import com.example.smartgrid_mobile.ui.common.SectionCard
import com.example.smartgrid_mobile.ui.common.SectionLabel
import com.example.smartgrid_mobile.ui.common.formatKWh
import com.example.smartgrid_mobile.ui.common.formatWindow
import com.example.smartgrid_mobile.ui.common.reservationTypeLabel

/** Side of the rendered QR code; large enough to scan from a phone screen. */
private val QrSize = 240.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionQrScreen(
    viewModel: TransactionQrViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    reservationId: String? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Opens on the booking the caller asked for, once that booking has loaded.
    LaunchedEffect(reservationId) { viewModel.preselect(reservationId) }

    val selected = state.selected

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Transaction QR", fontWeight = FontWeight.SemiBold) },
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            MessageBanner(state.errorMessage, BannerTone.ERROR)

            // ---- Header ----------------------------------------------------
            PageHeaderCard(
                icon = Icons.Default.QrCode2,
                title = when {
                    state.loading -> "Loading codes"
                    selected == null -> "No QR codes yet"
                    else -> "Ready to scan"
                },
                subtitle = selected
                    ?.let { viewModel.stationName(it.stationId) }
                    ?: "Approved bookings only",
                // Says what the code is for, so it is not mistaken for a ticket.
                footnote = "Show this code to the grid operator at the node. " +
                    "They scan it to verify and finalise the transfer."
            )

            when {
                state.loading -> CenteredBox {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }

                selected == null -> CenteredBox {
                    EmptyStateBlock(
                        icon = Icons.Default.QrCode2,
                        title = "No QR codes yet",
                        body = "A code appears here once a grid operator approves one " +
                            "of your bookings."
                    )
                }

                else -> {
                    // Only worth choosing between codes when there is more than one.
                    if (state.codes.size > 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionLabel("Approved bookings")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(state.codes, key = { it.id }) { reservation ->
                                    PillChip(
                                        label = viewModel.stationName(reservation.stationId),
                                        selected = reservation.id == selected.id,
                                        onClick = { viewModel.onSelect(reservation.id) }
                                    )
                                }
                            }
                        }
                    }

                    QrCodeCard(token = selected.qrToken.orEmpty())

                    BookingDetails(
                        reservation = selected,
                        stationName = viewModel.stationName(selected.stationId)
                    )
                }
            }
        }
    }
}

/** White panel holding the code itself, kept light so scanners read it in any theme. */
@Composable
private fun QrCodeCard(token: String) {
    val sizePx = with(LocalDensity.current) { QrSize.roundToPx() }
    val bitmap = remember(token, sizePx) { encodeQrCode(token, sizePx) }

    Surface(
        color = Color.White,
        contentColor = Color.Black,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Transaction QR code",
                    modifier = Modifier.size(QrSize)
                )
            } else {
                // Encoding only fails on a malformed token, which is a server problem.
                Text(
                    text = "This booking's code could not be displayed. " +
                        "Refresh, or ask the operator to look it up.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = "Reference ${shortReference(token)}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** The booking the code belongs to, so the prosumer can check it before showing it. */
@Composable
private fun BookingDetails(reservation: ReservationDto, stationName: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Booking")
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
                    fontWeight = FontWeight.Medium
                )
            }

            InfoRow("Grid node", stationName)
            InfoRow("Direction", reservationTypeLabel(reservation.type))
            InfoRow("Energy", formatKWh(reservation.energyKWh))
        }
    }
}

/** Last characters of the token, shown so a code can be named out loud. */
private fun shortReference(token: String): String =
    if (token.length <= 6) token.uppercase() else token.takeLast(6).uppercase()
