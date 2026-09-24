/*
 * File: ReservationsController.cs
 * Purpose: Energy reservation endpoints - booking, updating and cancelling
 *          (web and mobile), approval, QR verification and completion by
 *          operators, and booking views and dashboard counts. Prosumers are
 *          always limited to their own reservations. Business rules live in
 *          EnergyReservationService.
 * Author:  <your name>
 * Created: 2026
 */
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartMicrogrid.Api.DTOs.Requests;
using SmartMicrogrid.Api.DTOs.Responses;
using SmartMicrogrid.Api.Helpers;
using SmartMicrogrid.Api.Models;
using SmartMicrogrid.Api.Models.Enums;
using SmartMicrogrid.Api.Services;
using SmartMicrogrid.Api.Services.Interfaces;

namespace SmartMicrogrid.Api.Controllers
{
    [ApiController]
    [Route("api/reservations")]
    [Produces("application/json")]
    public class ReservationsController : ControllerBase
    {
        private readonly IEnergyReservationService _reservationService;

        // Receives the reservation service.
        public ReservationsController(IEnergyReservationService reservationService)
        {
            _reservationService = reservationService;
        }

        // True when the signed-in user is a prosumer.
        private bool IsProsumer => User.IsInRole(AppRoles.Prosumer);

        // GET api/reservations - searches reservations; prosumers only see their own.
        // completedBy lets a Grid Operator ask for only the transfers they personally finalised.
        [HttpGet]
        public async Task<ActionResult<IEnumerable<ReservationResponse>>> Search(
            [FromQuery] string? prosumerNic, [FromQuery] string? stationId, [FromQuery] ReservationStatus? status,
            [FromQuery] string? completedBy, [FromQuery] DateTime? from, [FromQuery] DateTime? to,
            CancellationToken cancellationToken)
        {
            var scopedNic = IsProsumer ? User.GetNic() : prosumerNic;
            var reservations = await _reservationService.SearchAsync(scopedNic, stationId, status, completedBy, from, to, cancellationToken);
            return Ok(reservations.Select(ToResponse));
        }

        // GET api/reservations/upcoming - current and pending bookings; staff must pass prosumerNic.
        [HttpGet("upcoming")]
        public async Task<ActionResult<IEnumerable<ReservationResponse>>> GetUpcoming([FromQuery] string? prosumerNic, CancellationToken cancellationToken)
        {
            var reservations = await _reservationService.GetUpcomingAsync(ResolveProsumerNic(prosumerNic), cancellationToken);
            return Ok(reservations.Select(ToResponse));
        }

        // GET api/reservations/history - completed, cancelled and past bookings; staff must pass prosumerNic.
        [HttpGet("history")]
        public async Task<ActionResult<IEnumerable<ReservationResponse>>> GetHistory([FromQuery] string? prosumerNic, CancellationToken cancellationToken)
        {
            var reservations = await _reservationService.GetHistoryAsync(ResolveProsumerNic(prosumerNic), cancellationToken);
            return Ok(reservations.Select(ToResponse));
        }

        // GET api/reservations/dashboard - pending and approved-upcoming counts; prosumers get their own counts.
        [HttpGet("dashboard")]
        public async Task<ActionResult<ReservationCountsResponse>> GetDashboard([FromQuery] string? stationId, CancellationToken cancellationToken)
        {
            var prosumerNic = IsProsumer ? User.GetNic() : null;
            var counts = await _reservationService.GetDashboardCountsAsync(prosumerNic, stationId, cancellationToken);
            return Ok(counts);
        }

        // GET api/reservations/{id} - returns one reservation; prosumers can only see their own.
        [HttpGet("{id}")]
        public async Task<ActionResult<ReservationResponse>> GetById(string id, CancellationToken cancellationToken)
        {
            var reservation = await _reservationService.GetByIdAsync(id, cancellationToken);

            // Report another prosumer's reservation as not found rather than revealing it exists.
            if (reservation is null || (IsProsumer && reservation.ProsumerNic != User.GetNic()))
                throw ServiceException.NotFound("Reservation not found.");

            return Ok(ToResponse(reservation));
        }

