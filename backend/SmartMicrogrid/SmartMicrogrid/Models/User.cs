/*
 * File: User.cs
 * Purpose: Represents any system user - Backoffice officers and Grid Operators
 *          (web and mobile) and Solar Prosumers (mobile). The National Identity
 *          Card (NIC) number is the primary key. Maps to the "Users" collection
 *          ("User's detail" in the marking scheme).
 * Author:  <your name>
 * Created: 2026
 */
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;
using SmartMicrogrid.Api.Models.Enums;

namespace SmartMicrogrid.Api.Models
{
    public class User
    {
        // NIC number used as the primary key (stored as _id).
        [BsonId]
        public string Nic { get; set; } = string.Empty;

        [BsonElement("fullName")]
        public string FullName { get; set; } = string.Empty;

        [BsonElement("email")]
        public string Email { get; set; } = string.Empty;

        [BsonElement("phone")]
        public string Phone { get; set; } = string.Empty;

        [BsonElement("address")]
        public string Address { get; set; } = string.Empty;

        // Hashed password only - never store plain text.
        [BsonElement("passwordHash")]
        public string PasswordHash { get; set; } = string.Empty;

        [BsonElement("role")]
        [BsonRepresentation(BsonType.String)]
        public UserRole Role { get; set; } = UserRole.Prosumer;

        [BsonElement("status")]
        [BsonRepresentation(BsonType.String)]
        public AccountStatus Status { get; set; } = AccountStatus.PendingActivation;

        // Solar array capacity in kW, relevant to prosumers only.
        [BsonElement("solarCapacityKW")]
        public double? SolarCapacityKW { get; set; }

        [BsonElement("createdAt")]
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

        [BsonElement("updatedAt")]
        public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
    }
}
