/* ============================================================================
 * File        : ProsumerViewModel.kt
 * Purpose     : Backs the prosumer home, profile-edit, password and account
 *               deactivation screens. Reads the cached SQLite session and
 *               refreshes it from GET /auth/me on every visit.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.prosumer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartgrid_mobile.data.ApiResult
import com.example.smartgrid_mobile.data.remote.AccountStatus
import com.example.smartgrid_mobile.data.remote.UpdateProfileRequest
import com.example.smartgrid_mobile.data.remote.UserDto
import com.example.smartgrid_mobile.data.repository.AuthRepository
import com.example.smartgrid_mobile.ui.auth.validateCapacity
import com.example.smartgrid_mobile.ui.auth.validateEmail
import com.example.smartgrid_mobile.ui.auth.validatePassword
import com.example.smartgrid_mobile.ui.auth.validatePhone
import com.example.smartgrid_mobile.ui.auth.validateRequired
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProsumerUiState(
    val refreshing: Boolean = false,
    val working: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val loggedOut: Boolean = false,
    val actionComplete: Boolean = false
)

/** Keys for the per-field error map on the profile and password forms. */
object ProfileField {
    const val FULL_NAME = "fullName"
    const val EMAIL = "email"
    const val PHONE = "phone"
    const val ADDRESS = "address"
    const val CAPACITY = "solarCapacityKW"
    const val CURRENT_PASSWORD = "currentPassword"
    const val NEW_PASSWORD = "newPassword"
    const val CONFIRM_PASSWORD = "confirmPassword"
}

class ProsumerViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(ProsumerUiState())
    val state: StateFlow<ProsumerUiState> = _state.asStateFlow()

    /** The signed-in profile, served from SQLite and refreshed from the API. */
    val user: StateFlow<UserDto?> = repository.session
        .map { it?.user }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.session.value?.user)

    init {
        refresh()
    }

    /** Pulls the latest profile so status changes made in the web app show up here. */
    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true) }
            val result = repository.refreshProfile()
            _state.update {
                it.copy(
                    refreshing = false,
                    errorMessage = (result as? ApiResult.Failure)?.message ?: it.errorMessage
                )
            }
        }
    }

    /** Saves profile edits through PUT /users/{nic}. */
    fun saveProfile(
        fullName: String,
        email: String,
        phone: String,
        address: String,
        capacity: String
    ) {
        val nic = user.value?.nic
        if (nic.isNullOrBlank()) {
            _state.update { it.copy(errorMessage = "No signed-in account was found.") }
            return
        }

        val errors = buildMap {
            validateRequired(fullName, "Full name")?.let { put(ProfileField.FULL_NAME, it) }
            validateEmail(email)?.let { put(ProfileField.EMAIL, it) }
            validatePhone(phone)?.let { put(ProfileField.PHONE, it) }
            validateRequired(address, "Address")?.let { put(ProfileField.ADDRESS, it) }
            validateCapacity(capacity)?.let { put(ProfileField.CAPACITY, it) }
        }
        if (errors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = errors, errorMessage = null) }
            return
        }

        val request = UpdateProfileRequest(
            fullName = fullName.trim(),
            email = email.trim(),
            phone = phone.trim(),
            address = address.trim(),
            solarCapacityKW = capacity.trim().toDouble()
        )
        runAction("Profile updated.") { repository.updateProfile(nic, request) }
    }

    /** Changes the password through PUT /users/me/password. */
    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        val errors = buildMap {
            validateRequired(currentPassword, "Current password")
                ?.let { put(ProfileField.CURRENT_PASSWORD, it) }
            validatePassword(newPassword)?.let { put(ProfileField.NEW_PASSWORD, it) }
            if (confirmPassword != newPassword) {
                put(ProfileField.CONFIRM_PASSWORD, "Passwords do not match.")
            }
        }
        if (errors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = errors, errorMessage = null) }
            return
        }

        runAction("Password changed.") { repository.changePassword(currentPassword, newPassword) }
    }

    /** Requests deactivation; the API rejects this with 409 unless the account is Active. */
    fun requestDeactivation() {
        runAction("Deactivation requested. A backoffice officer will review it.") {
            repository.requestDeactivation()
        }
    }

    /** True when the account is Active, which is what the deactivation request needs. */
    fun canRequestDeactivation(): Boolean = user.value?.status == AccountStatus.ACTIVE

    /** Clears the local SQLite session and sends the user back to the login screen. */
    fun logout() {
        repository.logout()
        _state.update { it.copy(loggedOut = true) }
    }

    /** Dismisses the current error or success banner. */
    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, successMessage = null) }
    }

    /** Consumed by a screen once it has reacted to a completed action. */
    fun onActionHandled() {
        _state.update { it.copy(actionComplete = false) }
    }

    /** Shared plumbing for the write operations: spinner, then success or error banner. */
    private fun <T> runAction(successMessage: String, block: suspend () -> ApiResult<T>) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    working = true,
                    errorMessage = null,
                    successMessage = null,
                    fieldErrors = emptyMap()
                )
            }
            when (val result = block()) {
                is ApiResult.Success -> _state.update {
                    it.copy(working = false, successMessage = successMessage, actionComplete = true)
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(working = false, errorMessage = result.message)
                }
            }
        }
    }
}
