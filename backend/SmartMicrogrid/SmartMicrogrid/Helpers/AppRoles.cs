/*
 * File: AppRoles.cs
 * Purpose: Role names for [Authorize(Roles = ...)] attributes, derived from
 *          the UserRole enum so they cannot drift out of sync with it.
 * Author:  <your name>
 * Created: 2026
 */
using SmartMicrogrid.Api.Models.Enums;

namespace SmartMicrogrid.Api.Helpers
{
    public static class AppRoles
    {
        public const string Backoffice = nameof(UserRole.Backoffice);

        public const string GridOperator = nameof(UserRole.GridOperator);

        public const string Prosumer = nameof(UserRole.Prosumer);

        // Backoffice officers and Grid Operators.
        public const string Staff = Backoffice + "," + GridOperator;
    }
}
