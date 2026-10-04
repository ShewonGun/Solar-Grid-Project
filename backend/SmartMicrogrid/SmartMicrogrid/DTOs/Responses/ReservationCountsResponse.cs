/*
 * File: ReservationCountsResponse.cs
 * Purpose: Counts shown on the web and mobile dashboards - pending
 *          reservations and approved reservations that are still to come.
 * Author:  Aseni Thennakoon
 * Created: 2026-09-29
 */
namespace SmartMicrogrid.Api.DTOs.Responses
{
    public class ReservationCountsResponse
    {
        public long Pending { get; set; }

        public long ApprovedUpcoming { get; set; }
    }
}
