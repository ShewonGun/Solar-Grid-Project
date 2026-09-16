/*
 * File: ReservationCountsResponse.cs
 * Purpose: Counts shown on the web and mobile dashboards - pending
 *          reservations and approved reservations that are still to come.
 * Author:  <your name>
 * Created: 2026
 */
namespace SmartMicrogrid.Api.DTOs.Responses
{
    public class ReservationCountsResponse
    {
        public long Pending { get; set; }

        public long ApprovedUpcoming { get; set; }
    }
}
