/*
 * File: AuthController.cs
 * Purpose: Authentication endpoints shared by the web and mobile apps -
 *          login (returns a JWT), prosumer self-registration, and reading the
 *          signed-in user's own details from their token.
 * Author:  <your name>
 * Created: 2026
 */
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartMicrogrid.Api.DTOs.Requests;
using SmartMicrogrid.Api.DTOs.Responses;
using SmartMicrogrid.Api.Helpers;
using SmartMicrogrid.Api.Services.Interfaces;

namespace SmartMicrogrid.Api.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class AuthController : ControllerBase
    {
        private readonly IUserService _userService;
        private readonly ITokenService _tokenService;

        // Receives the user and token services.
        public AuthController(IUserService userService, ITokenService tokenService)
        {
            _userService = userService;
            _tokenService = tokenService;
        }

        // POST api/auth/login - signs in with NIC or email and password and returns a JWT.
        [AllowAnonymous]
        [HttpPost("login")]
        public async Task<ActionResult<LoginResponse>> Login([FromBody] LoginRequest request, CancellationToken cancellationToken)
        {
            var user = await _userService.LoginAsync(request.Identifier, request.Password, cancellationToken);
            return Ok(_tokenService.CreateLoginResponse(user));
        }

        // POST api/auth/register - prosumer self-registration; the account waits for Backoffice activation.
        [AllowAnonymous]
        [HttpPost("register")]
        public async Task<ActionResult<UserResponse>> Register([FromBody] RegisterProsumerRequest request, CancellationToken cancellationToken)
        {
            var user = await _userService.RegisterProsumerAsync(request.ToUser(), request.Password, cancellationToken);
            return StatusCode(StatusCodes.Status201Created, UserResponse.FromUser(user));
        }

        // GET api/auth/me - returns the signed-in user's details using the NIC from their token.
        [HttpGet("me")]
        public async Task<ActionResult<UserResponse>> Me(CancellationToken cancellationToken)
        {
            var user = await _userService.RequireUserAsync(User.GetNic(), [], cancellationToken);
            return Ok(UserResponse.FromUser(user));
        }
    }
}
