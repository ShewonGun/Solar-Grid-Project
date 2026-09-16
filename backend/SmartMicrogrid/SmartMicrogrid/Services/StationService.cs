/*
 * File: StationService.cs
 * Purpose: Service layer for solar microgrid nodes (SolarStationInfo).
 *          Holds all data access and business logic for creating, reading,
 *          updating, activating/deactivating and deleting stations.
 *          Station management is Backoffice only, and a station cannot be
 *          deactivated while it has active energy reservations.
 * Author:  <your name>
 * Created: 2026
 */
using MongoDB.Driver;
using SmartMicrogrid.Api.Data;
using SmartMicrogrid.Api.DTOs.Responses;
using SmartMicrogrid.Api.Helpers;
using SmartMicrogrid.Api.Models;
using SmartMicrogrid.Api.Models.Enums;
using SmartMicrogrid.Api.Services.Interfaces;

namespace SmartMicrogrid.Api.Services
{
    public class StationService : IStationService
    {
        public const string CollectionName = "SolarStationInfo";

        // Largest search radius allowed for the nearby-stations map.
        public const double MaxNearbyRadiusKm = 500;

        private static readonly FindOneAndUpdateOptions<SolarStationInfo> ReturnUpdated = new() { ReturnDocument = ReturnDocument.After };

        private readonly IMongoCollection<SolarStationInfo> _stations;
        private readonly IMongoCollection<EnergyBookingSlot> _slots;
        private readonly IMongoCollection<EnergyReservation> _reservations;
        private readonly IUserService _users;

        // Resolves the station, slot and reservation collections from the shared MongoDB context.
        public StationService(MongoDbContext context, IUserService users)
        {
            _stations = context.GetCollection<SolarStationInfo>(CollectionName);
            _slots = context.GetCollection<EnergyBookingSlot>(EnergyBookingSlotService.CollectionName);
            _reservations = context.GetCollection<EnergyReservation>(EnergyReservationService.CollectionName);
            _users = users;
        }

        // Returns all stations sorted by name, optionally only the active ones.
        public async Task<List<SolarStationInfo>> GetAllAsync(bool activeOnly = false, CancellationToken cancellationToken = default)
        {
            var filter = activeOnly
                ? Builders<SolarStationInfo>.Filter.Eq(s => s.IsActive, true)
                : Builders<SolarStationInfo>.Filter.Empty;

            return await _stations.Find(filter)
                .SortBy(s => s.StationName)
                .ToListAsync(cancellationToken);
        }

        // Returns a single station by its id, or null if it does not exist.
        public async Task<SolarStationInfo?> GetByIdAsync(string id, CancellationToken cancellationToken = default)
        {
            if (!ValidationHelper.IsValidObjectId(id))
                return null;

            return await _stations.Find(s => s.Id == id)
                .FirstOrDefaultAsync(cancellationToken);
        }

        // Returns active stations within radiusKm of a location, nearest first, with their distance.
        public async Task<List<NearbyStationResponse>> GetNearbyAsync(double latitude, double longitude, double radiusKm, CancellationToken cancellationToken = default)
        {
            if (latitude is < -90 or > 90 || longitude is < -180 or > 180)
                throw ServiceException.BadRequest("Latitude must be between -90 and 90 and longitude between -180 and 180.");

            if (radiusKm <= 0 || radiusKm > MaxNearbyRadiusKm)
                throw ServiceException.BadRequest($"Radius must be greater than 0 and at most {MaxNearbyRadiusKm} km.");

            var stations = await GetAllAsync(activeOnly: true, cancellationToken);

            return stations
                .Select(s => NearbyStationResponse.FromModel(s, GeoHelper.DistanceKm(latitude, longitude, s.Latitude, s.Longitude)))
                .Where(s => s.DistanceKm <= radiusKm)
                .OrderBy(s => s.DistanceKm)
                .ToList();
        }

        // Creates a new station; Backoffice only. The database assigns the id.
        public async Task<SolarStationInfo> CreateAsync(SolarStationInfo station, string actorNic, CancellationToken cancellationToken = default)
        {
            await _users.RequireUserAsync(actorNic, [UserRole.Backoffice], cancellationToken);
            Validate(station);

            var now = DateTime.UtcNow;
            station.Id = null;
            station.IsActive = true;
            station.CreatedAt = now;
            station.UpdatedAt = now;

            await _stations.InsertOneAsync(station, cancellationToken: cancellationToken);
            return station;
        }

