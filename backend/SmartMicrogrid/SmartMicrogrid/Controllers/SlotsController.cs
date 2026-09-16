/*
 * File: SlotsController.cs
 * Purpose: Energy booking slot endpoints - slot management and availability
 *          updates for Backoffice and Grid Operators, and the list of
 *          bookable slots for prosumers. Business rules live in
 *          EnergyBookingSlotService.
 * Author:  <your name>
 * Created: 2026
 */
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartMicrogrid.Api.DTOs.Requests;
using SmartMicrogrid.Api.DTOs.Responses;
using SmartMicrogrid.Api.Helpers;
using SmartMicrogrid.Api.Models.Enums;
using SmartMicrogrid.Api.Services;
using SmartMicrogrid.Api.Services.Interfaces;

namespace SmartMicrogrid.Api.Controllers
{
    [ApiController]
    [Route("api/slots")]
    [Produces("application/json")]
    public class SlotsController : ControllerBase
    {
        private readonly IEnergyBookingSlotService _slotService;

        // Receives the booking slot service.
        public SlotsController(IEnergyBookingSlotService slotService)
        {
            _slotService = slotService;
        }

        // GET api/slots/bookable - slots prosumers can book now, optionally for one station.
        [HttpGet("bookable")]
        public async Task<ActionResult<IEnumerable<SlotResponse>>> GetBookable([FromQuery] string? stationId, CancellationToken cancellationToken)
        {
            var slots = await _slotService.GetBookableAsync(stationId, cancellationToken);
            return Ok(slots.Select(SlotResponse.FromModel));
        }

        // GET api/stations/{stationId}/slots - all of a station's slots by time range and status; Backoffice or Grid Operator.
        [Authorize(Roles = AppRoles.Staff)]
        [HttpGet("~/api/stations/{stationId}/slots")]
        public async Task<ActionResult<IEnumerable<SlotResponse>>> GetByStation(
            string stationId, [FromQuery] DateTime? from, [FromQuery] DateTime? to, [FromQuery] SlotStatus? status, CancellationToken cancellationToken)
        {
            var slots = await _slotService.GetByStationAsync(stationId, from, to, status, cancellationToken);
            return Ok(slots.Select(SlotResponse.FromModel));
        }

        // GET api/slots/{id} - returns one slot.
        [HttpGet("{id}")]
        public async Task<ActionResult<SlotResponse>> GetById(string id, CancellationToken cancellationToken)
        {
            var slot = await _slotService.GetByIdAsync(id, cancellationToken)
                ?? throw ServiceException.NotFound("Energy slot not found.");

            return Ok(SlotResponse.FromModel(slot));
        }

        // POST api/slots - creates a slot on a station's battery slot; Backoffice or Grid Operator.
        [Authorize(Roles = AppRoles.Staff)]
        [HttpPost]
        public async Task<ActionResult<SlotResponse>> Create([FromBody] CreateSlotRequest request, CancellationToken cancellationToken)
        {
            var slot = await _slotService.CreateAsync(request.ToModel(), User.GetNic(), cancellationToken);
            return CreatedAtAction(nameof(GetById), new { id = slot.Id }, SlotResponse.FromModel(slot));
        }

        // PUT api/slots/{id} - updates an unreserved slot; Backoffice or Grid Operator.
        [Authorize(Roles = AppRoles.Staff)]
        [HttpPut("{id}")]
        public async Task<ActionResult<SlotResponse>> Update(string id, [FromBody] UpdateSlotRequest request, CancellationToken cancellationToken)
        {
            var slot = await _slotService.UpdateAsync(id, request.ToModel(), User.GetNic(), cancellationToken);
            return Ok(SlotResponse.FromModel(slot));
        }

        // PATCH api/slots/{id}/availability - opens or closes an unreserved slot; Backoffice or Grid Operator.
        [Authorize(Roles = AppRoles.Staff)]
        [HttpPatch("{id}/availability")]
        public async Task<ActionResult<SlotResponse>> SetAvailability(string id, [FromBody] SetSlotAvailabilityRequest request, CancellationToken cancellationToken)
        {
            var slot = await _slotService.SetAvailabilityAsync(id, request.IsAvailable.GetValueOrDefault(), User.GetNic(), cancellationToken);
            return Ok(SlotResponse.FromModel(slot));
        }

        // DELETE api/slots/{id} - deletes an unreserved slot; Backoffice or Grid Operator.
        [Authorize(Roles = AppRoles.Staff)]
        [HttpDelete("{id}")]
        public async Task<IActionResult> Delete(string id, CancellationToken cancellationToken)
        {
            await _slotService.DeleteAsync(id, User.GetNic(), cancellationToken);
            return NoContent();
        }
    }
}
