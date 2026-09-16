/*
 * File: CreateUserRequest.cs
 * Purpose: Request body for POST api/users, used by Backoffice officers to
 *          create Backoffice, Grid Operator or Prosumer accounts.
 * Author:  <your name>
 * Created: 2026
 */
using System.ComponentModel.DataAnnotations;
using SmartMicrogrid.Api.Models;
using SmartMicrogrid.Api.Models.Enums;

namespace SmartMicrogrid.Api.DTOs.Requests
{
    public class CreateUserRequest
    {
        [Required]
        public string Nic { get; set; } = string.Empty;

        [Required]
        [StringLength(100)]
        public string FullName { get; set; } = string.Empty;

        [Required]
        [EmailAddress]
        public string Email { get; set; } = string.Empty;

        [StringLength(20)]
        public string Phone { get; set; } = string.Empty;

        [StringLength(250)]
        public string Address { get; set; } = string.Empty;

        [Required]
        public UserRole? Role { get; set; }

        [Range(0, double.MaxValue)]
        public double? SolarCapacityKW { get; set; }

        [Required]
        [MinLength(6)]
        public string Password { get; set; } = string.Empty;

        // Maps the request to a new User model; the password is hashed by the service, not copied here.
        public User ToModel()
        {
            return new User
            {
                Nic = Nic,
                FullName = FullName,
                Email = Email,
                Phone = Phone,
                Address = Address,
                Role = Role.GetValueOrDefault(),
                SolarCapacityKW = SolarCapacityKW
            };
        }
    }
}
