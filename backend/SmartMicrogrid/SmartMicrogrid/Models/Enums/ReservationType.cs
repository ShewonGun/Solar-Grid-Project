/*
 * File: ReservationType.cs
 * Purpose: Direction of the energy transfer in a reservation.
 * Author:  Aseni Thennakoon
 * Created: 2026-09-29
 */
namespace SmartMicrogrid.Api.Models.Enums
{
    public enum ReservationType
    {
        // Prosumer delivers surplus energy into the grid storage.
        DropOff,

        // Prosumer draws energy from the grid storage.
        Charging
    }
}
