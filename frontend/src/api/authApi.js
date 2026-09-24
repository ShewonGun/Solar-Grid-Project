/*
 * File: authApi.js
 * Purpose: Wraps the api/auth endpoints the web console uses - signing in and
 *          reading the signed-in user's own profile. Prosumer registration
 *          (POST api/auth/register) has no wrapper here: prosumers register
 *          from the mobile app, and the web console has no prosumer screens to
 *          call it from.
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

/* GET api/auth/me - returns the profile of the currently signed-in user. */
export async function getCurrentUser() {
  const { data } = await apiClient.get('/auth/me')
  return data
}
