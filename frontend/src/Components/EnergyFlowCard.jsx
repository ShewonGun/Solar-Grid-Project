/*
 * File: EnergyFlowCard.jsx
 * Purpose: Shows how much energy is moving through the network each day, split
 *          by direction - prosumers dropping off surplus versus drawing power
 *          to charge. A booking's kWh only counts once it is Approved or
 *          Completed; a Pending request is not yet real, and a Cancelled one
 *          never happened, so counting either would overstate actual flow.
 *          The two directions are two shades of the same hue rather than a
 *          second colour, since this console reserves amber for status and
 *          brand accents elsewhere and a plain lightness split needs no
 *          colour-blindness check to stay readable.
 * Author:  <your name>
 * Created: 2026
 */
import { useMemo } from 'react'

import { Panel } from './PageControls'

/** Only these statuses represent energy that has moved or will move. */
const COUNTED_STATUSES = ['Approved', 'Completed']

/** Height of the chart's plot area, in pixels. */
const PLOT_HEIGHT = 132

/*
 * Buckets reservations into one entry per day between `start` and `end`
 * (inclusive), summing kWh by direction. Days outside the counted statuses or
 * outside the range are simply not represented in any bucket.
 */
function bucketByDayAndType(reservations, start, end) {
  const days = []
  const cursor = new Date(start)
  cursor.setHours(0, 0, 0, 0)
  const endOfRange = new Date(end)
  endOfRange.setHours(0, 0, 0, 0)

  while (cursor.getTime() <= endOfRange.getTime()) {
    days.push({
      iso: cursor.toISOString(),
      date: new Date(cursor),
      label: cursor.toLocaleDateString(undefined, { day: '2-digit', month: 'short' }),
      dropOffKWh: 0,
      chargingKWh: 0,
    })
    cursor.setDate(cursor.getDate() + 1)
  }

  reservations
    .filter((reservation) => COUNTED_STATUSES.includes(reservation.status))
    .forEach((reservation) => {
      const day = new Date(reservation.reservationStart)
      day.setHours(0, 0, 0, 0)

      const bucket = days.find((entry) => entry.date.getTime() === day.getTime())

      if (!bucket) {
        return
      }

      if (reservation.type === 'DropOff') {
        bucket.dropOffKWh += reservation.energyKWh
      } else {
        bucket.chargingKWh += reservation.energyKWh
      }
    })

  return days
}

/*
 * `reservations` should cover a wider window than the dashboard's 7-day
 * booking list - past days need Completed bookings to show real history, not
 * just what is still upcoming. `start`/`end` bound the days drawn.
 */
export default function EnergyFlowCard({ reservations, start, end }) {
  const days = useMemo(() => bucketByDayAndType(reservations, start, end), [reservations, start, end])

  const totals = days.reduce(
    (sum, day) => ({
      dropOff: sum.dropOff + day.dropOffKWh,
      charging: sum.charging + day.chargingKWh,
    }),
    { dropOff: 0, charging: 0 },
  )

  const peak = Math.max(1, ...days.map((day) => day.dropOffKWh + day.chargingKWh))

  return (
    <Panel title="Energy flow">
      <div className="mb-4 flex flex-wrap items-center gap-4 text-xs text-slate-500">
        <span className="flex items-center gap-1.5">
          <span className="h-2 w-2 shrink-0 rounded-xs bg-slate-700" aria-hidden="true" />
          Drop-off &middot;{' '}
          <span className="font-medium tabular-nums text-slate-700">
            {totals.dropOff.toFixed(1)} kWh
          </span>
        </span>
        <span className="flex items-center gap-1.5">
          <span className="h-2 w-2 shrink-0 rounded-xs bg-slate-300" aria-hidden="true" />
          Charging &middot;{' '}
          <span className="font-medium tabular-nums text-slate-700">
            {totals.charging.toFixed(1)} kWh
          </span>
        </span>
        <span className="ml-auto text-slate-400">Approved and completed bookings only</span>
      </div>

      {totals.dropOff + totals.charging === 0 ? (
        <p className="py-10 text-center text-sm text-slate-400">
          No approved or completed energy transfers in this window yet.
        </p>
      ) : (
        <>
          <div className="flex items-end gap-1" style={{ height: `${PLOT_HEIGHT}px` }}>
            {days.map((day) => {
              const dayTotal = day.dropOffKWh + day.chargingKWh
              const dropOffHeight = (day.dropOffKWh / peak) * PLOT_HEIGHT
              const chargingHeight = (day.chargingKWh / peak) * PLOT_HEIGHT

              return (
                <div
                  key={day.iso}
                  className="flex flex-1 flex-col items-center justify-end"
                  title={`${day.label}: ${day.dropOffKWh.toFixed(1)} kWh drop-off, ${day.chargingKWh.toFixed(1)} kWh charging`}
                >
                  <div
                    className={`w-full ${dayTotal > 0 ? '' : 'bg-slate-100'}`}
                    style={{ height: `${dayTotal > 0 ? Math.max(dropOffHeight + chargingHeight, 2) : 2}px` }}
                  >
                    {dayTotal > 0 ? (
                      <div className="flex h-full w-full flex-col justify-end">
                        <div
                          className="w-full bg-slate-300"
                          style={{ height: `${chargingHeight}px` }}
                        />
                        <div
                          className="w-full bg-slate-700"
                          style={{ height: `${dropOffHeight}px` }}
                        />
                      </div>
                    ) : null}
                  </div>
                </div>
              )
            })}
          </div>

          {/* Day labels, thinned out so a three-week span does not overlap. */}
          <div className="mt-2 flex gap-1 border-t border-slate-200 pt-2">
            {days.map((day, index) => (
              <span
                key={day.iso}
                className="flex-1 text-center text-[10px] text-slate-400"
              >
                {index % 2 === 0 ? day.label : ''}
              </span>
            ))}
          </div>
        </>
      )}
    </Panel>
  )
}
