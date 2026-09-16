/*
 * File: IEnergyBookingSlotService.cs
 * Purpose: Contract for managing energy booking slots and reserving/releasing
 *          them atomically. Implemented by EnergyBookingSlotService.
 * Author:  <your name>
 * Created: 2026
 */
using SmartMicrogrid.Api.Models;
using SmartMicrogrid.Api.Models.Enums;

namespace SmartMicrogrid.Api.Services.Interfaces
{
    public interface IEnergyBookingSlotService
    {
        // Returns a station's slots, optionally by start-time range and status.
        Task<List<EnergyBookingSlot>> GetByStationAsync(string stationId, DateTime? from = null, DateTime? to = null, SlotStatus? status = null, CancellationToken cancellationToken = default);

        // Returns slots prosumers can book now, optionally for one station.
        Task<List<EnergyBookingSlot>> GetBookableAsync(string? stationId = null, CancellationToken cancellationToken = default);

        // Returns a single slot by id, or null if it does not exist.
        Task<EnergyBookingSlot?> GetByIdAsync(string id, CancellationToken cancellationToken = default);

        // Creates a slot on an active station; Backoffice or Grid Operator only.
        Task<EnergyBookingSlot> CreateAsync(EnergyBookingSlot slot, string actorNic, CancellationToken cancellationToken = default);

        // Updates an unreserved slot; Backoffice or Grid Operator only.
        Task<EnergyBookingSlot> UpdateAsync(string id, EnergyBookingSlot slot, string actorNic, CancellationToken cancellationToken = default);

        // Marks an unreserved slot available or unavailable; Backoffice or Grid Operator only.
        Task<EnergyBookingSlot> SetAvailabilityAsync(string id, bool isAvailable, string actorNic, CancellationToken cancellationToken = default);

        // Deletes an unreserved slot; Backoffice or Grid Operator only.
        Task DeleteAsync(string id, string actorNic, CancellationToken cancellationToken = default);

        // Atomically marks an available slot as reserved; returns false if it was not available.
        Task<bool> TryReserveAsync(string id, CancellationToken cancellationToken);

        // Returns a reserved slot to available.
        Task ReleaseAsync(string id, CancellationToken cancellationToken);
    }
}
