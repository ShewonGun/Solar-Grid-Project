/*
 * File: UserService.cs
 * Purpose: Service layer for users (Users collection). Handles prosumer
 *          self-registration, Backoffice creation of users, login, profile
 *          and password changes, and the account status workflow
 *          (pending activation, deactivation requests, deactivation - which
 *          also cancels the prosumer's open bookings and frees their slots - and
 *          reactivation - reactivation is Backoffice only). Passwords are
 *          stored as salted PBKDF2 hashes.
 * Author:  <your name>
 * Created: 2026
 */
using System.Net.Mail;
using System.Security.Cryptography;
using System.Text.RegularExpressions;
using MongoDB.Bson;
using MongoDB.Driver;
using SmartMicrogrid.Api.Data;
using SmartMicrogrid.Api.Models;
using SmartMicrogrid.Api.Models.Enums;
using SmartMicrogrid.Api.Services.Interfaces;

namespace SmartMicrogrid.Api.Services
{
    public class UserService : IUserService
    {
        public const string CollectionName = "Users";

        private const int MinPasswordLength = 6;
        private const int SaltSize = 16;
        private const int HashSize = 32;
        private const int HashIterations = 100_000;

        // Sri Lankan NIC: old format is 9 digits followed by V or X, new format is 12 digits.
        private static readonly Regex NicPattern = new(@"^(\d{9}[VX]|\d{12})$", RegexOptions.Compiled);

        private static readonly FindOneAndUpdateOptions<User> ReturnUpdated = new() { ReturnDocument = ReturnDocument.After };

        private readonly IMongoCollection<User> _users;
        private readonly IMongoCollection<EnergyReservation> _reservations;
        private readonly IMongoCollection<EnergyBookingSlot> _slots;

        // Resolves the Users collection, plus the reservation and slot collections needed
        // to release a deactivated prosumer's bookings, from the shared MongoDB context.
        public UserService(MongoDbContext context)
        {
            _users = context.GetCollection<User>(CollectionName);
            _reservations = context.GetCollection<EnergyReservation>(EnergyReservationService.CollectionName);
            _slots = context.GetCollection<EnergyBookingSlot>(EnergyBookingSlotService.CollectionName);
        }

        // Trims and upper-cases a NIC so "123456789v" and "123456789V " match the same user.
        public static string NormalizeNic(string? nic)
        {
            return (nic ?? string.Empty).Trim().ToUpperInvariant();
        }

        // Returns users filtered by role, status and a name/NIC/email search term, sorted by name.
        public async Task<List<User>> GetAllAsync(UserRole? role = null, AccountStatus? status = null, string? search = null, CancellationToken cancellationToken = default)
        {
            var f = Builders<User>.Filter;
            var filter = f.Empty;

            if (role.HasValue)
                filter &= f.Eq(u => u.Role, role.Value);

            if (status.HasValue)
                filter &= f.Eq(u => u.Status, status.Value);

            if (!string.IsNullOrWhiteSpace(search))
            {
                var regex = new BsonRegularExpression(Regex.Escape(search.Trim()), "i");
                filter &= f.Regex(u => u.FullName, regex) | f.Regex(u => u.Nic, regex) | f.Regex(u => u.Email, regex);
            }

            return await _users.Find(filter)
                .SortBy(u => u.FullName)
                .ToListAsync(cancellationToken);
        }

        // Returns prosumer accounts waiting for Backoffice activation (the web "pending activation" view).
        public Task<List<User>> GetPendingActivationsAsync(CancellationToken cancellationToken = default)
        {
            return GetAllAsync(UserRole.Prosumer, AccountStatus.PendingActivation, cancellationToken: cancellationToken);
        }

        // Returns a single user by NIC, or null if it does not exist.
        public async Task<User?> GetByNicAsync(string nic, CancellationToken cancellationToken = default)
        {
            var key = NormalizeNic(nic);
            return await _users.Find(u => u.Nic == key)
                .FirstOrDefaultAsync(cancellationToken);
        }

        // Loads the calling user and checks the account can be used and has one of the allowed roles.
        // An empty role list allows any role.
        public async Task<User> RequireUserAsync(string actorNic, UserRole[] allowedRoles, CancellationToken cancellationToken = default)
        {
            var user = await GetByNicAsync(actorNic, cancellationToken)
                ?? throw ServiceException.Unauthorized("User not found.");

            if (!CanSignIn(user.Status))
                throw ServiceException.Forbidden("This account is not active.");

            if (allowedRoles.Length > 0 && !allowedRoles.Contains(user.Role))
                throw ServiceException.Forbidden("You are not allowed to perform this action.");

            return user;
        }

