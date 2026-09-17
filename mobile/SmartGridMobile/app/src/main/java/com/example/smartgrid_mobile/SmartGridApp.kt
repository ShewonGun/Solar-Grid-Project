/* ============================================================================
 * File        : SmartGridApp.kt
 * Purpose     : Application entry point. Bootstraps the service locator so the
 *               SQLite session store is ready before the first screen loads.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
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
