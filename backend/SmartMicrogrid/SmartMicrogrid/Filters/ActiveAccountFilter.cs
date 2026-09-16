/*
 * File: ActiveAccountFilter.cs
 * Purpose: Rejects requests made with a still-valid token by an account that
 *          has since been deactivated or is not yet activated. Write endpoints
 *          already reach this rule through the services, but read endpoints
 *          take the NIC straight from the token, so without this filter a
 *          deactivated user could keep reading data until their token expired.
 *          Applied to every action except those marked [AllowAnonymous].
 * Author:  <your name>
 * Created: 2026
 */
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc.Filters;
using SmartMicrogrid.Api.Helpers;
using SmartMicrogrid.Api.Services.Interfaces;

namespace SmartMicrogrid.Api.Filters
{
    public class ActiveAccountFilter : IAsyncActionFilter
    {
        private readonly IUserService _userService;

        // Receives the user service that owns the account status rule.
        public ActiveAccountFilter(IUserService userService)
        {
            _userService = userService;
        }

        // Loads the calling user and lets the request through only if the account can still be used.
        public async Task OnActionExecutionAsync(ActionExecutingContext context, ActionExecutionDelegate next)
        {
            // Anonymous endpoints (login, registration, the database ping) have no user to check.
            var allowsAnonymous = context.ActionDescriptor.EndpointMetadata.OfType<IAllowAnonymous>().Any();

            if (!allowsAnonymous && context.HttpContext.User.Identity?.IsAuthenticated == true)
            {
                // Throws 401 or 403 if the account is missing, pending activation or deactivated.
                await _userService.RequireUserAsync(context.HttpContext.User.GetNic(), [], context.HttpContext.RequestAborted);
            }

            await next();
        }
    }
}
