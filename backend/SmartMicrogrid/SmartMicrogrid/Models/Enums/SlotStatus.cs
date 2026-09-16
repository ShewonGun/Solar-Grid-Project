/*
 * File: SlotStatus.cs
 * Purpose: Availability states of an energy booking slot.
 * Author:  <your name>
 * Created: 2026
 */
namespace SmartMicrogrid.Api.Models.Enums
{
    public enum SlotStatus
    {
        Available,

        Reserved,

        // Taken out of service by a Grid Operator (e.g. maintenance).
        Unavailable
    }
}
