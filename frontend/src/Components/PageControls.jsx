/*
 * File: PageControls.jsx
 * Purpose: The console's shared interface primitives - page headers, panels,
 *          data-table styling, buttons, status pills, toolbars, skeletons,
 *          banners and dialogs. Every screen is assembled from these, which is
 *          what keeps spacing, density and type consistent across the console
 *          instead of each page inventing its own.
 * Author:  <your name>
 * Created: 2026
 */
import { useEffect } from 'react'
import { Link } from 'react-router-dom'

import { IconClose, IconInbox } from './Icons'

/* ------------------------------------------------------------------ layout */

/*
 * Heading block for a screen. The title carries the page, the description sits
 * under it, and actions align to the right on one baseline.
 */
export function PageHeader({ title, description, children }) {
  return (
    <header className="mb-6 flex flex-wrap items-start justify-between gap-4">
      <div className="min-w-0">
        <h1 className="text-xl font-semibold tracking-tight text-slate-900">{title}</h1>
        {description ? (
          <p className="mt-1 max-w-2xl text-sm leading-relaxed text-slate-500">{description}</p>
        ) : null}
      </div>
      {children ? (
        <div className="flex w-full flex-wrap shrink-0 items-center gap-2 sm:w-auto">
          {children}
        </div>
      ) : null}
    </header>
  )
}

/*
 * A bordered surface. `title` adds a header strip with optional `actions`;
 * `flush` removes the body padding, which is what a table needs so its rows
 * meet the panel edge.
 */
export function Panel({ title, meta, actions, flush = false, className = '', children }) {
  return (
    <section
      className={`border border-slate-200 bg-white shadow-[0_1px_2px_rgba(15,23,42,0.04)] ${className}`}
    >
      {title ? (
        <header className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 px-4 py-3">
          <div className="flex items-baseline gap-2.5">
            <h2 className="text-sm font-semibold tracking-tight text-slate-900">{title}</h2>
            {meta ? <span className="text-xs text-slate-400">{meta}</span> : null}
          </div>
          {actions ? <div className="flex items-center gap-2">{actions}</div> : null}
        </header>
      ) : null}
      <div className={flush ? '' : 'p-4'}>{children}</div>
    </section>
  )
}

/* Strip above a table for search and filter controls. */
export function Toolbar({ children, onSubmit }) {
  const content = (
    <div className="flex flex-col items-stretch gap-3 border-b border-slate-200 bg-slate-50/70 px-4 py-3 sm:flex-row sm:flex-wrap sm:items-end">
      {children}
    </div>
  )

  return onSubmit ? <form onSubmit={onSubmit}>{content}</form> : content
}

/* ------------------------------------------------------------- form fields */

const FIELD_CLASSES =
  'h-9 w-full rounded-xs border border-slate-300 bg-white px-2.5 text-sm text-slate-900 outline-none transition-colors placeholder:text-slate-400 hover:border-slate-400 focus:border-slate-900 focus:ring-1 focus:ring-slate-900 disabled:bg-slate-50 disabled:text-slate-400'

/*
 * Compact labelled control for a toolbar - renders a select or an input, with
 * an optional leading icon. The icon is positioned against the control itself
 * rather than the whole field, so it stays centred whatever the label does.
 */
export function FilterField({
  label,
  as = 'input',
  icon: FieldIcon,
  className = '',
  children,
  ...props
}) {
  return (
    <label className={`flex w-full flex-col gap-1 sm:w-auto ${className}`}>
      <span className="text-[11px] font-medium uppercase tracking-[0.07em] text-slate-500">
        {label}
      </span>

      <span className="relative block">
        {FieldIcon ? (
          <FieldIcon className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
        ) : null}

        {as === 'select' ? (
          <select className={FIELD_CLASSES} {...props}>
            {children}
          </select>
        ) : (
          <input className={`${FIELD_CLASSES} ${FieldIcon ? 'pl-8' : ''}`} {...props} />
        )}
      </span>
    </label>
  )
}

/* ------------------------------------------------------------------ tables */

/*
 * Scroll container that keeps a wide table inside its panel on a screen wide
 * enough to show one - below the "responsive-table" breakpoint in index.css,
 * the table becomes a stack of cards instead, and `minWidth` stops applying.
 */
export function TableWrap({ minWidth = '60rem', children }) {
  return (
    <div className="overflow-x-auto">
      <table className="responsive-table w-full text-left text-sm" style={{ minWidth }}>
        {children}
      </table>
    </div>
  )
}

