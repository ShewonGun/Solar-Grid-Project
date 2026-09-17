/* ============================================================================
 * File        : ProsumerBottomBar.kt
 * Purpose     : Bottom navigation bar shown on the prosumer tab destinations.
 *               Holds the tab list in one place and reports the route the user
 *               picked; the navigation graph owns the actual navigation.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** One entry in the prosumer bottom navigation bar. */
private data class ProsumerTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

/** The four top-level prosumer destinations, in display order. */
private val ProsumerTabs = listOf(
    ProsumerTab(Routes.PROSUMER_HOME, "Home", Icons.Default.Home),
    ProsumerTab(Routes.PROSUMER_BOOK_SLOT, "Book", Icons.Default.EventAvailable),
    ProsumerTab(Routes.PROSUMER_BOOKINGS, "Bookings", Icons.Default.History),
    ProsumerTab(Routes.PROSUMER_PROFILE, "Profile", Icons.Default.Person)
)

/** Draws the bar and reports the selected route back to the caller. */
@Composable
fun ProsumerBottomBar(currentRoute: String?, onSelect: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme

    Column {
        // Hairline, because the bar and the page share the same light surface.
        HorizontalDivider(color = colors.outline)
        NavigationBar(
            containerColor = colors.surface,
            tonalElevation = 0.dp
        ) {
            ProsumerTabs.forEach { tab ->
                val selected = currentRoute == tab.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { if (!selected) onSelect(tab.route) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = {
                        Text(
                            text = tab.label,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.primary,
                        selectedTextColor = colors.primary,
                        indicatorColor = colors.primaryContainer,
                        unselectedIconColor = colors.onSurfaceVariant,
                        unselectedTextColor = colors.onSurfaceVariant
                    )
                )
            }
        }
    }
}
