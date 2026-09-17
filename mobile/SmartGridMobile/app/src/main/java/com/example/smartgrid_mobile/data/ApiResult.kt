/* ============================================================================
 * File        : ApiResult.kt
 * Purpose     : Small result wrapper so every screen handles success and failure
 *               from the Web API the same way, without throwing across layers.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.data

sealed interface ApiResult<out T> {

    /** The call succeeded and carries the decoded payload. */
    data class Success<T>(val data: T) : ApiResult<T>

    /** The call failed; [httpCode] is null for connectivity or parsing problems. */
    data class Failure(val message: String, val httpCode: Int? = null) : ApiResult<Nothing>
}

/** Convenience accessor used by view models that only need the happy path value. */
fun <T> ApiResult<T>.dataOrNull(): T? = (this as? ApiResult.Success)?.data
