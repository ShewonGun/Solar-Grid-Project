/*
 * File: ErrorResponse.cs
 * Purpose: JSON body returned for every handled error, so the web and mobile
 *          apps can show the message to the user. Validation failures also
 *          list the invalid fields.
 * Author:  <your name>
 * Created: 2026
 */
using System.Text.Json.Serialization;

namespace SmartMicrogrid.Api.DTOs.Responses
{
    public class ErrorResponse
    {
        public int StatusCode { get; set; }

        public string Message { get; set; } = string.Empty;

        // Field name to error messages; only present for validation failures.
        [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
        public IDictionary<string, string[]>? Errors { get; set; }
    }
}
