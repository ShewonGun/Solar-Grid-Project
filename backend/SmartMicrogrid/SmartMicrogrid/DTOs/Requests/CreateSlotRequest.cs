/*
 * File: CreateSlotRequest.cs
 * Purpose: Request body for POST api/slots, used by Backoffice and Grid
 *          Operators to open a booking window on a station's battery slot.
 * Author:  <your name>
 * Created: 2026
 */
using System.ComponentModel.DataAnnotations;
using SmartMicrogrid.Api.Models;
using SmartMicrogrid.Api.Models.Enums;

namespace SmartMicrogrid.Api.DTOs.Requests
{
    public class CreateSlotRequest
    {
        [Required]
        public string StationId { get; set; } = string.Empty;

        [Required]
        [Range(1, int.MaxValue)]
        public int? BatterySlotNumber { get; set; }

        // UTC, e.g. "2026-09-20T08:00:00Z".
        [Required]
        public DateTime? StartTime { get; set; }

        [Required]
        public DateTime? EndTime { get; set; }

        [Required]
        [Range(double.Epsilon, double.MaxValue, ErrorMessage = "Capacity must be greater than 0 kWh.")]
        public double? CapacityKWh { get; set; }

        public bool IsAvailable { get; set; } = true;

        // Maps the request to a new EnergyBookingSlot model.
        public EnergyBookingSlot ToModel()
        {
            return new EnergyBookingSlot
            {
                StationId = StationId,
                BatterySlotNumber = BatterySlotNumber.GetValueOrDefault(),
                StartTime = StartTime.GetValueOrDefault(),
                EndTime = EndTime.GetValueOrDefault(),
                CapacityKWh = CapacityKWh.GetValueOrDefault(),
                Status = IsAvailable ? SlotStatus.Available : SlotStatus.Unavailable
            };
        }
    }
}
