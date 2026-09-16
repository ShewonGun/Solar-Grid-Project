/*
 * File: ChangePasswordModal.jsx
 * Purpose: Dialog that changes the signed-in user's own password through
 *          PUT api/users/me/password. The current password is verified by the
 *          service, not here; this dialog only checks that the new password is
 *          long enough and was typed the same way twice.
 * Author:  <your name>
 * Created: 2026
 */
import { useState } from 'react'

import { toApiError } from '../api/client'
import { changeOwnPassword } from '../api/usersApi'
import { PasswordField } from './FormControls'
import { Banner, Button, Modal } from './PageControls'
import {
  MIN_PASSWORD_LENGTH,
  isFormValid,
  validatePassword,
  validatePasswordConfirmation,
} from '../utils/validation'

const EMPTY_FORM = { currentPassword: '', newPassword: '', confirmPassword: '' }

/*
 * Checks the form. Whether the current password is right is the service's
 * decision - all this can tell is that one was entered.
 */
function validateForm(form) {
  return {
    currentPassword: form.currentPassword ? '' : 'Enter your current password.',
    newPassword: validatePassword(form.newPassword),
    confirmPassword: validatePasswordConfirmation(form.newPassword, form.confirmPassword),
  }
}

/* `onSaved` reports success to the page behind; `onClose` dismisses the dialog. */
export default function ChangePasswordModal({ onClose, onSaved }) {
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
   * Sends the change to the Web API. A wrong current password comes back from
   * the service and is shown as it was sent.
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
      await changeOwnPassword(form.currentPassword, form.newPassword)

      onSaved('Your password has been changed.')
      onClose()
    } catch (failure) {
      const { message, fieldErrors } = toApiError(failure)

      setApiError(message)
      setErrors((current) => ({ ...current, ...fieldErrors }))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal
      title="Change password"
      description="You stay signed in on this device after changing it."
      onClose={onClose}
    >
      <Banner tone="error">{apiError}</Banner>

      <form onSubmit={handleSubmit} noValidate>
        <div className="space-y-5">
          <PasswordField
            label="Current password"
            name="currentPassword"
            autoComplete="current-password"
            autoFocus
            value={form.currentPassword}
            onChange={handleChange}
            onBlur={handleBlur}
            error={errors.currentPassword}
            disabled={busy}
          />

          <PasswordField
            label="New password"
            name="newPassword"
            autoComplete="new-password"
            placeholder={`At least ${MIN_PASSWORD_LENGTH} characters`}
            value={form.newPassword}
            onChange={handleChange}
            onBlur={handleBlur}
            error={errors.newPassword}
            disabled={busy}
          />

          <PasswordField
            label="Confirm new password"
            name="confirmPassword"
            autoComplete="new-password"
            value={form.confirmPassword}
            onChange={handleChange}
            onBlur={handleBlur}
            error={errors.confirmPassword}
            disabled={busy}
          />
        </div>

        <div className="mt-7 flex justify-end gap-2 border-t border-slate-200 pt-5">
          <Button type="button" variant="secondary" onClick={onClose} disabled={busy}>
            Cancel
          </Button>
          <Button type="submit" disabled={busy}>
            {busy ? 'Changing...' : 'Change password'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