/* Column heading. `align` right-aligns numeric and action columns. */
export function TH({ align = 'left', className = '', children }) {
  return (
    <th
      scope="col"
      className={`whitespace-nowrap border-b border-slate-200 bg-slate-50 px-4 py-2.5 text-[11px] font-semibold uppercase tracking-[0.07em] text-slate-500 ${
        align === 'right' ? 'text-right' : 'text-left'
      } ${className}`}
    >
      {children}
    </th>
  )
}

/*
 * Table cell. `numeric` right-aligns the value and switches on tabular figures
 * so a column of numbers lines up digit for digit. `label` names the column
 * for the card layout a phone-width screen switches to (index.css reads it
 * back off the cell as data-label); leave it out on a cell that needs no
 * caption there, such as a row of action buttons.
 */
export function TD({ numeric = false, label, className = '', children, ...props }) {
  return (
    <td
      data-label={label}
      className={`border-b border-slate-100 px-4 py-2.5 align-middle text-slate-600 ${
        numeric ? 'text-right tabular-nums' : ''
      } ${className}`}
      {...props}
    >
      {children}
    </td>
  )
}

/* Table row with a hover state, so the eye can track across wide rows. */
export function TR({ className = '', children }) {
  return (
    <tr className={`transition-colors hover:bg-slate-50/80 ${className}`}>{children}</tr>
  )
}

/* Groups the action buttons at the end of a row. */
export function RowActions({ children }) {
  return <div className="flex items-center justify-end gap-1.5">{children}</div>
}

/* ----------------------------------------------------------------- buttons */

const BUTTON_VARIANTS = {
  primary:
    'border border-slate-800 bg-slate-800 text-white hover:border-slate-700 hover:bg-slate-700 focus-visible:ring-slate-900 disabled:border-slate-300 disabled:bg-slate-300',
  secondary:
    'border border-slate-300 bg-white text-slate-700 hover:border-slate-400 hover:bg-slate-50 hover:text-slate-900 focus-visible:ring-slate-400 disabled:text-slate-300',
  danger:
    'border border-slate-300 bg-white text-red-700 hover:border-red-400 hover:bg-red-50 focus-visible:ring-red-400 disabled:border-slate-200 disabled:text-red-300',
  ghost:
    'border border-transparent bg-transparent text-slate-500 hover:bg-slate-100 hover:text-slate-900 focus-visible:ring-slate-400 disabled:text-slate-300',
}

const BUTTON_SIZES = {
  sm: 'h-7 gap-1.5 px-2.5 text-xs',
  md: 'h-9 gap-2 px-3 text-sm',
}

const BUTTON_BASE =
  'inline-flex shrink-0 items-center justify-center rounded-xs font-medium transition-colors focus:outline-none focus-visible:ring-1 focus-visible:ring-offset-1 disabled:cursor-not-allowed'

/* Button for actions on the signed-in screens. */
export function Button({
  variant = 'primary',
  size = 'md',
  type = 'button',
  className = '',
  ...props
}) {
  return (
    <button
      type={type}
      className={`${BUTTON_BASE} ${BUTTON_SIZES[size]} ${BUTTON_VARIANTS[variant]} ${className}`}
      {...props}
    />
  )
}

/* Link styled as a button, for actions that navigate rather than submit. */
export function ButtonLink({ variant = 'primary', size = 'md', className = '', ...props }) {
  return (
    <Link
      className={`${BUTTON_BASE} ${BUTTON_SIZES[size]} ${BUTTON_VARIANTS[variant]} ${className}`}
      {...props}
    />
  )
}

/* ------------------------------------------------------------------ status */

const PILL_STYLES = {
  active: 'border-emerald-200 bg-emerald-50 text-emerald-800',
  inactive: 'border-slate-200 bg-slate-50 text-slate-600',
  warning: 'border-amber-200 bg-amber-50 text-amber-800',
  danger: 'border-red-200 bg-red-50 text-red-800',
}

const PILL_DOTS = {
  active: 'bg-emerald-600',
  inactive: 'bg-slate-400',
  warning: 'bg-amber-500',
  danger: 'bg-red-600',
}

/*
 * Status pill. The dot is a secondary cue only - the label always spells the
 * status out, because several of these colours are hard to tell apart for a
 * reader with red-green colour blindness.
 */
