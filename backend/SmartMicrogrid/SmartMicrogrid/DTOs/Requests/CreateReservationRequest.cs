/*
 * File: CreateReservationRequest.cs
 * Purpose: Request body for POST api/reservations. Prosumers always book for
 *          themselves; Backoffice and Grid Operators must give the prosumer's
 *          NIC to book on their behalf.
 * Author:  <your name>
 * Created: 2026
 */
using System.ComponentModel.DataAnnotations;
using SmartMicrogrid.Api.Models.Enums;

namespace SmartMicrogrid.Api.DTOs.Requests
{
    public class CreateReservationRequest
    {
        // Ignored for prosumers; required when staff book for a prosumer.
        public string? ProsumerNic { get; set; }

        [Required]
        public string SlotId { get; set; } = string.Empty;

        [Required]
        public ReservationType? Type { get; set; }

        [Required]
        [Range(double.Epsilon, double.MaxValue, ErrorMessage = "Energy must be greater than 0 kWh.")]
        public double? EnergyKWh { get; set; }
    }
}
