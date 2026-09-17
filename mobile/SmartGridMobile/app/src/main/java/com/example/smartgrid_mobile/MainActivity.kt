/* ============================================================================
 * File        : MainActivity.kt
 * Purpose     : Single activity host for the SmartGrid mobile client. Applies
 *               the app theme and hands control to the navigation graph.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.smartgrid_mobile.navigation.SmartGridNavHost
import com.example.smartgrid_mobile.ui.theme.SmartGridMobileTheme

class MainActivity : ComponentActivity() {

    /** Sets up the Compose content tree for the whole application. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartGridMobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SmartGridNavHost()
                }
            }
        }
    }
}
