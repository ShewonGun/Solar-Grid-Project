/* ============================================================================
 * File        : ViewModelFactory.kt
 * Purpose     : Builds the view models by hand from the service locator, so the
 *               project stays free of any dependency-injection framework.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.smartgrid_mobile.ui.auth.LoginViewModel
import com.example.smartgrid_mobile.ui.auth.RegisterViewModel
import com.example.smartgrid_mobile.ui.booking.BookingViewModel
import com.example.smartgrid_mobile.ui.booking.MyBookingsViewModel
import com.example.smartgrid_mobile.ui.prosumer.ProsumerViewModel
import com.example.smartgrid_mobile.ui.qr.TransactionQrViewModel

object AppViewModelFactory : ViewModelProvider.Factory {

    /** Maps a view model class onto its constructor, injecting the shared repository. */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = ServiceLocator.authRepository
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) ->
                LoginViewModel(repository) as T

            modelClass.isAssignableFrom(RegisterViewModel::class.java) ->
                RegisterViewModel(repository) as T

            modelClass.isAssignableFrom(BookingViewModel::class.java) ->
                BookingViewModel(ServiceLocator.reservationRepository) as T

            modelClass.isAssignableFrom(MyBookingsViewModel::class.java) ->
                MyBookingsViewModel(ServiceLocator.reservationRepository) as T

            modelClass.isAssignableFrom(TransactionQrViewModel::class.java) ->
                TransactionQrViewModel(ServiceLocator.reservationRepository) as T

            modelClass.isAssignableFrom(ProsumerViewModel::class.java) ->
                ProsumerViewModel(repository) as T

            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
