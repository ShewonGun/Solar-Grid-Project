/* ============================================================================
 * File        : LoginScreen.kt
 * Purpose     : Sign-in screen for the SmartGrid mobile client. Accepts a NIC
 *               or an e-mail address, authenticates against the Web API and
 *               reports the role-specific home screen to the navigation layer.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartgrid_mobile.ui.common.BannerTone
import com.example.smartgrid_mobile.ui.common.FormField
import com.example.smartgrid_mobile.ui.common.MessageBanner
import com.example.smartgrid_mobile.ui.common.PasswordField
import com.example.smartgrid_mobile.ui.common.PrimaryButton
import com.example.smartgrid_mobile.ui.common.SectionCard

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoggedIn: (LoginDestination) -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Hands a successful login over to the navigation graph exactly once.
    LaunchedEffect(state.destination) {
        state.destination?.let {
            onLoggedIn(it)
            viewModel.onNavigationHandled()
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandHeader()

            Spacer(Modifier.height(28.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MessageBanner(state.infoMessage, BannerTone.SUCCESS)
                MessageBanner(state.errorMessage, BannerTone.ERROR)

                SectionCard {
                    Text(
                        text = "Sign in",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    FormField(
                        value = state.identifier,
                        onValueChange = viewModel::onIdentifierChange,
                        label = "NIC or email",
                        enabled = !state.loading,
                        isError = state.identifierError != null,
                        supportingText = state.identifierError,
                        keyboardType = KeyboardType.Text,
                        leadingIcon = Icons.Default.Person
                    )

                    PasswordField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = "Password",
                        enabled = !state.loading,
                        isError = state.passwordError != null,
                        supportingText = state.passwordError,
                        imeAction = ImeAction.Done,
                        leadingIcon = Icons.Default.Lock
                    )

                    PrimaryButton(
                        text = "Sign in",
                        onClick = viewModel::submit,
                        loading = state.loading
                    )
                }

                InlineAction(
                    label = "New prosumer?",
                    actionText = "Create an account",
                    onAction = {
                        viewModel.clearInfo()
                        onCreateAccount()
                    },
                    enabled = !state.loading
                )
            }
        }
    }
}

/** Application mark and tagline shown above the sign-in card. */
@Composable
private fun BrandHeader() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.size(64.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    Text(
        text = "SmartGrid",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Solar microgrid energy trading",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

/** Centred "label + text action" line used under the sign-in card. */
@Composable
private fun InlineAction(
    label: String,
    actionText: String,
    onAction: () -> Unit,
    enabled: Boolean
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onAction, enabled = enabled) {
            Text(actionText, fontWeight = FontWeight.SemiBold)
        }
    }
}
