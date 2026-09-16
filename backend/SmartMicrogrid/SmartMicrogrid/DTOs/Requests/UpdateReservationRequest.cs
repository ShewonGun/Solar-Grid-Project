/*
 * File: UpdateReservationRequest.cs
 * Purpose: Request body for PUT api/reservations/{id}. Every field is
 *          optional; only the fields provided are changed.
 * Author:  <your name>
 * Created: 2026
 */
using System.ComponentModel.DataAnnotations;
using SmartMicrogrid.Api.Models.Enums;

namespace SmartMicrogrid.Api.DTOs.Requests
{
    public class UpdateReservationRequest
    {
        // Move the reservation to a different slot.
        public string? SlotId { get; set; }

        public ReservationType? Type { get; set; }

        [Range(double.Epsilon, double.MaxValue, ErrorMessage = "Energy must be greater than 0 kWh.")]
        public double? EnergyKWh { get; set; }
    }
}
