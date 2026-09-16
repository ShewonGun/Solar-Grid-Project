/*
 * File: CorsSettings.cs
 * Purpose: Strongly typed "Cors" configuration section listing the web app
 *          origins allowed to call the API from a browser. The Android app
 *          is not affected by CORS.
 * Author:  <your name>
 * Created: 2026
 */
namespace SmartMicrogrid.Api.Settings
{
    public class CorsSettings
    {
        public const string SectionName = "Cors";

        public const string PolicyName = "WebClient";

        // Exact origins, e.g. "http://localhost:5173" or "https://microgrid.example.com".
        public string[] AllowedOrigins { get; set; } = [];
    }
}
