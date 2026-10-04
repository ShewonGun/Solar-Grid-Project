/*
 * File: CancelReservationRequest.cs
 * Purpose: Request body for POST api/reservations/{id}/cancel.
 * Author:  Aseni Thennakoon
 * Created: 2026-09-29
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
