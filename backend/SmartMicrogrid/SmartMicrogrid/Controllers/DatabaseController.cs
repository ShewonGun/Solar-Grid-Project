/*
 * File: DatabaseController.cs
 * Purpose: Health-check endpoint used to confirm the API can reach MongoDB,
 *          e.g. after deploying to IIS.
 * Author:  <your name>
 * Created: 2026
 */
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartMicrogrid.Api.Data;

namespace SmartMicrogrid.Api.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class DatabaseController : ControllerBase
    {
        private readonly MongoDbContext _context;
        private readonly ILogger<DatabaseController> _logger;

        // Receives the MongoDB context and logger.
        public DatabaseController(MongoDbContext context, ILogger<DatabaseController> logger)
        {
            _context = context;
            _logger = logger;
        }

        // GET api/database/ping - returns 200 if MongoDB responds, otherwise 503.
        // Error details are logged, not returned, so connection information is not exposed.
        [AllowAnonymous]
        [HttpGet("ping", Name = "PingDatabase")]
        public async Task<IActionResult> Ping(CancellationToken cancellationToken)
        {
            try
            {
                await _context.PingAsync(cancellationToken);
                return Ok(new { status = "connected" });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "MongoDB ping failed.");
                return StatusCode(StatusCodes.Status503ServiceUnavailable, new { status = "unavailable" });
            }
        }
    }
}
