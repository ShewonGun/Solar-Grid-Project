/*
 * File: IUserService.cs
 * Purpose: Contract for user registration, login, profile management and the
 *          account status workflow. Implemented by UserService.
 * Author:  <your name>
 * Created: 2026
 */
using SmartMicrogrid.Api.Models;
using SmartMicrogrid.Api.Models.Enums;

namespace SmartMicrogrid.Api.Services.Interfaces
{
    public interface IUserService
    {
        // Returns users filtered by role, status and a name/NIC/email search term.
        Task<List<User>> GetAllAsync(UserRole? role = null, AccountStatus? status = null, string? search = null, CancellationToken cancellationToken = default);

        // Returns prosumer accounts waiting for Backoffice activation.
        Task<List<User>> GetPendingActivationsAsync(CancellationToken cancellationToken = default);

        // Returns a single user by NIC, or null if it does not exist.
        Task<User?> GetByNicAsync(string nic, CancellationToken cancellationToken = default);

        // Loads the calling user and checks the account is usable and has an allowed role.
        Task<User> RequireUserAsync(string actorNic, UserRole[] allowedRoles, CancellationToken cancellationToken = default);

        // Registers a prosumer from the mobile app, pending Backoffice activation.
        Task<User> RegisterProsumerAsync(User user, string password, CancellationToken cancellationToken = default);

        // Creates an active account of any role; Backoffice only.
        Task<User> CreateUserAsync(User user, string password, string actorNic, CancellationToken cancellationToken = default);

        // Creates the given Backoffice account if no Backoffice user exists; returns true if one was created.
        Task<bool> EnsureBackofficeExistsAsync(User admin, string password, CancellationToken cancellationToken = default);

        // Signs a user in by NIC or email and password.
        Task<User> LoginAsync(string identifier, string password, CancellationToken cancellationToken = default);

        // Updates profile details for the user themselves or, for Backoffice, anyone.
        Task<User> UpdateProfileAsync(string nic, User changes, string actorNic, CancellationToken cancellationToken = default);

        // Changes the caller's own password after checking the current one.
        Task ChangePasswordAsync(string nic, string currentPassword, string newPassword, CancellationToken cancellationToken = default);

        // Records a prosumer's request to deactivate their own account.
        Task<User> RequestDeactivationAsync(string nic, CancellationToken cancellationToken = default);

        // Activates or reactivates an account; Backoffice only.
        Task<User> ActivateAsync(string nic, string actorNic, CancellationToken cancellationToken = default);

        // Deactivates an account; Backoffice only.
        Task<User> DeactivateAsync(string nic, string actorNic, CancellationToken cancellationToken = default);
    }
}
