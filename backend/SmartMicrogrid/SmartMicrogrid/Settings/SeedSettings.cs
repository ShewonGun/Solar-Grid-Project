/*
 * File: SeedSettings.cs
 * Purpose: Strongly typed "Seed" configuration section describing the first
 *          Backoffice account created on startup when none exists. The
 *          password is a secret and is supplied through user secrets
 *          (development) or environment variables (IIS).
 * Author:  <your name>
 * Created: 2026
 */
namespace SmartMicrogrid.Api.Settings
{
    public class SeedSettings
    {
        public const string SectionName = "Seed";

        public string AdminNic { get; set; } = string.Empty;

        public string AdminFullName { get; set; } = string.Empty;

        public string AdminEmail { get; set; } = string.Empty;

        public string AdminPassword { get; set; } = string.Empty;
    }
}
