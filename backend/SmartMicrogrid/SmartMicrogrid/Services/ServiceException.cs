/*
 * File: ServiceException.cs
 * Purpose: Exception thrown by the service layer when a request breaks a
 *          business rule, is not permitted, or refers to missing data.
 *          Carries the HTTP status code the controllers should return.
 * Author:  <your name>
 * Created: 2026
 */
namespace SmartMicrogrid.Api.Services
{
    public class ServiceException : Exception
    {
        public int StatusCode { get; }

        // Creates an exception with a message and the HTTP status code to return.
        public ServiceException(string message, int statusCode = StatusCodes.Status400BadRequest)
            : base(message)
        {
            StatusCode = statusCode;
        }

        // 400 - the request data is invalid or breaks a business rule.
        public static ServiceException BadRequest(string message) => new(message, StatusCodes.Status400BadRequest);

        // 401 - the caller could not be authenticated.
        public static ServiceException Unauthorized(string message) => new(message, StatusCodes.Status401Unauthorized);

        // 403 - the caller is authenticated but not allowed to do this.
        public static ServiceException Forbidden(string message) => new(message, StatusCodes.Status403Forbidden);

        // 404 - the referenced record does not exist.
        public static ServiceException NotFound(string message) => new(message, StatusCodes.Status404NotFound);

        // 409 - the record's current state does not allow this action.
        public static ServiceException Conflict(string message) => new(message, StatusCodes.Status409Conflict);
    }
}
