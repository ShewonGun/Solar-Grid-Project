/*
 * File: CancelReservationRequest.cs
 * Purpose: Request body for POST api/reservations/{id}/cancel.
 * Author:  <your name>
 * Created: 2026
 */
using System.ComponentModel.DataAnnotations;

namespace SmartMicrogrid.Api.DTOs.Requests
{
    public class CancelReservationRequest
    {
        [StringLength(500)]
        public string? Reason { get; set; }
    }
}
