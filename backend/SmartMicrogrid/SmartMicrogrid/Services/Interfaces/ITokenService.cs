/*
 * File: ITokenService.cs
 * Purpose: Contract for issuing JWT login tokens. Implemented by TokenService.
 * Author:  <your name>
 * Created: 2026
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
