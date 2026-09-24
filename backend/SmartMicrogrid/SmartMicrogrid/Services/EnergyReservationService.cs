/*
 * File: EnergyReservationService.cs
 * Purpose: Service layer for energy reservations (EnergyReservation
 *          collection). Handles creating, updating and cancelling bookings
 *          (must start within 7 days; updates and cancellations need at
 *          least 12 hours' notice), approval with a secure QR token, QR
 *          verification and completion by Grid Operators, and the booking
 *          views and dashboard counts used by the web and mobile apps.
 * Author:  <your name>
 * Created: 2026
 */
using System.Security.Cryptography;
using MongoDB.Driver;
using SmartMicrogrid.Api.Data;
using SmartMicrogrid.Api.DTOs.Responses;
using SmartMicrogrid.Api.Helpers;
using SmartMicrogrid.Api.Models;
using SmartMicrogrid.Api.Models.Enums;
using SmartMicrogrid.Api.Services.Interfaces;

namespace SmartMicrogrid.Api.Services
{
    public class EnergyReservationService : IEnergyReservationService
    {
        public const string CollectionName = "EnergyReservation";

        // Reservations must start no more than this many days from now.
        public const int MaxDaysAhead = 7;

        // Updates and cancellations need at least this many hours before the start time.
        public const int MinNoticeHours = 12;

        // Statuses of reservations that are still in progress (not completed or cancelled).
        public static readonly ReservationStatus[] OpenStatuses = [ReservationStatus.Pending, ReservationStatus.Approved];

        private static readonly FindOneAndUpdateOptions<EnergyReservation> ReturnUpdated = new() { ReturnDocument = ReturnDocument.After };

        private readonly IMongoCollection<EnergyReservation> _reservations;
        private readonly IUserService _users;
        private readonly IStationService _stations;
        private readonly IEnergyBookingSlotService _slots;

        // Resolves the EnergyReservation collection and the services this one depends on.
        public EnergyReservationService(MongoDbContext context, IUserService users, IStationService stations, IEnergyBookingSlotService slots)
        {
            _reservations = context.GetCollection<EnergyReservation>(CollectionName);
            _users = users;
            _stations = stations;
            _slots = slots;
        }

        // Returns a single reservation by id, or null if it does not exist.
        public async Task<EnergyReservation?> GetByIdAsync(string id, CancellationToken cancellationToken = default)
        {
            if (!ValidationHelper.IsValidObjectId(id))
                return null;

            return await _reservations.Find(r => r.Id == id)
                .FirstOrDefaultAsync(cancellationToken);
        }

        // Searches reservations by prosumer, station, status, completing operator and start-date range, newest first.
        public async Task<List<EnergyReservation>> SearchAsync(string? prosumerNic = null, string? stationId = null, ReservationStatus? status = null, string? completedBy = null, DateTime? from = null, DateTime? to = null, CancellationToken cancellationToken = default)
        {
            var f = Builders<EnergyReservation>.Filter;
            var filter = f.Empty;

            if (!string.IsNullOrWhiteSpace(prosumerNic))
                filter &= f.Eq(r => r.ProsumerNic, UserService.NormalizeNic(prosumerNic));

            if (!string.IsNullOrWhiteSpace(stationId))
            {
                if (!ValidationHelper.IsValidObjectId(stationId))
                    return [];
                filter &= f.Eq(r => r.StationId, stationId);
            }

            if (status.HasValue)
                filter &= f.Eq(r => r.Status, status.Value);

            // Lets a Grid Operator ask for only the transfers they personally finalised.
            if (!string.IsNullOrWhiteSpace(completedBy))
                filter &= f.Eq(r => r.CompletedBy, UserService.NormalizeNic(completedBy));

            if (from.HasValue)
                filter &= f.Gte(r => r.ReservationStart, ValidationHelper.ToUtc(from.Value));

            if (to.HasValue)
                filter &= f.Lte(r => r.ReservationStart, ValidationHelper.ToUtc(to.Value));

            return await _reservations.Find(filter)
                .SortByDescending(r => r.ReservationStart)
                .ToListAsync(cancellationToken);
        }

