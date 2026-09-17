/* ============================================================================
 * File        : SessionStore.kt
 * Purpose     : Reads and writes the signed-in user's session (bearer token and
 *               cached profile) in the local SQLite database, and exposes it to
 *               the UI as an observable state flow.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.data.local

import android.content.ContentValues
import android.content.Context
import com.example.smartgrid_mobile.data.remote.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The locally persisted session: what the app knows without calling the API. */
data class LocalSession(
    val token: String,
    val expiresAt: String?,
    val user: UserDto
)

class SessionStore(context: Context) {

    private val helper = SmartGridDbHelper(context)

    private val _session = MutableStateFlow(readFromDb())

    /** Current session, or null when nobody is signed in on this device. */
    val session: StateFlow<LocalSession?> = _session.asStateFlow()

    /** Bearer token read straight from the cached state, for the OkHttp interceptor. */
    fun token(): String? = _session.value?.token

    /** Persists a freshly issued token together with the profile it belongs to. */
    fun save(token: String, expiresAt: String?, user: UserDto) {
        writeToDb(token, expiresAt, user)
        _session.value = LocalSession(token, expiresAt, user)
    }

    /** Refreshes only the cached profile, keeping the existing token in place. */
    fun updateUser(user: UserDto) {
        val current = _session.value ?: return
        save(current.token, current.expiresAt, user)
    }

    /** Clears the local session on sign-out or when the server rejects the token. */
    fun clear() {
        helper.writableDatabase.delete(SmartGridDbHelper.TABLE_SESSION, null, null)
        _session.value = null
    }

    /** Inserts (or replaces) the single session row in SQLite. */
    private fun writeToDb(token: String, expiresAt: String?, user: UserDto) {
        val values = ContentValues().apply {
            put(SmartGridDbHelper.COL_ID, 1)
            put(SmartGridDbHelper.COL_TOKEN, token)
            put(SmartGridDbHelper.COL_EXPIRES_AT, expiresAt)
            put(SmartGridDbHelper.COL_NIC, user.nic)
            put(SmartGridDbHelper.COL_FULL_NAME, user.fullName)
            put(SmartGridDbHelper.COL_EMAIL, user.email)
            put(SmartGridDbHelper.COL_PHONE, user.phone)
            put(SmartGridDbHelper.COL_ADDRESS, user.address)
            put(SmartGridDbHelper.COL_CAPACITY, user.solarCapacityKW)
            put(SmartGridDbHelper.COL_ROLE, user.role)
            put(SmartGridDbHelper.COL_STATUS, user.status)
        }
        helper.writableDatabase.insertWithOnConflict(
            SmartGridDbHelper.TABLE_SESSION,
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    /** Loads the stored session at start-up so a returning user skips the login screen. */
    private fun readFromDb(): LocalSession? {
        helper.readableDatabase.query(
            SmartGridDbHelper.TABLE_SESSION,
            null, null, null, null, null, null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null

            fun text(name: String): String? {
                val index = cursor.getColumnIndexOrThrow(name)
                return if (cursor.isNull(index)) null else cursor.getString(index)
            }

            fun real(name: String): Double? {
                val index = cursor.getColumnIndexOrThrow(name)
                return if (cursor.isNull(index)) null else cursor.getDouble(index)
            }

            val user = UserDto(
                nic = text(SmartGridDbHelper.COL_NIC).orEmpty(),
                fullName = text(SmartGridDbHelper.COL_FULL_NAME),
                email = text(SmartGridDbHelper.COL_EMAIL),
                phone = text(SmartGridDbHelper.COL_PHONE),
                address = text(SmartGridDbHelper.COL_ADDRESS),
                solarCapacityKW = real(SmartGridDbHelper.COL_CAPACITY),
                role = text(SmartGridDbHelper.COL_ROLE),
                status = text(SmartGridDbHelper.COL_STATUS)
            )
            val token = text(SmartGridDbHelper.COL_TOKEN) ?: return null
            return LocalSession(token, text(SmartGridDbHelper.COL_EXPIRES_AT), user)
        }
    }
}