        // Registers a prosumer from the mobile app; the account waits for Backoffice activation.
        public async Task<User> RegisterProsumerAsync(User user, string password, CancellationToken cancellationToken = default)
        {
            user.Role = UserRole.Prosumer;
            user.Status = AccountStatus.PendingActivation;

            return await InsertAsync(user, password, cancellationToken);
        }

        // Creates a Backoffice, Grid Operator or Prosumer account from the web app; Backoffice only.
        // Accounts created by Backoffice are active immediately.
        public async Task<User> CreateUserAsync(User user, string password, string actorNic, CancellationToken cancellationToken = default)
        {
            await RequireUserAsync(actorNic, [UserRole.Backoffice], cancellationToken);

            user.Status = AccountStatus.Active;

            return await InsertAsync(user, password, cancellationToken);
        }

        // Creates the given Backoffice account if no Backoffice user exists yet; returns true if one was created.
        // Used at startup so the system always has someone who can administer it.
        public async Task<bool> EnsureBackofficeExistsAsync(User admin, string password, CancellationToken cancellationToken = default)
        {
            var backofficeExists = await _users.Find(u => u.Role == UserRole.Backoffice)
                .AnyAsync(cancellationToken);

            if (backofficeExists)
                return false;

            admin.Role = UserRole.Backoffice;
            admin.Status = AccountStatus.Active;
            await InsertAsync(admin, password, cancellationToken);
            return true;
        }

        // Signs a user in by NIC or email and password, rejecting accounts that are not active.
        public async Task<User> LoginAsync(string identifier, string password, CancellationToken cancellationToken = default)
        {
            var nic = NormalizeNic(identifier);
            var email = (identifier ?? string.Empty).Trim().ToLowerInvariant();

            var user = await _users.Find(u => u.Nic == nic || u.Email == email)
                .FirstOrDefaultAsync(cancellationToken);

            if (user is null || !VerifyPassword(password ?? string.Empty, user.PasswordHash))
                throw ServiceException.Unauthorized("Invalid NIC/email or password.");

            return user.Status switch
            {
                AccountStatus.PendingActivation => throw ServiceException.Forbidden("Your account is waiting for activation by a Backoffice officer."),
                AccountStatus.Deactivated => throw ServiceException.Forbidden("Your account has been deactivated. Please contact a Backoffice officer."),
                _ => user
            };
        }

        // Updates a user's profile details; users can edit their own profile, Backoffice can edit anyone's.
        // Role, status and password are not changed here.
        public async Task<User> UpdateProfileAsync(string nic, User changes, string actorNic, CancellationToken cancellationToken = default)
        {
            var actor = await RequireUserAsync(actorNic, [], cancellationToken);
            var key = NormalizeNic(nic);

            if (actor.Role != UserRole.Backoffice && actor.Nic != key)
                throw ServiceException.Forbidden("You can only edit your own profile.");

            ValidateProfile(changes);
            await EnsureEmailAvailableAsync(changes.Email, key, cancellationToken);

            var update = Builders<User>.Update
                .Set(u => u.FullName, changes.FullName)
                .Set(u => u.Email, changes.Email)
                .Set(u => u.Phone, changes.Phone)
                .Set(u => u.Address, changes.Address)
                .Set(u => u.SolarCapacityKW, changes.SolarCapacityKW)
                .Set(u => u.UpdatedAt, DateTime.UtcNow);

            return await _users.FindOneAndUpdateAsync(u => u.Nic == key, update, ReturnUpdated, cancellationToken)
                ?? throw ServiceException.NotFound("User not found.");
        }

        // Changes the caller's own password after checking the current one.
        public async Task ChangePasswordAsync(string nic, string currentPassword, string newPassword, CancellationToken cancellationToken = default)
        {
            var user = await RequireUserAsync(nic, [], cancellationToken);

            if (!VerifyPassword(currentPassword ?? string.Empty, user.PasswordHash))
                throw ServiceException.BadRequest("The current password is incorrect.");

            ValidatePassword(newPassword);

            var update = Builders<User>.Update
                .Set(u => u.PasswordHash, HashPassword(newPassword))
                .Set(u => u.UpdatedAt, DateTime.UtcNow);

            await _users.UpdateOneAsync(u => u.Nic == user.Nic, update, cancellationToken: cancellationToken);
        }

