/*
 * File: StationRow.jsx
 * Purpose: How one microgrid node appears in the Stations list - as a table
 *          row above the "lg" breakpoint (StationRow) and as its own card
 *          below it (StationCard). A table row squeezed sideways is genuinely
 *          hard to use on a phone, so rather than lean on the generic
 *          label/value stacking every other table in the console falls back
 *          to, this page gets a card purpose-built for touch: a name and
 *          status up top, the key figures in a small grid, and the action
 *          buttons free to wrap onto their own line. Both pull from the same
 *          station object and the same handlers, so Stations.jsx renders
 *          whichever one the viewport calls for without duplicating any logic.
 * Author:  <your name>
 * Created: 2026
 */
import { Button, ButtonLink, RowActions, StatusPill, TD, TR } from './PageControls'

/* Formats a coordinate pair for display. */
function formatLocation(station) {
  return `${station.latitude.toFixed(4)}, ${station.longitude.toFixed(4)}`
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

/* The three action buttons shared by the row and the card. */
function StationActions({ station, canManage, busy, onRequestDeactivate, onActivate, onEdit, onDelete }) {
  return (
    <>
      <ButtonLink to={`/stations/${station.id}/slots`} variant="secondary" size="sm">
        Slots
      </ButtonLink>

      {canManage ? (
        <>
          <Button variant="secondary" size="sm" onClick={() => onEdit(station)}>
            Edit
          </Button>
          {/* Deactivating needs confirming; reactivating does not, since it is
              the reversible, low-risk direction. */}
          <Button
            variant={station.isActive ? 'danger' : 'secondary'}
            size="sm"
            disabled={busy}
            onClick={() =>
              station.isActive ? onRequestDeactivate(station) : onActivate(station)
            }
          >
            {station.isActive ? 'Deactivate' : 'Activate'}
          </Button>
          <Button variant="danger" size="sm" disabled={busy} onClick={() => onDelete(station)}>
            Delete
          </Button>
        </>
      ) : null}
    </>
  )
}

/* One row of the stations table, shown at "lg" and above. */
export function StationRow(props) {
  const { station } = props

  return (
    <TR>
      <TD label="Node" className="text-slate-900">
        <span className="block font-medium">{station.stationName}</span>
        <span className="mt-0.5 block text-xs tabular-nums text-slate-400">
          {formatLocation(station)}
        </span>
      </TD>
      <TD label="Capacity" numeric>{station.capacityKWh} kWh</TD>
      <TD label="Slots" numeric>{station.totalBatterySlots}</TD>
      <TD label="Schedule">
        {station.operatingSchedule || <span className="text-slate-300">Not set</span>}
      </TD>
      <TD label="Status">
        <StatusPill tone={station.isActive ? 'active' : 'inactive'}>
          {station.isActive ? 'Active' : 'Inactive'}
        </StatusPill>
      </TD>
      <TD>
        <RowActions>
          <StationActions {...props} />
        </RowActions>
      </TD>
    </TR>
  )
}

/* One node, as its own card - shown below "lg" in place of the table. */
export function StationCard(props) {
  const { station, canManage, busy, onRequestDeactivate, onActivate, onEdit, onDelete } = props

  return (
    <div className="border border-slate-200 bg-white p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate text-sm font-medium text-slate-900">{station.stationName}</p>
          <p className="mt-0.5 text-xs tabular-nums text-slate-400">{formatLocation(station)}</p>
        </div>
        <StatusPill tone={station.isActive ? 'active' : 'inactive'}>
          {station.isActive ? 'Active' : 'Inactive'}
        </StatusPill>
      </div>

      <div className="mt-3 grid grid-cols-2 gap-3 border-t border-slate-100 pt-3">
        <CardStat label="Capacity" value={`${station.capacityKWh} kWh`} />
        <CardStat label="Battery slots" value={station.totalBatterySlots} />
        <CardStat
          label="Schedule"
          wide
          value={station.operatingSchedule || <span className="text-slate-300">Not set</span>}
        />
      </div>

      <div className="mt-3 flex flex-wrap justify-end gap-2 border-t border-slate-100 pt-3">
        <StationActions
          station={station}
          canManage={canManage}
          busy={busy}
          onRequestDeactivate={onRequestDeactivate}
          onActivate={onActivate}
          onEdit={onEdit}
          onDelete={onDelete}
        />
      </div>
    </div>
  )
}
