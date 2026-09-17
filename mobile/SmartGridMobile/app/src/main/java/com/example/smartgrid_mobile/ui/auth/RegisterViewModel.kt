/* ============================================================================
 * File        : RegisterViewModel.kt
 * Purpose     : Holds the prosumer self-registration form state and drives
 *               POST /auth/register. The new account is created server-side in
 *               PendingActivation state, so no session is issued here.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartgrid_mobile.data.ApiResult
import com.example.smartgrid_mobile.data.remote.RegisterRequest
import com.example.smartgrid_mobile.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val nic: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val solarCapacityKW: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fieldErrors: Map<String, String> = emptyMap(),
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val registeredNic: String? = null
)

/** Keys used for the per-field error map, kept in one place to avoid typos. */
object RegisterField {
    const val NIC = "nic"
    const val FULL_NAME = "fullName"
    const val EMAIL = "email"
    const val PHONE = "phone"
    const val ADDRESS = "address"
    const val CAPACITY = "solarCapacityKW"
    const val PASSWORD = "password"
    const val CONFIRM = "confirmPassword"
}

class RegisterViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    /** Updates the NIC field. */
    fun onNicChange(value: String) = edit(RegisterField.NIC) { it.copy(nic = value) }

    /** Updates the full-name field. */
    fun onFullNameChange(value: String) =
        edit(RegisterField.FULL_NAME) { it.copy(fullName = value) }

    /** Updates the e-mail field. */
    fun onEmailChange(value: String) = edit(RegisterField.EMAIL) { it.copy(email = value) }

    /** Updates the phone field. */
    fun onPhoneChange(value: String) = edit(RegisterField.PHONE) { it.copy(phone = value) }

    /** Updates the address field. */
    fun onAddressChange(value: String) = edit(RegisterField.ADDRESS) { it.copy(address = value) }

    /** Updates the installed solar capacity field. */
    fun onCapacityChange(value: String) =
        edit(RegisterField.CAPACITY) { it.copy(solarCapacityKW = value) }

    /** Updates the password field. */
    fun onPasswordChange(value: String) =
        edit(RegisterField.PASSWORD) { it.copy(password = value) }

    /** Updates the confirm-password field. */
    fun onConfirmPasswordChange(value: String) =
        edit(RegisterField.CONFIRM) { it.copy(confirmPassword = value) }

    /** Consumed by the navigation layer once the success hand-off has happened. */
    fun onNavigationHandled() {
        _state.update { it.copy(registeredNic = null) }
    }

    /** Validates every field and, if the form is clean, registers the prosumer. */
    fun submit() {
        val current = _state.value
        val errors = buildMap {
            validateNic(current.nic)?.let { put(RegisterField.NIC, it) }
            validateRequired(current.fullName, "Full name")
                ?.let { put(RegisterField.FULL_NAME, it) }
            validateEmail(current.email)?.let { put(RegisterField.EMAIL, it) }
            validatePhone(current.phone)?.let { put(RegisterField.PHONE, it) }
            validateRequired(current.address, "Address")?.let { put(RegisterField.ADDRESS, it) }
            validateCapacity(current.solarCapacityKW)?.let { put(RegisterField.CAPACITY, it) }
            validatePassword(current.password)?.let { put(RegisterField.PASSWORD, it) }
            if (current.confirmPassword != current.password) {
                put(RegisterField.CONFIRM, "Passwords do not match.")
            }
        }

        if (errors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = errors, errorMessage = null) }
            return
        }

        val request = RegisterRequest(
            nic = current.nic.trim().uppercase(),
            fullName = current.fullName.trim(),
            email = current.email.trim(),
            phone = current.phone.trim(),
            address = current.address.trim(),
            solarCapacityKW = current.solarCapacityKW.trim().toDouble(),
            password = current.password
        )

        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorMessage = null, fieldErrors = emptyMap()) }

            when (val result = repository.register(request)) {
                is ApiResult.Success -> _state.update {
                    it.copy(loading = false, registeredNic = request.nic)
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, errorMessage = result.message)
                }
            }
        }
    }

    /** Applies a field edit and clears that field's error plus the form-level error. */
    private fun edit(field: String, transform: (RegisterUiState) -> RegisterUiState) {
        _state.update {
            transform(it).copy(
                fieldErrors = it.fieldErrors - field,
                errorMessage = null
            )
        }
    }
}
