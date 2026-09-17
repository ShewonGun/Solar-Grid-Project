/* ============================================================================
 * File        : AccountStatusVisuals.kt
 * Purpose     : Turns the raw account status string sent by the Web API into a
 *               readable label and a colour pair for the status chip.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.smartgrid_mobile.data.remote.AccountStatus

/** Display label plus chip colours for one account status. */
data class StatusVisuals(
    val label: String,
    val container: Color,
    val content: Color
)

/** Maps an account status onto its chip presentation. */
@Composable
fun statusVisuals(status: String?): StatusVisuals {
    val colors = MaterialTheme.colorScheme
    return when (status) {
        AccountStatus.ACTIVE ->
            StatusVisuals("Active", colors.primaryContainer, colors.onPrimaryContainer)

        AccountStatus.PENDING_ACTIVATION ->
            StatusVisuals("Pending activation", colors.tertiaryContainer, colors.onTertiaryContainer)

        AccountStatus.DEACTIVATION_REQUESTED ->
            StatusVisuals(
                "Deactivation requested",
                colors.tertiaryContainer,
                colors.onTertiaryContainer
            )

        AccountStatus.DEACTIVATED ->
            StatusVisuals("Deactivated", colors.errorContainer, colors.onErrorContainer)

        else -> StatusVisuals(
            status ?: "Unknown",
            colors.surfaceVariant,
            colors.onSurfaceVariant
        )
    }
}

/** The explanatory note shown under the status chip, or null when nothing is pending. */
fun statusNote(status: String?): String? = when (status) {
    AccountStatus.PENDING_ACTIVATION ->
        "Your account is waiting for a backoffice officer to activate it. " +
            "Reservation features unlock once it is approved."

    AccountStatus.DEACTIVATION_REQUESTED ->
        "You have asked for this account to be deactivated. " +
            "Only a backoffice officer can reactivate it afterwards."

    AccountStatus.DEACTIVATED ->
        "This account is deactivated. Contact a backoffice officer to have it reactivated."

    else -> null
}
