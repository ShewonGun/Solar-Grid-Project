/*
 * File: FormControls.jsx
 * Purpose: Small presentational form building blocks used on the Login page -
 *          a labelled text field, a password field with a show/hide toggle, a
 *          status banner for API messages and a submit button with a busy
 *          state. All of them use the tight corner radius and flat borders
 *          that define the console's minimal look.
 * Author:  <your name>
 * Created: 2026
 */
import { useId, useState } from 'react'

import { IconSpinner } from './Icons'

const BASE_INPUT_CLASSES =
  'w-full rounded-xs border bg-white px-3 py-2.5 text-sm text-slate-900 outline-none transition-colors placeholder:text-slate-400 focus:ring-1 disabled:bg-slate-50 disabled:text-slate-400'

const VALID_INPUT_CLASSES =
  'border-slate-300 hover:border-slate-400 focus:border-slate-900 focus:ring-slate-900'

const INVALID_INPUT_CLASSES = 'border-red-500 focus:border-red-600 focus:ring-red-600'

const LABEL_CLASSES = 'text-xs font-medium uppercase tracking-[0.08em] text-slate-600'

/*
 * Builds the class list for an input, switching to the error palette when the
 * field has a message to show.
 */
function inputClasses(hasError, extra = '') {
  return `${BASE_INPUT_CLASSES} ${hasError ? INVALID_INPUT_CLASSES : VALID_INPUT_CLASSES} ${extra}`
}

/*
 * Renders the validation message for a field, or its hint when there is no
 * error. Returns null when the field has neither.
 */
function FieldMessage({ id, error, hint }) {
  if (error) {
    return (
      <p id={`${id}-error`} className="mt-1.5 text-xs text-red-600">
        {error}
      </p>
    )
  }

  if (hint) {
    return (
      <p id={`${id}-hint`} className="mt-1.5 text-xs leading-relaxed text-slate-400">
        {hint}
      </p>
    )
  }

  return null
}

/*
 * A labelled input with inline validation text. `error` is shown once the field
 * has been left or the form submitted, so the user is not corrected while still
 * typing the first character.
 */
export function TextField({
  label,
  error,
  hint,
  optional = false,
  className = '',
  ...inputProps
}) {
  const id = useId()
  const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined

  return (
    <div className={className}>
      <label htmlFor={id} className="mb-1.5 flex items-baseline justify-between gap-3">
        <span className={LABEL_CLASSES}>{label}</span>
        {optional ? (
          <span className="text-xs font-normal normal-case text-slate-400">Optional</span>
        ) : null}
      </label>

      <input
        id={id}
        className={inputClasses(Boolean(error))}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={describedBy}
        {...inputProps}
      />

      <FieldMessage id={id} error={error} hint={hint} />
    </div>
  )
}

/*
 * A password input with a show/hide toggle. The toggle is local state because
 * nothing outside this control needs to know whether the value is visible.
 */
export function PasswordField({ label, error, hint, className = '', ...inputProps }) {
  const id = useId()
  const [visible, setVisible] = useState(false)
  const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined

  return (
    <div className={className}>
      <label htmlFor={id} className={`mb-1.5 block ${LABEL_CLASSES}`}>
        {label}
      </label>

      <div className="relative">
        <input
          id={id}
          type={visible ? 'text' : 'password'}
          className={inputClasses(Boolean(error), 'pr-14')}
          aria-invalid={error ? 'true' : undefined}
          aria-describedby={describedBy}
          {...inputProps}
        />
        <button
          type="button"
          onClick={() => setVisible((current) => !current)}
          className="absolute inset-y-0 right-0 px-3 text-xs font-medium text-slate-400 transition-colors hover:text-slate-900"
          aria-label={visible ? 'Hide password' : 'Show password'}
        >
          {visible ? 'Hide' : 'Show'}
        </button>
      </div>

      <FieldMessage id={id} error={error} hint={hint} />
    </div>
  )
}

const BANNER_STYLES = {
  error: 'border-red-500 bg-red-50 text-red-700',
  success: 'border-emerald-600 bg-emerald-50 text-emerald-800',
  info: 'border-slate-900 bg-slate-50 text-slate-700',
}

/*
 * Banner for a message that came back from the Web API. It is announced to
 * screen readers so a failed sign-in is not silent for keyboard users.
 */
export function StatusBanner({ tone = 'error', children }) {
  if (!children) {
    return null
  }

  return (
    <div
      role={tone === 'error' ? 'alert' : 'status'}
      className={`rounded-xs border-l-2 px-3.5 py-3 text-sm leading-relaxed ${BANNER_STYLES[tone]}`}
    >
      {children}
    </div>
  )
}

/* Full-width submit button that shows a spinner while the request is in flight. */
export function SubmitButton({ busy = false, busyLabel = 'Please wait...', children }) {
  return (
    <button
      type="submit"
      disabled={busy}
      className="flex w-full items-center justify-center gap-2 rounded-xs bg-slate-900 px-4 py-2.5 text-sm font-medium tracking-tight text-white transition-colors hover:bg-slate-700 focus:outline-none focus:ring-1 focus:ring-slate-900 focus:ring-offset-2 disabled:cursor-not-allowed disabled:bg-slate-400"
    >
      {busy ? (
        <>
          <IconSpinner className="h-3.5 w-3.5 animate-spin" />
          {busyLabel}
        </>
      ) : (
        children
      )}
    </button>
  )
}
