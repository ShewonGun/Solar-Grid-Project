/* ============================================================================
 * File        : ReservationVisuals.kt
 * Purpose     : Turns the raw reservation status and type strings sent by the
 *               Web API into readable labels and chip colours, so the booking
 *               screens present them the same way everywhere.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.example.smartgrid_mobile.data.remote.ReservationStatuses
import com.example.smartgrid_mobile.data.remote.ReservationTypes
import com.example.smartgrid_mobile.ui.theme.successColors

/** Maps a reservation status onto its chip presentation. */
@Composable
fun reservationStatusVisuals(status: String?): StatusVisuals {
    val colors = MaterialTheme.colorScheme
    val success = successColors()
    return when (status) {
        ReservationStatuses.PENDING ->
            StatusVisuals("Pending", colors.tertiaryContainer, colors.onTertiaryContainer)

        // Green, matching the "active" account chip rather than the brand orange.
        ReservationStatuses.APPROVED ->
            StatusVisuals("Approved", success.container, success.onContainer)

        ReservationStatuses.COMPLETED ->
            StatusVisuals("Completed", colors.secondaryContainer, colors.onSecondaryContainer)

        ReservationStatuses.CANCELLED ->
            StatusVisuals("Cancelled", colors.errorContainer, colors.onErrorContainer)

        else -> StatusVisuals(
            status ?: "Unknown",
            colors.surfaceVariant,
            colors.onSurfaceVariant
        )
    }
}

/** Readable label for the direction of the energy transfer. */
fun reservationTypeLabel(type: String?): String = when (type) {
    ReservationTypes.DROP_OFF -> "Drop off"
    ReservationTypes.CHARGING -> "Charge"
    else -> type ?: "-"
}
