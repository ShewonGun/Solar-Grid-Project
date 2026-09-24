/*
 * File: PendingRow.jsx
 * Purpose: How one waiting account appears in the Pending activations queue -
 *          as a table row above the "lg" breakpoint (PendingRow) and as its
 *          own card below it (PendingCard), the same split StationRow.jsx and
 *          the others use. Both pull from the same account object and the same
 *          onActivate handler, so PendingActivations.jsx renders whichever the
 *          viewport calls for without duplicating any logic.
 * Author:  <your name>
 * Created: 2026
 */
import { Button, RowActions, StatusPill, TD, TR } from './PageControls'

/* Formats the date an account registered. */
function formatDate(value) {
  return new Date(value).toLocaleDateString(undefined, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
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

/* The declared solar array size, or a muted placeholder when none was given. */
function solarArrayText(account) {
  return account.solarCapacityKW === null || account.solarCapacityKW === undefined
    ? <span className="text-slate-300">Not given</span>
    : `${account.solarCapacityKW} kW`
}

/* One row of the pending-activation queue, shown at "lg" and above. */
export function PendingRow({ account, busy, onActivate }) {
  return (
    <TR>
      <TD label="Prosumer" className="text-slate-900">
        <span className="block font-medium">{account.fullName}</span>
        <span className="mt-0.5 block text-xs tabular-nums text-slate-400">
          NIC {account.nic}
        </span>
      </TD>
      <TD label="Contact">
        <span className="block">{account.email}</span>
        {account.phone ? (
          <span className="mt-0.5 block text-xs tabular-nums text-slate-400">
            {account.phone}
          </span>
        ) : null}
      </TD>
      <TD label="Solar array" numeric>{solarArrayText(account)}</TD>
      <TD label="Registered" className="tabular-nums">{formatDate(account.createdAt)}</TD>
      <TD label="Status">
        <StatusPill tone="warning">Pending activation</StatusPill>
      </TD>
      <TD>
        <RowActions>
          <Button size="sm" disabled={busy} onClick={() => onActivate(account)}>
            {busy ? 'Activating...' : 'Activate'}
          </Button>
        </RowActions>
      </TD>
    </TR>
  )
}

/* One waiting account, as its own card - shown below "lg" in place of the table. */
export function PendingCard({ account, busy, onActivate }) {
  return (
    <div className="border border-slate-200 bg-white p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate text-sm font-medium text-slate-900">{account.fullName}</p>
          <p className="mt-0.5 text-xs tabular-nums text-slate-400">NIC {account.nic}</p>
        </div>
        <StatusPill tone="warning">Pending</StatusPill>
      </div>

      <div className="mt-3 grid grid-cols-2 gap-3 border-t border-slate-100 pt-3">
        <CardStat
          label="Contact"
          wide
          value={
            <>
              <span className="block truncate">{account.email}</span>
              {account.phone ? (
                <span className="block tabular-nums text-slate-500">{account.phone}</span>
              ) : null}
            </>
          }
        />
        <CardStat label="Solar array" value={solarArrayText(account)} />
        <CardStat label="Registered" value={formatDate(account.createdAt)} />
      </div>

      <div className="mt-3 flex justify-end border-t border-slate-100 pt-3">
        <Button size="sm" disabled={busy} onClick={() => onActivate(account)}>
          {busy ? 'Activating...' : 'Activate'}
        </Button>
      </div>
    </div>
  )
}
