/*
 * File: ReservationResponse.cs
 * Purpose: Energy reservation details returned to the web and mobile apps.
 *          The QR token is only included for the prosumer who owns the
 *          reservation, so it cannot be copied from staff listings.
 * Author:  <your name>
 * Created: 2026
 */
using SmartMicrogrid.Api.Models;
using SmartMicrogrid.Api.Models.Enums;

namespace SmartMicrogrid.Api.DTOs.Responses
{
    public class ReservationResponse
    {
        public string Id { get; set; } = string.Empty;

        public string ProsumerNic { get; set; } = string.Empty;

        public string StationId { get; set; } = string.Empty;

        public string SlotId { get; set; } = string.Empty;

        public ReservationType Type { get; set; }

        public double EnergyKWh { get; set; }

        public DateTime ReservationStart { get; set; }

        public DateTime ReservationEnd { get; set; }

        public ReservationStatus Status { get; set; }

        // Only set for the owning prosumer once the reservation is approved.
        public string? QrToken { get; set; }

        public string? ApprovedBy { get; set; }

        public DateTime? ApprovedAt { get; set; }

        public string? CompletedBy { get; set; }

        public DateTime? CompletedAt { get; set; }

        public string? CancelledBy { get; set; }

        public DateTime? CancelledAt { get; set; }

        public string? CancellationReason { get; set; }

        public DateTime CreatedAt { get; set; }

        public DateTime UpdatedAt { get; set; }

        // Maps an EnergyReservation model to a response, including the QR token only when requested.
        public static ReservationResponse FromModel(EnergyReservation reservation, bool includeQrToken)
        {
            return new ReservationResponse
            {
                Id = reservation.Id ?? string.Empty,
                ProsumerNic = reservation.ProsumerNic,
                StationId = reservation.StationId,
                SlotId = reservation.SlotId,
                Type = reservation.Type,
                EnergyKWh = reservation.EnergyKWh,
                ReservationStart = reservation.ReservationStart,
                ReservationEnd = reservation.ReservationEnd,
                Status = reservation.Status,
                QrToken = includeQrToken ? reservation.QrToken : null,
                ApprovedBy = reservation.ApprovedBy,
                ApprovedAt = reservation.ApprovedAt,
                CompletedBy = reservation.CompletedBy,
                CompletedAt = reservation.CompletedAt,
                CancelledBy = reservation.CancelledBy,
                CancelledAt = reservation.CancelledAt,
                CancellationReason = reservation.CancellationReason,
                CreatedAt = reservation.CreatedAt,
                UpdatedAt = reservation.UpdatedAt
            };
        }
    }
}
