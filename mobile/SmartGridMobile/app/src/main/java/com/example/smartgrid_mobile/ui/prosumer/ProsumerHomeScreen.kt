/* ============================================================================
 * File        : ProsumerHomeScreen.kt
 * Purpose     : Home screen shown to a solar prosumer after sign-in. Opens on a
 *               greeting header carrying the account status and the headline
 *               figures, then a quick-action grid for the reservation features,
 *               the profile details and the account actions.
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProsumerHomeScreen(
    viewModel: ProsumerViewModel,
    onEditProfile: () -> Unit,
    onBookSlot: () -> Unit,
    onMyBookings: () -> Unit,
    onTransactionQr: () -> Unit,
    onNearbyNodes: () -> Unit,
    onChangePassword: () -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user by viewModel.user.collectAsStateWithLifecycle()
    var confirmDeactivation by remember { mutableStateOf(false) }

    val visuals = statusVisuals(user?.status)
    val note = statusNote(user?.status)
    val active = user?.status == AccountStatus.ACTIVE

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("VoltShare", fontWeight = FontWeight.SemiBold) },
                // Matches the page background so the header card below leads the eye.
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = viewModel::refresh, enabled = !state.refreshing) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = viewModel::logout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
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
        ) {
            if (state.refreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                MessageBanner(state.successMessage, BannerTone.SUCCESS)
                MessageBanner(state.errorMessage, BannerTone.ERROR)
                MessageBanner(note, BannerTone.INFO)

                // ---- Greeting header -------------------------------------
                GreetingHeader(
                    name = user?.fullName.orEmpty().ifBlank { "Prosumer" },
                    email = user?.email.orEmpty(),
                    pending = state.counts?.pending,
                    approvedUpcoming = state.counts?.approvedUpcoming,
                    statusLabel = visuals.label,
                    statusContainer = visuals.container,
                    statusContent = visuals.content
                )

                // ---- Energy trading --------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Energy trading")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ActionTile(
                            icon = Icons.Default.EventAvailable,
                            title = "Reserve a slot",
                            subtitle = "Book a drop-off or charge",
                            enabled = active,
                            onClick = onBookSlot,
                            modifier = Modifier.weight(1f)
                        )
                        ActionTile(
                            icon = Icons.Default.History,
                            title = "My bookings",
                            subtitle = "Pending and past",
                            enabled = active,
                            onClick = onMyBookings,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ActionTile(
                            icon = Icons.Default.QrCode2,
                            title = "Transaction QR",
                            subtitle = "Shown to the operator",
                            enabled = active,
                            onClick = onTransactionQr,
                            modifier = Modifier.weight(1f)
                        )
                        ActionTile(
                            icon = Icons.Default.Map,
                            title = "Nearby nodes",
                            subtitle = "Find microgrid hubs",
                            enabled = true,
                            onClick = onNearbyNodes,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ---- Profile details -------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("My details")
                    SectionCard {
                        InfoRow("Email", user?.email)
                        InfoRow("Phone", user?.phone)
                        InfoRow("Address", user?.address)
                        InfoRow(
                            "Solar capacity",
                            user?.solarCapacityKW?.let { formatCapacity(it) }
                        )
                    }
                }

                // ---- Account ---------------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            subtitle = if (active) {
                                "Ask the backoffice to close this account"
                            } else {
                                "Only available while the account is active"
                            },
                            enabled = active && !state.working,
                            destructive = true,
                            onClick = { confirmDeactivation = true }
                        )
                    }
                }
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

/**
 * Brand-tinted header card: who is signed in, what state the account is in and
 * the two figures worth seeing without scrolling.
 */
@Composable
private fun GreetingHeader(
    name: String,
    email: String,
    pending: Int?,
    approvedUpcoming: Int?,
    statusLabel: String,
    statusContainer: androidx.compose.ui.graphics.Color,
    statusContent: androidx.compose.ui.graphics.Color
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(initials = initialsOf(name))
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greeting(),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (email.isNotBlank()) {
                        Text(text = email, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            StatusChip(
                text = statusLabel,
                container = statusContainer,
                content = statusContent
            )

            // Live reservation counts from GET /reservations/dashboard; a dash
            // until the first response arrives.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    icon = Icons.Default.HourglassTop,
                    label = "Pending",
                    value = pending?.toString() ?: "-",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    icon = Icons.Default.EventAvailable,
                    label = "Approved ahead",
                    value = approvedUpcoming?.toString() ?: "-",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** One headline figure inside the greeting header. */
@Composable
private fun StatTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/** Square-ish card used for the reservation shortcuts. */
@Composable
private fun ActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val accent = if (enabled) colors.primary else colors.onSurfaceVariant
    val badge = if (enabled) colors.primaryContainer else colors.surfaceVariant

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outline),
        modifier = modifier
            .height(138.dp)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(color = badge, shape = RoundedCornerShape(50), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) colors.onSurface else colors.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                // Disabled tiles say why instead of describing a feature you cannot open.
                text = if (enabled) subtitle else "Coming soon",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1
            )
        }
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
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(50),
        modifier = Modifier.size(52.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Tappable list row used for every account action. */
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

/** Time-of-day greeting; java.util is used because minSdk 24 has no java.time. */
private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..20 -> "Good evening"
    else -> "Good night"
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
