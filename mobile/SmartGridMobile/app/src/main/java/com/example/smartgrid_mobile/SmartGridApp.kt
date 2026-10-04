/* ============================================================================
 * File        : SmartGridApp.kt
 * Purpose     : Application entry point. Bootstraps the service locator so the
 *               SQLite session store is ready before the first screen loads.
 * ==========================================================================*/
package com.example.smartgrid_mobile

import android.app.Application
import com.example.smartgrid_mobile.core.ServiceLocator

class SmartGridApp : Application() {

    /** Wires up the shared dependencies once per process. */
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
