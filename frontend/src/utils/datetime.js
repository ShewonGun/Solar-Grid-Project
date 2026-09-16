/*
 * File: datetime.js
 * Purpose: Converts between the ISO timestamps the Web API exchanges and the
 *          "YYYY-MM-DDTHH:mm" local strings an <input type="datetime-local">
 *          works in. The API stores everything in UTC, so these two functions
 *          are the only place the console crosses between the two.
 * Author:  <your name>
 * Created: 2026
 */

/* Pads a number to two digits, as the input value format requires. */
function pad(value) {
  return String(value).padStart(2, '0')
}

/*
 * Turns an ISO timestamp from the API into the local value a datetime-local
 * input expects. Returns an empty string for a missing or unparseable date.
 */
export function toLocalInputValue(iso) {
  if (!iso) {
    return ''
  }

  const date = new Date(iso)

  if (Number.isNaN(date.getTime())) {
    return ''
  }

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(
    date.getHours(),
  )}:${pad(date.getMinutes())}`
}

/*
 * Turns a datetime-local value back into an ISO timestamp for the API. The
 * browser reads the value in the viewer's own time zone, so this is where a
 * local wall-clock time becomes the UTC instant the service stores.
 */
export function fromLocalInputValue(value) {
  if (!value) {
    return null
  }

  const date = new Date(value)

  return Number.isNaN(date.getTime()) ? null : date.toISOString()
}

/* The current local time in datetime-local format, for a field's `min`. */
export function nowInputValue() {
  return toLocalInputValue(new Date().toISOString())
}
