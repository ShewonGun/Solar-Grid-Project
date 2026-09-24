/*
 * File: Login.jsx
 * Purpose: Sign-in page for the Smart Solar Microgrid web console. Collects a
 *          NIC or email address and a password, posts them to
 *          POST api/auth/login, stores the returned JWT, and sends the user to
 *          the home screen for their role. Credentials and account status are
 *          verified by the Web API - this page only presents the result.
 * Author:  <your name>
 * Created: 2026
 */
import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'

import { login } from '../api/authApi'
import { toApiError } from '../api/client'
import { homeRouteForRole, writeSession } from '../auth/session'
import AuthLayout from '../Components/AuthLayout'
import { PasswordField, StatusBanner, SubmitButton, TextField } from '../Components/FormControls'
import { isFormValid, validatePassword, validateRequired } from '../utils/validation'

const EMPTY_FORM = { identifier: '', password: '' }

// Only these roles may use the web console; prosumers use the mobile app instead.
const STAFF_ROLES = ['Backoffice', 'GridOperator']

/*
 * Checks the whole form and returns a message per field. The API re-validates
 * everything; this only saves the user a round trip.
 */
function validateForm(form) {
  return {
    identifier: validateRequired(form.identifier, 'NIC or email'),
    password: validatePassword(form.password),
  }
}

export default function Login() {
  const navigate = useNavigate()
  const location = useLocation()

  const [form, setForm] = useState(EMPTY_FORM)
  const [errors, setErrors] = useState({})
  const [apiError, setApiError] = useState('')
  const [busy, setBusy] = useState(false)

  // A page the user was sent away from before signing in, if any.
  const redirectTo = location.state?.from
  // Message carried over from another page, e.g. after registering an account.
  const notice = location.state?.notice

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
   * Validates the form, signs in through the Web API, stores the session and
   * redirects. Failures from the service (wrong password, account pending
   * activation, account deactivated) are shown in the banner as they are sent.
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
      const session = await login(form.identifier.trim(), form.password)

      if (!STAFF_ROLES.includes(session.user.role)) {
        setApiError('This console is for back-office officers and grid operators. Prosumers should use the mobile app.')
        return
      }

      writeSession(session)
      navigate(redirectTo ?? homeRouteForRole(session.user.role), { replace: true })
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
      title="Sign in"
      subtitle="Back-office officers and grid operators use this console to run the microgrid."
      headline="Trade the sunshine your rooftop does not need."
      footer={
        <p>
          New solar prosumer?{' '}
          <Link
            to="/signup"
            className="font-medium text-slate-900 underline decoration-amber-400 decoration-2 underline-offset-4 transition-colors hover:decoration-slate-900"
          >
            Create an account
          </Link>
        </p>
      }
    >
      <form onSubmit={handleSubmit} noValidate className="space-y-5">
        {notice ? <StatusBanner tone="success">{notice}</StatusBanner> : null}
        <StatusBanner tone="error">{apiError}</StatusBanner>

        <TextField
          label="NIC or email"
          name="identifier"
          type="text"
          autoComplete="username"
          autoFocus
          placeholder="200012345678 or you@example.com"
          value={form.identifier}
          onChange={handleChange}
          onBlur={handleBlur}
          error={errors.identifier}
          disabled={busy}
        />

        <PasswordField
          label="Password"
          name="password"
          autoComplete="current-password"
          placeholder="Enter your password"
          value={form.password}
          onChange={handleChange}
          onBlur={handleBlur}
          error={errors.password}
          disabled={busy}
        />

        <SubmitButton busy={busy} busyLabel="Signing in...">
          Sign in
        </SubmitButton>

        <p className="text-xs leading-relaxed text-slate-400">
          Accounts awaiting activation cannot sign in until a back-office officer approves them.
        </p>
      </form>
    </AuthLayout>
  )
}
