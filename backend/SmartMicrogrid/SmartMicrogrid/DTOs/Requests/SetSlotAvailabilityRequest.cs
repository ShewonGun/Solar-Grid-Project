/*
 * File: SetSlotAvailabilityRequest.cs
 * Purpose: Request body for PATCH api/slots/{id}/availability, used by Grid
 *          Operators to open or close a battery slot window.
 * Author:  <your name>
 * Created: 2026
 */
using System.ComponentModel.DataAnnotations;

namespace SmartMicrogrid.Api.DTOs.Requests
{
    public class SetSlotAvailabilityRequest
    {
        [Required]
        public bool? IsAvailable { get; set; }
    }
}
