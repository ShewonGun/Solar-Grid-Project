/*
 * File: EnergyBookingSlotService.cs
 * Purpose: Service layer for energy booking slots (EnergyBookingSlots
 *          collection). Backoffice and Grid Operators create slots on a
 *          station's battery slots and update their availability. Slots
 *          cannot overlap on the same battery slot, and reserved slots cannot
 *          be edited or deleted. Also provides atomic reserve/release used by
 *          the reservation service to prevent double booking.
 * Author:  <your name>
 * Created: 2026
 */
using MongoDB.Driver;
using SmartMicrogrid.Api.Data;
using SmartMicrogrid.Api.Helpers;
using SmartMicrogrid.Api.Models;
using SmartMicrogrid.Api.Models.Enums;
using SmartMicrogrid.Api.Services.Interfaces;

namespace SmartMicrogrid.Api.Services
{
    public class EnergyBookingSlotService : IEnergyBookingSlotService
    {
        public const string CollectionName = "EnergyBookingSlots";

        private static readonly FindOneAndUpdateOptions<EnergyBookingSlot> ReturnUpdated = new() { ReturnDocument = ReturnDocument.After };

        private readonly IMongoCollection<EnergyBookingSlot> _slots;
        private readonly IStationService _stations;
        private readonly IUserService _users;

        // Resolves the EnergyBookingSlots collection and the services this one depends on.
        public EnergyBookingSlotService(MongoDbContext context, IStationService stations, IUserService users)
        {
            _slots = context.GetCollection<EnergyBookingSlot>(CollectionName);
            _stations = stations;
            _users = users;
        }

        // Returns a station's slots, optionally within a start-time range and by status, in time order.
        public async Task<List<EnergyBookingSlot>> GetByStationAsync(string stationId, DateTime? from = null, DateTime? to = null, SlotStatus? status = null, CancellationToken cancellationToken = default)
        {
            if (!ValidationHelper.IsValidObjectId(stationId))
                return [];

            var f = Builders<EnergyBookingSlot>.Filter;
            var filter = f.Eq(s => s.StationId, stationId);

            if (from.HasValue)
                filter &= f.Gte(s => s.StartTime, ValidationHelper.ToUtc(from.Value));

            if (to.HasValue)
                filter &= f.Lte(s => s.StartTime, ValidationHelper.ToUtc(to.Value));

            if (status.HasValue)
                filter &= f.Eq(s => s.Status, status.Value);

            return await _slots.Find(filter)
                .SortBy(s => s.StartTime)
                .ThenBy(s => s.BatterySlotNumber)
                .ToListAsync(cancellationToken);
        }

        // Returns slots a prosumer can book now: available, at an active station, starting within the 7-day window.
        public async Task<List<EnergyBookingSlot>> GetBookableAsync(string? stationId = null, CancellationToken cancellationToken = default)
        {
            var activeStationIds = (await _stations.GetAllAsync(activeOnly: true, cancellationToken))
                .Select(s => s.Id!)
                .ToList();

            if (!string.IsNullOrWhiteSpace(stationId))
                activeStationIds = activeStationIds.Where(id => id == stationId).ToList();

            if (activeStationIds.Count == 0)
                return [];

            var now = DateTime.UtcNow;
            var f = Builders<EnergyBookingSlot>.Filter;
            var filter = f.In(s => s.StationId, activeStationIds)
                & f.Eq(s => s.Status, SlotStatus.Available)
                & f.Gt(s => s.StartTime, now)
                & f.Lte(s => s.StartTime, now.AddDays(EnergyReservationService.MaxDaysAhead));

            return await _slots.Find(filter)
                .SortBy(s => s.StartTime)
                .ThenBy(s => s.BatterySlotNumber)
                .ToListAsync(cancellationToken);
        }

        // Returns a single slot by id, or null if it does not exist.
        public async Task<EnergyBookingSlot?> GetByIdAsync(string id, CancellationToken cancellationToken = default)
        {
            if (!ValidationHelper.IsValidObjectId(id))
                return null;

            return await _slots.Find(s => s.Id == id)
                .FirstOrDefaultAsync(cancellationToken);
        }

