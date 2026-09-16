/*
 * File: reservationRules.js
 * Purpose: Read-only helpers that describe the reservation rules the Web API
 *          enforces, so the console can explain them before the user acts -
 *          greying out an action that would be refused and saying why. These
 *          never decide anything: the service re-checks every rule and its
 *          answer is the one that counts.
 * Author:  <your name>
 * Created: 2026
 */

/** Reservations must start within this many days (API: MaxDaysAhead). */
export const BOOKING_WINDOW_DAYS = 7

/** Updates and cancellations need this much notice (API: MinNoticeHours). */
export const MIN_NOTICE_HOURS = 12

/** Statuses that can still be changed or cancelled. */
const OPEN_STATUSES = ['Pending', 'Approved']

/* Hours between now and the given time; negative once it has passed. */
export function hoursUntil(value) {
  return (new Date(value).getTime() - Date.now()) / 3_600_000
}

/*
 * Explains whether a reservation can still be changed or cancelled, returning
 * { allowed, reason }. The reason is written for the user, not the log.
 */
export function modificationState(reservation) {
  if (!OPEN_STATUSES.includes(reservation.status)) {
    return {
      allowed: false,
      reason: `A ${reservation.status.toLowerCase()} reservation cannot be changed.`,
    }
  }

  const remaining = hoursUntil(reservation.reservationStart)

  if (remaining < 0) {
    return { allowed: false, reason: 'This reservation has already started.' }
  }

  if (remaining < MIN_NOTICE_HOURS) {
    return {
      allowed: false,
      reason: `Changes and cancellations need at least ${MIN_NOTICE_HOURS} hours' notice, and this booking starts in under ${Math.ceil(remaining)}.`,
    }
  }

  return { allowed: true, reason: '' }
}

/*
 * True when a slot starts far enough ahead to be a valid target for moving an
 * existing reservation - the API applies the same notice rule to the new slot.
 */
export function slotAcceptsReschedule(slot) {
  return hoursUntil(slot.startTime) >= MIN_NOTICE_HOURS
}

/* Formats a date and time for tables and pickers. */
export function formatDateTime(value) {
  return new Date(value).toLocaleString(undefined, {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/* Formats the time range of a slot, e.g. "14:00 - 16:00". */
export function formatTimeRange(startTime, endTime) {
  const options = { hour: '2-digit', minute: '2-digit' }

  return `${new Date(startTime).toLocaleTimeString(undefined, options)} - ${new Date(
    endTime,
  ).toLocaleTimeString(undefined, options)}`
}
