/*
 * File: SlotResponse.cs
 * Purpose: Energy booking slot details returned to the web and mobile apps.
 * Author:  <your name>
 * Created: 2026
 */
using SmartMicrogrid.Api.Models;
using SmartMicrogrid.Api.Models.Enums;

namespace SmartMicrogrid.Api.DTOs.Responses
{
    public class SlotResponse
    {
        public string Id { get; set; } = string.Empty;

        public string StationId { get; set; } = string.Empty;

        public int BatterySlotNumber { get; set; }

        public DateTime StartTime { get; set; }

        public DateTime EndTime { get; set; }

        public double CapacityKWh { get; set; }

        public SlotStatus Status { get; set; }

        public string? UpdatedBy { get; set; }

        public DateTime CreatedAt { get; set; }

        public DateTime UpdatedAt { get; set; }

        // Maps an EnergyBookingSlot model to a response.
        public static SlotResponse FromModel(EnergyBookingSlot slot)
        {
            return new SlotResponse
            {
                Id = slot.Id ?? string.Empty,
                StationId = slot.StationId,
                BatterySlotNumber = slot.BatterySlotNumber,
                StartTime = slot.StartTime,
                EndTime = slot.EndTime,
                CapacityKWh = slot.CapacityKWh,
                Status = slot.Status,
                UpdatedBy = slot.UpdatedBy,
                CreatedAt = slot.CreatedAt,
                UpdatedAt = slot.UpdatedAt
            };
        }
    }
}
