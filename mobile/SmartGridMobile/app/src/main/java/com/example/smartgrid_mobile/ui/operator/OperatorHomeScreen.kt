/* ============================================================================
 * File        : OperatorHomeScreen.kt
 * Purpose     : Landing screen for Grid Operator and Backoffice accounts on the
 *               mobile client. Opens the QR scanner used to verify a prosumer's
 *               transaction code and finalise the energy transfer.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.operator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartgrid_mobile.data.remote.UserDto
import com.example.smartgrid_mobile.ui.common.IconBadge
import com.example.smartgrid_mobile.ui.common.InfoRow
import com.example.smartgrid_mobile.ui.common.PageHeaderCard
import com.example.smartgrid_mobile.ui.common.PrimaryButton
import com.example.smartgrid_mobile.ui.common.SectionCard
import com.example.smartgrid_mobile.ui.common.SectionLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorHomeScreen(
    user: UserDto?,
    onScanQr: () -> Unit,
    onNearbyNodes: () -> Unit,
    onHistory: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Operator mode", fontWeight = FontWeight.SemiBold) },
                // Sits on the page background, matching the rest of the app.
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = onSignOut) {
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            PageHeaderCard(
                icon = Icons.Default.QrCodeScanner,
                title = "Verify a transfer",
                subtitle = user?.fullName.orEmpty().ifBlank { "Grid operator" },
                // The one job this screen exists for, stated plainly.
                footnote = "Scan the prosumer's transaction QR code to confirm the " +
                    "booking and finalise the energy transfer."
            )

            PrimaryButton(text = "Scan transaction QR", onClick = onScanQr)

            // ---- Grid nodes -------------------------------------------------
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Grid nodes")
                SectionCard {
                    ActionRow(
                        icon = Icons.Default.Map,
                        title = "Nearby nodes",
                        subtitle = "View microgrid hubs on the map",
                        onClick = onNearbyNodes
                    )
                }
            }

            // ---- Activity ---------------------------------------------------
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Activity")
                SectionCard {
                    ActionRow(
                        icon = Icons.Default.History,
                        title = "Completed transfers",
                        subtitle = "Jobs you've scanned and finalised",
                        onClick = onHistory
                    )
                }
            }

            // ---- How the job works ---------------------------------------
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("How it works")
                SectionCard {
                    StepRow(1, "Scan", "Read the code the prosumer shows you.")
                    StepRow(2, "Verify", "The service confirms the booking and its status.")
                    StepRow(3, "Finalise", "Mark the transfer as done once energy has moved.")
                }
            }

            // ---- Signed-in account ---------------------------------------
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Signed-in account")
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(
                            icon = Icons.Default.Badge,
                            container = MaterialTheme.colorScheme.primaryContainer,
                            tint = MaterialTheme.colorScheme.primary,
                            size = 40.dp
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = user?.fullName.orEmpty().ifBlank { "Operator" },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    InfoRow("NIC", user?.nic)
                    InfoRow("Email", user?.email)
                    InfoRow("Role", user?.role)
                    InfoRow("Status", user?.status)
                }
            }
        }
    }
}

/** Tappable list row used for the grid-node actions on this screen. */
@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** One numbered step in the short explanation of the operator job. */
@Composable
private fun StepRow(number: Int, title: String, detail: String) {
    val colors = MaterialTheme.colorScheme

    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
            shape = RoundedCornerShape(50),
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}
