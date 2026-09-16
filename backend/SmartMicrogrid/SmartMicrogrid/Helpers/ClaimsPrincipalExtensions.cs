/*
 * File: ClaimsPrincipalExtensions.cs
 * Purpose: Reads the signed-in user's identity from a validated JWT, so
 *          controllers pass the NIC from the token - never from the request
 *          body - to the service layer.
 * Author:  <your name>
 * Created: 2026
 */
using System.Security.Claims;
using SmartMicrogrid.Api.Services;

namespace SmartMicrogrid.Api.Helpers
{
    public static class ClaimsPrincipalExtensions
    {
        // Returns the NIC of the signed-in user, or throws 401 if the token has no NIC.
        public static string GetNic(this ClaimsPrincipal principal)
        {
            var nic = principal.FindFirstValue(AppClaimTypes.Nic);

            if (string.IsNullOrWhiteSpace(nic))
                throw ServiceException.Unauthorized("You must be signed in.");

            return nic;
        }
    }
}
