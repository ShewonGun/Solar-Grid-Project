/*
 * File: EnergyBookingSlot.cs
 * Purpose: Represents a bookable time window on one battery storage slot of a
 *          solar station. Grid Operators update slot availability. Maps to the
 *          "EnergyBookingSlots" collection named in the marking scheme.
 * Author:  <your name>
 * Created: 2026
 */
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;
using SmartMicrogrid.Api.Models.Enums;

namespace SmartMicrogrid.Api.Models
{
    public class EnergyBookingSlot
    {
        [BsonId]
        [BsonRepresentation(BsonType.ObjectId)]
        public string? Id { get; set; }

        // Reference to SolarStationInfo.Id.
        [BsonElement("stationId")]
        [BsonRepresentation(BsonType.ObjectId)]
        public string StationId { get; set; } = string.Empty;

        // Physical battery slot number at the station (1..TotalBatterySlots).
        [BsonElement("batterySlotNumber")]
        public int BatterySlotNumber { get; set; }

        [BsonElement("startTime")]
        public DateTime StartTime { get; set; }

        [BsonElement("endTime")]
        public DateTime EndTime { get; set; }

        // Energy that can be stored or drawn in this window, in kWh.
        [BsonElement("capacityKWh")]
        public double CapacityKWh { get; set; }

        [BsonElement("status")]
        [BsonRepresentation(BsonType.String)]
        public SlotStatus Status { get; set; } = SlotStatus.Available;

        // NIC of the Grid Operator who last changed availability.
        [BsonElement("updatedBy")]
        public string? UpdatedBy { get; set; }

        [BsonElement("createdAt")]
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

        [BsonElement("updatedAt")]
        public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
    }
}
