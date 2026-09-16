/*
 * File: DatabaseInitializer.cs
 * Purpose: Runs once at startup to prepare MongoDB - creates the indexes that
 *          enforce uniqueness (email, QR token) and speed up common queries,
 *          and seeds the first Backoffice user so the system can be
 *          administered. Safe to run on every startup.
 * Author:  <your name>
 * Created: 2026
 */
using Microsoft.Extensions.Options;
using MongoDB.Bson;
using MongoDB.Driver;
using SmartMicrogrid.Api.Models;
using SmartMicrogrid.Api.Services;
using SmartMicrogrid.Api.Services.Interfaces;
using SmartMicrogrid.Api.Settings;

namespace SmartMicrogrid.Api.Data
{
    public class DatabaseInitializer
    {
        private readonly MongoDbContext _context;
        private readonly IUserService _userService;
        private readonly SeedSettings _seedSettings;
        private readonly ILogger<DatabaseInitializer> _logger;

        // Receives the database context, user service, seed settings and logger.
        public DatabaseInitializer(MongoDbContext context, IUserService userService, IOptions<SeedSettings> seedSettings, ILogger<DatabaseInitializer> logger)
        {
            _context = context;
            _userService = userService;
            _seedSettings = seedSettings.Value;
            _logger = logger;
        }

        // Creates indexes and seeds the first Backoffice user.
        public async Task InitializeAsync(CancellationToken cancellationToken = default)
        {
            await CreateIndexesAsync(cancellationToken);
            await SeedBackofficeUserAsync(cancellationToken);
        }

        // Creates the indexes for all collections; existing indexes with the same definition are left unchanged.
        private async Task CreateIndexesAsync(CancellationToken cancellationToken)
        {
            var users = _context.GetCollection<User>(UserService.CollectionName);
            await users.Indexes.CreateManyAsync(
            [
                new CreateIndexModel<User>(
                    Builders<User>.IndexKeys.Ascending(u => u.Email),
                    new CreateIndexOptions { Name = "ux_email", Unique = true }),
                new CreateIndexModel<User>(
                    Builders<User>.IndexKeys.Ascending(u => u.Role).Ascending(u => u.Status),
                    new CreateIndexOptions { Name = "ix_role_status" })
            ], cancellationToken);

            var slots = _context.GetCollection<EnergyBookingSlot>(EnergyBookingSlotService.CollectionName);
            await slots.Indexes.CreateManyAsync(
            [
                new CreateIndexModel<EnergyBookingSlot>(
                    Builders<EnergyBookingSlot>.IndexKeys.Ascending(s => s.StationId).Ascending(s => s.BatterySlotNumber).Ascending(s => s.StartTime),
                    new CreateIndexOptions { Name = "ix_station_battery_start" }),
                new CreateIndexModel<EnergyBookingSlot>(
                    Builders<EnergyBookingSlot>.IndexKeys.Ascending(s => s.Status).Ascending(s => s.StartTime),
                    new CreateIndexOptions { Name = "ix_status_start" })
            ], cancellationToken);

            var reservations = _context.GetCollection<EnergyReservation>(EnergyReservationService.CollectionName);
            await reservations.Indexes.CreateManyAsync(
            [
                // Unique only where a token exists, so many reservations can have no token.
                new CreateIndexModel<EnergyReservation>(
                    Builders<EnergyReservation>.IndexKeys.Ascending(r => r.QrToken),
                    new CreateIndexOptions<EnergyReservation>
                    {
                        Name = "ux_qr_token",
                        Unique = true,
                        PartialFilterExpression = Builders<EnergyReservation>.Filter.Type(r => r.QrToken, BsonType.String)
                    }),
                new CreateIndexModel<EnergyReservation>(
                    Builders<EnergyReservation>.IndexKeys.Ascending(r => r.ProsumerNic).Descending(r => r.ReservationStart),
                    new CreateIndexOptions { Name = "ix_prosumer_start" }),
                new CreateIndexModel<EnergyReservation>(
                    Builders<EnergyReservation>.IndexKeys.Ascending(r => r.StationId).Ascending(r => r.Status),
                    new CreateIndexOptions { Name = "ix_station_status" })
            ], cancellationToken);
        }

        // Creates the configured Backoffice user if no Backoffice user exists yet.
        private async Task SeedBackofficeUserAsync(CancellationToken cancellationToken)
        {
            if (string.IsNullOrWhiteSpace(_seedSettings.AdminNic) || string.IsNullOrWhiteSpace(_seedSettings.AdminPassword))
            {
                _logger.LogInformation("Seed:AdminNic or Seed:AdminPassword is not configured; skipping Backoffice user seeding.");
                return;
            }

            var admin = new User
            {
                Nic = _seedSettings.AdminNic,
                FullName = _seedSettings.AdminFullName,
                Email = _seedSettings.AdminEmail
            };

            if (await _userService.EnsureBackofficeExistsAsync(admin, _seedSettings.AdminPassword, cancellationToken))
            {
                _logger.LogInformation("Seeded initial Backoffice user {Nic}.", admin.Nic);
            }
        }
    }
}
