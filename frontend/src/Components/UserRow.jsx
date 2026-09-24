/*
 * File: UserRow.jsx
 * Purpose: How one account appears in the Users list - as a table row above
 *          the "lg" breakpoint (UserRow) and as its own card below it
 *          (UserCard), the same split StationRow.jsx and ReservationRow.jsx
 *          use and for the same reason: a card purpose-built for touch reads
 *          better on a phone than the generic label/value stacking every other
 *          table falls back to. Both pull from the same user object and the
 *          same handlers, so Users.jsx renders whichever the viewport calls
 *          for without duplicating any logic.
 * Author:  <your name>
 * Created: 2026
 */
import { Button, RowActions, StatusPill, TD, TR } from './PageControls'

/*
 * Status colours are a secondary cue only - every pill also spells the status
 * out, because several of these are hard to tell apart for a reader with
 * red-green colour blindness.
 */
const STATUS_TONES = {
  Active: 'active',
  PendingActivation: 'warning',
  DeactivationRequested: 'warning',
  Deactivated: 'danger',
}

const STATUS_LABELS = {
  Active: 'Active',
  PendingActivation: 'Pending activation',
  DeactivationRequested: 'Deactivation requested',
  Deactivated: 'Deactivated',
}

const ROLE_LABELS = {
  Backoffice: 'Back-office officer',
  GridOperator: 'Grid operator',
  Prosumer: 'Solar prosumer',
}

/* One label-and-value pair in the card's stat grid. `wide` spans both columns. */
function CardStat({ label, value, wide = false }) {
  return (
    <div className={wide ? 'col-span-2' : ''}>
      <span className="block text-[11px] font-medium uppercase tracking-[0.07em] text-slate-500">
        {label}
      </span>
      <span className="mt-0.5 block text-sm text-slate-900">{value}</span>
    </div>
  )
}

/* The Edit/Activate/Deactivate buttons shared by the row and the card. */
function UserActions({ user, canActivate, canDeactivate, busy, onEdit, onActivate, onDeactivate }) {
  return (
    <>
      <Button variant="secondary" size="sm" onClick={() => onEdit(user)}>
        Edit
      </Button>

      {canActivate ? (
        <Button size="sm" disabled={busy} onClick={() => onActivate(user)}>
          Activate
        </Button>
      ) : null}

      {canDeactivate ? (
        <Button variant="danger" size="sm" disabled={busy} onClick={() => onDeactivate(user)}>
          Deactivate
        </Button>
      ) : null}
    </>
  )
}

/* One row of the users table, shown at "lg" and above. */
export function UserRow({ user, isSelf, busy, onEdit, onActivate, onDeactivate }) {
  const canActivate = user.status !== 'Active'
  // The API refuses to let an officer deactivate their own account.
  const canDeactivate = user.status !== 'Deactivated' && !isSelf

  return (
    <TR>
      <TD label="Name" className="text-slate-900">
        <span className="block font-medium">{user.fullName}</span>
        <span className="mt-0.5 block text-xs tabular-nums text-slate-400">NIC {user.nic}</span>
      </TD>
      <TD label="Contact">
        <span className="block">{user.email}</span>
        {user.phone ? (
          <span className="mt-0.5 block text-xs tabular-nums text-slate-400">{user.phone}</span>
        ) : null}
      </TD>
      <TD label="Role">{ROLE_LABELS[user.role]}</TD>
      <TD label="Status">
        <StatusPill tone={STATUS_TONES[user.status]}>{STATUS_LABELS[user.status]}</StatusPill>
      </TD>
      <TD>
        <div className="flex flex-col items-end gap-1">
          <RowActions>
            <UserActions
              user={user}
              canActivate={canActivate}
              canDeactivate={canDeactivate}
              busy={busy}
              onEdit={onEdit}
              onActivate={onActivate}
              onDeactivate={onDeactivate}
            />
          </RowActions>

          {isSelf ? (
            <span className="text-[11px] text-slate-400">Your own account</span>
          ) : null}
        </div>
      </TD>
    </TR>
  )
}

/* One account, as its own card - shown below "lg" in place of the table. */
export function UserCard({ user, isSelf, busy, onEdit, onActivate, onDeactivate }) {
  const canActivate = user.status !== 'Active'
  const canDeactivate = user.status !== 'Deactivated' && !isSelf

  return (
    <div className="border border-slate-200 bg-white p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate text-sm font-medium text-slate-900">{user.fullName}</p>
          <p className="mt-0.5 text-xs tabular-nums text-slate-400">NIC {user.nic}</p>
        </div>
        <StatusPill tone={STATUS_TONES[user.status]}>{STATUS_LABELS[user.status]}</StatusPill>
      </div>

      <div className="mt-3 grid grid-cols-2 gap-3 border-t border-slate-100 pt-3">
        <CardStat label="Role" value={ROLE_LABELS[user.role]} />
        <CardStat
          label="Contact"
          value={
            <>
              <span className="block truncate">{user.email}</span>
              {user.phone ? (
                <span className="block tabular-nums text-slate-500">{user.phone}</span>
              ) : null}
            </>
          }
        />
      </div>

      <div className="mt-3 border-t border-slate-100 pt-3">
        <div className="flex flex-wrap justify-end gap-2">
          <UserActions
            user={user}
            canActivate={canActivate}
            canDeactivate={canDeactivate}
            busy={busy}
            onEdit={onEdit}
            onActivate={onActivate}
            onDeactivate={onDeactivate}
          />
        </div>

        {isSelf ? <p className="mt-2 text-xs text-slate-400">Your own account</p> : null}
      </div>
    </div>
  )
}
