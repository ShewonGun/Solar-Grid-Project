/*
 * File: UserResponse.cs
 * Purpose: User details returned to the web and mobile apps. Deliberately
 *          leaves out the password hash stored on the User model.
 * Author:  <your name>
 * Created: 2026
 */
using SmartMicrogrid.Api.Models;
using SmartMicrogrid.Api.Models.Enums;

namespace SmartMicrogrid.Api.DTOs.Responses
{
    public class UserResponse
    {
        public string Nic { get; set; } = string.Empty;

        public string FullName { get; set; } = string.Empty;

        public string Email { get; set; } = string.Empty;

        public string Phone { get; set; } = string.Empty;

        public string Address { get; set; } = string.Empty;

        public UserRole Role { get; set; }

        public AccountStatus Status { get; set; }

        public double? SolarCapacityKW { get; set; }

        public DateTime CreatedAt { get; set; }

        public DateTime UpdatedAt { get; set; }

        // Maps a User model to a response without the password hash.
        public static UserResponse FromUser(User user)
        {
            return new UserResponse
            {
                Nic = user.Nic,
                FullName = user.FullName,
                Email = user.Email,
                Phone = user.Phone,
                Address = user.Address,
                Role = user.Role,
                Status = user.Status,
                SolarCapacityKW = user.SolarCapacityKW,
                CreatedAt = user.CreatedAt,
                UpdatedAt = user.UpdatedAt
            };
        }
    }
}