        // Current and pending bookings for a prosumer: pending or approved and not yet ended, soonest first.
        public async Task<List<EnergyReservation>> GetUpcomingAsync(string prosumerNic, CancellationToken cancellationToken = default)
        {
            var f = Builders<EnergyReservation>.Filter;
            var filter = f.Eq(r => r.ProsumerNic, UserService.NormalizeNic(prosumerNic))
                & f.In(r => r.Status, OpenStatuses)
                & f.Gt(r => r.ReservationEnd, DateTime.UtcNow);

            return await _reservations.Find(filter)
                .SortBy(r => r.ReservationStart)
                .ToListAsync(cancellationToken);
        }

        // Booking history for a prosumer: completed, cancelled or already-ended reservations, most recent first.
        public async Task<List<EnergyReservation>> GetHistoryAsync(string prosumerNic, CancellationToken cancellationToken = default)
        {
            var f = Builders<EnergyReservation>.Filter;
            var filter = f.Eq(r => r.ProsumerNic, UserService.NormalizeNic(prosumerNic))
                & (f.In(r => r.Status, [ReservationStatus.Completed, ReservationStatus.Cancelled])
                   | f.Lte(r => r.ReservationEnd, DateTime.UtcNow));

            return await _reservations.Find(filter)
                .SortByDescending(r => r.ReservationStart)
                .ToListAsync(cancellationToken);
        }

        // Dashboard counts of pending reservations and approved future reservations,
        // optionally for one prosumer and/or one station.
        public async Task<ReservationCountsResponse> GetDashboardCountsAsync(string? prosumerNic = null, string? stationId = null, CancellationToken cancellationToken = default)
        {
            var now = DateTime.UtcNow;
            var f = Builders<EnergyReservation>.Filter;
            var scope = f.Empty;

            if (!string.IsNullOrWhiteSpace(prosumerNic))
                scope &= f.Eq(r => r.ProsumerNic, UserService.NormalizeNic(prosumerNic));

            if (!string.IsNullOrWhiteSpace(stationId))
            {
                if (!ValidationHelper.IsValidObjectId(stationId))
                    return new ReservationCountsResponse();
                scope &= f.Eq(r => r.StationId, stationId);
            }

            var pending = await _reservations.CountDocumentsAsync(
                scope & f.Eq(r => r.Status, ReservationStatus.Pending) & f.Gt(r => r.ReservationEnd, now),
                cancellationToken: cancellationToken);

            var approvedUpcoming = await _reservations.CountDocumentsAsync(
                scope & f.Eq(r => r.Status, ReservationStatus.Approved) & f.Gt(r => r.ReservationStart, now),
                cancellationToken: cancellationToken);

            return new ReservationCountsResponse { Pending = pending, ApprovedUpcoming = approvedUpcoming };
        }

        // Books a slot for a prosumer. Prosumers book for themselves; Backoffice and Grid Operators can book for any prosumer.
        // The slot must be available, at an active station and start within 7 days. The new reservation is Pending.
        public async Task<EnergyReservation> CreateAsync(string prosumerNic, string slotId, ReservationType type, double energyKWh, string actorNic, CancellationToken cancellationToken = default)
        {
            var actor = await _users.RequireUserAsync(actorNic, [], cancellationToken);
            var prosumer = await RequireProsumerAsync(prosumerNic, actor, cancellationToken);
            var slot = await RequireBookableSlotAsync(slotId, cancellationToken);
            ValidateEnergy(energyKWh, slot);

            if (!await _slots.TryReserveAsync(slot.Id!, cancellationToken))
                throw ServiceException.Conflict("This slot has just been booked by someone else.");

            var now = DateTime.UtcNow;
            var reservation = new EnergyReservation
            {
                ProsumerNic = prosumer.Nic,
                StationId = slot.StationId,
                SlotId = slot.Id!,
                Type = type,
                EnergyKWh = energyKWh,
                ReservationStart = slot.StartTime,
                ReservationEnd = slot.EndTime,
                Status = ReservationStatus.Pending,
                CreatedAt = now,
                UpdatedAt = now
            };

            try
            {
                await _reservations.InsertOneAsync(reservation, cancellationToken: cancellationToken);
            }
            catch
            {
                // Don't leave the slot locked if the reservation could not be saved.
                await _slots.ReleaseAsync(slot.Id!, CancellationToken.None);
                throw;
            }

            return reservation;
        }

