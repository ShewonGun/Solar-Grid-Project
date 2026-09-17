/* ============================================================================
 * File        : Components.kt
 * Purpose     : Small reusable UI building blocks (fields, banners, cards,
 *               status chips) shared by the authentication and prosumer
 *               screens so the whole app keeps one visual language.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.smartgrid_mobile.ui.theme.successColors

/** Severity of a [MessageBanner]; drives its colour only. */
enum class BannerTone { INFO, SUCCESS, ERROR }

/**
 * Flat, full-width notice used for API errors, pending-activation notes and
 * post-action confirmations. Renders nothing when [message] is null or blank.
 */
@Composable
fun MessageBanner(
    message: String?,
    tone: BannerTone,
    modifier: Modifier = Modifier
) {
    if (message.isNullOrBlank()) return

    val colors = MaterialTheme.colorScheme
    val container: Color
    val content: Color
    val icon: ImageVector
    when (tone) {
        BannerTone.ERROR -> {
            container = colors.errorContainer
            content = colors.onErrorContainer
            icon = Icons.Default.Error
        }
        BannerTone.SUCCESS -> {
            // Success keeps its own green so it does not read as a brand banner.
            val success = successColors()
            container = success.container
            content = success.onContainer
            icon = Icons.Default.CheckCircle
        }
        BannerTone.INFO -> {
            container = colors.tertiaryContainer
            content = colors.onTertiaryContainer
            icon = Icons.Default.Info
        }
    }

    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Labelled outlined text field used across every form in the app. */
@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    leadingIcon: ImageVector? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        isError = isError,
        shape = RoundedCornerShape(10.dp),
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction)
    )
}

/** Password field with a show/hide toggle; the toggle state is local to the field. */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    leadingIcon: ImageVector? = null
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        isError = isError,
        shape = RoundedCornerShape(10.dp),
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
        supportingText = supportingText?.let { { Text(it) } },
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) {
                        Icons.Default.VisibilityOff
                    } else {
                        Icons.Default.Visibility
                    },
                    contentDescription = if (visible) "Hide password" else "Show password"
                )
            }
        }
    )
}

/** Primary call-to-action that swaps its label for a spinner while [loading]. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(10.dp),
        contentPadding = ButtonDefaults.ContentPadding,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Bordered surface used for every grouped block of content. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
        }
    }
}

/** Muted section heading used above card groups. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/** Compact pill showing an account status or role. */
@Composable
fun StatusChip(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

/** Label/value row used on the profile summary. */
@Composable
fun InfoRow(label: String, value: String?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = if (value.isNullOrBlank()) "-" else value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

/** Circular icon badge used in page headers, list rows and dialogs. */
@Composable
fun IconBadge(
    icon: ImageVector,
    container: Color,
    tint: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Surface(color = container, shape = RoundedCornerShape(50), modifier = modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(size / 2)
            )
        }
    }
}

/**
 * Brand-tinted card that opens a page: an icon badge, the headline figure or
 * title, a scope line and an optional footnote such as a business rule.
 */
@Composable
fun PageHeaderCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    footnote: String? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(22.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(
                icon = icon,
                container = MaterialTheme.colorScheme.surface,
                tint = MaterialTheme.colorScheme.primary,
                size = 48.dp
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
                if (footnote != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(text = footnote, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** Small tinted fact pill, used for the secondary details on a list card. */
@Composable
fun MetaPill(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Bordered card that behaves like a radio button, used for the direction pickers. */
@Composable
fun SelectableOption(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = if (selected) colors.primaryContainer else colors.surface,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) colors.primary else colors.outline
        ),
        modifier = modifier.clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

/** Pill-shaped filter used for the grid-node and tab selectors. */
@Composable
fun PillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = if (selected) colors.primary else colors.surface,
        contentColor = if (selected) colors.onPrimary else colors.onSurface,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, if (selected) colors.primary else colors.outline),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

/** Icon, headline and explanation shown in place of an empty list. */
@Composable
fun EmptyStateBlock(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconBadge(
            icon = icon,
            container = MaterialTheme.colorScheme.surfaceVariant,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            size = 56.dp
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Centres a loading spinner or empty state in the remaining list space. */
@Composable
fun CenteredBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
