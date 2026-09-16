/*
 * File: validation.js
 * Purpose: Client-side form checks used by the authentication pages. These
 *          mirror the rules the Web API enforces (UserService.ValidateNic,
 *          ValidateProfile and ValidatePassword) purely so the user gets
 *          instant feedback - the service remains the authority and re-checks
 *          everything it is sent.
 * Author:  <your name>
 * Created: 2026
 */

/* Sri Lankan NIC: nine digits followed by V or X, or twelve digits. */
const NIC_PATTERN = /^(\d{9}[VX]|\d{12})$/

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/** Shortest password the API accepts (RegisterProsumerRequest.Password). */
export const MIN_PASSWORD_LENGTH = 6

/*
 * Normalises a NIC the same way the API does before it is stored or compared:
 * trimmed and upper-cased.
 */
export function normaliseNic(nic) {
  return (nic ?? '').trim().toUpperCase()
}

/* Returns an error message for the NIC field, or an empty string when valid. */
export function validateNic(nic) {
  const value = normaliseNic(nic)

  if (!value) {
    return 'NIC is required.'
  }

  if (!NIC_PATTERN.test(value)) {
    return 'Enter a valid NIC - 9 digits followed by V or X, or 12 digits.'
  }

  return ''
}

/* Returns an error message for a required text field, or an empty string. */
export function validateRequired(value, label) {
  return (value ?? '').trim() ? '' : `${label} is required.`
}

/* Returns an error message for the email field, or an empty string when valid. */
export function validateEmail(email) {
  const value = (email ?? '').trim()

  if (!value) {
    return 'Email is required.'
  }

  return EMAIL_PATTERN.test(value) ? '' : 'Enter a valid email address.'
}

/* Returns an error message for the password field, or an empty string. */
export function validatePassword(password) {
  if (!password) {
    return 'Password is required.'
  }

  return password.length < MIN_PASSWORD_LENGTH
    ? `Password must be at least ${MIN_PASSWORD_LENGTH} characters.`
    : ''
}

/*
 * Returns an error message when the confirmation does not match the password.
 * This check exists only in the UI; the API is sent a single password.
 */
export function validatePasswordConfirmation(password, confirmation) {
  if (!confirmation) {
    return 'Please re-enter your password.'
  }

  return password === confirmation ? '' : 'Passwords do not match.'
}

/*
 * Validates the optional solar array capacity. Blank is allowed; anything else
 * must be a number that is not negative, matching the API's Range check.
 */
export function validateSolarCapacity(value) {
  const text = (value ?? '').trim()

  if (!text) {
    return ''
  }

  const parsed = Number(text)

  if (!Number.isFinite(parsed)) {
    return 'Enter solar capacity as a number, for example 5.5.'
  }

  return parsed < 0 ? 'Solar capacity cannot be negative.' : ''
}

/*
 * Returns true when every value in an errors object is an empty string, i.e.
 * the form has nothing left to correct.
 */
export function isFormValid(errors) {
  return Object.values(errors).every((message) => !message)
}
