/* ============================================================================
 * File        : OperatorHomeScreen.kt
 * Purpose     : Landing screen for Grid Operator and Backoffice accounts that
 *               sign in on the mobile client. Role-based routing is in place;
 *               the QR verification and map tools land in the operator build.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.operator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartgrid_mobile.data.remote.UserDto
import com.example.smartgrid_mobile.ui.common.BannerTone
import com.example.smartgrid_mobile.ui.common.InfoRow
import com.example.smartgrid_mobile.ui.common.MessageBanner
import com.example.smartgrid_mobile.ui.common.SectionCard
import com.example.smartgrid_mobile.ui.common.SectionLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorHomeScreen(
    user: UserDto?,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Operator mode", fontWeight = FontWeight.SemiBold) },
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MessageBanner(
                message = "Signed in as an operator. QR verification and the station map " +
                    "are delivered with the operator module.",
                tone = BannerTone.INFO
            )

            SectionLabel("Signed-in account")
            SectionCard {
                Text(
                    text = user?.fullName.orEmpty().ifBlank { "Operator" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                InfoRow("NIC", user?.nic)
                InfoRow("Email", user?.email)
                InfoRow("Role", user?.role)
                InfoRow("Status", user?.status)
            }
        }
    }
}
