/*
 * File: LoginRequest.cs
 * Purpose: Request body for POST api/auth/login. Users sign in with their
 *          NIC or email address and password.
 * Author:  <your name>
 * Created: 2026
 */
using System.ComponentModel.DataAnnotations;

namespace SmartMicrogrid.Api.DTOs.Requests
{
    public class LoginRequest
    {
        // NIC number or email address.
        [Required]
        public string Identifier { get; set; } = string.Empty;

        [Required]
        public string Password { get; set; } = string.Empty;
    }
}
