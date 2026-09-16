/*
 * File: ValidationHelper.cs
 * Purpose: Small shared helpers used by the service layer - validating
 *          MongoDB ObjectId strings and normalising DateTime values to UTC
 *          before they are compared or stored.
 * Author:  <your name>
 * Created: 2026
 */
using MongoDB.Bson;

namespace SmartMicrogrid.Api.Helpers
{
    public static class ValidationHelper
    {
        // Returns true if the value is a valid 24-character MongoDB ObjectId.
        public static bool IsValidObjectId(string? id)
        {
            return !string.IsNullOrWhiteSpace(id) && ObjectId.TryParse(id, out _);
        }

        // Converts a DateTime to UTC; values with no kind are assumed to already be UTC.
        public static DateTime ToUtc(DateTime value)
        {
            return value.Kind switch
            {
                DateTimeKind.Utc => value,
                DateTimeKind.Local => value.ToUniversalTime(),
                _ => DateTime.SpecifyKind(value, DateTimeKind.Utc)
            };
        }
    }
}
