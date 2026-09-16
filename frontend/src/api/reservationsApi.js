/*
 * File: reservationsApi.js
 * Purpose: Wraps the api/reservations endpoints of the Web API. The 7-day
 *          booking window, the 12-hour notice rule, approval and QR handling
 *          all live in the service - this module only carries the request and
 *          hands back the reply.
 * Author:  <your name>
 * Created: 2026
 */
import { apiClient } from './client'

/*
 * GET api/reservations - searches reservations. Every filter is optional;
 * prosumers are scoped to their own bookings by the service regardless.
 */
export async function searchReservations({ prosumerNic, stationId, status, from, to } = {}) {
  const { data } = await apiClient.get('/reservations', {
    params: {
      prosumerNic,
      stationId,
      status,
      from: from instanceof Date ? from.toISOString() : from,
      to: to instanceof Date ? to.toISOString() : to,
    },
  })

  return data
}

/*
 * GET api/reservations/dashboard - the pending count and the count of approved
 * future reservations, optionally narrowed to one station.
 */
export async function getDashboardCounts(stationId) {
  const { data } = await apiClient.get('/reservations/dashboard', { params: { stationId } })
  return data
}

/* GET api/reservations/{id} - a single reservation. */
export async function getReservation(id) {
  const { data } = await apiClient.get(`/reservations/${id}`)
  return data
}

/*
 * POST api/reservations - books a slot for a prosumer. Staff pass the
 * prosumer's NIC; the booking is created as Pending.
 */
export async function createReservation({ prosumerNic, slotId, type, energyKWh }) {
  const { data } = await apiClient.post('/reservations', {
    prosumerNic,
    slotId,
    type,
    energyKWh,
  })

  return data
}

/*
 * PUT api/reservations/{id} - changes the slot, type or energy amount. The
 * service resets the booking to Pending and revokes its QR code.
 */
export async function updateReservation(id, { slotId, type, energyKWh }) {
  const { data } = await apiClient.put(`/reservations/${id}`, { slotId, type, energyKWh })
  return data
}

/* POST api/reservations/{id}/approve - approves a pending booking; staff only. */
export async function approveReservation(id) {
  const { data } = await apiClient.post(`/reservations/${id}/approve`)
  return data
}

/* POST api/reservations/{id}/cancel - cancels a booking with an optional reason. */
export async function cancelReservation(id, reason) {
  const { data } = await apiClient.post(`/reservations/${id}/cancel`, { reason })
  return data
}
