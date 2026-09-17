/* ============================================================================
 * File        : LoginViewModel.kt
 * Purpose     : Holds the login form state and drives POST /auth/login, then
 *               reports which role-specific home screen should be opened.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartgrid_mobile.data.ApiResult
import com.example.smartgrid_mobile.data.remote.Roles
import com.example.smartgrid_mobile.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where a successful login should take the user. */
enum class LoginDestination { PROSUMER, OPERATOR }

data class LoginUiState(
    val identifier: String = "",
    val password: String = "",
    val identifierError: String? = null,
    val passwordError: String? = null,
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val destination: LoginDestination? = null
)

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    /** Updates the NIC/email field and clears any message shown for it. */
    fun onIdentifierChange(value: String) {
        _state.update { it.copy(identifier = value, identifierError = null, errorMessage = null) }
    }

    /** Updates the password field and clears any message shown for it. */
    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, passwordError = null, errorMessage = null) }
    }

    /** Shows a one-off notice, e.g. the pending-activation note after registering. */
    fun showInfo(message: String?) {
        _state.update { it.copy(infoMessage = message) }
    }

    /** Dismisses the info notice once the user has read it. */
    fun clearInfo() {
        _state.update { it.copy(infoMessage = null) }
    }

    /** Consumed by the navigation layer after it has handled a successful login. */
    fun onNavigationHandled() {
        _state.update { it.copy(destination = null) }
    }

    /** Validates the form and, if it passes, authenticates against the Web API. */
    fun submit() {
        val current = _state.value
        val identifierError = validateRequired(current.identifier, "NIC or email")
        val passwordError = validateRequired(current.password, "Password")

        if (identifierError != null || passwordError != null) {
            _state.update {
                it.copy(identifierError = identifierError, passwordError = passwordError)
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorMessage = null, infoMessage = null) }

            when (val result = repository.login(current.identifier, current.password)) {
                is ApiResult.Success -> {
                    val destination = when (result.data.user.role) {
                        Roles.OPERATOR, Roles.BACKOFFICE -> LoginDestination.OPERATOR
                        else -> LoginDestination.PROSUMER
                    }
                    _state.update {
                        it.copy(loading = false, password = "", destination = destination)
                    }
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, errorMessage = result.message)
                }
            }
        }
    }
}
