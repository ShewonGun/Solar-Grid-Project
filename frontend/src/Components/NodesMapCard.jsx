/*
 * File: NodesMapCard.jsx
 * Purpose: Dashboard card plotting every microgrid node on a Google map, so a
 *          Back-office officer or Grid Operator can see the network's spread
 *          at a glance. Markers come from the station list the dashboard has
 *          already loaded - no separate request. Selecting one opens a small
 *          panel with the node's details and a link through to its battery
 *          slots.
 * Author:  <your name>
 * Created: 2026
 */
import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { APIProvider, AdvancedMarker, InfoWindow, Map, Pin } from '@vis.gl/react-google-maps'

import { Panel } from './PageControls'

/** Roughly the middle of Sri Lanka - the fallback centre when no node has coordinates yet. */
const DEFAULT_CENTER = { lat: 7.8731, lng: 80.7718 }
const DEFAULT_ZOOM = 8
const FALLBACK_ZOOM = 7

/*
 * Google's published demo Map ID. Advanced markers require one; this ID needs
 * no Cloud Console setup and is meant for exactly this - development and
 * testing. Swap in a Map ID of your own (Cloud Console > Map Management) if
 * you want custom map styling later.
 */
const DEMO_MAP_ID = 'DEMO_MAP_ID'

/* The average position of every plottable node, or the fallback centre when there are none. */
function centerOf(nodes) {
  if (nodes.length === 0) {
    return DEFAULT_CENTER
  }

  const total = nodes.reduce(
    (sum, node) => ({ lat: sum.lat + node.latitude, lng: sum.lng + node.longitude }),
    { lat: 0, lng: 0 },
  )

  return { lat: total.lat / nodes.length, lng: total.lng / nodes.length }
}

/*
 * `stations` is the same array the dashboard already fetched from
 * GET /api/stations - this card does not call the Web API itself.
 */
export default function NodesMapCard({ stations }) {
  const [selectedId, setSelectedId] = useState(null)
  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY

  // Only nodes with real coordinates can be plotted; every node the API
  // returns has them, but this guards against a stray malformed record.
  const nodes = useMemo(
    () =>
      stations.filter(
        (station) => Number.isFinite(station.latitude) && Number.isFinite(station.longitude),
      ),
    [stations],
  )

  const center = useMemo(() => centerOf(nodes), [nodes])
  const selected = nodes.find((node) => node.id === selectedId) ?? null

  // The map needs a key from the Google Cloud Console; without one this card
  // explains what is missing instead of rendering a broken map.
  if (!apiKey) {
    return (
      <Panel title="Microgrid nodes">
        <p className="text-sm text-slate-500">
          Set <code className="rounded-xs bg-slate-100 px-1 py-0.5">VITE_GOOGLE_MAPS_API_KEY</code>{' '}
          in <code className="rounded-xs bg-slate-100 px-1 py-0.5">.env</code> to show nodes on a
          map.
        </p>
      </Panel>
    )
  }

  return (
    <Panel title="Microgrid nodes" meta={`${nodes.length} plotted`} flush>
      <div className="h-96 w-full">
        <APIProvider apiKey={apiKey}>
          <Map
            mapId={DEMO_MAP_ID}
            defaultCenter={center}
            defaultZoom={nodes.length > 0 ? DEFAULT_ZOOM : FALLBACK_ZOOM}
            gestureHandling="greedy"
            fullscreenControl={false}
            streetViewControl={false}
            mapTypeControl={false}
          >
            {nodes.map((node) => (
              <AdvancedMarker
                key={node.id}
                position={{ lat: node.latitude, lng: node.longitude }}
                onClick={() => setSelectedId(node.id)}
              >
                {/* Active nodes in the brand amber; inactive ones recede to grey. */}
                <Pin
                  background={node.isActive ? '#fbbf24' : '#cbd5e1'}
                  borderColor={node.isActive ? '#0f172a' : '#64748b'}
                  glyphColor={node.isActive ? '#0f172a' : '#475569'}
                />
              </AdvancedMarker>
            ))}

            {selected ? (
              <InfoWindow
                position={{ lat: selected.latitude, lng: selected.longitude }}
                pixelOffset={[0, -36]}
                onCloseClick={() => setSelectedId(null)}
              >
                <div className="min-w-48 font-sans">
                  <p className="text-sm font-semibold text-slate-900">{selected.stationName}</p>
                  <p className="mt-1 text-xs text-slate-500">
                    {selected.isActive ? 'Active' : 'Inactive'} &middot; {selected.capacityKWh} kWh
                    &middot; {selected.totalBatterySlots} slots
                  </p>
                  <Link
                    to={`/stations/${selected.id}/slots`}
                    className="mt-2 inline-block text-xs font-medium text-amber-600 underline decoration-amber-400 decoration-2 underline-offset-2 hover:text-slate-900"
                  >
                    View battery slots
                  </Link>
                </div>
              </InfoWindow>
            ) : null}
          </Map>
        </APIProvider>
      </div>
    </Panel>
  )
}
