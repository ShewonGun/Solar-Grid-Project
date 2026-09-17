/* ============================================================================
 * File        : RegisterScreen.kt
 * Purpose     : Prosumer self-registration screen. NIC is the primary key, so
 *               it is captured first and validated before the request is sent.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.auth

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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartgrid_mobile.ui.common.BannerTone
import com.example.smartgrid_mobile.ui.common.FormField
import com.example.smartgrid_mobile.ui.common.MessageBanner
import com.example.smartgrid_mobile.ui.common.PasswordField
import com.example.smartgrid_mobile.ui.common.PrimaryButton
import com.example.smartgrid_mobile.ui.common.SectionCard
import com.example.smartgrid_mobile.ui.common.SectionLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onRegistered: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Sends the caller back to the login screen once the account has been created.
    LaunchedEffect(state.registeredNic) {
        state.registeredNic?.let {
            onRegistered(it)
            viewModel.onNavigationHandled()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Create account", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !state.loading) {
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Register as a solar prosumer. A backoffice officer activates " +
                        "the account before the first sign-in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                MessageBanner(state.errorMessage, BannerTone.ERROR)

                SectionLabel("Identity")
                SectionCard {
                    FormField(
                        value = state.nic,
                        onValueChange = viewModel::onNicChange,
                        label = "NIC (primary key)",
                        enabled = !state.loading,
                        isError = state.fieldErrors.containsKey(RegisterField.NIC),
                        supportingText = state.fieldErrors[RegisterField.NIC]
                    )
                    FormField(
                        value = state.fullName,
                        onValueChange = viewModel::onFullNameChange,
                        label = "Full name",
                        enabled = !state.loading,
                        isError = state.fieldErrors.containsKey(RegisterField.FULL_NAME),
                        supportingText = state.fieldErrors[RegisterField.FULL_NAME]
                    )
                }

                SectionLabel("Contact")
                SectionCard {
                    FormField(
                        value = state.email,
                        onValueChange = viewModel::onEmailChange,
                        label = "Email",
                        enabled = !state.loading,
                        isError = state.fieldErrors.containsKey(RegisterField.EMAIL),
                        supportingText = state.fieldErrors[RegisterField.EMAIL],
                        keyboardType = KeyboardType.Email
                    )
                    FormField(
                        value = state.phone,
                        onValueChange = viewModel::onPhoneChange,
                        label = "Phone",
                        enabled = !state.loading,
                        isError = state.fieldErrors.containsKey(RegisterField.PHONE),
                        supportingText = state.fieldErrors[RegisterField.PHONE],
                        keyboardType = KeyboardType.Phone
                    )
                    FormField(
                        value = state.address,
                        onValueChange = viewModel::onAddressChange,
                        label = "Address",
                        enabled = !state.loading,
                        singleLine = false,
                        isError = state.fieldErrors.containsKey(RegisterField.ADDRESS),
                        supportingText = state.fieldErrors[RegisterField.ADDRESS]
                    )
                }

                SectionLabel("Solar installation")
                SectionCard {
                    FormField(
                        value = state.solarCapacityKW,
                        onValueChange = viewModel::onCapacityChange,
                        label = "Installed capacity (kW)",
                        enabled = !state.loading,
                        isError = state.fieldErrors.containsKey(RegisterField.CAPACITY),
                        supportingText = state.fieldErrors[RegisterField.CAPACITY]
                            ?: "Panel array rating, for example 5.5",
                        keyboardType = KeyboardType.Decimal
                    )
                }

                SectionLabel("Security")
                SectionCard {
                    PasswordField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = "Password",
                        enabled = !state.loading,
                        isError = state.fieldErrors.containsKey(RegisterField.PASSWORD),
                        supportingText = state.fieldErrors[RegisterField.PASSWORD]
                            ?: "At least 8 characters"
                    )
                    PasswordField(
                        value = state.confirmPassword,
                        onValueChange = viewModel::onConfirmPasswordChange,
                        label = "Confirm password",
                        enabled = !state.loading,
                        isError = state.fieldErrors.containsKey(RegisterField.CONFIRM),
                        supportingText = state.fieldErrors[RegisterField.CONFIRM],
                        imeAction = ImeAction.Done
                    )
                }

                PrimaryButton(
                    text = "Create account",
                    onClick = viewModel::submit,
                    loading = state.loading
                )
            }
        }
    }
}