export function StatusPill({ tone = 'inactive', children }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 whitespace-nowrap rounded-xs border px-2 py-0.5 text-xs font-medium ${PILL_STYLES[tone]}`}
    >
      <span className={`h-1.5 w-1.5 shrink-0 ${PILL_DOTS[tone]}`} aria-hidden="true" />
      {children}
    </span>
  )
}

/* ------------------------------------------------------------ placeholders */

/* Grey block standing in for content that is still loading. */
export function Skeleton({ className = '' }) {
  return <div className={`animate-pulse bg-slate-100 ${className}`} aria-hidden="true" />
}

/*
 * Placeholder shown while a screen's first request is in flight. It mimics the
 * shape of a table so the layout does not jump when the rows arrive.
 */
export function LoadingState({ label = 'Loading...' }) {
  return (
    <div
      className="border border-slate-200 bg-white p-4 shadow-[0_1px_2px_rgba(15,23,42,0.04)]"
      role="status"
      aria-label={label}
    >
      <Skeleton className="h-8 w-full" />
      <div className="mt-2 space-y-2">
        {[0, 1, 2, 3, 4].map((row) => (
          <Skeleton key={row} className="h-11 w-full" />
        ))}
      </div>
    </div>
  )
}

/* Placeholder shown when a list came back with nothing in it. */
export function EmptyState({ title, description, children }) {
  return (
    <div className="flex flex-col items-center justify-center px-5 py-14 text-center">
      <span className="mb-3 grid h-10 w-10 place-items-center rounded-xs border border-slate-200 bg-slate-50 text-slate-400">
        <IconInbox className="h-5 w-5" />
      </span>
      <p className="text-sm font-medium text-slate-800">{title}</p>
      {description ? (
        <p className="mt-1 max-w-sm text-sm leading-relaxed text-slate-400">{description}</p>
      ) : null}
      {children ? <div className="mt-5 flex justify-center">{children}</div> : null}
    </div>
  )
}

/* ------------------------------------------------------- messages & dialogs */

const BANNER_STYLES = {
  error: 'border-red-200 bg-red-50 text-red-800',
  success: 'border-emerald-200 bg-emerald-50 text-emerald-900',
  info: 'border-slate-200 bg-slate-50 text-slate-700',
}

const BANNER_ACCENTS = {
  error: 'bg-red-600',
  success: 'bg-emerald-600',
  info: 'bg-slate-500',
}

/*
 * Banner for a message returned by the Web API. Errors are announced to screen
 * readers so a failed action is not silent.
 */
export function Banner({ tone = 'error', children }) {
  if (!children) {
    return null
  }

  return (
    <div
      role={tone === 'error' ? 'alert' : 'status'}
      className={`mb-4 flex overflow-hidden rounded-xs border ${BANNER_STYLES[tone]}`}
    >
      <span className={`w-0.5 shrink-0 ${BANNER_ACCENTS[tone]}`} aria-hidden="true" />
      <p className="px-3.5 py-2.5 text-sm leading-relaxed">{children}</p>
    </div>
  )
}

/** Panel widths a dialog can take. */
const MODAL_SIZES = {
  md: 'max-w-md',
  lg: 'max-w-2xl',
}

/*
 * Modal dialog for an action that needs confirming or a short form. Closes on
 * Escape and on a click outside the panel, so it never traps the user on a
 * screen they did not mean to open. The body scrolls internally, so a tall form
 * stays usable on a small display.
 */
export function Modal({ title, description, size = 'md', onClose, children }) {
  useEffect(() => {
    /* Closes the dialog when Escape is pressed. */
    function handleKeyDown(event) {
      if (event.key === 'Escape') {
        onClose()
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose])

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label={title}
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 px-5 py-10 backdrop-blur-[1px]"
      onMouseDown={(event) => {
        // Only a click on the backdrop itself closes the dialog.
        if (event.target === event.currentTarget) {
          onClose()
        }
      }}
    >
      <div
        className={`flex max-h-full w-full flex-col border border-slate-300 bg-white shadow-xl ${MODAL_SIZES[size]}`}
      >
        <header className="flex items-start justify-between gap-4 border-b border-slate-200 px-5 py-4">
          <div>
            <h2 className="text-base font-semibold tracking-tight text-slate-900">{title}</h2>
            {description ? (
              <p className="mt-1 text-sm leading-relaxed text-slate-500">{description}</p>
            ) : null}
          </div>
          <Button variant="ghost" size="sm" onClick={onClose} aria-label="Close dialog">
            <IconClose className="h-4 w-4" />
          </Button>
        </header>
        <div className="overflow-y-auto px-5 py-5">{children}</div>
      </div>
    </div>
  )
}
