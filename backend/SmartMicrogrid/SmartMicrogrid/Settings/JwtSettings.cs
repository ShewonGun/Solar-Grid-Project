/*
 * File: JwtSettings.cs
 * Purpose: Strongly typed "Jwt" configuration section used to issue and
 *          validate login tokens. The signing key is a secret and is supplied
 *          through user secrets (development) or environment variables (IIS),
 *          not appsettings.json.
 * Author:  <your name>
 * Created: 2026
 */
namespace SmartMicrogrid.Api.Settings
{
    public class JwtSettings
    {
        public const string SectionName = "Jwt";

        // HMAC-SHA256 needs a key of at least 256 bits.
        public const int MinKeyBytes = 32;

        public string Key { get; set; } = string.Empty;

        public string Issuer { get; set; } = string.Empty;

        public string Audience { get; set; } = string.Empty;

        // How long a login token stays valid.
        public int ExpiryMinutes { get; set; } = 480;
    }
}
