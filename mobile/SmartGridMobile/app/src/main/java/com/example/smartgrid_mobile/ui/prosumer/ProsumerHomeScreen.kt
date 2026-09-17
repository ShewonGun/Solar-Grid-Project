/* ============================================================================
 * File        : ProsumerHomeScreen.kt
 * Purpose     : Home screen shown to a solar prosumer after sign-in. Presents
 *               the account summary read live from the Web API, the account
 *               actions (edit profile, change password, request deactivation)
 *               and the entry points for the reservation features.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.prosumer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartgrid_mobile.data.remote.AccountStatus
import com.example.smartgrid_mobile.ui.common.BannerTone
import com.example.smartgrid_mobile.ui.common.InfoRow
import com.example.smartgrid_mobile.ui.common.MessageBanner
import com.example.smartgrid_mobile.ui.common.SectionCard
import com.example.smartgrid_mobile.ui.common.SectionLabel
import com.example.smartgrid_mobile.ui.common.StatusChip
import com.example.smartgrid_mobile.ui.common.statusNote
import com.example.smartgrid_mobile.ui.common.statusVisuals

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProsumerHomeScreen(
    viewModel: ProsumerViewModel,
    onEditProfile: () -> Unit,
    onBookSlot: () -> Unit,
    onMyBookings: () -> Unit,
    onChangePassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user by viewModel.user.collectAsStateWithLifecycle()
    var confirmDeactivation by remember { mutableStateOf(false) }

    val visuals = statusVisuals(user?.status)
    val note = statusNote(user?.status)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SmartGrid", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = viewModel::refresh, enabled = !state.refreshing) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = viewModel::logout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
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
            if (state.refreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MessageBanner(state.successMessage, BannerTone.SUCCESS)
                MessageBanner(state.errorMessage, BannerTone.ERROR)
                MessageBanner(note, BannerTone.INFO)

                // ---- Account summary -------------------------------------
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(initials = initialsOf(user?.fullName))
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user?.fullName.orEmpty().ifBlank { "Prosumer" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = user?.email.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    StatusChip(
                        text = visuals.label,
                        container = visuals.container,
                        content = visuals.content
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    InfoRow("NIC", user?.nic)
                    InfoRow("Phone", user?.phone)
                    InfoRow("Address", user?.address)
                    InfoRow(
                        "Solar capacity",
                        user?.solarCapacityKW?.let { formatCapacity(it) }
                    )
                }

                // ---- Energy trading --------------------------------------
                SectionLabel("Energy trading")
                SectionCard {
                    ActionRow(
                        icon = Icons.Default.EventAvailable,
                        title = "Reserve an energy slot",
                        subtitle = "Book a drop-off or charging slot at a grid node",
                        enabled = user?.status == AccountStatus.ACTIVE,
                        onClick = onBookSlot
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    ActionRow(
                        icon = Icons.Default.History,
                        title = "My bookings",
                        subtitle = "Pending, approved and past reservations",
                        enabled = user?.status == AccountStatus.ACTIVE,
                        onClick = onMyBookings
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    ActionRow(
                        icon = Icons.Default.QrCode2,
                        title = "Transaction QR code",
                        subtitle = "Shown to the grid operator once a booking is approved",
                        enabled = false,
                        onClick = {}
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    ActionRow(
                        icon = Icons.Default.Map,
                        title = "Nearby grid nodes",
                        subtitle = "Locate microgrid hubs on the map",
                        enabled = false,
                        onClick = {}
                    )
                }

                // ---- Account ---------------------------------------------
                SectionLabel("Account")
                SectionCard {
                    ActionRow(
                        icon = Icons.Default.Badge,
                        title = "Edit profile",
                        subtitle = "Update contact details and solar capacity",
                        onClick = onEditProfile
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    ActionRow(
                        icon = Icons.Default.Lock,
                        title = "Change password",
                        subtitle = "Set a new sign-in password",
                        onClick = onChangePassword
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    ActionRow(
                        icon = Icons.Default.PersonOff,
                        title = "Request deactivation",
                        subtitle = if (user?.status == AccountStatus.ACTIVE) {
                            "Ask the backoffice to close this account"
                        } else {
                            "Only available while the account is active"
                        },
                        enabled = user?.status == AccountStatus.ACTIVE && !state.working,
                        destructive = true,
                        onClick = { confirmDeactivation = true }
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (confirmDeactivation) {
        DeactivationDialog(
            onConfirm = {
                confirmDeactivation = false
                viewModel.requestDeactivation()
            },
            onDismiss = { confirmDeactivation = false }
        )
    }
}

/** Confirmation dialog shown before the deactivation request is sent. */
@Composable
private fun DeactivationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request deactivation?") },
        text = {
            Text(
                "Your account will be marked as deactivation requested. " +
                    "Only a backoffice officer can reactivate it afterwards."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Request", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Circular monogram standing in for a profile photo. */
@Composable
private fun Avatar(initials: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(50),
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Tappable list row used for every navigation and account action. */
@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    destructive: Boolean = false
) {
    val accent = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        destructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val titleColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        destructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (enabled) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Builds up to two initials from the stored full name. */
private fun initialsOf(fullName: String?): String {
    val parts = fullName?.trim()?.split(Regex("\\s+"))?.filter { it.isNotBlank() }.orEmpty()
    return when {
        parts.isEmpty() -> "SG"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

/** Renders the capacity without a trailing ".0" for whole numbers. */
private fun formatCapacity(value: Double): String {
    val trimmed = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
    return "$trimmed kW"
}
