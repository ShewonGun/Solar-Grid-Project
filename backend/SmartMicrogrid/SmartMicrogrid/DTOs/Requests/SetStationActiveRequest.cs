/*
 * File: SetStationActiveRequest.cs
 * Purpose: Request body for PATCH api/stations/{id}/active.
 * Author:  Shewon Gunarathne
 * Created: 2026-09-24
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
