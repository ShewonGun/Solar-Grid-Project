/*
 * File: UpdateProfileRequest.cs
 * Purpose: Request body for PUT api/users/{nic}. Changes profile details
 *          only; role, status and password have their own endpoints.
 * Author:  <your name>
 * Created: 2026
 */
using System.ComponentModel.DataAnnotations;
using SmartMicrogrid.Api.Models;

namespace SmartMicrogrid.Api.DTOs.Requests
{
    public class UpdateProfileRequest
    {
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

        [Range(0, double.MaxValue)]
        public double? SolarCapacityKW { get; set; }

        // Maps the request to a User model holding only the editable profile fields.
        public User ToModel()
        {
            return new User
            {
                FullName = FullName,
                Email = Email,
                Phone = Phone,
                Address = Address,
                SolarCapacityKW = SolarCapacityKW
            };
        }
    }
}
