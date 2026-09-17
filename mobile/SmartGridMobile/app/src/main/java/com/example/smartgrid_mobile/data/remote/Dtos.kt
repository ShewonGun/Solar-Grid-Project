/* ============================================================================
 * File        : Dtos.kt
 * Purpose     : Data transfer objects exchanged with the SmartGrid Web API
 *               (authentication and prosumer account management endpoints).
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.data.remote

import com.google.gson.annotations.SerializedName

/** Roles returned by the Web API in [UserDto.role]. */
object Roles {
    const val PROSUMER = "Prosumer"
    const val OPERATOR = "GridOperator"
    const val BACKOFFICE = "Backoffice"
}

/** Account lifecycle states returned by the Web API in [UserDto.status]. */
object AccountStatus {
    const val PENDING_ACTIVATION = "PendingActivation"
    const val ACTIVE = "Active"
    const val DEACTIVATION_REQUESTED = "DeactivationRequested"
    const val DEACTIVATED = "Deactivated"
}

/** Request body for POST /auth/login. [identifier] is either the NIC or the e-mail. */
data class LoginRequest(
    val identifier: String,
    val password: String
)

/** Request body for POST /auth/register (prosumer self-registration, NIC is the PK). */
data class RegisterRequest(
    val nic: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val address: String,
    val solarCapacityKW: Double,
    val password: String
)

/** Request body for PUT /users/{nic} (edit own profile). */
data class UpdateProfileRequest(
    val fullName: String,
    val email: String,
    val phone: String,
    val address: String,
    val solarCapacityKW: Double
)

/** Request body for PUT /users/me/password. */
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

/** Response body for POST /auth/login. */
data class LoginResponse(
    val token: String,
    val expiresAt: String?,
    val user: UserDto
)

/** A user account as stored server-side; NIC is the primary key. */
data class UserDto(
    val nic: String,
    val fullName: String?,
    val email: String?,
    val phone: String?,
    val address: String?,
    @SerializedName(value = "solarCapacityKW", alternate = ["solarCapacityKw"])
    val solarCapacityKW: Double?,
    val role: String?,
    val status: String?
)

/**
 * Error envelope used by the Web API. Several shapes are tolerated so that a
 * plain ASP.NET ProblemDetails response is rendered just as well as a custom one.
 */
data class ApiErrorDto(
    val message: String?,
    val error: String?,
    val title: String?,
    val detail: String?
) {
    /** Picks the first non-blank field so the UI always has something to display. */
    fun bestMessage(): String? =
        listOf(message, error, detail, title).firstOrNull { !it.isNullOrBlank() }
}

/** Direction of an energy transfer, as the Web API serialises ReservationType. */
object ReservationTypes {
    const val DROP_OFF = "DropOff"
    const val CHARGING = "Charging"
}

/** Reservation workflow states returned by the Web API in [ReservationDto.status]. */
object ReservationStatuses {
    const val PENDING = "Pending"
    const val APPROVED = "Approved"
    const val COMPLETED = "Completed"
    const val CANCELLED = "Cancelled"
}

/** Slot availability states returned by the Web API in [SlotDto.status]. */
object SlotStatuses {
    const val AVAILABLE = "Available"
    const val RESERVED = "Reserved"
    const val UNAVAILABLE = "Unavailable"
}

/** A solar grid node, as returned by GET /stations. */
data class StationDto(
    val id: String,
    val stationName: String?,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName(value = "capacityKWh", alternate = ["capacityKwh"])
    val capacityKWh: Double?,
    val totalBatterySlots: Int?,
    val operatingSchedule: String?,
    val isActive: Boolean?
)

/** Dashboard counts for the signed-in prosumer, from GET /reservations/dashboard. */
data class ReservationCountsDto(
    val pending: Int?,
    val approvedUpcoming: Int?
)

/**
 * Request body for the operator endpoints POST /reservations/verify-qr and
 * POST /reservations/complete. The token is whatever the scanner read; the API
 * decides whether it is genuine and still usable.
 */
data class QrTokenRequest(val qrToken: String)

/** A grid node with its distance from the caller, as returned by GET /stations/nearby. */
data class NearbyStationDto(
    val station: StationDto,
    val distanceKm: Double?
)

/** One battery booking slot at a station, as returned by GET /slots/bookable. */
data class SlotDto(
    val id: String,
    val stationId: String,
    val batterySlotNumber: Int?,
    val startTime: String?,
    val endTime: String?,
    @SerializedName(value = "capacityKWh", alternate = ["capacityKwh"])
    val capacityKWh: Double?,
    val status: String?
)

/**
 * Request body for POST /reservations. The prosumer NIC is taken from the
 * bearer token server-side, so it is deliberately not sent from the app.
 */
data class CreateReservationRequest(
    val slotId: String,
    val type: String,
    @SerializedName("energyKWh")
    val energyKWh: Double
)

/** An energy reservation. [qrToken] is only populated once the booking is approved. */
data class ReservationDto(
    val id: String,
    val prosumerNic: String?,
    val stationId: String?,
    val slotId: String?,
    val type: String?,
    @SerializedName(value = "energyKWh", alternate = ["energyKwh"])
    val energyKWh: Double?,
    val reservationStart: String?,
    val reservationEnd: String?,
    val status: String?,
    val qrToken: String?,
    val cancellationReason: String?
)

/**
 * Request body for PUT /reservations/{id}. Every field is optional - only the
 * ones sent are changed. The API requires at least 12 hours' notice.
 */
data class UpdateReservationRequest(
    val slotId: String? = null,
    val type: String? = null,
    @SerializedName("energyKWh")
    val energyKWh: Double? = null
)

/** Request body for POST /reservations/{id}/cancel; the reason is optional. */
data class CancelReservationRequest(
    val reason: String? = null
)
