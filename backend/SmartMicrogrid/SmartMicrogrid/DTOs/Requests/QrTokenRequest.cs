/*
 * File: QrTokenRequest.cs
 * Purpose: Request body for POST api/reservations/verify-qr and
 *          POST api/reservations/complete, carrying the token scanned from a
 *          prosumer's QR code.
 * Author:  <your name>
 * Created: 2026
 */
using System.ComponentModel.DataAnnotations;

namespace SmartMicrogrid.Api.DTOs.Requests
{
    public class QrTokenRequest
    {
        [Required]
        public string QrToken { get; set; } = string.Empty;
    }
}
