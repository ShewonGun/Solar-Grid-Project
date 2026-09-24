/*
 * File: client.js
 * Purpose: The single axios instance every API call in this web app shares. It
 *          points at the Smart Microgrid Web API, attaches the signed-in user's
 *          JWT to each request, and clears the session when the service reports
 *          the token is no longer accepted. The web app is a UI layer only, so
 *          all data and business logic is reached through here.
 * Author:  <your name>
 * Created: 2026
 */
import axios from 'axios'

import { clearSession, readSession } from '../auth/session'

/*
 * Dispatched on window whenever the stored session changes - the API rejected
 * the token, or the user signed out. Components listen for it through
 * the useSession hook so every screen reacts to a sign-out together.
 */
export const SESSION_CHANGED_EVENT = 'smartmicrogrid:session-changed'

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081/api'

export const apiClient = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

// Attaches the bearer token to every outgoing request when a session exists.
apiClient.interceptors.request.use((config) => {
  const session = readSession()

  if (session) {
    config.headers.Authorization = `Bearer ${session.token}`
  }

  return config
})

// Clears the stored session when the API answers 401, so an expired or revoked
// token cannot leave the user on a screen that will never load.
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401 && readSession()) {
      clearSession()
      window.dispatchEvent(new Event(SESSION_CHANGED_EVENT))
    }

    return Promise.reject(error)
  },
)

const NETWORK_ERROR_MESSAGE =
  'Could not reach the Smart Microgrid service. Check your connection and try again.'

const UNEXPECTED_ERROR_MESSAGE = 'Something went wrong. Please try again.'

/*
 * Normalises any failure thrown by axios into { message, fieldErrors } so the
 * pages can render it without knowing about axios. The Web API always replies
 * with an ErrorResponse body, so its message is preferred when present.
 */
export function toApiError(error) {
  const response = error?.response

  // No response means the request never reached the server (service down,
  // wrong base URL, CORS, or a dropped connection).
  if (!response) {
    return { message: NETWORK_ERROR_MESSAGE, fieldErrors: {} }
  }

  const body = response.data ?? {}

  return {
    message: body.message?.trim() || UNEXPECTED_ERROR_MESSAGE,
    fieldErrors: normaliseFieldErrors(body.errors),
  }
}

/*
 * Lower-cases the keys of the API's validation dictionary so a form field named
 * "fullName" matches the server's "FullName" entry.
 */
function normaliseFieldErrors(errors) {
  if (!errors) {
    return {}
  }

  return Object.entries(errors).reduce((result, [key, messages]) => {
    result[key.toLowerCase()] = messages?.[0] ?? ''
    return result
  }, {})
}
