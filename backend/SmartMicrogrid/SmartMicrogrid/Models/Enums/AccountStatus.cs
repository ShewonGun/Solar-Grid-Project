/*
 * File: AccountStatus.cs
 * Purpose: Lifecycle states of a user account, from registration through
 *          activation, deactivation requests and deactivation.
 * Author:  <your name>
 * Created: 2026
 */
namespace SmartMicrogrid.Api.Models.Enums
{
    public enum AccountStatus
    {
        // Registered but waiting for a Backoffice officer to activate.
        PendingActivation,

        Active,

        // Prosumer asked to deactivate from the mobile app.
        DeactivationRequested,

        // Only a Backoffice officer can move an account out of this state.
        Deactivated
    }
}
