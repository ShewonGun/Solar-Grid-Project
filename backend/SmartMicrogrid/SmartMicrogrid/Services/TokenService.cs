/*
 * File: TokenService.cs
 * Purpose: Issues signed JWT login tokens (HMAC-SHA256) carrying the user's
 *          NIC, name and role. Clients send the token in the Authorization
 *          header, and controllers read the NIC from it instead of trusting
 *          the request body.
 * Author:  <your name>
 * Created: 2026
 */
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;
using SmartMicrogrid.Api.DTOs.Responses;
using SmartMicrogrid.Api.Helpers;
using SmartMicrogrid.Api.Models;
using SmartMicrogrid.Api.Services.Interfaces;
using SmartMicrogrid.Api.Settings;

namespace SmartMicrogrid.Api.Services
{
    public class TokenService : ITokenService
    {
        private readonly JwtSettings _settings;
        private readonly SigningCredentials _signingCredentials;

        // Builds the signing credentials once from the configured JWT key.
        public TokenService(IOptions<JwtSettings> settings)
        {
            _settings = settings.Value;
            var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(_settings.Key));
            _signingCredentials = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);
        }

        // Issues a signed token for the user and returns it with the user's details.
        public LoginResponse CreateLoginResponse(User user)
        {
            var expiresAt = DateTime.UtcNow.AddMinutes(_settings.ExpiryMinutes);

            var claims = new[]
            {
                new Claim(AppClaimTypes.Nic, user.Nic),
                new Claim(AppClaimTypes.Name, user.FullName),
                new Claim(AppClaimTypes.Role, user.Role.ToString()),
                new Claim(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString())
            };

            var token = new JwtSecurityToken(
                issuer: _settings.Issuer,
                audience: _settings.Audience,
                claims: claims,
                expires: expiresAt,
                signingCredentials: _signingCredentials);

            return new LoginResponse
            {
                Token = new JwtSecurityTokenHandler().WriteToken(token),
                ExpiresAt = expiresAt,
                User = UserResponse.FromUser(user)
            };
        }
    }
}
