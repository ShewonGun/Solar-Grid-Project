/*
 * File: slotsApi.js
 * Purpose: Wraps the api/slots endpoints of the Web API - the bookable slots a
 *          reservation can be made against, and the per-station slot
 *          management a Grid Operator uses. The service decides what counts as
 *          bookable; this module only carries the request and reply.
 * Author:  <your name>
 * Created: 2026
 */
import { apiClient } from './client'

/*
 * GET api/slots/bookable - slots that can be booked right now. The service
 * already limits these to available slots at active stations that start in the
 * future and inside the 7-day reservation window.
 */
export async function getBookableSlots(stationId) {
  const { data } = await apiClient.get('/slots/bookable', { params: { stationId } })
  return data
}

/* GET api/stations/{stationId}/slots - every slot at one station; staff only. */
export async function getStationSlots(stationId, { from, to, status } = {}) {
  const { data } = await apiClient.get(`/stations/${stationId}/slots`, {
    params: {
      from: from instanceof Date ? from.toISOString() : from,
      to: to instanceof Date ? to.toISOString() : to,
      status,
    },
  })

  return data
}

/*
 * Fetches every slot across a set of stations, one request per station run in
 * parallel. There is no endpoint that returns slots for every node in one
 * call, so the dashboard's cross-node cards (utilisation, inventory) call this
 * once and derive both from the same data rather than each fetching it again.
 */
export async function getSlotsForStations(stationIds, params = {}) {
  const results = await Promise.all(
    stationIds.map((stationId) => getStationSlots(stationId, params)),
  )

  return results.flat()
}

/* GET api/slots/{id} - a single slot. */
export async function getSlot(id) {
  const { data } = await apiClient.get(`/slots/${id}`)
  return data
}

/* POST api/slots - creates a bookable window on one battery slot. */
export async function createSlot(request) {
  const { data } = await apiClient.post('/slots', request)
  return data
}

/* PUT api/slots/{id} - updates a slot's window, battery number or capacity. */
export async function updateSlot(id, request) {
  const { data } = await apiClient.put(`/slots/${id}`, request)
  return data
}

/* PATCH api/slots/{id}/availability - takes a slot in or out of service. */
export async function setSlotAvailability(id, isAvailable) {
  const { data } = await apiClient.patch(`/slots/${id}/availability`, { isAvailable })
  return data
}

/* DELETE api/slots/{id} - removes a slot. */
export async function deleteSlot(id) {
  await apiClient.delete(`/slots/${id}`)
}
