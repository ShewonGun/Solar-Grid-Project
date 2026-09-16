/*
 * File: GlobalExceptionHandler.cs
 * Purpose: Central error handling for the API. Turns a ServiceException into
 *          its HTTP status code with a JSON message, and any other exception
 *          into a 500 that is logged but does not expose internal details.
 * Author:  <your name>
 * Created: 2026
 */
using Microsoft.AspNetCore.Diagnostics;
using SmartMicrogrid.Api.DTOs.Responses;
using SmartMicrogrid.Api.Services;

namespace SmartMicrogrid.Api.Middleware
{
    public class GlobalExceptionHandler : IExceptionHandler
    {
        private readonly ILogger<GlobalExceptionHandler> _logger;

        // Receives the logger used to record unexpected errors.
        public GlobalExceptionHandler(ILogger<GlobalExceptionHandler> logger)
        {
            _logger = logger;
        }

        // Writes an ErrorResponse for the exception and marks it as handled.
        public async ValueTask<bool> TryHandleAsync(HttpContext httpContext, Exception exception, CancellationToken cancellationToken)
        {
            var error = new ErrorResponse();

            if (exception is ServiceException serviceException)
            {
                error.StatusCode = serviceException.StatusCode;
                error.Message = serviceException.Message;
            }
            else
            {
                _logger.LogError(exception, "Unhandled exception while processing {Method} {Path}.",
                    httpContext.Request.Method, httpContext.Request.Path);

                error.StatusCode = StatusCodes.Status500InternalServerError;
                error.Message = "An unexpected error occurred. Please try again later.";
            }

            httpContext.Response.StatusCode = error.StatusCode;
            await httpContext.Response.WriteAsJsonAsync(error, cancellationToken);
            return true;
        }
    }
}
