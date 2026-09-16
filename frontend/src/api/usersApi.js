/*
 * File: usersApi.js
 * Purpose: Wraps the api/users endpoints of the Web API. All of these are
 *          Back-office only except profile editing, and the service enforces
 *          that - notably that a deactivated account can only be brought back
 *          by a Back-office officer.
 * Author:  <your name>
 * Created: 2026
 */
import { apiClient } from './client'

/* GET api/users - users filtered by role, status and a name/NIC/email search. */
export async function getUsers({ role, status, search } = {}) {
  const { data } = await apiClient.get('/users', { params: { role, status, search } })
  return data
}

/* GET api/users/pending-activations - prosumer accounts waiting for activation. */
export async function getPendingActivations() {
  const { data } = await apiClient.get('/users/pending-activations')
  return data
}

/* GET api/users/{nic} - a single user by NIC. */
export async function getUser(nic) {
  const { data } = await apiClient.get(`/users/${nic}`)
  return data
}

/* POST api/users - creates a Back-office or Grid Operator account; active at once. */
export async function createUser(request) {
  const { data } = await apiClient.post('/users', request)
  return data
}

/* PUT api/users/{nic} - updates a user's profile details. */
export async function updateUser(nic, request) {
  const { data } = await apiClient.put(`/users/${nic}`, request)
  return data
}

/*
 * PUT api/users/me/password - changes the signed-in user's own password. The
 * service verifies the current password before accepting the new one.
 */
export async function changeOwnPassword(currentPassword, newPassword) {
  await apiClient.put('/users/me/password', { currentPassword, newPassword })
}

/* POST api/users/{nic}/activate - activates or reactivates an account. */
export async function activateUser(nic) {
  const { data } = await apiClient.post(`/users/${nic}/activate`)
  return data
}

/* POST api/users/{nic}/deactivate - deactivates an account. */
export async function deactivateUser(nic) {
  const { data } = await apiClient.post(`/users/${nic}/deactivate`)
  return data
}
