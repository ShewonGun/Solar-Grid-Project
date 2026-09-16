/*
 * File: StationsController.cs
 * Purpose: Solar station (microgrid node) endpoints - Backoffice station
 *          management on the web, and station listings and the nearby-stations
 *          map on mobile. Business rules live in StationService.
 * Author:  <your name>
 * Created: 2026
 */
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.ModelBinding;
using SmartMicrogrid.Api.DTOs.Requests;
using SmartMicrogrid.Api.DTOs.Responses;
using SmartMicrogrid.Api.Helpers;
using SmartMicrogrid.Api.Services;
using SmartMicrogrid.Api.Services.Interfaces;

namespace SmartMicrogrid.Api.Controllers
{
    [ApiController]
    [Route("api/stations")]
    [Produces("application/json")]
    public class StationsController : ControllerBase
    {
        private readonly IStationService _stationService;

        // Receives the station service.
        public StationsController(IStationService stationService)
        {
            _stationService = stationService;
        }

        // GET api/stations - lists stations; prosumers only ever see active stations.
        [HttpGet]
        public async Task<ActionResult<IEnumerable<StationResponse>>> GetAll([FromQuery] bool activeOnly, CancellationToken cancellationToken)
        {
            var onlyActive = activeOnly || User.IsInRole(AppRoles.Prosumer);
            var stations = await _stationService.GetAllAsync(onlyActive, cancellationToken);
            return Ok(stations.Select(StationResponse.FromModel));
        }

        // GET api/stations/nearby - active stations within radiusKm of a location, nearest first, for the map.
        [HttpGet("nearby")]
        public async Task<ActionResult<IEnumerable<NearbyStationResponse>>> GetNearby(
            [FromQuery, BindRequired] double latitude, [FromQuery, BindRequired] double longitude, [FromQuery] double radiusKm = 10, CancellationToken cancellationToken = default)
        {
            var stations = await _stationService.GetNearbyAsync(latitude, longitude, radiusKm, cancellationToken);
            return Ok(stations);
        }

        // GET api/stations/{id} - returns one station; prosumers only ever see active ones.
        [HttpGet("{id}")]
        public async Task<ActionResult<StationResponse>> GetById(string id, CancellationToken cancellationToken)
        {
            var station = await _stationService.GetByIdAsync(id, cancellationToken);

            // Matches GetAll, where prosumers never see deactivated stations. A deactivated
            // station is reported as not found rather than revealing that it exists.
            if (station is null || (User.IsInRole(AppRoles.Prosumer) && !station.IsActive))
                throw ServiceException.NotFound("Station not found.");

            return Ok(StationResponse.FromModel(station));
        }

        // POST api/stations - creates a station; Backoffice only.
        [Authorize(Roles = AppRoles.Backoffice)]
        [HttpPost]
        public async Task<ActionResult<StationResponse>> Create([FromBody] StationRequest request, CancellationToken cancellationToken)
        {
            var station = await _stationService.CreateAsync(request.ToModel(), User.GetNic(), cancellationToken);
            return CreatedAtAction(nameof(GetById), new { id = station.Id }, StationResponse.FromModel(station));
        }

        // PUT api/stations/{id} - updates a station's details and schedule; Backoffice only.
        [Authorize(Roles = AppRoles.Backoffice)]
        [HttpPut("{id}")]
        public async Task<ActionResult<StationResponse>> Update(string id, [FromBody] StationRequest request, CancellationToken cancellationToken)
        {
            var station = await _stationService.UpdateAsync(id, request.ToModel(), User.GetNic(), cancellationToken);
            return Ok(StationResponse.FromModel(station));
        }

        // PATCH api/stations/{id}/active - activates or deactivates a station; Backoffice only.
        [Authorize(Roles = AppRoles.Backoffice)]
        [HttpPatch("{id}/active")]
        public async Task<ActionResult<StationResponse>> SetActive(string id, [FromBody] SetStationActiveRequest request, CancellationToken cancellationToken)
        {
            var station = await _stationService.SetActiveAsync(id, request.IsActive.GetValueOrDefault(), User.GetNic(), cancellationToken);
            return Ok(StationResponse.FromModel(station));
        }

        // DELETE api/stations/{id} - deletes a station that has no reservations; Backoffice only.
        [Authorize(Roles = AppRoles.Backoffice)]
        [HttpDelete("{id}")]
        public async Task<IActionResult> Delete(string id, CancellationToken cancellationToken)
        {
            await _stationService.DeleteAsync(id, User.GetNic(), cancellationToken);
            return NoContent();
        }
    }
}