        // Changes a reservation's slot, type or energy amount; needs at least 12 hours' notice.
        // A changed reservation goes back to Pending and needs approval again (its QR code is revoked).
        public async Task<EnergyReservation> UpdateAsync(string id, string? newSlotId, ReservationType? type, double? energyKWh, string actorNic, CancellationToken cancellationToken = default)
        {
            var actor = await _users.RequireUserAsync(actorNic, [], cancellationToken);
            var reservation = await RequireExistingAsync(id, cancellationToken);
            EnsureCanModify(reservation, actor);

            var changingSlot = !string.IsNullOrWhiteSpace(newSlotId) && newSlotId != reservation.SlotId;
            var slot = changingSlot
                ? await RequireBookableSlotAsync(newSlotId!, cancellationToken)
                : await _slots.GetByIdAsync(reservation.SlotId, cancellationToken)
                    ?? throw ServiceException.NotFound("The reserved slot no longer exists.");

            // Moving to another slot must also respect the notice rule, so a booking cannot be
            // rescheduled onto a slot that starts sooner than the notice period allows.
            if (changingSlot && slot.StartTime - DateTime.UtcNow < TimeSpan.FromHours(MinNoticeHours))
                throw ServiceException.BadRequest($"A reservation can only be moved to a slot that starts at least {MinNoticeHours} hours from now.");

            var energy = energyKWh ?? reservation.EnergyKWh;
            ValidateEnergy(energy, slot);

            if (changingSlot && !await _slots.TryReserveAsync(slot.Id!, cancellationToken))
                throw ServiceException.Conflict("The new slot has just been booked by someone else.");

            var update = Builders<EnergyReservation>.Update
                .Set(r => r.SlotId, slot.Id!)
                .Set(r => r.StationId, slot.StationId)
                .Set(r => r.ReservationStart, slot.StartTime)
                .Set(r => r.ReservationEnd, slot.EndTime)
                .Set(r => r.Type, type ?? reservation.Type)
                .Set(r => r.EnergyKWh, energy)
                .Set(r => r.Status, ReservationStatus.Pending)
                .Set(r => r.QrToken, (string?)null)
                .Set(r => r.ApprovedBy, (string?)null)
                .Set(r => r.ApprovedAt, (DateTime?)null)
                .Set(r => r.UpdatedAt, DateTime.UtcNow);

            // Only apply if nobody else changed the status in the meantime.
            var updated = await _reservations.FindOneAndUpdateAsync(
                r => r.Id == reservation.Id && r.Status == reservation.Status, update, ReturnUpdated, cancellationToken);

            if (updated is null)
            {
                if (changingSlot)
                    await _slots.ReleaseAsync(slot.Id!, CancellationToken.None);
                throw ServiceException.Conflict("This reservation was changed by someone else. Please reload and try again.");
            }

            if (changingSlot)
                await _slots.ReleaseAsync(reservation.SlotId, cancellationToken);

            return updated;
        }

