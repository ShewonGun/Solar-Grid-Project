/*
 * File: UserFormModal.jsx
 * Purpose: Dialog that creates a console user and edits an existing profile -
 *          passing a nic switches it to edit. Accounts created here are made
 *          active immediately by the service, unlike a prosumer who signs up
 *          for themselves and waits for activation. Role, NIC and password are
 *          set only on creation; the update endpoint carries profile details.
 * Author:  <your name>
 * Created: 2026
 */
import { useEffect, useState } from 'react'

import { toApiError } from '../api/client'
import { createUser, getUser, updateUser } from '../api/usersApi'
import { TextField, PasswordField } from './FormControls'
import { Banner, Button, Modal } from './PageControls'
import {
  MIN_PASSWORD_LENGTH,
  isFormValid,
  normaliseNic,
  validateEmail,
  validateNic,
  validatePassword,
  validateRequired,
  validateSolarCapacity,
} from '../utils/validation'

const EMPTY_FORM = {
  nic: '',
  fullName: '',
  email: '',
  phone: '',
  address: '',
  role: 'GridOperator',
  solarCapacityKW: '',
  password: '',
}

/** The roles a Back-office officer can create from the console. */
const ROLE_OPTIONS = [
  { value: 'GridOperator', label: 'Grid operator', hint: 'Runs day-to-day node operations.' },
  { value: 'Backoffice', label: 'Back-office officer', hint: 'Administers the whole system.' },
  { value: 'Prosumer', label: 'Solar prosumer', hint: 'Books energy slots from the mobile app.' },
]

const CONTROL_CLASSES =
  'w-full rounded-xs border border-slate-300 bg-white px-2.5 py-2 text-sm text-slate-900 outline-none transition-colors hover:border-slate-400 focus:border-slate-900 focus:ring-1 focus:ring-slate-900 disabled:bg-slate-50 disabled:text-slate-400'

/*
 * Checks the form against the rules the API applies. NIC, role and password
 * only exist on creation, so they are skipped when editing.
 */
function validateForm(form, isEditing) {
  return {
    nic: isEditing ? '' : validateNic(form.nic),
    fullName: validateRequired(form.fullName, 'Full name'),
    email: validateEmail(form.email),
    phone: '',
    address: '',
    solarCapacityKW: validateSolarCapacity(form.solarCapacityKW),
    password: isEditing ? '' : validatePassword(form.password),
  }
}

/*
 * `nic` selects the account to edit, or is left out to create one. `onSaved` is
 * called after the service accepts the change so the list behind the dialog can
 * reload; `onClose` dismisses it.
 */
