/*
 * File: IEnergyReservationService.cs
 * Purpose: Contract for the energy reservation workflow, QR verification and
 *          booking views. Implemented by EnergyReservationService.
 * Author:  <your name>
 * Created: 2026
 */
using SmartMicrogrid.Api.DTOs.Responses;
using SmartMicrogrid.Api.Models;
using SmartMicrogrid.Api.Models.Enums;

namespace SmartMicrogrid.Api.Services.Interfaces
{
    public interface IEnergyReservationService
    {
        // Returns a single reservation by id, or null if it does not exist.
        Task<EnergyReservation?> GetByIdAsync(string id, CancellationToken cancellationToken = default);

        // Searches reservations by prosumer, station, status, completing operator and start-date range.
        Task<List<EnergyReservation>> SearchAsync(string? prosumerNic = null, string? stationId = null, ReservationStatus? status = null, string? completedBy = null, DateTime? from = null, DateTime? to = null, CancellationToken cancellationToken = default);

        // Returns a prosumer's current and pending bookings.
        Task<List<EnergyReservation>> GetUpcomingAsync(string prosumerNic, CancellationToken cancellationToken = default);

        // Returns a prosumer's booking history.
        Task<List<EnergyReservation>> GetHistoryAsync(string prosumerNic, CancellationToken cancellationToken = default);

        // Returns pending and approved-upcoming counts, optionally for one prosumer and/or station.
        Task<ReservationCountsResponse> GetDashboardCountsAsync(string? prosumerNic = null, string? stationId = null, CancellationToken cancellationToken = default);

        // Books a slot for a prosumer within the 7-day window.
        Task<EnergyReservation> CreateAsync(string prosumerNic, string slotId, ReservationType type, double energyKWh, string actorNic, CancellationToken cancellationToken = default);

        // Changes a reservation with at least 12 hours' notice; it returns to Pending.
        Task<EnergyReservation> UpdateAsync(string id, string? newSlotId, ReservationType? type, double? energyKWh, string actorNic, CancellationToken cancellationToken = default);

        // Cancels a reservation with at least 12 hours' notice and frees its slot.
        Task<EnergyReservation> CancelAsync(string id, string? reason, string actorNic, CancellationToken cancellationToken = default);

        // Approves a pending reservation and issues its QR token; Backoffice or Grid Operator only.
        Task<EnergyReservation> ApproveAsync(string id, string actorNic, CancellationToken cancellationToken = default);

        // Looks up and checks the reservation for a scanned QR token; Backoffice or Grid Operator only.
        Task<EnergyReservation> VerifyQrAsync(string qrToken, string actorNic, CancellationToken cancellationToken = default);

        // Marks the reservation for a scanned QR token as completed; Backoffice or Grid Operator only.
        Task<EnergyReservation> CompleteAsync(string qrToken, string actorNic, CancellationToken cancellationToken = default);
    }
}
