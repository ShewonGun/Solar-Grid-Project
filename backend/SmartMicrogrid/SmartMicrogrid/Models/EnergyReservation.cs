/*
 * File: EnergyReservation.cs
 * Purpose: Represents a prosumer's reservation of an energy booking slot for
 *          energy drop-off or charging. Tracks approval, the secure QR token
 *          scanned by Grid Operators, and completion/cancellation. Maps to the
 *          "EnergyReservation" collection named in the marking scheme.
 * Author:  <your name>
 * Created: 2026
 */
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;
using SmartMicrogrid.Api.Models.Enums;

namespace SmartMicrogrid.Api.Models
{
    public class EnergyReservation
    {
        [BsonId]
        [BsonRepresentation(BsonType.ObjectId)]
        public string? Id { get; set; }

        // Reference to User.Nic (the prosumer).
        [BsonElement("prosumerNic")]
        public string ProsumerNic { get; set; } = string.Empty;

        // Reference to SolarStationInfo.Id.
        [BsonElement("stationId")]
        [BsonRepresentation(BsonType.ObjectId)]
        public string StationId { get; set; } = string.Empty;

        // Reference to EnergyBookingSlot.Id.
        [BsonElement("slotId")]
        [BsonRepresentation(BsonType.ObjectId)]
        public string SlotId { get; set; } = string.Empty;

        [BsonElement("type")]
        [BsonRepresentation(BsonType.String)]
        public ReservationType Type { get; set; } = ReservationType.DropOff;

        [BsonElement("energyKWh")]
        public double EnergyKWh { get; set; }

        // Must be within 7 days of booking; update/cancel needs 12 hours' notice.
        [BsonElement("reservationStart")]
        public DateTime ReservationStart { get; set; }

        [BsonElement("reservationEnd")]
        public DateTime ReservationEnd { get; set; }

        [BsonElement("status")]
        [BsonRepresentation(BsonType.String)]
        public ReservationStatus Status { get; set; } = ReservationStatus.Pending;

        // Random token encoded in the QR code once approved; verified by the API on scan.
        [BsonElement("qrToken")]
        public string? QrToken { get; set; }

        // NIC of the Backoffice/Grid Operator user who approved it.
        [BsonElement("approvedBy")]
        public string? ApprovedBy { get; set; }

        [BsonElement("approvedAt")]
        public DateTime? ApprovedAt { get; set; }

        // NIC of the Grid Operator who scanned the QR and finalised the transfer.
        [BsonElement("completedBy")]
        public string? CompletedBy { get; set; }

        [BsonElement("completedAt")]
        public DateTime? CompletedAt { get; set; }

        // NIC of whoever cancelled (the prosumer or a Grid Operator).
        [BsonElement("cancelledBy")]
        public string? CancelledBy { get; set; }

        [BsonElement("cancelledAt")]
        public DateTime? CancelledAt { get; set; }

        [BsonElement("cancellationReason")]
        public string? CancellationReason { get; set; }

        [BsonElement("createdAt")]
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

        [BsonElement("updatedAt")]
        public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
    }
}
