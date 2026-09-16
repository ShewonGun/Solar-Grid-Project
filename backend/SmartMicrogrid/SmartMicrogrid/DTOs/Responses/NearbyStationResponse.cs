/*
 * File: NearbyStationResponse.cs
 * Purpose: A station plotted on the mobile map, with its distance from the
 *          prosumer's current location.
 * Author:  <your name>
 * Created: 2026
 */
using SmartMicrogrid.Api.Models;

namespace SmartMicrogrid.Api.DTOs.Responses
{
    public class NearbyStationResponse
    {
        public StationResponse Station { get; set; } = new();

        // Straight-line distance from the requested location, in kilometres.
        public double DistanceKm { get; set; }

        // Maps a station and its distance to a response, rounding the distance to 2 decimal places.
        public static NearbyStationResponse FromModel(SolarStationInfo station, double distanceKm)
        {
            return new NearbyStationResponse
            {
                Station = StationResponse.FromModel(station),
                DistanceKm = Math.Round(distanceKm, 2)
            };
        }
    }
}
