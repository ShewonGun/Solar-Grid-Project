/*
 * File: SlotRow.jsx
 * Purpose: How one battery slot appears in a node's slot list - as a table row
 *          above the "lg" breakpoint (SlotRow) and as its own card below it
 *          (SlotCard), the same split StationRow.jsx and the others use. Both
 *          pull from the same slot object and the same handlers, so
 *          StationSlots.jsx renders whichever the viewport calls for without
 *          duplicating any logic.
 * Author:  <your name>
 * Created: 2026
 */
import { Button, RowActions, StatusPill, TD, TR } from './PageControls'
import { formatDateTime, formatTimeRange } from '../utils/reservationRules'

/*
 * Status colours are a secondary cue only - every pill also spells the status
 * out, so neither view depends on telling two colours apart.
 */
const STATUS_TONES = {
  Available: 'active',
  Reserved: 'warning',
  Unavailable: 'inactive',
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

/* The Edit/Hold-or-Release/Delete buttons shared by the row and the card. */
function SlotActions({ slot, isReserved, busy, onEdit, onRequestHold, onRelease, onDelete }) {
  const isAvailable = slot.status === 'Available'

  return (
    <>
      <Button
        variant="secondary"
        size="sm"
        disabled={busy || isReserved}
        onClick={() => onEdit(slot)}
      >
        Edit
      </Button>

      {/* Holding a slot needs confirming; releasing one back for booking does
          not, since it is the reversible, low-risk direction. */}
      <Button
        variant="secondary"
        size="sm"
        disabled={busy || isReserved}
        onClick={() => (isAvailable ? onRequestHold(slot) : onRelease(slot))}
      >
        {isAvailable ? 'Hold' : 'Release'}
      </Button>

      <Button variant="danger" size="sm" disabled={busy || isReserved} onClick={() => onDelete(slot)}>
        Delete
      </Button>
    </>
  )
}

/* One row of the battery slot table, shown at "lg" and above. */
export function SlotRow({ slot, busy, onEdit, onRequestHold, onRelease, onDelete }) {
  // A reserved slot is locked by the service until its booking is cancelled.
  const isReserved = slot.status === 'Reserved'

  return (
    <TR>
      <TD label="Battery" numeric className="font-medium text-slate-900">
        {slot.batterySlotNumber}
      </TD>
      <TD label="Window" className="text-slate-900">
        <span className="block font-medium tabular-nums">{formatDateTime(slot.startTime)}</span>
        <span className="mt-0.5 block text-xs tabular-nums text-slate-400">
          {formatTimeRange(slot.startTime, slot.endTime)}
        </span>
      </TD>
      <TD label="Capacity" numeric>{slot.capacityKWh} kWh</TD>
      <TD label="Status">
        <StatusPill tone={STATUS_TONES[slot.status]}>{slot.status}</StatusPill>
      </TD>
      <TD label="Updated by" className="tabular-nums">
        {slot.updatedBy || <span className="text-slate-300">-</span>}
      </TD>
      <TD>
        <div className="flex flex-col items-end gap-1">
          <RowActions>
            <SlotActions
              slot={slot}
              isReserved={isReserved}
              busy={busy}
              onEdit={onEdit}
              onRequestHold={onRequestHold}
              onRelease={onRelease}
              onDelete={onDelete}
            />
          </RowActions>

          {isReserved ? (
            <span className="text-[11px] leading-snug text-slate-400">
              Reserved - cancel the booking first
            </span>
          ) : null}
        </div>
      </TD>
    </TR>
  )
}

/* One battery slot, as its own card - shown below "lg" in place of the table. */
export function SlotCard({ slot, busy, onEdit, onRequestHold, onRelease, onDelete }) {
  const isReserved = slot.status === 'Reserved'

  return (
    <div className="border border-slate-200 bg-white p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-sm font-medium text-slate-900">Battery {slot.batterySlotNumber}</p>
          <p className="mt-0.5 text-xs tabular-nums text-slate-400">
            {formatDateTime(slot.startTime)}
          </p>
        </div>
        <StatusPill tone={STATUS_TONES[slot.status]}>{slot.status}</StatusPill>
      </div>

      <div className="mt-3 grid grid-cols-2 gap-3 border-t border-slate-100 pt-3">
        <CardStat label="Window" wide value={formatTimeRange(slot.startTime, slot.endTime)} />
        <CardStat label="Capacity" value={`${slot.capacityKWh} kWh`} />
        <CardStat
          label="Updated by"
          value={slot.updatedBy || <span className="text-slate-300">-</span>}
        />
      </div>

      <div className="mt-3 border-t border-slate-100 pt-3">
        <div className="flex flex-wrap justify-end gap-2">
          <SlotActions
            slot={slot}
            isReserved={isReserved}
            busy={busy}
            onEdit={onEdit}
            onRequestHold={onRequestHold}
            onRelease={onRelease}
            onDelete={onDelete}
          />
        </div>

        {isReserved ? (
          <p className="mt-2 text-xs leading-relaxed text-slate-400">
            Reserved - cancel the booking first
          </p>
        ) : null}
      </div>
    </div>
  )
}