        // Creates a slot on an active station's battery slot; Backoffice or Grid Operator only.
        public async Task<EnergyBookingSlot> CreateAsync(EnergyBookingSlot slot, string actorNic, CancellationToken cancellationToken = default)
        {
            var actor = await _users.RequireUserAsync(actorNic, [UserRole.Backoffice, UserRole.GridOperator], cancellationToken);
            var station = await RequireActiveStationAsync(slot.StationId, cancellationToken);
            await ValidateAsync(slot, station, null, cancellationToken);

            var now = DateTime.UtcNow;
            slot.Id = null;
            slot.Status = slot.Status == SlotStatus.Unavailable ? SlotStatus.Unavailable : SlotStatus.Available;
            slot.UpdatedBy = actor.Nic;
            slot.CreatedAt = now;
            slot.UpdatedAt = now;

            await _slots.InsertOneAsync(slot, cancellationToken: cancellationToken);
            return slot;
        }

        // Updates a slot's battery slot, times, capacity and availability; not allowed once reserved.
        // The station a slot belongs to cannot be changed.
        public async Task<EnergyBookingSlot> UpdateAsync(string id, EnergyBookingSlot slot, string actorNic, CancellationToken cancellationToken = default)
        {
            var actor = await _users.RequireUserAsync(actorNic, [UserRole.Backoffice, UserRole.GridOperator], cancellationToken);
            var existing = await RequireEditableAsync(id, cancellationToken);
            var station = await RequireActiveStationAsync(existing.StationId, cancellationToken);

            slot.StationId = existing.StationId;
            await ValidateAsync(slot, station, id, cancellationToken);

            var status = slot.Status == SlotStatus.Unavailable ? SlotStatus.Unavailable : SlotStatus.Available;
            var update = Builders<EnergyBookingSlot>.Update
                .Set(s => s.BatterySlotNumber, slot.BatterySlotNumber)
                .Set(s => s.StartTime, slot.StartTime)
                .Set(s => s.EndTime, slot.EndTime)
                .Set(s => s.CapacityKWh, slot.CapacityKWh)
                .Set(s => s.Status, status)
                .Set(s => s.UpdatedBy, actor.Nic)
                .Set(s => s.UpdatedAt, DateTime.UtcNow);

            return await _slots.FindOneAndUpdateAsync(s => s.Id == id && s.Status != SlotStatus.Reserved, update, ReturnUpdated, cancellationToken)
                ?? throw ServiceException.Conflict("This slot has just been reserved and can no longer be edited.");
        }

        // Marks a slot available or unavailable (e.g. for maintenance); Backoffice or Grid Operator only.
        public async Task<EnergyBookingSlot> SetAvailabilityAsync(string id, bool isAvailable, string actorNic, CancellationToken cancellationToken = default)
        {
            var actor = await _users.RequireUserAsync(actorNic, [UserRole.Backoffice, UserRole.GridOperator], cancellationToken);
            await RequireEditableAsync(id, cancellationToken);

            var update = Builders<EnergyBookingSlot>.Update
                .Set(s => s.Status, isAvailable ? SlotStatus.Available : SlotStatus.Unavailable)
                .Set(s => s.UpdatedBy, actor.Nic)
                .Set(s => s.UpdatedAt, DateTime.UtcNow);

            return await _slots.FindOneAndUpdateAsync(s => s.Id == id && s.Status != SlotStatus.Reserved, update, ReturnUpdated, cancellationToken)
                ?? throw ServiceException.Conflict("This slot has just been reserved and its availability cannot be changed.");
        }

        // Deletes a slot that is not reserved; Backoffice or Grid Operator only.
        public async Task DeleteAsync(string id, string actorNic, CancellationToken cancellationToken = default)
        {
            await _users.RequireUserAsync(actorNic, [UserRole.Backoffice, UserRole.GridOperator], cancellationToken);
            await RequireEditableAsync(id, cancellationToken);

            var result = await _slots.DeleteOneAsync(s => s.Id == id && s.Status != SlotStatus.Reserved, cancellationToken);
            if (result.DeletedCount == 0)
                throw ServiceException.Conflict("This slot has just been reserved and cannot be deleted.");
        }

