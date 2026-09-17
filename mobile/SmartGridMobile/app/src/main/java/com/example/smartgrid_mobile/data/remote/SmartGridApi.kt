/* ============================================================================
 * File        : SmartGridApi.kt
 * Purpose     : Retrofit description of the SmartGrid Web API endpoints used by
 *               the prosumer authentication and account-management screens.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface SmartGridApi {

    /** Authenticates with NIC or e-mail and returns the bearer token plus profile. */
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    /** Registers a new prosumer; the account is created in PendingActivation state. */
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<UserDto>

    /** Reloads the signed-in user's profile from the service. */
    @GET("auth/me")
    suspend fun me(): Response<UserDto>

    /** Updates the signed-in prosumer's own profile, keyed by NIC. */
    @PUT("users/{nic}")
    suspend fun updateProfile(
        @Path("nic") nic: String,
        @Body body: UpdateProfileRequest
    ): Response<UserDto>

    /** Changes the signed-in user's password. */
    @PUT("users/me/password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Response<Unit>

    /** Asks the backoffice to deactivate the account; fails with 409 unless Active. */
    @POST("users/me/deactivation-request")
    suspend fun requestDeactivation(): Response<Unit>

    /** Lists every grid node, used to name and filter the bookable slots. */
    @GET("stations")
    suspend fun getStations(): Response<List<StationDto>>

    /** Slots a prosumer may book right now, optionally narrowed to one station. */
    @GET("slots/bookable")
    suspend fun getBookableSlots(
        @Query("stationId") stationId: String? = null
    ): Response<List<SlotDto>>

    /** Books a slot; the API enforces the 7-day window and slot availability. */
    @POST("reservations")
    suspend fun createReservation(@Body body: CreateReservationRequest): Response<ReservationDto>

    /** Pending and approved bookings that have not ended yet, soonest first. */
    @GET("reservations/upcoming")
    suspend fun getUpcomingReservations(): Response<List<ReservationDto>>

    /** Completed, cancelled and already-ended bookings, most recent first. */
    @GET("reservations/history")
    suspend fun getReservationHistory(): Response<List<ReservationDto>>

    /** Changes a booking; the API requires at least 12 hours notice. */
    @PUT("reservations/{id}")
    suspend fun updateReservation(
        @Path("id") id: String,
        @Body body: UpdateReservationRequest
    ): Response<ReservationDto>

    /** Cancels a booking and frees its slot; same 12-hour notice rule. */
    @POST("reservations/{id}/cancel")
    suspend fun cancelReservation(
        @Path("id") id: String,
        @Body body: CancelReservationRequest
    ): Response<ReservationDto>
}
