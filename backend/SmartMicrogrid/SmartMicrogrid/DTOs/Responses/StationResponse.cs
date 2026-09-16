/*
 * File: StationResponse.cs
 * Purpose: Solar station details returned to the web and mobile apps.
 * Author:  <your name>
 * Created: 2026
 */
using SmartMicrogrid.Api.Models;

namespace SmartMicrogrid.Api.DTOs.Responses
{
    public class StationResponse
    {
        public string Id { get; set; } = string.Empty;

        public string StationName { get; set; } = string.Empty;

        public double Latitude { get; set; }

        public double Longitude { get; set; }

        public double CapacityKWh { get; set; }

        public int TotalBatterySlots { get; set; }

        public string OperatingSchedule { get; set; } = string.Empty;

        public bool IsActive { get; set; }

        public DateTime CreatedAt { get; set; }

        public DateTime UpdatedAt { get; set; }

        // Maps a SolarStationInfo model to a response.
        public static StationResponse FromModel(SolarStationInfo station)
        {
            return new StationResponse
            {
                Id = station.Id ?? string.Empty,
                StationName = station.StationName,
                Latitude = station.Latitude,
                Longitude = station.Longitude,
                CapacityKWh = station.CapacityKWh,
                TotalBatterySlots = station.TotalBatterySlots,
                OperatingSchedule = station.OperatingSchedule,
                IsActive = station.IsActive,
                CreatedAt = station.CreatedAt,
                UpdatedAt = station.UpdatedAt
            };
        }
    }
}
