/*
 * File: IStationService.cs
 * Purpose: Contract for managing solar microgrid stations. Implemented by
 *          StationService.
 * Author:  <your name>
 * Created: 2026
 */
using SmartMicrogrid.Api.DTOs.Responses;
using SmartMicrogrid.Api.Models;

namespace SmartMicrogrid.Api.Services.Interfaces
{
    public interface IStationService
    {
        // Returns all stations, optionally only the active ones.
        Task<List<SolarStationInfo>> GetAllAsync(bool activeOnly = false, CancellationToken cancellationToken = default);

        // Returns a single station by id, or null if it does not exist.
        Task<SolarStationInfo?> GetByIdAsync(string id, CancellationToken cancellationToken = default);

        // Returns active stations within radiusKm of a location, nearest first.
        Task<List<NearbyStationResponse>> GetNearbyAsync(double latitude, double longitude, double radiusKm, CancellationToken cancellationToken = default);

        // Creates a new station; Backoffice only.
        Task<SolarStationInfo> CreateAsync(SolarStationInfo station, string actorNic, CancellationToken cancellationToken = default);

        // Updates a station's details and schedule; Backoffice only.
        Task<SolarStationInfo> UpdateAsync(string id, SolarStationInfo station, string actorNic, CancellationToken cancellationToken = default);

        // Activates or deactivates a station, blocking deactivation while reservations are active; Backoffice only.
        Task<SolarStationInfo> SetActiveAsync(string id, bool isActive, string actorNic, CancellationToken cancellationToken = default);

        // Deletes a station with no reservations, along with its slots; Backoffice only.
        Task DeleteAsync(string id, string actorNic, CancellationToken cancellationToken = default);

        // Returns true if the station has pending or approved reservations that have not ended.
        Task<bool> HasActiveReservationsAsync(string id, CancellationToken cancellationToken = default);
    }
}
