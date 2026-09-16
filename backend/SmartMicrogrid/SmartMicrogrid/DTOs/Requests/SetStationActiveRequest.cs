/*
 * File: SetStationActiveRequest.cs
 * Purpose: Request body for PATCH api/stations/{id}/active.
 * Author:  <your name>
 * Created: 2026
 */
using System.ComponentModel.DataAnnotations;

namespace SmartMicrogrid.Api.DTOs.Requests
{
    public class SetStationActiveRequest
    {
        [Required]
        public bool? IsActive { get; set; }
    }
}