        // Lets a prosumer ask for their own account to be deactivated from the mobile app.
        public async Task<User> RequestDeactivationAsync(string nic, CancellationToken cancellationToken = default)
        {
            var user = await RequireUserAsync(nic, [UserRole.Prosumer], cancellationToken);

            if (user.Status != AccountStatus.Active)
                throw ServiceException.Conflict("A deactivation request has already been made for this account.");

            return await SetStatusAsync(user.Nic, AccountStatus.DeactivationRequested, cancellationToken);
        }

        // Activates a pending account or reactivates a deactivated one; Backoffice only.
        public async Task<User> ActivateAsync(string nic, string actorNic, CancellationToken cancellationToken = default)
        {
            await RequireUserAsync(actorNic, [UserRole.Backoffice], cancellationToken);

            var user = await GetByNicAsync(nic, cancellationToken)
                ?? throw ServiceException.NotFound("User not found.");

            if (user.Status == AccountStatus.Active)
                throw ServiceException.Conflict("This account is already active.");

            return await SetStatusAsync(user.Nic, AccountStatus.Active, cancellationToken);
        }

        // Deactivates an account (e.g. after a prosumer's request); Backoffice only and not their own account.
        public async Task<User> DeactivateAsync(string nic, string actorNic, CancellationToken cancellationToken = default)
        {
            var actor = await RequireUserAsync(actorNic, [UserRole.Backoffice], cancellationToken);

            var user = await GetByNicAsync(nic, cancellationToken)
                ?? throw ServiceException.NotFound("User not found.");

            if (user.Nic == actor.Nic)
                throw ServiceException.BadRequest("You cannot deactivate your own account.");

            if (user.Status == AccountStatus.Deactivated)
                throw ServiceException.Conflict("This account is already deactivated.");

            // Free any bookings this prosumer still holds before the account goes inactive.
            await CancelOpenReservationsAsync(user.Nic, actor.Nic, cancellationToken);

            return await SetStatusAsync(user.Nic, AccountStatus.Deactivated, cancellationToken);
        }

        // Cancels a user's pending and approved reservations that have not ended yet and returns
        // their slots to Available. Used when an account is deactivated, so a slot is never left
        // locked by an account that can no longer manage it.
        private async Task CancelOpenReservationsAsync(string nic, string actorNic, CancellationToken cancellationToken)
        {
            var f = Builders<EnergyReservation>.Filter;
            var openFilter = f.Eq(r => r.ProsumerNic, nic)
                & f.In(r => r.Status, EnergyReservationService.OpenStatuses)
                & f.Gt(r => r.ReservationEnd, DateTime.UtcNow);

            var open = await _reservations.Find(openFilter).ToListAsync(cancellationToken);
            if (open.Count == 0)
                return;

            var now = DateTime.UtcNow;

            await _reservations.UpdateManyAsync(
                f.In(r => r.Id, open.Select(r => r.Id)),
                Builders<EnergyReservation>.Update
                    .Set(r => r.Status, ReservationStatus.Cancelled)
                    .Set(r => r.CancelledBy, actorNic)
                    .Set(r => r.CancelledAt, now)
                    .Set(r => r.CancellationReason, "The prosumer's account was deactivated.")
                    .Set(r => r.QrToken, (string?)null)
                    .Set(r => r.UpdatedAt, now),
                cancellationToken: cancellationToken);

            // Only slots still held by these reservations are freed; anything already
            // Available or taken out of service is left alone.
            var slotFilter = Builders<EnergyBookingSlot>.Filter.In(s => s.Id, open.Select(r => r.SlotId).Distinct())
                & Builders<EnergyBookingSlot>.Filter.Eq(s => s.Status, SlotStatus.Reserved);

            await _slots.UpdateManyAsync(
                slotFilter,
                Builders<EnergyBookingSlot>.Update
                    .Set(s => s.Status, SlotStatus.Available)
                    .Set(s => s.UpdatedAt, now),
                cancellationToken: cancellationToken);
        }

