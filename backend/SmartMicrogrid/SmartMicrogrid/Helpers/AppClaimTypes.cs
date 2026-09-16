/*
 * File: AppClaimTypes.cs
 * Purpose: Names of the claims written into login tokens by TokenService and
 *          read back when validating requests, kept in one place so issuing
 *          and reading always agree.
 * Author:  <your name>
 * Created: 2026
 */
namespace SmartMicrogrid.Api.Helpers
{
    public static class AppClaimTypes
    {
        // JWT subject claim, holding the user's NIC.
        public const string Nic = "sub";

        // The user's full name.
        public const string Name = "name";

        // The user's role (Backoffice, GridOperator or Prosumer).
        public const string Role = "role";
    }
}
