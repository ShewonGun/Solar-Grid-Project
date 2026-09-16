/*
 * File: stationsApi.js
 * Purpose: Wraps the api/stations endpoints of the Web API - listing, reading,
 *          creating, updating, activating and deleting microgrid nodes. Every
 *          rule (validation, the block on deactivating a node that still has
 *          active reservations) lives in the service; this module only carries
 *          the request and hands back the reply.
 * Author:  <your name>
 * Created: 2026
 */
import { apiClient } from './client'

/* GET api/stations - all nodes, or only the active ones when asked. */
export async function getStations(activeOnly = false) {
  const { data } = await apiClient.get('/stations', { params: { activeOnly } })
  return data
}

/* GET api/stations/{id} - a single node. */
export async function getStation(id) {
  const { data } = await apiClient.get(`/stations/${id}`)
  return data
}

/* POST api/stations - registers a new microgrid node; Backoffice only. */
export async function createStation(request) {
  const { data } = await apiClient.post('/stations', request)
  return data
}

/* PUT api/stations/{id} - updates a node's details and schedule. */
export async function updateStation(id, request) {
  const { data } = await apiClient.put(`/stations/${id}`, request)
  return data
}

/*
 * PATCH api/stations/{id}/active - activates or deactivates a node. The API
 * refuses to deactivate one that still has active energy reservations.
 */
export async function setStationActive(id, isActive) {
  const { data } = await apiClient.patch(`/stations/${id}/active`, { isActive })
  return data
}

/*
 * DELETE api/stations/{id} - removes a node. The API refuses when the node has
 * reservations and asks for it to be deactivated instead.
 */
export async function deleteStation(id) {
  await apiClient.delete(`/stations/${id}`)
}
