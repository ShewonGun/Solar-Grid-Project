/*
 * File: ITokenService.cs
 * Purpose: Contract for issuing JWT login tokens. Implemented by TokenService.
 * Author:  Mahen Perera
 * Created: 2026-09-25
 */
using SmartMicrogrid.Api.DTOs.Responses;
using SmartMicrogrid.Api.Models;

namespace SmartMicrogrid.Api.Services.Interfaces
{
    public interface ITokenService
    {
        // Issues a signed token for the user and returns it with the user's details.
        LoginResponse CreateLoginResponse(User user);
    }
}
