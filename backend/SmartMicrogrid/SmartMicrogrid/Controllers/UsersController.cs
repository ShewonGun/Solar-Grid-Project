/*
 * File: UsersController.cs
 * Purpose: User management endpoints - Backoffice user administration and
 *          pending activations (web), and self-service profile, password and
 *          deactivation requests (mobile). Business rules live in UserService.
 * Author:  <your name>
 * Created: 2026
 */
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartMicrogrid.Api.DTOs.Requests;
using SmartMicrogrid.Api.DTOs.Responses;
using SmartMicrogrid.Api.Helpers;
using SmartMicrogrid.Api.Models.Enums;
using SmartMicrogrid.Api.Services;
using SmartMicrogrid.Api.Services.Interfaces;

namespace SmartMicrogrid.Api.Controllers
{
    [ApiController]
    [Route("api/users")]
    [Produces("application/json")]
    public class UsersController : ControllerBase
    {
        private readonly IUserService _userService;

        // Receives the user service.
        public UsersController(IUserService userService)
        {
            _userService = userService;
        }

        // GET api/users - lists users filtered by role, status and search text; Backoffice only.
        [Authorize(Roles = AppRoles.Backoffice)]
        [HttpGet]
        public async Task<ActionResult<IEnumerable<UserResponse>>> GetAll(
            [FromQuery] UserRole? role, [FromQuery] AccountStatus? status, [FromQuery] string? search, CancellationToken cancellationToken)
        {
            var users = await _userService.GetAllAsync(role, status, search, cancellationToken);
            return Ok(users.Select(UserResponse.FromUser));
        }

        // GET api/users/pending-activations - prosumer accounts waiting for activation; Backoffice only.
        [Authorize(Roles = AppRoles.Backoffice)]
        [HttpGet("pending-activations")]
        public async Task<ActionResult<IEnumerable<UserResponse>>> GetPendingActivations(CancellationToken cancellationToken)
        {
            var users = await _userService.GetPendingActivationsAsync(cancellationToken);
            return Ok(users.Select(UserResponse.FromUser));
        }

        // GET api/users/{nic} - returns one user; Backoffice can view anyone, other users only themselves.
        [HttpGet("{nic}")]
        public async Task<ActionResult<UserResponse>> GetByNic(string nic, CancellationToken cancellationToken)
        {
            if (!User.IsInRole(AppRoles.Backoffice) && !IsCurrentUser(nic))
                throw ServiceException.Forbidden("You can only view your own profile.");

            var user = await _userService.GetByNicAsync(nic, cancellationToken)
                ?? throw ServiceException.NotFound("User not found.");

            return Ok(UserResponse.FromUser(user));
        }

        // POST api/users - creates an active Backoffice, Grid Operator or Prosumer account; Backoffice only.
        [Authorize(Roles = AppRoles.Backoffice)]
        [HttpPost]
        public async Task<ActionResult<UserResponse>> Create([FromBody] CreateUserRequest request, CancellationToken cancellationToken)
        {
            var user = await _userService.CreateUserAsync(request.ToModel(), request.Password, User.GetNic(), cancellationToken);
            return CreatedAtAction(nameof(GetByNic), new { nic = user.Nic }, UserResponse.FromUser(user));
        }

        // PUT api/users/{nic} - updates profile details; users edit themselves, Backoffice can edit anyone.
        [HttpPut("{nic}")]
        public async Task<ActionResult<UserResponse>> UpdateProfile(string nic, [FromBody] UpdateProfileRequest request, CancellationToken cancellationToken)
        {
            var user = await _userService.UpdateProfileAsync(nic, request.ToModel(), User.GetNic(), cancellationToken);
            return Ok(UserResponse.FromUser(user));
        }

        // PUT api/users/me/password - changes the signed-in user's password.
        [HttpPut("me/password")]
        public async Task<IActionResult> ChangePassword([FromBody] ChangePasswordRequest request, CancellationToken cancellationToken)
        {
            await _userService.ChangePasswordAsync(User.GetNic(), request.CurrentPassword, request.NewPassword, cancellationToken);
            return NoContent();
        }

        // POST api/users/me/deactivation-request - a prosumer asks for their account to be deactivated.
        [Authorize(Roles = AppRoles.Prosumer)]
        [HttpPost("me/deactivation-request")]
        public async Task<ActionResult<UserResponse>> RequestDeactivation(CancellationToken cancellationToken)
        {
            var user = await _userService.RequestDeactivationAsync(User.GetNic(), cancellationToken);
            return Ok(UserResponse.FromUser(user));
        }

        // POST api/users/{nic}/activate - activates or reactivates an account; Backoffice only.
        [Authorize(Roles = AppRoles.Backoffice)]
        [HttpPost("{nic}/activate")]
        public async Task<ActionResult<UserResponse>> Activate(string nic, CancellationToken cancellationToken)
        {
            var user = await _userService.ActivateAsync(nic, User.GetNic(), cancellationToken);
            return Ok(UserResponse.FromUser(user));
        }

        // POST api/users/{nic}/deactivate - deactivates an account; Backoffice only.
        [Authorize(Roles = AppRoles.Backoffice)]
        [HttpPost("{nic}/deactivate")]
        public async Task<ActionResult<UserResponse>> Deactivate(string nic, CancellationToken cancellationToken)
        {
            var user = await _userService.DeactivateAsync(nic, User.GetNic(), cancellationToken);
            return Ok(UserResponse.FromUser(user));
        }

        // Returns true if the NIC belongs to the signed-in user.
        private bool IsCurrentUser(string nic)
        {
            return string.Equals(nic.Trim(), User.GetNic(), StringComparison.OrdinalIgnoreCase);
        }
    }
}
