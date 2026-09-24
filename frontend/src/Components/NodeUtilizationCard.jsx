/*
 * File: NodeUtilizationCard.jsx
 * Purpose: Ranks microgrid nodes by how much of this week's opened battery
 *          slots are already reserved, so a Back-office officer can see which
 *          nodes need more battery slots and which are sitting idle without
 *          opening each node's slot page one at a time. Utilisation is a share
 *          of opened windows, not of a node's physical battery count - a node
 *          with plenty of physical slots but few windows opened this week can
 *          still show as fully booked.
 * Author:  <your name>
 * Created: 2026
 */
import { useMemo } from 'react'

import { Panel } from './PageControls'

/** Utilisation at or above this share is called out as near capacity. */
const NEAR_CAPACITY = 0.85

/*
 * For each node, counts this window's slots and how many are Reserved.
 * Returns nodes sorted busiest first; a node with no slots opened this window
 * gets `utilisation: null` rather than a misleading 0%.
 */
function rankByUtilisation(stations, slots) {
  return stations
    .map((station) => {
      const nodeSlots = slots.filter((slot) => slot.stationId === station.id)
      const reserved = nodeSlots.filter((slot) => slot.status === 'Reserved').length

      return {
        id: station.id,
        name: station.stationName,
        total: nodeSlots.length,
        reserved,
        utilisation: nodeSlots.length > 0 ? reserved / nodeSlots.length : null,
      }
    })
    .sort((a, b) => (b.utilisation ?? -1) - (a.utilisation ?? -1))
}

/*
 * `stations` and `slots` are what the dashboard already loaded - this card
 * does no fetching of its own, only the ranking above.
 */
export default function NodeUtilizationCard({ stations, slots }) {
  const ranked = useMemo(() => rankByUtilisation(stations, slots), [stations, slots])

  return (
    <Panel
      title="Node utilisation"
      meta="Reserved share of this week's opened slots"
    >
      {ranked.length === 0 ? (
        <p className="py-6 text-center text-sm text-slate-400">No microgrid nodes yet.</p>
      ) : (
        <ul className="space-y-3">
          {ranked.map((node) => {
            const hasData = node.utilisation !== null
            const percent = hasData ? Math.round(node.utilisation * 100) : null
            const nearCapacity = hasData && node.utilisation >= NEAR_CAPACITY

            return (
              <li key={node.id}>
                <div className="mb-1 flex items-baseline justify-between gap-3">
                  <span className="truncate text-sm font-medium text-slate-900">
                    {node.name}
                  </span>
                  <span className="shrink-0 text-xs tabular-nums text-slate-500">
                    {hasData ? (
                      <>
                        {node.reserved}/{node.total} slots
                        {nearCapacity ? (
                          <span className="ml-1.5 font-medium text-amber-700">
                            &middot; Near capacity
                          </span>
                        ) : null}
                      </>
                    ) : (
                      'No slots opened'
                    )}
                  </span>
                </div>

                {/* Bar length is the only numeric encoding here; colour stays
                    one consistent hue so it never competes with the length. */}
                <div className="h-2 w-full overflow-hidden rounded-xs bg-slate-100">
                  <div
                    className={`h-full rounded-xs ${hasData ? 'bg-slate-700' : ''}`}
                    style={{ width: hasData ? `${Math.max(percent, 2)}%` : '0%' }}
                    title={hasData ? `${percent}% reserved` : 'No slots opened this week'}
                  />
                </div>
              </li>
            )
          })}
        </ul>
      )}
    </Panel>
  )
}
