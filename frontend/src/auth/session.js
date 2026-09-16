/*
 * File: session.js
 * Purpose: Reads and writes the signed-in session (the JWT, its expiry and the
 *          user profile returned by the Web API) in localStorage, so a page
 *          refresh does not sign the user out. Only the token and profile are
 *          kept here - no business data is cached in the browser.
 * Author:  <your name>
 * Created: 2026
 */
const STORAGE_KEY = 'smartmicrogrid.session'

/*
 * Returns the stored session, or null when there is none, it cannot be parsed,
 * or its token has already expired.
 */
export function readSession() {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)

    if (!raw) {
      return null
    }

    const session = JSON.parse(raw)

    if (!session?.token || !session.user?.nic || isExpired(session.expiresAt)) {
      clearSession()
      return null
    }

    return session
  } catch {
    // Corrupt or unavailable storage is treated as "not signed in".
    return null
  }
}

/*
 * Persists the { token, expiresAt, user } payload returned by
 * POST /api/auth/login.
 */
export function writeSession(session) {
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
  } catch {
    // Storage can be full or blocked; the caller still holds the session.
  }
}

/* Removes the stored session on sign-out or when the API rejects the token. */
export function clearSession() {
  try {
    window.localStorage.removeItem(STORAGE_KEY)
  } catch {
    // Nothing further to do.
  }
}

/* Returns true when the token's expiry time has already passed. */
function isExpired(expiresAt) {
  const expiry = Date.parse(expiresAt)
  return Number.isFinite(expiry) && expiry <= Date.now()
}

/*
 * Maps a user role returned by the API to the home route for that role, so
 * login lands each user on the screens they are allowed to use.
 */
export function homeRouteForRole(role) {
  switch (role) {
    case 'Backoffice':
      return '/backoffice'
    case 'GridOperator':
      return '/operator'
    default:
      return '/prosumer'
  }
}
