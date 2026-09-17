/* ============================================================================
 * File        : AuthRepository.kt
 * Purpose     : Single entry point for authentication and prosumer account
 *               management. Calls the FAT Web API (all business rules live
 *               there) and mirrors the resulting session into local SQLite.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.data.repository

import com.example.smartgrid_mobile.data.ApiResult
import com.example.smartgrid_mobile.data.local.LocalSession
import com.example.smartgrid_mobile.data.local.SessionStore
import com.example.smartgrid_mobile.data.remote.ChangePasswordRequest
import com.example.smartgrid_mobile.data.remote.LoginRequest
import com.example.smartgrid_mobile.data.remote.LoginResponse
import com.example.smartgrid_mobile.data.remote.RegisterRequest
import com.example.smartgrid_mobile.data.remote.SmartGridApi
import com.example.smartgrid_mobile.data.remote.UpdateProfileRequest
import com.example.smartgrid_mobile.data.remote.UserDto
import kotlinx.coroutines.flow.StateFlow

class AuthRepository(
    private val api: SmartGridApi,
    private val sessionStore: SessionStore
) {

    /** Observable local session, so the navigation graph knows who is signed in. */
    val session: StateFlow<LocalSession?> = sessionStore.session

    /** Signs in with NIC or e-mail and persists the issued token in SQLite. */
    suspend fun login(identifier: String, password: String): ApiResult<LoginResponse> =
        apiCall({ api.login(LoginRequest(identifier.trim(), password)) }) { body ->
            sessionStore.save(body.token, body.expiresAt, body.user)
        }

    /** Self-registers a prosumer; the account stays PendingActivation until approved. */
    suspend fun register(request: RegisterRequest): ApiResult<UserDto> =
        apiCall({ api.register(request) })

    /** Reloads the profile from the service and refreshes the SQLite cache. */
    suspend fun refreshProfile(): ApiResult<UserDto> =
        apiCall({ api.me() }) { user -> sessionStore.updateUser(user) }

    /** Saves profile edits for the signed-in prosumer, keyed by their NIC. */
    suspend fun updateProfile(nic: String, request: UpdateProfileRequest): ApiResult<UserDto> =
        apiCall({ api.updateProfile(nic, request) }) { user -> sessionStore.updateUser(user) }

    /** Changes the password of the signed-in user. */
    suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<Unit> =
        apiCall({ api.changePassword(ChangePasswordRequest(currentPassword, newPassword)) })

    /** Requests account deactivation; the API answers 409 when not currently Active. */
    suspend fun requestDeactivation(): ApiResult<Unit> =
        apiCall({ api.requestDeactivation() }) { refreshProfile() }

    /** Drops the local session so the app returns to the login screen. */
    fun logout() = sessionStore.clear()
}
