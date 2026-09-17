/* ============================================================================
 * File        : DeviceLocationProvider.kt
 * Purpose     : Reads the device's last known position through Play Services so
 *               the node map can centre on the prosumer and ask the Web API for
 *               the nodes nearest to them. Location is optional throughout.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** A position on the earth, kept free of any Play Services type for the UI layer. */
data class DeviceLocation(val latitude: Double, val longitude: Double)

class DeviceLocationProvider(private val context: Context) {

    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    /** True once the user has granted either the coarse or the fine location permission. */
    fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    /**
     * Returns the current position, or null when the permission is missing, the
     * hardware reports nothing, or Play Services fails. The caller always has a
     * path that works without a location.
     */
    suspend fun current(): DeviceLocation? {
        if (!hasPermission()) return null

        return try {
            suspendCancellableCoroutine { continuation ->
                val cancellation = CancellationTokenSource()

                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token)
                    .addOnSuccessListener { location ->
                        continuation.resume(
                            location?.let { DeviceLocation(it.latitude, it.longitude) }
                        )
                    }
                    .addOnFailureListener { continuation.resume(null) }

                // Stops the location request if the screen goes away mid-lookup.
                continuation.invokeOnCancellation { cancellation.cancel() }
            }
        } catch (e: SecurityException) {
            // The permission was revoked between the check and the call.
            null
        }
    }
}
