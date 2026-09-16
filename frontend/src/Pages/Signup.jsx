/*
 * File: Signup.jsx
 * Purpose: Registration page for solar prosumers. Collects the profile the Web
 *          API needs - NIC (the primary key), name, contact details and solar
 *          array capacity - and posts it to POST api/auth/register. The API
 *          creates the account as PendingActivation, so on success this page
 *          explains that a back-office officer must activate it before sign-in.
 * Author:  <your name>
 * Created: 2026
 */
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { registerProsumer } from '../api/authApi'
import { toApiError } from '../api/client'
import AuthLayout from '../Components/AuthLayout'
import { PasswordField, StatusBanner, SubmitButton, TextField } from '../Components/FormControls'
import {
  MIN_PASSWORD_LENGTH,
  isFormValid,
  normaliseNic,
  validateEmail,
  validateNic,
  validatePassword,
  validatePasswordConfirmation,
  validateRequired,
  validateSolarCapacity,
} from '../utils/validation'

const EMPTY_FORM = {
  nic: '',
  fullName: '',
  email: '',
  phone: '',
  address: '',
  solarCapacityKW: '',
  password: '',
  confirmPassword: '',
}

/*
 * Checks the whole form and returns a message per field. Phone and address are
 * optional on the API's RegisterProsumerRequest, so they are not required here.
 */
function validateForm(form) {
  return {
    nic: validateNic(form.nic),
    fullName: validateRequired(form.fullName, 'Full name'),
    email: validateEmail(form.email),
    phone: '',
    address: '',
    solarCapacityKW: validateSolarCapacity(form.solarCapacityKW),
    password: validatePassword(form.password),
    confirmPassword: validatePasswordConfirmation(form.password, form.confirmPassword),
  }
}

/*
 * Builds the RegisterProsumerRequest body from the form, trimming text and
 * sending solar capacity as a number or null rather than an empty string.
 */
function toRequest(form) {
  const capacity = form.solarCapacityKW.trim()

  return {
    nic: normaliseNic(form.nic),
    fullName: form.fullName.trim(),
    email: form.email.trim().toLowerCase(),
    phone: form.phone.trim(),
    address: form.address.trim(),
    solarCapacityKW: capacity ? Number(capacity) : null,
    password: form.password,
  }
}

export default function Signup() {
  const navigate = useNavigate()

  const [form, setForm] = useState(EMPTY_FORM)
  const [errors, setErrors] = useState({})
  const [apiError, setApiError] = useState('')
  const [busy, setBusy] = useState(false)

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
    setErrors((current) => ({ ...current, [name]: validateForm(form)[name] }))
  }

  /*
   * Validates the form and registers the prosumer through the Web API. On
   * success the user is sent to the login page with a notice explaining that
   * the account is waiting for activation; duplicate NIC or email conflicts
   * come back from the service and are shown in the banner.
   */
  async function handleSubmit(event) {
    event.preventDefault()

    const nextErrors = validateForm(form)
    setErrors(nextErrors)

    if (!isFormValid(nextErrors)) {
      return
    }

    setBusy(true)
    setApiError('')

    try {
      const user = await registerProsumer(toRequest(form))

      navigate('/login', {
        replace: true,
        state: {
          notice: `Account created for NIC ${user.nic}. A back-office officer will activate it before you can sign in.`,
        },
      })
    } catch (error) {
      const { message, fieldErrors } = toApiError(error)

      setApiError(message)
      setErrors((current) => ({ ...current, ...fieldErrors }))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthLayout
      title="Create account"
      subtitle="Register your solar property to reserve energy drop-off and charging slots."
      headline="Put your surplus energy on the grid."
      footer={
        <p>
          Already registered?{' '}
          <Link
            to="/login"
            className="font-medium text-slate-900 underline decoration-amber-400 decoration-2 underline-offset-4 transition-colors hover:decoration-slate-900"
          >
            Sign in instead
          </Link>
        </p>
      }
    >
      <form onSubmit={handleSubmit} noValidate className="space-y-5">
        <StatusBanner tone="error">{apiError}</StatusBanner>

        <TextField
          label="NIC"
          name="nic"
          type="text"
          autoComplete="off"
          autoFocus
          placeholder="200012345678"
          value={form.nic}
          onChange={handleChange}
          onBlur={handleBlur}
          error={errors.nic}
          hint="Your NIC identifies your account across the microgrid and cannot be changed later."
          disabled={busy}
        />

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

        {/* Stacked rather than side by side: the form column is a quarter of the screen. */}
        <div className="grid gap-5">
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

          <TextField
            label="Solar capacity (kW)"
            name="solarCapacityKW"
            type="number"
            min="0"
            step="0.1"
            inputMode="decimal"
            placeholder="5.5"
            value={form.solarCapacityKW}
            onChange={handleChange}
            onBlur={handleBlur}
            error={errors.solarCapacityKW}
            optional
            disabled={busy}
          />
        </div>

        <TextField
          label="Address"
          name="address"
          type="text"
          autoComplete="street-address"
          placeholder="Where your solar array is installed"
          value={form.address}
          onChange={handleChange}
          error={errors.address}
          optional
          disabled={busy}
        />

        <PasswordField
          label="Password"
          name="password"
          autoComplete="new-password"
          placeholder={`At least ${MIN_PASSWORD_LENGTH} characters`}
          value={form.password}
          onChange={handleChange}
          onBlur={handleBlur}
          error={errors.password}
          disabled={busy}
        />

        <PasswordField
          label="Confirm password"
          name="confirmPassword"
          autoComplete="new-password"
          placeholder="Re-enter your password"
          value={form.confirmPassword}
          onChange={handleChange}
          onBlur={handleBlur}
          error={errors.confirmPassword}
          disabled={busy}
        />

        <SubmitButton busy={busy} busyLabel="Creating account...">
          Create account
        </SubmitButton>

        <p className="text-xs leading-relaxed text-slate-400">
          New accounts stay pending until a back-office officer activates them.
        </p>
      </form>
    </AuthLayout>
  )
}
