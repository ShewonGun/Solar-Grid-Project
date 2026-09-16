/*
 * File: DashboardParts.jsx
 * Purpose: The display pieces the dashboard is built from - the stat tiles that
 *          carry the headline counts, and the column chart of bookings across
 *          the 7-day reservation window. The counts are a handful of headline
 *          numbers, so they are tiles rather than a chart; the week's spread is
 *          a magnitude comparison, so it is a single-hue column chart.
 * Author:  <your name>
 * Created: 2026
 */
import { Link } from 'react-router-dom'

import { IconChevronRight } from './Icons'
import { Panel } from './PageControls'

/*
 * One headline number. `to` turns the whole tile into a link through to the
 * screen that explains the number; `tone` accents the value for a count that
 * needs attention, never as the only signal - the label always says what it is.
 */
export function StatTile({ label, value, caption, to, icon: TileIcon, tone = 'default' }) {
  const isAttention = tone === 'attention'

  const body = (
    <>
      <div className="flex items-start justify-between gap-3">
        <span className="text-[11px] font-semibold uppercase tracking-[0.07em] text-slate-500">
          {label}
        </span>
        {TileIcon ? (
          <span
            className={`grid h-7 w-7 shrink-0 place-items-center rounded-xs border ${
              isAttention
                ? 'border-amber-200 bg-amber-50 text-amber-700'
                : 'border-slate-200 bg-slate-50 text-slate-400'
            }`}
          >
            <TileIcon className="h-4 w-4" />
          </span>
        ) : null}
      </div>

      <span
        className={`mt-3 block text-[1.75rem] font-semibold leading-none tabular-nums tracking-tight ${
          isAttention ? 'text-amber-700' : 'text-slate-900'
        }`}
      >
        {value}
      </span>

      <span className="mt-2 flex items-center gap-1 text-xs text-slate-400">
        {caption}
        {to ? <IconChevronRight className="h-3 w-3" /> : null}
      </span>
    </>
  )

  const surface =
    'block border border-slate-200 bg-white p-4 shadow-[0_1px_2px_rgba(15,23,42,0.04)]'

  if (to) {
    return (
      <Link to={to} className={`${surface} transition-colors hover:border-slate-400`}>
        {body}
      </Link>
    )
  }

  return <div className={surface}>{body}</div>
}

/** Height of the chart's plot area, in pixels. */
const PLOT_HEIGHT = 132

/*
 * Column chart of how many reservations fall on each day of the 7-day booking
 * window. One series, so the title names it and no legend is needed; the bars
 * carry direct labels, so the numbers are readable without reading the colour.
 */
export function WeekBookingsChart({ days }) {
  // A flat set of zeroes would divide by zero below, so the scale floors at 1.
  const peak = Math.max(1, ...days.map((day) => day.count))
  const total = days.reduce((sum, day) => sum + day.count, 0)

  return (
    <Panel
      title="Bookings across the next 7 days"
      meta={`${total} in total`}
    >
      <p className="-mt-1 mb-5 text-xs text-slate-400">
        Reservations must be booked inside this window.
      </p>

      {/* Plot area. Each column is labelled with its own count, so the chart is
          readable without relying on bar height alone. */}
      <div className="flex items-end gap-1.5" style={{ height: `${PLOT_HEIGHT}px` }}>
        {days.map((day) => {
          const barHeight = day.count === 0 ? 2 : Math.round((day.count / peak) * PLOT_HEIGHT)

          return (
            <div key={day.iso} className="flex flex-1 flex-col items-center justify-end gap-1.5">
              <span className="text-xs font-medium tabular-nums text-slate-600">
                {day.count > 0 ? day.count : ''}
              </span>
              <div
                className={`w-full rounded-xs transition-colors ${
                  day.count > 0 ? 'bg-slate-700 hover:bg-slate-900' : 'bg-slate-100'
                }`}
                style={{ height: `${barHeight}px` }}
                title={`${day.label}: ${day.count} reservation${day.count === 1 ? '' : 's'}`}
              />
            </div>
          )
        })}
      </div>

      {/* Baseline and day labels, kept recessive so the bars stay the subject. */}
      <div className="mt-2 flex gap-1.5 border-t border-slate-200 pt-2">
        {days.map((day) => (
          <span key={day.iso} className="flex-1 text-center text-[11px] text-slate-400">
            {day.label}
          </span>
        ))}
      </div>
    </Panel>
  )
}
