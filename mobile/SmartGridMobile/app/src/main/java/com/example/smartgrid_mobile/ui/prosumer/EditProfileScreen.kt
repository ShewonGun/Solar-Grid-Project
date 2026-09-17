/* ============================================================================
 * File        : EditProfileScreen.kt
 * Purpose     : Lets a prosumer edit their own profile. The form is seeded from
 *               the SQLite-cached profile and saved through PUT /users/{nic}.
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.smartgrid_mobile.ui.common.PrimaryButton
import com.example.smartgrid_mobile.ui.common.SectionCard
import com.example.smartgrid_mobile.ui.common.SectionLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: ProsumerViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user by viewModel.user.collectAsStateWithLifecycle()

    var fullName by remember(user?.nic) { mutableStateOf(user?.fullName.orEmpty()) }
    var email by remember(user?.nic) { mutableStateOf(user?.email.orEmpty()) }
    var phone by remember(user?.nic) { mutableStateOf(user?.phone.orEmpty()) }
    var address by remember(user?.nic) { mutableStateOf(user?.address.orEmpty()) }
    var capacity by remember(user?.nic) {
        mutableStateOf(user?.solarCapacityKW?.toString().orEmpty())
    }

    // Returns to the home screen as soon as the API confirms the update.
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
                title = { Text("Edit profile", fontWeight = FontWeight.SemiBold) },
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MessageBanner(state.errorMessage, BannerTone.ERROR)

                Text(
                    text = "NIC ${user?.nic.orEmpty()} is the account key and cannot be changed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SectionLabel("Details")
                SectionCard {
                    FormField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = "Full name",
                        enabled = !state.working,
                        isError = state.fieldErrors.containsKey(ProfileField.FULL_NAME),
                        supportingText = state.fieldErrors[ProfileField.FULL_NAME]
                    )
                    FormField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        enabled = !state.working,
                        isError = state.fieldErrors.containsKey(ProfileField.EMAIL),
                        supportingText = state.fieldErrors[ProfileField.EMAIL],
                        keyboardType = KeyboardType.Email
                    )
                    FormField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = "Phone",
                        enabled = !state.working,
                        isError = state.fieldErrors.containsKey(ProfileField.PHONE),
                        supportingText = state.fieldErrors[ProfileField.PHONE],
                        keyboardType = KeyboardType.Phone
                    )
                    FormField(
                        value = address,
                        onValueChange = { address = it },
                        label = "Address",
                        enabled = !state.working,
                        singleLine = false,
                        isError = state.fieldErrors.containsKey(ProfileField.ADDRESS),
                        supportingText = state.fieldErrors[ProfileField.ADDRESS]
                    )
                }

                SectionLabel("Solar installation")
                SectionCard {
                    FormField(
                        value = capacity,
                        onValueChange = { capacity = it },
                        label = "Installed capacity (kW)",
                        enabled = !state.working,
                        isError = state.fieldErrors.containsKey(ProfileField.CAPACITY),
                        supportingText = state.fieldErrors[ProfileField.CAPACITY],
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    )
                }

                PrimaryButton(
                    text = "Save changes",
                    onClick = {
                        viewModel.saveProfile(fullName, email, phone, address, capacity)
                    },
                    loading = state.working
                )
            }
        }
    }
}
