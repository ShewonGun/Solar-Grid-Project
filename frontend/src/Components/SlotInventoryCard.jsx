/*
 * File: SlotInventoryCard.jsx
 * Purpose: A network-wide read of how many of this week's battery slots are
 *          free, booked or held out of service, so a Back-office officer can
 *          tell at a glance whether the network has spare room to take more
 *          bookings or is running tight. This is the aggregate view; the node
 *          utilisation card next to it is where a specific node is called out.
 *          The three states reuse the exact colours StatusPill already gives
 *          Available/Reserved/Unavailable elsewhere in the console, since this
 *          is the same status, not a new one that needs its own palette.
 * Author:  <your name>
 * Created: 2026
 */
import { useMemo } from 'react'

import { Panel } from './PageControls'

/** Matches StatusPill's tone colours for the same three slot statuses. */
const SEGMENTS = [
  { status: 'Available', label: 'Available', bar: 'bg-emerald-600', dot: 'bg-emerald-600' },
  { status: 'Reserved', label: 'Reserved', bar: 'bg-amber-500', dot: 'bg-amber-500' },
  { status: 'Unavailable', label: 'Unavailable', bar: 'bg-slate-400', dot: 'bg-slate-400' },
]

/* Counts every slot by status, regardless of which node it belongs to. */
function countByStatus(slots) {
  return SEGMENTS.reduce((counts, segment) => {
    counts[segment.status] = slots.filter((slot) => slot.status === segment.status).length
    return counts
  }, {})
}

/* `slots` is the same cross-node list NodeUtilizationCard uses. */
export default function SlotInventoryCard({ slots }) {
  const counts = useMemo(() => countByStatus(slots), [slots])
  const total = slots.length

  return (
    <Panel title="Battery slot inventory" meta={`${total} slots this week`}>
      {total === 0 ? (
        <p className="py-6 text-center text-sm text-slate-400">No slots opened this week.</p>
      ) : (
        <>
          {/* One proportion bar - part-to-whole across the three statuses. */}
          <div className="flex h-3 w-full overflow-hidden rounded-xs bg-slate-100">
            {SEGMENTS.map((segment) => {
              const count = counts[segment.status]
              const share = total > 0 ? (count / total) * 100 : 0

              return count > 0 ? (
                <div
                  key={segment.status}
                  className={segment.bar}
                  style={{ width: `${share}%` }}
                  title={`${segment.label}: ${count} (${Math.round(share)}%)`}
                />
              ) : null
            })}
          </div>

          <ul className="mt-4 grid grid-cols-3 gap-3">
            {SEGMENTS.map((segment) => (
              <li key={segment.status}>
                <span className="flex items-center gap-1.5 text-xs text-slate-500">
                  <span className={`h-2 w-2 shrink-0 rounded-xs ${segment.dot}`} aria-hidden="true" />
                  {segment.label}
                </span>
                <span className="mt-1 block text-xl font-semibold tabular-nums text-slate-900">
                  {counts[segment.status]}
                </span>
              </li>
            ))}
          </ul>
        </>
      )}
    </Panel>
  )
}
