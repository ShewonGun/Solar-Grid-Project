/*
 * File: SolarStationInfo.cs
 * Purpose: Represents a solar microgrid hub/node - GPS location, capacity
 *          in kW/h, and available battery storage slots. Maps to the
 *          "SolarStationInfo" collection named in the marking scheme.
 * Author:  <your name>
 * Created: 2026
 */
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartMicrogrid.Api.Models
{
    public class SolarStationInfo
    {
        [BsonId]
        [BsonRepresentation(BsonType.ObjectId)]
        public string? Id { get; set; }

        [BsonElement("stationName")]
        public string StationName { get; set; } = string.Empty;

        [BsonElement("latitude")]
        public double Latitude { get; set; }

        [BsonElement("longitude")]
        public double Longitude { get; set; }

        // Capacity of the hub, in kW/h.
        [BsonElement("capacityKWh")]
        public double CapacityKWh { get; set; }

        // Total number of physical battery storage slots at this hub.
        [BsonElement("totalBatterySlots")]
        public int TotalBatterySlots { get; set; }

        // Free-text or structured operating schedule, e.g. "Mon-Sun 06:00-22:00".
        [BsonElement("operatingSchedule")]
        public string OperatingSchedule { get; set; } = string.Empty;

        [BsonElement("isActive")]
        public bool IsActive { get; set; } = true;

        [BsonElement("createdAt")]
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

        [BsonElement("updatedAt")]
        public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
    }
}
