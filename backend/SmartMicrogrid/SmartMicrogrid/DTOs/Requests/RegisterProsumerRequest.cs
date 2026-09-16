/*
 * File: RegisterProsumerRequest.cs
 * Purpose: Request body for POST api/auth/register, used by prosumers to
 *          create an account from the mobile app with their NIC as the key.
 * Author:  <your name>
 * Created: 2026
 */
using System.ComponentModel.DataAnnotations;
using SmartMicrogrid.Api.Models;

namespace SmartMicrogrid.Api.DTOs.Requests
{
    public class RegisterProsumerRequest
    {
        [Required]
        public string Nic { get; set; } = string.Empty;

        [Required]
        public string FullName { get; set; } = string.Empty;

        [Required]
        [EmailAddress]
        public string Email { get; set; } = string.Empty;

        public string Phone { get; set; } = string.Empty;

        public string Address { get; set; } = string.Empty;

        [Range(0, double.MaxValue)]
        public double? SolarCapacityKW { get; set; }

        [Required]
        [MinLength(6)]
        public string Password { get; set; } = string.Empty;

        // Maps the request to a new User model; the password is hashed by the service, not copied here.
        public User ToUser()
        {
            return new User
            {
                Nic = Nic,
                FullName = FullName,
                Email = Email,
                Phone = Phone,
                Address = Address,
                SolarCapacityKW = SolarCapacityKW
            };
        }
    }
}
