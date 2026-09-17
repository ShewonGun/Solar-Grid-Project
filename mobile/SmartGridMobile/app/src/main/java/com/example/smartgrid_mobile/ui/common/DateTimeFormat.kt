/* ============================================================================
 * File        : DateTimeFormat.kt
 * Purpose     : Turns the ISO-8601 UTC timestamps sent by the Web API into the
 *               short local-time strings the screens display. Built on
 *               SimpleDateFormat rather than java.time because the app targets
 *               minSdk 24 without core-library desugaring.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Fractional seconds and the zone suffix vary by value, so both are trimmed off. */
private val TRAILING_ZONE = Regex("""(\.\d+)?(Z|[+-]\d{2}:?\d{2})?$""")

/** Parser for the trimmed "yyyy-MM-dd'T'HH:mm:ss" form, always read as UTC. */
private fun utcParser() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
    isLenient = false
}

/** Formatter in the device's own time zone, so the user reads local wall-clock time. */
private fun localFormatter(pattern: String) = SimpleDateFormat(pattern, Locale.getDefault())

/**
 * Parses an API timestamp, returning null for anything unreadable so a bad
 * value degrades to a dash on screen instead of crashing the list.
 */
fun parseApiDateTime(value: String?): Date? {
    val text = value?.trim().orEmpty()
    if (text.isEmpty()) return null
    val trimmed = text.replace(TRAILING_ZONE, "")
    return runCatching { utcParser().parse(trimmed) }.getOrNull()
}

/** Short local date, e.g. "Wed 24 Sep". */
fun formatDate(value: String?): String {
    val date = parseApiDateTime(value) ?: return "-"
    return localFormatter("EEE d MMM").format(date)
}

/** Local time of day, e.g. "09:00". */
fun formatTime(value: String?): String {
    val date = parseApiDateTime(value) ?: return "-"
    return localFormatter("HH:mm").format(date)
}

/** Full local date and time, e.g. "Wed 24 Sep, 09:00". */
fun formatDateTime(value: String?): String {
    val date = parseApiDateTime(value) ?: return "-"
    return localFormatter("EEE d MMM, HH:mm").format(date)
}

/**
 * One-line slot window. Collapses to a single date when the slot starts and
 * ends on the same local day, which is the normal case.
 */
fun formatWindow(start: String?, end: String?): String {
    val from = parseApiDateTime(start) ?: return "-"
    val to = parseApiDateTime(end) ?: return formatDateTime(start)

    val day = localFormatter("EEE d MMM")
    val clock = localFormatter("HH:mm")

    return if (day.format(from) == day.format(to)) {
        "${day.format(from)}, ${clock.format(from)} - ${clock.format(to)}"
    } else {
        "${day.format(from)} ${clock.format(from)} - ${day.format(to)} ${clock.format(to)}"
    }
}

/** Renders a kWh amount without a trailing ".0" on whole numbers. */
fun formatKWh(value: Double?): String {
    if (value == null) return "-"
    val whole = value % 1.0 == 0.0
    return if (whole) "${value.toInt()} kWh" else String.format(Locale.US, "%.1f kWh", value)
}

/**
 * Hours between now and [value], negative once it is in the past, null when the
 * timestamp cannot be read. Used only to decide what the booking screen offers;
 * the 12-hour notice rule itself is enforced by the Web API.
 */
fun hoursUntil(value: String?): Double? {
    val date = parseApiDateTime(value) ?: return null
    return (date.time - System.currentTimeMillis()) / 3_600_000.0
}
