/*
 * File: authApi.js
 * Purpose: Wraps the api/auth endpoints of the Smart Microgrid Web API - sign
 *          in, prosumer self-registration and reading the signed-in user. Every
 *          rule (credential checks, NIC uniqueness, account activation) is
 *          applied by the service; this module only carries request and reply.
 * Author:  <your name>
 * Created: 2026
 */
import { apiClient } from './client'

/*
 * POST api/auth/login - signs a user in with their NIC or email and password.
 * Returns { token, expiresAt, user }.
 */
export async function login(identifier, password) {
  const { data } = await apiClient.post('/auth/login', { identifier, password })
  return data
}

/*
 * POST api/auth/register - registers a solar prosumer using their NIC as the
 * primary key. The API creates the account as PendingActivation; a Backoffice
 * officer must activate it before the prosumer can sign in.
 */
export async function registerProsumer(request) {
  const { data } = await apiClient.post('/auth/register', request)
  return data
}

/* GET api/auth/me - returns the profile of the currently signed-in user. */
export async function getCurrentUser() {
  const { data } = await apiClient.get('/auth/me')
  return data
}
