/* ============================================================================
 * File        : ChangePasswordScreen.kt
 * Purpose     : Lets the signed-in user replace their password through
 *               PUT /users/me/password.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.prosumer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartgrid_mobile.ui.common.BannerTone
import com.example.smartgrid_mobile.ui.common.MessageBanner
import com.example.smartgrid_mobile.ui.common.PageHeaderCard
import com.example.smartgrid_mobile.ui.common.PasswordField
import com.example.smartgrid_mobile.ui.common.PrimaryButton
import com.example.smartgrid_mobile.ui.common.SectionCard
import com.example.smartgrid_mobile.ui.common.SectionLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    viewModel: ProsumerViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Leaves the screen once the service has accepted the new password.
    LaunchedEffect(state.actionComplete) {
        if (state.actionComplete) {
            viewModel.onActionHandled()
            onSaved()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Change password", fontWeight = FontWeight.SemiBold) },
                // Sits on the page background, matching the prosumer tabs.
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !state.working) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                MessageBanner(state.errorMessage, BannerTone.ERROR)

                // ---- Header ----------------------------------------------
                PageHeaderCard(
                    icon = Icons.Default.Shield,
                    title = "Keep your account safe",
                    subtitle = "Sign-in password",
                    // The service re-checks the current password, so say so up front.
                    footnote = "Your current password is required to set a new one."
                )

                // ---- Password form ---------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("New password")
                    SectionCard {
                        PasswordField(
                            value = currentPassword,
                            onValueChange = { currentPassword = it },
                            label = "Current password",
                            enabled = !state.working,
                            isError = state.fieldErrors.containsKey(ProfileField.CURRENT_PASSWORD),
                            supportingText = state.fieldErrors[ProfileField.CURRENT_PASSWORD],
                            leadingIcon = Icons.Default.Lock
                        )
                        PasswordField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = "New password",
                            enabled = !state.working,
                            isError = state.fieldErrors.containsKey(ProfileField.NEW_PASSWORD),
                            supportingText = state.fieldErrors[ProfileField.NEW_PASSWORD]
                                ?: "At least 8 characters",
                            leadingIcon = Icons.Default.LockReset
                        )
                        PasswordField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = "Confirm new password",
                            enabled = !state.working,
                            isError = state.fieldErrors.containsKey(ProfileField.CONFIRM_PASSWORD),
                            supportingText = state.fieldErrors[ProfileField.CONFIRM_PASSWORD],
                            imeAction = ImeAction.Done,
                            leadingIcon = Icons.Default.LockReset
                        )
                    }
                }

                PrimaryButton(
                    text = "Update password",
                    onClick = {
                        viewModel.changePassword(currentPassword, newPassword, confirmPassword)
                    },
                    loading = state.working
                )
            }
        }
    }
}
