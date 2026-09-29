/*
 * File: ProsumerDirectoryEntry.cs
 * Purpose: Minimal prosumer lookup entry for the reservation-creation NIC
 *          picker - just enough to search and identify an account, not the
 *          full profile UserResponse exposes.
 * Author:  Shewon Gunarathne
 * Created: 2026-09-29
 */
using SmartMicrogrid.Api.Models;

namespace SmartMicrogrid.Api.DTOs.Responses
{
    public class ProsumerDirectoryEntry
    {
        public string Nic { get; set; } = string.Empty;

        public string FullName { get; set; } = string.Empty;

        public static ProsumerDirectoryEntry FromUser(User user)
        {
            return new ProsumerDirectoryEntry
            {
                Nic = user.Nic,
                FullName = user.FullName
            };
        }
    }
}