        // Cancels a reservation and frees its slot; needs at least 12 hours' notice.
        // Prosumers cancel their own; Backoffice and Grid Operators can cancel on a prosumer's behalf.
        public async Task<EnergyReservation> CancelAsync(string id, string? reason, string actorNic, CancellationToken cancellationToken = default)
        {
            var actor = await _users.RequireUserAsync(actorNic, [], cancellationToken);
            var reservation = await RequireExistingAsync(id, cancellationToken);
            EnsureCanModify(reservation, actor);

            var update = Builders<EnergyReservation>.Update
                .Set(r => r.Status, ReservationStatus.Cancelled)
                .Set(r => r.CancelledBy, actor.Nic)
                .Set(r => r.CancelledAt, DateTime.UtcNow)
                .Set(r => r.CancellationReason, string.IsNullOrWhiteSpace(reason) ? null : reason.Trim())
                .Set(r => r.QrToken, (string?)null)
                .Set(r => r.UpdatedAt, DateTime.UtcNow);

            var updated = await _reservations.FindOneAndUpdateAsync(
                r => r.Id == reservation.Id && r.Status == reservation.Status, update, ReturnUpdated, cancellationToken)
                ?? throw ServiceException.Conflict("This reservation was changed by someone else. Please reload and try again.");

            await _slots.ReleaseAsync(reservation.SlotId, cancellationToken);
            return updated;
        }

        // Approves a pending reservation and issues the secure token encoded in the prosumer's QR code;
        // Backoffice or Grid Operator only.
        public async Task<EnergyReservation> ApproveAsync(string id, string actorNic, CancellationToken cancellationToken = default)
        {
            var actor = await _users.RequireUserAsync(actorNic, [UserRole.Backoffice, UserRole.GridOperator], cancellationToken);
            var reservation = await RequireExistingAsync(id, cancellationToken);

            if (reservation.Status != ReservationStatus.Pending)
                throw ServiceException.Conflict($"Only pending reservations can be approved. This one is {reservation.Status}.");

            if (reservation.ReservationStart <= DateTime.UtcNow)
                throw ServiceException.BadRequest("This reservation's start time has already passed.");

            var now = DateTime.UtcNow;
            var update = Builders<EnergyReservation>.Update
                .Set(r => r.Status, ReservationStatus.Approved)
                .Set(r => r.QrToken, GenerateQrToken())
                .Set(r => r.ApprovedBy, actor.Nic)
                .Set(r => r.ApprovedAt, now)
                .Set(r => r.UpdatedAt, now);

            return await _reservations.FindOneAndUpdateAsync(
                r => r.Id == reservation.Id && r.Status == ReservationStatus.Pending, update, ReturnUpdated, cancellationToken)
                ?? throw ServiceException.Conflict("This reservation was changed by someone else. Please reload and try again.");
        }

        // Looks up the reservation for a scanned QR token so the operator can check it before finalising;
        // Backoffice or Grid Operator only.
        public async Task<EnergyReservation> VerifyQrAsync(string qrToken, string actorNic, CancellationToken cancellationToken = default)
        {
            await _users.RequireUserAsync(actorNic, [UserRole.Backoffice, UserRole.GridOperator], cancellationToken);
            return await RequireApprovedByTokenAsync(qrToken, cancellationToken);
        }

        // Finalises the energy transfer for a scanned QR token and marks the reservation Completed;
        // Backoffice or Grid Operator only.
        public async Task<EnergyReservation> CompleteAsync(string qrToken, string actorNic, CancellationToken cancellationToken = default)
        {
            var actor = await _users.RequireUserAsync(actorNic, [UserRole.Backoffice, UserRole.GridOperator], cancellationToken);
            var reservation = await RequireApprovedByTokenAsync(qrToken, cancellationToken);

            var now = DateTime.UtcNow;
            var update = Builders<EnergyReservation>.Update
                .Set(r => r.Status, ReservationStatus.Completed)
                .Set(r => r.CompletedBy, actor.Nic)
                .Set(r => r.CompletedAt, now)
                .Set(r => r.UpdatedAt, now);

            return await _reservations.FindOneAndUpdateAsync(
                r => r.Id == reservation.Id && r.Status == ReservationStatus.Approved, update, ReturnUpdated, cancellationToken)
                ?? throw ServiceException.Conflict("This reservation has already been processed.");
        }

        // Loads a reservation by id or throws NotFound.
        private async Task<EnergyReservation> RequireExistingAsync(string id, CancellationToken cancellationToken)
        {
            return await GetByIdAsync(id, cancellationToken)
                ?? throw ServiceException.NotFound("Reservation not found.");
        }

