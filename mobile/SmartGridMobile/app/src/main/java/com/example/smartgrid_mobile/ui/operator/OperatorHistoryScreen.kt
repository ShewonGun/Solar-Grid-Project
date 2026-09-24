/* ============================================================================
 * File        : OperatorHistoryScreen.kt
 * Purpose     : Lists the energy transfers this Grid Operator has personally
 *               scanned and finalised, most recent first, so they can look
 *               back on their own completed jobs without asking Backoffice.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-24
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.operator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartgrid_mobile.data.remote.ReservationDto
import com.example.smartgrid_mobile.data.remote.ReservationTypes
import com.example.smartgrid_mobile.ui.common.CenteredBox
import com.example.smartgrid_mobile.ui.common.EmptyStateBlock
import com.example.smartgrid_mobile.ui.common.IconBadge
import com.example.smartgrid_mobile.ui.common.MessageBanner
import com.example.smartgrid_mobile.ui.common.BannerTone
import com.example.smartgrid_mobile.ui.common.MetaPill
import com.example.smartgrid_mobile.ui.common.StatusChip
import com.example.smartgrid_mobile.ui.common.formatDateTime
import com.example.smartgrid_mobile.ui.common.formatKWh
import com.example.smartgrid_mobile.ui.common.reservationStatusVisuals
import com.example.smartgrid_mobile.ui.common.reservationTypeLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorHistoryScreen(
    viewModel: OperatorHistoryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Completed transfers", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
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
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            MessageBanner(message = state.errorMessage, tone = BannerTone.ERROR)
            if (state.errorMessage != null) Spacer(Modifier.width(12.dp))

            when {
                state.loading && state.completed.isEmpty() -> CenteredBox {
                    CircularProgressIndicator()
                }

                state.completed.isEmpty() -> CenteredBox {
                    EmptyStateBlock(
                        icon = Icons.Default.History,
                        title = "No completed transfers yet",
                        body = "Jobs you scan and finalise appear here, most recent first."
                    )
                }

                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.completed, key = { it.id }) { reservation ->
                        CompletedTransferCard(
                            reservation = reservation,
                            stationName = viewModel.stationName(reservation.stationId)
                        )
                    }
                }
            }
        }
    }
}

/** One completed transfer: who it was for, where, and when this operator finalised it. */
@Composable
private fun CompletedTransferCard(reservation: ReservationDto, stationName: String) {
    val colors = MaterialTheme.colorScheme
    val visuals = reservationStatusVisuals(reservation.status)

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
                    icon = Icons.Default.CheckCircle,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Badge,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = reservation.prosumerNic.orEmpty().ifBlank { "-" },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Finalised ${formatDateTime(reservation.completedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}