        // Validates, hashes the password and inserts a new user, rejecting duplicate NICs and emails.
        private async Task<User> InsertAsync(User user, string password, CancellationToken cancellationToken)
        {
            user.Nic = NormalizeNic(user.Nic);
            ValidateNic(user.Nic);
            ValidateProfile(user);
            ValidatePassword(password);

            if (await GetByNicAsync(user.Nic, cancellationToken) is not null)
                throw ServiceException.Conflict("An account with this NIC already exists.");

            await EnsureEmailAvailableAsync(user.Email, null, cancellationToken);

            var now = DateTime.UtcNow;
            user.PasswordHash = HashPassword(password);
            user.CreatedAt = now;
            user.UpdatedAt = now;

            try
            {
                await _users.InsertOneAsync(user, cancellationToken: cancellationToken);
            }
            catch (MongoWriteException ex) when (ex.WriteError?.Category == ServerErrorCategory.DuplicateKey)
            {
                throw ServiceException.Conflict("An account with this NIC already exists.");
            }

            return user;
        }

        // Sets a user's account status and returns the updated user.
        private async Task<User> SetStatusAsync(string nic, AccountStatus status, CancellationToken cancellationToken)
        {
            var update = Builders<User>.Update
                .Set(u => u.Status, status)
                .Set(u => u.UpdatedAt, DateTime.UtcNow);

            return await _users.FindOneAndUpdateAsync(u => u.Nic == nic, update, ReturnUpdated, cancellationToken)
                ?? throw ServiceException.NotFound("User not found.");
        }

        // Throws if another user (other than excludeNic) already uses this email.
        private async Task EnsureEmailAvailableAsync(string email, string? excludeNic, CancellationToken cancellationToken)
        {
            var taken = await _users.Find(u => u.Email == email && u.Nic != excludeNic)
                .AnyAsync(cancellationToken);

            if (taken)
                throw ServiceException.Conflict("This email is already used by another account.");
        }

        // Returns true for statuses that are allowed to sign in and use the system.
        private static bool CanSignIn(AccountStatus status)
        {
            return status is AccountStatus.Active or AccountStatus.DeactivationRequested;
        }

        // Checks the NIC matches the old (9 digits + V/X) or new (12 digits) Sri Lankan format.
        private static void ValidateNic(string nic)
        {
            if (!NicPattern.IsMatch(nic))
                throw ServiceException.BadRequest("NIC must be 9 digits followed by V or X, or 12 digits.");
        }

        // Trims profile fields and checks the required ones are present and valid.
        private static void ValidateProfile(User user)
        {
            user.FullName = (user.FullName ?? string.Empty).Trim();
            user.Email = (user.Email ?? string.Empty).Trim().ToLowerInvariant();
            user.Phone = (user.Phone ?? string.Empty).Trim();
            user.Address = (user.Address ?? string.Empty).Trim();

            if (user.FullName.Length == 0)
                throw ServiceException.BadRequest("Full name is required.");

            if (!MailAddress.TryCreate(user.Email, out _))
                throw ServiceException.BadRequest("A valid email address is required.");

            if (user.SolarCapacityKW is < 0)
                throw ServiceException.BadRequest("Solar capacity cannot be negative.");
        }

        // Checks the password meets the minimum length.
        private static void ValidatePassword(string? password)
        {
            if (string.IsNullOrEmpty(password) || password.Length < MinPasswordLength)
                throw ServiceException.BadRequest($"Password must be at least {MinPasswordLength} characters.");
        }

        // Hashes a password with PBKDF2-SHA256 and a random salt, stored as "iterations.salt.hash".
        private static string HashPassword(string password)
        {
            var salt = RandomNumberGenerator.GetBytes(SaltSize);
            var hash = Rfc2898DeriveBytes.Pbkdf2(password, salt, HashIterations, HashAlgorithmName.SHA256, HashSize);
            return $"{HashIterations}.{Convert.ToBase64String(salt)}.{Convert.ToBase64String(hash)}";
        }

        // Checks a password against a stored hash using a constant-time comparison.
        private static bool VerifyPassword(string password, string storedHash)
        {
            var parts = storedHash.Split('.');
            if (parts.Length != 3 || !int.TryParse(parts[0], out var iterations))
                return false;

            try
            {
                var salt = Convert.FromBase64String(parts[1]);
                var expected = Convert.FromBase64String(parts[2]);
                var actual = Rfc2898DeriveBytes.Pbkdf2(password, salt, iterations, HashAlgorithmName.SHA256, expected.Length);
                return CryptographicOperations.FixedTimeEquals(actual, expected);
            }
            catch (FormatException)
            {
                return false;
            }
        }
    }
}
