/*
 * File: ReservationRow.jsx
 * Purpose: How one booking appears in the Reservations list - as a table row
 *          above the "lg" breakpoint (ReservationRow) and as its own card
 *          below it (ReservationCard), the same split StationRow.jsx uses and
 *          for the same reason: a row of six columns squeezed sideways is hard
 *          to use on a phone, so this page gets a card purpose-built for touch
 *          instead of the generic label/value stacking the other tables fall
 *          back to. Both pull from the same reservation object and the same
 *          handlers, so Reservations.jsx renders whichever the viewport calls
 *          for without duplicating any logic.
 * Author:  <your name>
 * Created: 2026
 */
import { Button, RowActions, StatusPill, TD, TR } from './PageControls'
import { formatDateTime, modificationState } from '../utils/reservationRules'

/*
 * Status colours are a secondary cue only - every pill also spells the status
 * out, because approved-green and cancelled-red are nearly identical to a
 * reader with deuteranopia.
 */
const STATUS_TONES = {
  Pending: 'warning',
  Approved: 'active',
  Completed: 'inactive',
  Cancelled: 'danger',
}

/* One label-and-value pair in the card's stat grid. */
function CardStat({ label, value }) {
  return (
    <div>
      <span className="block text-[11px] font-medium uppercase tracking-[0.07em] text-slate-500">
        {label}
      </span>
      <span className="mt-0.5 block text-sm text-slate-900">{value}</span>
    </div>
  )
}

/* The Approve/Edit/Cancel buttons shared by the row and the card. */
function ReservationActions({ reservation, allowed, busy, onApprove, onCancel, onEdit }) {
  const isPending = reservation.status === 'Pending'

  return (
    <>
      {isPending ? (
        <Button size="sm" disabled={busy} onClick={() => onApprove(reservation)}>
          Approve
        </Button>
      ) : null}

      <Button
        variant="secondary"
        size="sm"
        disabled={busy || !allowed}
        onClick={() => onEdit(reservation)}
      >
        Edit
      </Button>

      <Button
        variant="danger"
        size="sm"
        disabled={busy || !allowed}
        onClick={() => onCancel(reservation)}
      >
        Cancel
      </Button>
    </>
  )
}

/* One row of the reservations table, shown at "lg" and above. */
export function ReservationRow({ reservation, stationName, busy, onApprove, onCancel, onEdit }) {
  const { allowed, reason } = modificationState(reservation)

  return (
    <TR>
      <TD label="Starts" className="text-slate-900">
        <span className="block font-medium tabular-nums">
          {formatDateTime(reservation.reservationStart)}
        </span>
        <span className="mt-0.5 block text-xs text-slate-400">{stationName}</span>
      </TD>
      <TD label="Prosumer" className="tabular-nums">{reservation.prosumerNic}</TD>
      <TD label="Type">{reservation.type === 'DropOff' ? 'Drop-off' : 'Charging'}</TD>
      <TD label="Energy" numeric>{reservation.energyKWh} kWh</TD>
      <TD label="Status">
        <StatusPill tone={STATUS_TONES[reservation.status]}>{reservation.status}</StatusPill>
      </TD>
      <TD>
        <div className="flex flex-col items-end gap-1">
          <RowActions>
            <ReservationActions
              reservation={reservation}
              allowed={allowed}
              busy={busy}
              onApprove={onApprove}
              onCancel={onCancel}
              onEdit={onEdit}
            />
          </RowActions>

          {/* Says why the actions are unavailable, rather than silently greying out. */}
          {allowed ? null : (
            <span className="max-w-xs text-right text-[11px] leading-snug text-slate-400">
              {reason}
            </span>
          )}
        </div>
      </TD>
    </TR>
  )
}

/* One booking, as its own card - shown below "lg" in place of the table. */
export function ReservationCard({ reservation, stationName, busy, onApprove, onCancel, onEdit }) {
  const { allowed, reason } = modificationState(reservation)

  return (
    <div className="border border-slate-200 bg-white p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-sm font-medium tabular-nums text-slate-900">
            {formatDateTime(reservation.reservationStart)}
          </p>
          <p className="mt-0.5 truncate text-xs text-slate-400">{stationName}</p>
        </div>
        <StatusPill tone={STATUS_TONES[reservation.status]}>{reservation.status}</StatusPill>
      </div>

      <div className="mt-3 grid grid-cols-2 gap-3 border-t border-slate-100 pt-3">
        <CardStat label="Prosumer" value={<span className="tabular-nums">{reservation.prosumerNic}</span>} />
        <CardStat label="Type" value={reservation.type === 'DropOff' ? 'Drop-off' : 'Charging'} />
        <CardStat label="Energy" value={`${reservation.energyKWh} kWh`} />
      </div>

      <div className="mt-3 border-t border-slate-100 pt-3">
        <div className="flex flex-wrap justify-end gap-2">
          <ReservationActions
            reservation={reservation}
            allowed={allowed}
            busy={busy}
            onApprove={onApprove}
            onCancel={onCancel}
            onEdit={onEdit}
          />
        </div>

        {/* Says why the actions are unavailable, rather than silently greying out. */}
        {allowed ? null : (
          <p className="mt-2 text-xs leading-relaxed text-slate-400">{reason}</p>
        )}
      </div>
    </div>
  )
}