        // Resolves the prosumer a booking is for and checks the actor is allowed to book for them.
        private async Task<User> RequireProsumerAsync(string prosumerNic, User actor, CancellationToken cancellationToken)
        {
            var key = UserService.NormalizeNic(prosumerNic);

            if (actor.Role == UserRole.Prosumer && actor.Nic != key)
                throw ServiceException.Forbidden("Prosumers can only make reservations for themselves.");

            var prosumer = actor.Nic == key
                ? actor
                : await _users.GetByNicAsync(key, cancellationToken) ?? throw ServiceException.NotFound("Prosumer not found.");

            if (prosumer.Role != UserRole.Prosumer)
                throw ServiceException.BadRequest("Reservations can only be made for prosumer accounts.");

            if (prosumer.Status is not (AccountStatus.Active or AccountStatus.DeactivationRequested))
                throw ServiceException.Conflict("This prosumer account is not active.");

            return prosumer;
        }

        // Loads a slot and checks it is available, at an active station and starts within the 7-day window.
        private async Task<EnergyBookingSlot> RequireBookableSlotAsync(string slotId, CancellationToken cancellationToken)
        {
            var slot = await _slots.GetByIdAsync(slotId, cancellationToken)
                ?? throw ServiceException.NotFound("Energy slot not found.");

            if (slot.Status != SlotStatus.Available)
                throw ServiceException.Conflict("This slot is not available.");

            var station = await _stations.GetByIdAsync(slot.StationId, cancellationToken);
            if (station is null || !station.IsActive)
                throw ServiceException.Conflict("This station is not currently accepting reservations.");

            var now = DateTime.UtcNow;
            if (slot.StartTime <= now)
                throw ServiceException.BadRequest("This slot has already started.");

            if (slot.StartTime > now.AddDays(MaxDaysAhead))
                throw ServiceException.BadRequest($"Reservations must be made within {MaxDaysAhead} days.");

            return slot;
        }

        // Checks the actor may change this reservation, it is still open, and there are at least 12 hours' notice.
        private static void EnsureCanModify(EnergyReservation reservation, User actor)
        {
            if (actor.Role == UserRole.Prosumer && reservation.ProsumerNic != actor.Nic)
                throw ServiceException.Forbidden("You can only change your own reservations.");

            if (!OpenStatuses.Contains(reservation.Status))
                throw ServiceException.Conflict($"A {reservation.Status} reservation cannot be changed.");

            if (reservation.ReservationStart - DateTime.UtcNow < TimeSpan.FromHours(MinNoticeHours))
                throw ServiceException.BadRequest($"Reservations can only be updated or cancelled at least {MinNoticeHours} hours before they start.");
        }

        // Checks the requested energy is positive and fits in the slot's capacity.
        private static void ValidateEnergy(double energyKWh, EnergyBookingSlot slot)
        {
            if (energyKWh <= 0 || energyKWh > slot.CapacityKWh)
                throw ServiceException.BadRequest($"Energy must be greater than 0 and at most {slot.CapacityKWh} kWh for this slot.");
        }

        // Finds the reservation for a QR token and checks it is approved and not yet completed.
        private async Task<EnergyReservation> RequireApprovedByTokenAsync(string qrToken, CancellationToken cancellationToken)
        {
            if (string.IsNullOrWhiteSpace(qrToken))
                throw ServiceException.BadRequest("QR code is required.");

            var token = qrToken.Trim();
            var reservation = await _reservations.Find(r => r.QrToken == token)
                .FirstOrDefaultAsync(cancellationToken)
                ?? throw ServiceException.NotFound("This QR code is not valid.");

            if (reservation.Status == ReservationStatus.Completed)
                throw ServiceException.Conflict("This reservation has already been completed.");

            if (reservation.Status != ReservationStatus.Approved)
                throw ServiceException.Conflict($"This reservation is {reservation.Status} and cannot be processed.");

            return reservation;
        }

        // Generates an unguessable 256-bit token (64 hex characters) for the QR code.
        private static string GenerateQrToken()
        {
            return Convert.ToHexString(RandomNumberGenerator.GetBytes(32));
        }
    }
}
