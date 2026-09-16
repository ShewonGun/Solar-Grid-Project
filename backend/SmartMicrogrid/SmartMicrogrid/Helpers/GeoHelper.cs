/*
 * File: GeoHelper.cs
 * Purpose: Geographic calculations used to find grid nodes near a prosumer's
 *          location for the mobile map.
 * Author:  <your name>
 * Created: 2026
 */
namespace SmartMicrogrid.Api.Helpers
{
    public static class GeoHelper
    {
        private const double EarthRadiusKm = 6371.0;

        // Returns the great-circle distance between two GPS points in kilometres (haversine formula).
        public static double DistanceKm(double latitude1, double longitude1, double latitude2, double longitude2)
        {
            var deltaLatitude = ToRadians(latitude2 - latitude1);
            var deltaLongitude = ToRadians(longitude2 - longitude1);

            var a = Math.Sin(deltaLatitude / 2) * Math.Sin(deltaLatitude / 2)
                + Math.Cos(ToRadians(latitude1)) * Math.Cos(ToRadians(latitude2))
                * Math.Sin(deltaLongitude / 2) * Math.Sin(deltaLongitude / 2);

            return EarthRadiusKm * 2 * Math.Atan2(Math.Sqrt(a), Math.Sqrt(1 - a));
        }

        // Converts degrees to radians.
        private static double ToRadians(double degrees)
        {
            return degrees * Math.PI / 180.0;
        }
    }
}
