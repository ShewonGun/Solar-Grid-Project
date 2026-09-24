/* ============================================================================
 * File        : LoginScreen.kt
 * Purpose     : Sign-in screen for the SmartGrid mobile client. Accepts a NIC
 *               or an e-mail address, authenticates against the Web API and
 *               reports the role-specific home screen to the navigation layer.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartgrid_mobile.R
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                BrandHero()

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

/** Brand panel that opens the sign-in screen: app mark, name and tagline. */
@Composable
private fun BrandHero() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 28.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.voltshare_mark),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "VoltShare",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Solar microgrid energy trading",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
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
