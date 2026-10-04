/*
 * File: LoginResponse.cs
 * Purpose: Returned after a successful login - the JWT the client sends in
 *          the Authorization header, when it expires, and the user's details
 *          so the client can route to the correct home screen for the role.
 * Author:  Mahen Perera
 * Created: 2026-09-25
 */
namespace SmartMicrogrid.Api.DTOs.Responses
{
    public class LoginResponse
    {
        public string Token { get; set; } = string.Empty;

        public DateTime ExpiresAt { get; set; }

        public UserResponse User { get; set; } = new();
    }
}
