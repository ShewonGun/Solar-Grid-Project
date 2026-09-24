/*
 * File: LocationPicker.jsx
 * Purpose: A small Google map for setting a microgrid node's GPS location by
 *          clicking or dragging a pin, as an alternative to typing latitude
 *          and longitude by hand. Reports the chosen point as plain
 *          { lat, lng } numbers; the caller decides what to do with them - in
 *          StationFormModal, filling the same fields manual entry uses, so the
 *          two ways of setting a location stay one source of truth.
 * Author:  <your name>
 * Created: 2026
 */
import { APIProvider, Map, Marker } from '@vis.gl/react-google-maps'

/** Roughly the middle of Sri Lanka - shown until the officer picks a point. */
const DEFAULT_CENTER = { lat: 7.8731, lng: 80.7718 }
const DEFAULT_ZOOM = 7
const PICKED_ZOOM = 13

/*
 * `value` is the current point as { lat, lng }, or null when nothing has been
 * picked yet. `onChange` fires with a plain { lat, lng } on every click or
 * drag. The map's own initial view is read once from `value` at mount - give
 * this component a fresh `key` from the caller if it should re-centre later
 * (StationFormModal does this when switching back into map mode).
 */
export default function LocationPicker({ value, onChange }) {
  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY

  if (!apiKey) {
    return (
      <p className="rounded-xs border border-dashed border-slate-300 bg-slate-50 px-3 py-6 text-center text-sm text-slate-500">
        Set <code className="rounded-xs bg-slate-100 px-1 py-0.5">VITE_GOOGLE_MAPS_API_KEY</code>{' '}
        to pick a location on the map.
      </p>
    )
  }

  return (
    <div className="h-64 w-full overflow-hidden rounded-xs border border-slate-300">
      <APIProvider apiKey={apiKey}>
        <Map
          defaultCenter={value ?? DEFAULT_CENTER}
          defaultZoom={value ? PICKED_ZOOM : DEFAULT_ZOOM}
          gestureHandling="greedy"
          fullscreenControl={false}
          streetViewControl={false}
          mapTypeControl={false}
          onClick={(event) => {
            // The map's own click event already gives plain numbers.
            const point = event.detail.latLng

            if (point) {
              onChange({ lat: point.lat, lng: point.lng })
            }
          }}
        >
          {value ? (
            <Marker
              position={value}
              draggable
              onDragEnd={(event) => {
                // A raw Google Maps marker event: lat/lng are methods, not
                // plain numbers, unlike the map's own click event above.
                const point = event.latLng

                if (point) {
                  onChange({ lat: point.lat(), lng: point.lng() })
                }
              }}
            />
          ) : null}
        </Map>
      </APIProvider>
    </div>
  )
}
