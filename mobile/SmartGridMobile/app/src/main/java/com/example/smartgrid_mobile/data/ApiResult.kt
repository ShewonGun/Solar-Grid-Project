/* ============================================================================
 * File        : ApiResult.kt
 * Purpose     : Small result wrapper so every screen handles success and failure
 *               from the Web API the same way, without throwing across layers.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface ApiResult<out T> {

    /**
     * The call succeeded and carries the decoded payload. [fromCache] is true when
     * the service could not be reached and the value came from local SQLite, so a
     * screen can say it is showing saved data.
     */
    data class Success<T>(val data: T, val fromCache: Boolean = false) : ApiResult<T>

    /** The call failed; [httpCode] is null for connectivity or parsing problems. */
    data class Failure(val message: String, val httpCode: Int? = null) : ApiResult<Nothing>
}

/** Convenience accessor used by view models that only need the happy path value. */
fun <T> ApiResult<T>.dataOrNull(): T? = (this as? ApiResult.Success)?.data

/** Reshapes a successful payload, leaving a failure and its message untouched. */
fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data), fromCache)
    is ApiResult.Failure -> this
}

/**
 * Falls back to locally cached data when the call failed. An empty or missing
 * cache keeps the original failure, so the user still sees why it went wrong.
 * The read runs on the IO dispatcher, because SQLite must not touch the main thread.
 */
suspend fun <T> ApiResult<T>.orCached(cached: () -> T?): ApiResult<T> = when (this) {
    is ApiResult.Success -> this
    is ApiResult.Failure -> withContext(Dispatchers.IO) {
        cached()?.let { ApiResult.Success(it, fromCache = true) } ?: this@orCached
    }
}
