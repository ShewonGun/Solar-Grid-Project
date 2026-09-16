/*
 * File: UserRole.cs
 * Purpose: The three kinds of users in the system. Backoffice officers
 *          administer the system, Grid Operators run day-to-day operations,
 *          and Prosumers book energy slots from the mobile app.
 * Author:  <your name>
 * Created: 2026
 */
namespace SmartMicrogrid.Api.Models.Enums
{
    public enum UserRole
    {
        Backoffice,
        GridOperator,
        Prosumer
    }
}
