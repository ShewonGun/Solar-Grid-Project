/*
 * File: StationRequest.cs
 * Purpose: Request body for creating (POST api/stations) and updating
 *          (PUT api/stations/{id}) a solar station.
 * Author:  <your name>
 * Created: 2026
 */
using System.ComponentModel.DataAnnotations;
using SmartMicrogrid.Api.Models;

namespace SmartMicrogrid.Api.DTOs.Requests
{
    public class StationRequest
    {
        [Required]
        [StringLength(100)]
        public string StationName { get; set; } = string.Empty;

        [Required]
        [Range(-90, 90)]
        public double? Latitude { get; set; }

        [Required]
        [Range(-180, 180)]
        public double? Longitude { get; set; }

        [Required]
        [Range(double.Epsilon, double.MaxValue, ErrorMessage = "Capacity must be greater than 0 kWh.")]
        public double? CapacityKWh { get; set; }

        [Required]
        [Range(1, int.MaxValue)]
        public int? TotalBatterySlots { get; set; }

        [StringLength(200)]
        public string OperatingSchedule { get; set; } = string.Empty;

        // Maps the request to a SolarStationInfo model.
        public SolarStationInfo ToModel()
        {
            return new SolarStationInfo
            {
                StationName = StationName,
                Latitude = Latitude.GetValueOrDefault(),
                Longitude = Longitude.GetValueOrDefault(),
                CapacityKWh = CapacityKWh.GetValueOrDefault(),
                TotalBatterySlots = TotalBatterySlots.GetValueOrDefault(),
                OperatingSchedule = OperatingSchedule
            };
        }
    }
}
