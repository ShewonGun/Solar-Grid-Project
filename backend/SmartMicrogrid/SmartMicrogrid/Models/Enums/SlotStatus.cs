/*
 * File: SlotStatus.cs
 * Purpose: Availability states of an energy booking slot.
 * Author:  Shewon Gunarathne
 * Created: 2026-09-24
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
