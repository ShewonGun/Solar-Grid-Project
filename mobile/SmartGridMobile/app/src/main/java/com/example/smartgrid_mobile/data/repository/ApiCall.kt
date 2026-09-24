/* ============================================================================
 * File        : ApiCall.kt
 * Purpose     : One shared way of running a Retrofit call and turning the
 *               outcome into an ApiResult, so every repository reports network
 *               and API failures to the UI with the same wording.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.data.repository

import com.example.smartgrid_mobile.data.ApiResult
import com.example.smartgrid_mobile.data.remote.ApiErrorDto
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

private val gson = Gson()

/**
 * Runs a Retrofit call off the main thread and normalises the outcome into an
 * ApiResult, applying [onSuccess] before the value reaches the caller.
 */
internal suspend fun <T> apiCall(
    request: suspend () -> Response<T>,
    onSuccess: suspend (T) -> Unit = {}
): ApiResult<T> = withContext(Dispatchers.IO) {
    try {
        val response = request()
        if (response.isSuccessful) {
            @Suppress("UNCHECKED_CAST")
            val body = response.body() ?: (Unit as T)
            onSuccess(body)
            ApiResult.Success(body)
        } else {
            ApiResult.Failure(errorMessage(response), response.code())
        }
    } catch (e: IOException) {
        ApiResult.Failure(
            "Cannot reach the VoltShare service. Check that the API is running and " +
                "that this device can see it.",
            null
        )
    } catch (e: Exception) {
        ApiResult.Failure(e.message ?: "Unexpected error.", null)
    }
}

/** Extracts the explanatory message sent by the API, with sensible fallbacks. */
private fun errorMessage(response: Response<*>): String {
    val raw = runCatching { response.errorBody()?.string() }.getOrNull()
    val parsed = raw
        ?.takeIf { it.isNotBlank() && it.trimStart().startsWith("{") }
        ?.let { runCatching { gson.fromJson(it, ApiErrorDto::class.java) }.getOrNull() }
        ?.bestMessage()

    if (!parsed.isNullOrBlank()) return parsed
    if (!raw.isNullOrBlank() && !raw.trimStart().startsWith("<")) return raw.trim()

    return when (response.code()) {
        400 -> "Please check the details you entered."
        401 -> "Invalid credentials."
        403 -> "Your account is not active yet. A backoffice officer must activate it."
        404 -> "Not found."
        409 -> "That action conflicts with the current account status."
        else -> "Request failed (HTTP " + response.code() + ")."
    }
}