        // POST api/reservations - books a slot; prosumers book for themselves, staff give the prosumer's NIC.
        [HttpPost]
        public async Task<ActionResult<ReservationResponse>> Create([FromBody] CreateReservationRequest request, CancellationToken cancellationToken)
        {
            var reservation = await _reservationService.CreateAsync(
                ResolveProsumerNic(request.ProsumerNic),
                request.SlotId,
                request.Type.GetValueOrDefault(),
                request.EnergyKWh.GetValueOrDefault(),
                User.GetNic(),
                cancellationToken);

            return CreatedAtAction(nameof(GetById), new { id = reservation.Id }, ToResponse(reservation));
        }

        // PUT api/reservations/{id} - changes slot, type or energy with at least 12 hours' notice.
        [HttpPut("{id}")]
        public async Task<ActionResult<ReservationResponse>> Update(string id, [FromBody] UpdateReservationRequest request, CancellationToken cancellationToken)
        {
            var reservation = await _reservationService.UpdateAsync(id, request.SlotId, request.Type, request.EnergyKWh, User.GetNic(), cancellationToken);
            return Ok(ToResponse(reservation));
        }

        // POST api/reservations/{id}/cancel - cancels with at least 12 hours' notice.
        [HttpPost("{id}/cancel")]
        public async Task<ActionResult<ReservationResponse>> Cancel(string id, [FromBody] CancelReservationRequest request, CancellationToken cancellationToken)
        {
            var reservation = await _reservationService.CancelAsync(id, request.Reason, User.GetNic(), cancellationToken);
            return Ok(ToResponse(reservation));
        }

        // POST api/reservations/{id}/approve - approves a pending reservation and issues its QR token; staff only.
        [Authorize(Roles = AppRoles.Staff)]
        [HttpPost("{id}/approve")]
        public async Task<ActionResult<ReservationResponse>> Approve(string id, CancellationToken cancellationToken)
        {
            var reservation = await _reservationService.ApproveAsync(id, User.GetNic(), cancellationToken);
            return Ok(ToResponse(reservation));
        }

        // POST api/reservations/verify-qr - checks a scanned QR code against the server before finalising; staff only.
        [Authorize(Roles = AppRoles.Staff)]
        [HttpPost("verify-qr")]
        public async Task<ActionResult<ReservationResponse>> VerifyQr([FromBody] QrTokenRequest request, CancellationToken cancellationToken)
        {
            var reservation = await _reservationService.VerifyQrAsync(request.QrToken, User.GetNic(), cancellationToken);
            return Ok(ToResponse(reservation));
        }

        // POST api/reservations/complete - finalises the energy transfer for a scanned QR code; staff only.
        [Authorize(Roles = AppRoles.Staff)]
        [HttpPost("complete")]
        public async Task<ActionResult<ReservationResponse>> Complete([FromBody] QrTokenRequest request, CancellationToken cancellationToken)
        {
            var reservation = await _reservationService.CompleteAsync(request.QrToken, User.GetNic(), cancellationToken);
            return Ok(ToResponse(reservation));
        }

        // Returns the prosumer a request is for: prosumers always get themselves, staff must name one.
        private string ResolveProsumerNic(string? requestedNic)
        {
            if (IsProsumer)
                return User.GetNic();

            if (string.IsNullOrWhiteSpace(requestedNic))
                throw ServiceException.BadRequest("prosumerNic is required.");

            return requestedNic;
        }

        // Maps a reservation to a response, including the QR token only for the prosumer who owns it.
        private ReservationResponse ToResponse(EnergyReservation reservation)
        {
            var isOwner = IsProsumer && reservation.ProsumerNic == User.GetNic();
            return ReservationResponse.FromModel(reservation, includeQrToken: isOwner);
        }
    }
}
