/*
 * File: MongoDbSettings.cs
 * Purpose: Strongly typed "MongoDb" configuration section - the connection
 *          string and database name. The connection string is a secret and
 *          is supplied through user secrets (development) or environment
 *          variables (IIS), not appsettings.json.
 * Author:  <your name>
 * Created: 2026
 */
namespace SmartMicrogrid.Api.Settings
{
    public class MongoDbSettings
    {
        public const string SectionName = "MongoDb";

        public string ConnectionString { get; set; } = string.Empty;

        public string DatabaseName { get; set; } = string.Empty;
    }
}