        // Updates a station's details and schedule; Backoffice only. Use SetActiveAsync to change IsActive.
        public async Task<SolarStationInfo> UpdateAsync(string id, SolarStationInfo station, string actorNic, CancellationToken cancellationToken = default)
        {
            await _users.RequireUserAsync(actorNic, [UserRole.Backoffice], cancellationToken);
            await RequireExistingAsync(id, cancellationToken);
            Validate(station);

            // Battery slots cannot be reduced below slot numbers that already have booking slots.
            var slotBeyondCapacity = await _slots.Find(s => s.StationId == id && s.BatterySlotNumber > station.TotalBatterySlots)
                .AnyAsync(cancellationToken);
            if (slotBeyondCapacity)
                throw ServiceException.Conflict("Booking slots exist for battery slots above the new total. Remove them first.");

            var update = Builders<SolarStationInfo>.Update
                .Set(s => s.StationName, station.StationName)
                .Set(s => s.Latitude, station.Latitude)
                .Set(s => s.Longitude, station.Longitude)
                .Set(s => s.CapacityKWh, station.CapacityKWh)
                .Set(s => s.TotalBatterySlots, station.TotalBatterySlots)
                .Set(s => s.OperatingSchedule, station.OperatingSchedule)
                .Set(s => s.UpdatedAt, DateTime.UtcNow);

            return await _stations.FindOneAndUpdateAsync(s => s.Id == id, update, ReturnUpdated, cancellationToken)
                ?? throw ServiceException.NotFound("Station not found.");
        }

        // Activates or deactivates a station; Backoffice only.
        // Deactivation is blocked while the station has pending or approved reservations that have not ended.
        public async Task<SolarStationInfo> SetActiveAsync(string id, bool isActive, string actorNic, CancellationToken cancellationToken = default)
        {
            await _users.RequireUserAsync(actorNic, [UserRole.Backoffice], cancellationToken);
            await RequireExistingAsync(id, cancellationToken);

            if (!isActive && await HasActiveReservationsAsync(id, cancellationToken))
                throw ServiceException.Conflict("This station has active energy reservations and cannot be deactivated.");

            var update = Builders<SolarStationInfo>.Update
                .Set(s => s.IsActive, isActive)
                .Set(s => s.UpdatedAt, DateTime.UtcNow);

            return await _stations.FindOneAndUpdateAsync(s => s.Id == id, update, ReturnUpdated, cancellationToken)
                ?? throw ServiceException.NotFound("Station not found.");
        }

        // Deletes a station and its booking slots; Backoffice only.
        // Blocked if any reservation references the station, so booking history stays consistent.
        public async Task DeleteAsync(string id, string actorNic, CancellationToken cancellationToken = default)
        {
            await _users.RequireUserAsync(actorNic, [UserRole.Backoffice], cancellationToken);
            await RequireExistingAsync(id, cancellationToken);

            var hasReservations = await _reservations.Find(r => r.StationId == id)
                .AnyAsync(cancellationToken);
            if (hasReservations)
                throw ServiceException.Conflict("This station has reservations and cannot be deleted. Deactivate it instead.");

            await _stations.DeleteOneAsync(s => s.Id == id, cancellationToken);
            await _slots.DeleteManyAsync(s => s.StationId == id, cancellationToken);
        }

        // Returns true if the station has pending or approved reservations that have not ended yet.
        public async Task<bool> HasActiveReservationsAsync(string id, CancellationToken cancellationToken = default)
        {
            var now = DateTime.UtcNow;
            var f = Builders<EnergyReservation>.Filter;
            var filter = f.Eq(r => r.StationId, id)
                & f.In(r => r.Status, EnergyReservationService.OpenStatuses)
                & f.Gt(r => r.ReservationEnd, now);

            return await _reservations.Find(filter).AnyAsync(cancellationToken);
        }

        // Loads a station by id or throws NotFound.
        private async Task<SolarStationInfo> RequireExistingAsync(string id, CancellationToken cancellationToken)
        {
            return await GetByIdAsync(id, cancellationToken)
                ?? throw ServiceException.NotFound("Station not found.");
        }

        // Trims text fields and checks name, GPS coordinates, capacity and battery slots are valid.
        private static void Validate(SolarStationInfo station)
        {
            station.StationName = (station.StationName ?? string.Empty).Trim();
            station.OperatingSchedule = (station.OperatingSchedule ?? string.Empty).Trim();

            if (station.StationName.Length == 0)
                throw ServiceException.BadRequest("Station name is required.");

            if (station.Latitude is < -90 or > 90)
                throw ServiceException.BadRequest("Latitude must be between -90 and 90.");

            if (station.Longitude is < -180 or > 180)
                throw ServiceException.BadRequest("Longitude must be between -180 and 180.");

            if (station.CapacityKWh <= 0)
                throw ServiceException.BadRequest("Capacity must be greater than 0 kWh.");

            if (station.TotalBatterySlots <= 0)
                throw ServiceException.BadRequest("A station needs at least one battery slot.");
        }
    }
}
