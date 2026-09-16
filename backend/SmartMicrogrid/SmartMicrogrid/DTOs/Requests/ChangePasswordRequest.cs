/*
 * File: ChangePasswordRequest.cs
 * Purpose: Request body for PUT api/users/me/password.
 * Author:  <your name>
 * Created: 2026
 */
using System.ComponentModel.DataAnnotations;

namespace SmartMicrogrid.Api.DTOs.Requests
{
    public class ChangePasswordRequest
    {
        [Required]
        public string CurrentPassword { get; set; } = string.Empty;

        [Required]
        [MinLength(6)]
        public string NewPassword { get; set; } = string.Empty;
    }
}
