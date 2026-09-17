/* ============================================================================
 * File        : Validation.kt
 * Purpose     : Light client-side field checks. These only keep obviously bad
 *               input from reaching the network; the authoritative validation
 *               and every business rule stay in the FAT Web API.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.auth

/** Old-format (9 digits + V/X) and new-format (12 digits) Sri Lankan NIC numbers. */
private val NIC_PATTERN = Regex("^([0-9]{9}[VvXx]|[0-9]{12})$")

private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")

private val PHONE_PATTERN = Regex("^[0-9+][0-9\\s-]{6,14}$")

/** Returns an error message for the NIC field, or null when it looks valid. */
fun validateNic(nic: String): String? = when {
    nic.isBlank() -> "NIC is required."
    !NIC_PATTERN.matches(nic.trim()) -> "Enter a valid NIC (9 digits + V, or 12 digits)."
    else -> null
}

/** Returns an error message for the e-mail field, or null when it looks valid. */
fun validateEmail(email: String): String? = when {
    email.isBlank() -> "Email is required."
    !EMAIL_PATTERN.matches(email.trim()) -> "Enter a valid email address."
    else -> null
}

/** Returns an error message for the phone field, or null when it looks valid. */
fun validatePhone(phone: String): String? = when {
    phone.isBlank() -> "Phone number is required."
    !PHONE_PATTERN.matches(phone.trim()) -> "Enter a valid phone number."
    else -> null
}

/** Returns an error message for a required free-text field, or null. */
fun validateRequired(value: String, label: String): String? =
    if (value.isBlank()) "$label is required." else null

/** Returns an error message for the solar capacity field, or null when valid. */
fun validateCapacity(capacity: String): String? {
    if (capacity.isBlank()) return "Solar capacity is required."
    val parsed = capacity.trim().toDoubleOrNull()
        ?: return "Enter the capacity as a number, for example 5.5."
    return if (parsed <= 0.0) "Capacity must be greater than zero." else null
}

/** Returns an error message for a new password, or null when it is acceptable. */
fun validatePassword(password: String): String? = when {
    password.isBlank() -> "Password is required."
    password.length < 8 -> "Use at least 8 characters."
    else -> null
}