        // Atomically marks an available slot as reserved; returns false if it was not available.
        public async Task<bool> TryReserveAsync(string id, CancellationToken cancellationToken)
        {
            var update = Builders<EnergyBookingSlot>.Update
                .Set(s => s.Status, SlotStatus.Reserved)
                .Set(s => s.UpdatedAt, DateTime.UtcNow);

            var result = await _slots.UpdateOneAsync(s => s.Id == id && s.Status == SlotStatus.Available, update, cancellationToken: cancellationToken);
            return result.ModifiedCount > 0;
        }

        // Returns a reserved slot to available after its reservation is cancelled or moved.
        public async Task ReleaseAsync(string id, CancellationToken cancellationToken)
        {
            var update = Builders<EnergyBookingSlot>.Update
                .Set(s => s.Status, SlotStatus.Available)
                .Set(s => s.UpdatedAt, DateTime.UtcNow);

            await _slots.UpdateOneAsync(s => s.Id == id && s.Status == SlotStatus.Reserved, update, cancellationToken: cancellationToken);
        }

        // Loads a slot and throws if it does not exist or is already reserved.
        private async Task<EnergyBookingSlot> RequireEditableAsync(string id, CancellationToken cancellationToken)
        {
            var slot = await GetByIdAsync(id, cancellationToken)
                ?? throw ServiceException.NotFound("Energy slot not found.");

            if (slot.Status == SlotStatus.Reserved)
                throw ServiceException.Conflict("This slot is reserved and cannot be changed. Cancel the reservation first.");

            return slot;
        }

        // Loads a station and throws if it does not exist or is deactivated.
        private async Task<SolarStationInfo> RequireActiveStationAsync(string stationId, CancellationToken cancellationToken)
        {
            var station = await _stations.GetByIdAsync(stationId, cancellationToken)
                ?? throw ServiceException.NotFound("Station not found.");

            if (!station.IsActive)
                throw ServiceException.Conflict("This station is deactivated.");

            return station;
        }

        // Checks times, battery slot number and capacity, and that the slot does not overlap
        // another slot on the same battery slot (ignoring excludeId when updating).
        private async Task ValidateAsync(EnergyBookingSlot slot, SolarStationInfo station, string? excludeId, CancellationToken cancellationToken)
        {
            slot.StartTime = ValidationHelper.ToUtc(slot.StartTime);
            slot.EndTime = ValidationHelper.ToUtc(slot.EndTime);

            if (slot.EndTime <= slot.StartTime)
                throw ServiceException.BadRequest("End time must be after start time.");

            if (slot.StartTime <= DateTime.UtcNow)
                throw ServiceException.BadRequest("Slots must start in the future.");

            if (slot.BatterySlotNumber < 1 || slot.BatterySlotNumber > station.TotalBatterySlots)
                throw ServiceException.BadRequest($"Battery slot number must be between 1 and {station.TotalBatterySlots}.");

            if (slot.CapacityKWh <= 0 || slot.CapacityKWh > station.CapacityKWh)
                throw ServiceException.BadRequest($"Slot capacity must be greater than 0 and at most {station.CapacityKWh} kWh.");

            var f = Builders<EnergyBookingSlot>.Filter;
            var overlap = f.Eq(s => s.StationId, slot.StationId)
                & f.Eq(s => s.BatterySlotNumber, slot.BatterySlotNumber)
                & f.Lt(s => s.StartTime, slot.EndTime)
                & f.Gt(s => s.EndTime, slot.StartTime);

            if (excludeId is not null)
                overlap &= f.Ne(s => s.Id, excludeId);

            if (await _slots.Find(overlap).AnyAsync(cancellationToken))
                throw ServiceException.Conflict("This time overlaps another slot on the same battery slot.");
        }
    }
}
