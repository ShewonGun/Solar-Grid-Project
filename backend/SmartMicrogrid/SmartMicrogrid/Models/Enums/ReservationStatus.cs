/*
 * File: ReservationStatus.cs
 * Purpose: Workflow states of an energy reservation - pending approval,
 *          approved (QR issued), completed after the QR scan, or cancelled.
 * Author:  Aseni Thennakoon
 * Created: 2026-09-29
 */
namespace SmartMicrogrid.Api.Models.Enums
{
    public enum ReservationStatus
    {
        Pending,
        Approved,
        Completed,
        Cancelled
    }
}