export default function UserFormModal({ nic, onClose, onSaved }) {
  const isEditing = Boolean(nic)

  const [form, setForm] = useState(EMPTY_FORM)
  const [errors, setErrors] = useState({})
  const [apiError, setApiError] = useState('')
  const [loading, setLoading] = useState(isEditing)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    // Nothing to load when creating a new account.
    if (!isEditing) {
      return
    }

    let cancelled = false

    /* Loads the account being edited and fills the form with its details. */
    async function loadUser() {
      try {
        const user = await getUser(nic)

        if (!cancelled) {
          setForm({
            nic: user.nic,
            fullName: user.fullName,
            email: user.email,
            phone: user.phone ?? '',
            address: user.address ?? '',
            role: user.role,
            solarCapacityKW:
              user.solarCapacityKW === null || user.solarCapacityKW === undefined
                ? ''
                : String(user.solarCapacityKW),
            password: '',
          })
          setApiError('')
        }
      } catch (failure) {
        if (!cancelled) {
          setApiError(toApiError(failure).message)
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    loadUser()

    return () => {
      cancelled = true
    }
  }, [nic, isEditing])

  /* Keeps one field in state and clears its error as soon as the user edits it. */
  function handleChange(event) {
    const { name, value } = event.target

    setForm((current) => ({ ...current, [name]: value }))
    setErrors((current) => ({ ...current, [name]: '' }))
    setApiError('')
  }

  /* Re-checks a single field once the user leaves it. */
  function handleBlur(event) {
    const { name } = event.target
    setErrors((current) => ({ ...current, [name]: validateForm(form, isEditing)[name] }))
  }

  /*
   * Validates the form and saves through the Web API. Creating sends the full
   * account including role and password; editing sends only the profile fields
   * the update endpoint accepts.
   */
  async function handleSubmit(event) {
    event.preventDefault()

    const nextErrors = validateForm(form, isEditing)
    setErrors(nextErrors)

    if (!isFormValid(nextErrors)) {
      return
    }

    setBusy(true)
    setApiError('')

    const capacity = form.solarCapacityKW.trim()

    const profile = {
      fullName: form.fullName.trim(),
      email: form.email.trim().toLowerCase(),
      phone: form.phone.trim(),
      address: form.address.trim(),
      solarCapacityKW: capacity ? Number(capacity) : null,
    }

    try {
      if (isEditing) {
        await updateUser(nic, profile)
      } else {
        await createUser({
          ...profile,
          nic: normaliseNic(form.nic),
          role: form.role,
          password: form.password,
        })
      }

      onSaved(
        isEditing
          ? `${profile.fullName} updated.`
          : `${profile.fullName} created and activated.`,
      )
      onClose()
    } catch (failure) {
      const { message, fieldErrors } = toApiError(failure)

      setApiError(message)
      setErrors((current) => ({ ...current, ...fieldErrors }))
    } finally {
      setBusy(false)
    }
  }

  const selectedRole = ROLE_OPTIONS.find((option) => option.value === form.role)
  const isProsumer = form.role === 'Prosumer'

  return (
    <Modal
      title={isEditing ? 'Edit user' : 'Create user'}
      description={
        isEditing
          ? 'Profile details only. Role and NIC cannot be changed once an account exists.'
          : 'Accounts created here are active immediately and can sign in straight away.'
      }
      size="lg"
      onClose={onClose}
    >
      <Banner tone="error">{apiError}</Banner>

      {loading ? (
        <p className="py-8 text-center text-sm text-slate-400">Loading user...</p>
      ) : (
        <form onSubmit={handleSubmit} noValidate>
          <div className="space-y-5">
            {isEditing ? (
              <div className="border border-slate-200 bg-slate-50 px-3 py-2.5">
                <span className="text-[11px] font-medium uppercase tracking-[0.07em] text-slate-500">
                  Account
                </span>
                <p className="mt-1 text-sm tabular-nums text-slate-900">NIC {form.nic}</p>
              </div>
            ) : (
              <>
                <div>
                  <label
                    htmlFor="role"
                    className="mb-1.5 block text-[11px] font-medium uppercase tracking-[0.07em] text-slate-500"
                  >
                    Role
                  </label>
                  <select
                    id="role"
                    name="role"
                    value={form.role}
                    onChange={handleChange}
                    disabled={busy}
                    className={CONTROL_CLASSES}
                  >
                    {ROLE_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                  <p className="mt-1.5 text-xs text-slate-400">{selectedRole?.hint}</p>
                </div>

                <TextField
                  label="NIC"
                  name="nic"
                  type="text"
                  autoFocus
                  placeholder="200012345678"
                  value={form.nic}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  error={errors.nic}
                  hint="Used as the account's primary key and cannot be changed later."
                  disabled={busy}
                />
              </>
            )}

            <TextField
              label="Full name"
              name="fullName"
              type="text"
              autoComplete="name"
              placeholder="A. B. Perera"
              value={form.fullName}
              onChange={handleChange}
              onBlur={handleBlur}
              error={errors.fullName}
              disabled={busy}
            />

            <div className="grid gap-5 sm:grid-cols-2">
              <TextField
                label="Email"
                name="email"
                type="email"
                autoComplete="email"
                placeholder="you@example.com"
                value={form.email}
                onChange={handleChange}
                onBlur={handleBlur}
                error={errors.email}
                disabled={busy}
              />

              <TextField
                label="Phone"
                name="phone"
                type="tel"
                autoComplete="tel"
                placeholder="07X XXX XXXX"
                value={form.phone}
                onChange={handleChange}
                error={errors.phone}
                optional
                disabled={busy}
              />
            </div>

            <TextField
              label="Address"
              name="address"
              type="text"
              autoComplete="street-address"
              placeholder="Street address"
              value={form.address}
              onChange={handleChange}
              error={errors.address}
              optional
              disabled={busy}
            />

            {/* Solar capacity describes a prosumer's array, so it is only asked
                for when the account is one. */}
            {isProsumer ? (
              <TextField
                label="Solar capacity (kW)"
                name="solarCapacityKW"
                type="number"
                step="0.1"
                min="0"
                inputMode="decimal"
                placeholder="5.5"
                value={form.solarCapacityKW}
                onChange={handleChange}
                onBlur={handleBlur}
                error={errors.solarCapacityKW}
                optional
                disabled={busy}
              />
            ) : null}

            {isEditing ? null : (
              <PasswordField
                label="Password"
                name="password"
                autoComplete="new-password"
                placeholder={`At least ${MIN_PASSWORD_LENGTH} characters`}
                value={form.password}
                onChange={handleChange}
                onBlur={handleBlur}
                error={errors.password}
                hint="Share this with the user; they can change it after signing in."
                disabled={busy}
              />
            )}
          </div>

          <div className="mt-7 flex justify-end gap-2 border-t border-slate-200 pt-5">
            <Button type="button" variant="secondary" onClick={onClose} disabled={busy}>
              Cancel
            </Button>
            <Button type="submit" disabled={busy}>
              {busy ? 'Saving...' : isEditing ? 'Save changes' : 'Create user'}
            </Button>
          </div>
        </form>
      )}
    </Modal>
  )
}
