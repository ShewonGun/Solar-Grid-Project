/*
 * File: StationFormModal.jsx
 * Purpose: Dialog that registers a new microgrid node and edits an existing
 *          one - passing a stationId switches it to edit. Field checks here
 *          mirror the Web API's StationRequest so the user gets instant
 *          feedback; the service re-validates everything it is sent and stays
 *          the authority on what is accepted.
 * Author:  <your name>
 * Created: 2026
 */
import { useEffect, useState } from 'react'

import { toApiError } from '../api/client'
import { createStation, getStation, updateStation } from '../api/stationsApi'
import { TextField } from './FormControls'
import LocationPicker from './LocationPicker'
import { Banner, Button, Modal } from './PageControls'
import { isFormValid, validateRequired } from '../utils/validation'

/** Days offered in the schedule builder, in week order. */
const DAYS = [
  { value: 'Mon', label: 'Monday' },
  { value: 'Tue', label: 'Tuesday' },
  { value: 'Wed', label: 'Wednesday' },
  { value: 'Thu', label: 'Thursday' },
  { value: 'Fri', label: 'Friday' },
  { value: 'Sat', label: 'Saturday' },
  { value: 'Sun', label: 'Sunday' },
]

/** What the builder starts from - every day, a typical daytime window. */
const DEFAULT_SCHEDULE = { fromDay: 'Mon', toDay: 'Sun', openTime: '06:00', closeTime: '22:00' }

/*
 * Turns the builder's day range and time range into the text the API stores,
 * e.g. "Mon-Sun 06:00-22:00" - or "Sat 09:00-13:00" when the range is a single
 * day, since a day repeated on both ends would read oddly.
 */
function composeSchedule({ fromDay, toDay, openTime, closeTime }) {
  const dayPart = fromDay === toDay ? fromDay : `${fromDay}-${toDay}`
  return `${dayPart} ${openTime}-${closeTime}`
}

const CONTROL_CLASSES =
  'w-full rounded-xs border border-slate-300 bg-white px-2.5 py-2 text-sm text-slate-900 outline-none transition-colors hover:border-slate-400 focus:border-slate-900 focus:ring-1 focus:ring-slate-900 disabled:bg-slate-50 disabled:text-slate-400'

const EMPTY_FORM = {
  stationName: '',
  latitude: '',
  longitude: '',
  capacityKWh: '',
  totalBatterySlots: '',
  operatingSchedule: '',
}

/*
 * Checks a numeric field, returning an error message or an empty string.
 * `exclusiveMin` is used for capacity, which the API requires to be greater
 * than zero rather than zero or more.
 */
function validateNumber(value, label, { min, max, exclusiveMin = false, integer = false } = {}) {
  const text = (value ?? '').trim()

  if (!text) {
    return `${label} is required.`
  }

  const parsed = Number(text)

  if (!Number.isFinite(parsed)) {
    return `${label} must be a number.`
  }

  if (integer && !Number.isInteger(parsed)) {
    return `${label} must be a whole number.`
  }

  if (exclusiveMin && parsed <= min) {
    return `${label} must be greater than ${min}.`
  }

  if (!exclusiveMin && min !== undefined && parsed < min) {
    return `${label} must be at least ${min}.`
  }

  if (max !== undefined && parsed > max) {
    return `${label} must be at most ${max}.`
  }

  return ''
}

/* Checks the whole form and returns a message per field. */
function validateForm(form) {
  return {
    stationName: validateRequired(form.stationName, 'Node name'),
    latitude: validateNumber(form.latitude, 'Latitude', { min: -90, max: 90 }),
    longitude: validateNumber(form.longitude, 'Longitude', { min: -180, max: 180 }),
    capacityKWh: validateNumber(form.capacityKWh, 'Capacity', { min: 0, exclusiveMin: true }),
    totalBatterySlots: validateNumber(form.totalBatterySlots, 'Battery slots', {
      min: 1,
      integer: true,
    }),
    operatingSchedule: '',
  }
}

/* Builds the StationRequest body from the form, sending numbers as numbers. */
function toRequest(form) {
  return {
    stationName: form.stationName.trim(),
    latitude: Number(form.latitude),
    longitude: Number(form.longitude),
    capacityKWh: Number(form.capacityKWh),
    totalBatterySlots: Number(form.totalBatterySlots),
    operatingSchedule: form.operatingSchedule.trim(),
  }
}

/* Fills the form from a station returned by the API, as editable strings. */
function toForm(station) {
  return {
    stationName: station.stationName,
    latitude: String(station.latitude),
    longitude: String(station.longitude),
    capacityKWh: String(station.capacityKWh),
    totalBatterySlots: String(station.totalBatterySlots),
    operatingSchedule: station.operatingSchedule ?? '',
  }
}

/*
 * `stationId` selects the node to edit, or is left out to register a new one.
 * `onSaved` is called after the service accepts the change so the list behind
 * the dialog can reload; `onClose` dismisses it.
 */
export default function StationFormModal({ stationId, onClose, onSaved }) {
  const isEditing = Boolean(stationId)

  // A brand-new node starts with the builder's default schedule already
  // composed, so submitting without touching it saves something sensible
  // rather than a blank string.
  const [form, setForm] = useState(() =>
    isEditing ? EMPTY_FORM : { ...EMPTY_FORM, operatingSchedule: composeSchedule(DEFAULT_SCHEDULE) },
  )
  const [errors, setErrors] = useState({})
  const [apiError, setApiError] = useState('')
  const [loading, setLoading] = useState(isEditing)
  const [busy, setBusy] = useState(false)

  // Whether the location comes from typed numbers or a map click.
  const [locationMode, setLocationMode] = useState('manual')
  // Bumped every time the map picker is (re)opened, so it remounts and
  // re-centres on whatever is currently in the latitude/longitude fields
  // rather than staying wherever it was left after an earlier switch.
  const [mapResetKey, setMapResetKey] = useState(0)

  // Whether the schedule comes from the day/time builder or typed text.
  // Editing starts on the typed text, because an existing schedule can be any
  // free-form string and there is no safe way to read it back into the
  // builder's day/time fields; registering a new node starts on the builder,
  // since there is nothing yet to lose by defaulting to the easier path.
  const [scheduleMode, setScheduleMode] = useState(isEditing ? 'manual' : 'build')
  const [schedule, setSchedule] = useState(DEFAULT_SCHEDULE)

  useEffect(() => {
    // Nothing to load when registering a new node.
    if (!isEditing) {
      return
    }

    let cancelled = false

    /* Loads the node being edited and fills the form with its current values. */
    async function loadStation() {
      try {
        const station = await getStation(stationId)

        if (!cancelled) {
          setForm(toForm(station))
          setApiError('')
        }
      } catch (failure) {
        if (!cancelled) {
          setApiError(toApiError(failure).message)
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    loadStation()

    return () => {
      cancelled = true
    }
  }, [stationId, isEditing])

  /* Keeps one field in state and clears its error as soon as the user edits it. */
  function handleChange(event) {
    const { name, value } = event.target

    setForm((current) => ({ ...current, [name]: value }))
    setErrors((current) => ({ ...current, [name]: '' }))
    setApiError('')
  }

  /* Re-checks a single field once the user leaves it. */
  function handleBlur(event) {
    const { name } = event.target
    setErrors((current) => ({ ...current, [name]: validateForm(form)[name] }))
  }

  /*
   * Fills latitude and longitude from a map click or a marker drag. Both
   * fields update together, since a single point on the map always sets both
   * at once - there is no "half a location" the way manual entry allows
   * mid-typing.
   */
  function handleLocationPicked({ lat, lng }) {
    setForm((current) => ({ ...current, latitude: String(lat), longitude: String(lng) }))
    setErrors((current) => ({ ...current, latitude: '', longitude: '' }))
    setApiError('')
  }

  /* Switches to the map picker, reopening it centred on whatever is typed now. */
  function handleSwitchToMap() {
    setLocationMode('map')
    setMapResetKey((current) => current + 1)
  }

  /*
   * Switches to the day/time builder, replacing whatever text is there now
   * with the builder's own default - there is no reliable way to read an
   * arbitrary existing string back into day and time fields.
   */
  function handleSwitchToBuildSchedule() {
    setScheduleMode('build')
    setSchedule(DEFAULT_SCHEDULE)
    setForm((current) => ({ ...current, operatingSchedule: composeSchedule(DEFAULT_SCHEDULE) }))
    setErrors((current) => ({ ...current, operatingSchedule: '' }))
  }

  /* Applies one day/time change from the builder and recomposes the schedule text. */
  function handleScheduleFieldChange(event) {
    const { name, value } = event.target
    const next = { ...schedule, [name]: value }

    setSchedule(next)
    setForm((current) => ({ ...current, operatingSchedule: composeSchedule(next) }))
    setErrors((current) => ({ ...current, operatingSchedule: '' }))
  }

  /*
   * Validates the form and saves the node through the Web API. Validation
   * messages the service sends back are merged onto the matching fields.
   */
  async function handleSubmit(event) {
    event.preventDefault()

    const nextErrors = validateForm(form)
    setErrors(nextErrors)

    if (!isFormValid(nextErrors)) {
      return
    }

    setBusy(true)
    setApiError('')

    try {
      const request = toRequest(form)

      if (isEditing) {
        await updateStation(stationId, request)
      } else {
        await createStation(request)
      }

      onSaved(
        isEditing
          ? `${request.stationName} updated.`
          : `${request.stationName} registered.`,
      )
      onClose()
    } catch (failure) {
      const { message, fieldErrors } = toApiError(failure)

      setApiError(message)
      setErrors((current) => ({ ...current, ...fieldErrors }))
    } finally {
      setBusy(false)
    }
  }

  // The current latitude/longitude as a point for the map, or null when
  // either field is blank or not yet a real number.
  const pickedLocation =
    form.latitude.trim() !== '' &&
    form.longitude.trim() !== '' &&
    Number.isFinite(Number(form.latitude)) &&
    Number.isFinite(Number(form.longitude))
      ? { lat: Number(form.latitude), lng: Number(form.longitude) }
      : null

  return (
    <Modal
      title={isEditing ? 'Edit microgrid node' : 'Register microgrid node'}
      description="GPS location, capacity and battery storage slots for a solar grid hub."
      size="lg"
      onClose={onClose}
    >
      <Banner tone="error">{apiError}</Banner>

      {loading ? (
        <p className="py-8 text-center text-sm text-slate-400">Loading node...</p>
      ) : (
        <form onSubmit={handleSubmit} noValidate>
          <div className="space-y-5">
            <TextField
              label="Node name"
              name="stationName"
              type="text"
              maxLength={100}
              autoFocus
              placeholder="Malabe Solar Hub"
              value={form.stationName}
              onChange={handleChange}
              onBlur={handleBlur}
              error={errors.stationName}
              disabled={busy}
            />

            <div>
              <div className="mb-2 flex items-center justify-between gap-3">
                <span className="text-xs font-medium uppercase tracking-[0.08em] text-slate-600">
                  Location
                </span>

                {/* Switches how the location below is set; the underlying
                    latitude/longitude values are the same either way. */}
                <div className="flex rounded-xs border border-slate-300 p-0.5 text-xs">
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => setLocationMode('manual')}
                    className={`rounded-xs px-2.5 py-1 font-medium transition-colors ${
                      locationMode === 'manual'
                        ? 'bg-slate-900 text-white'
                        : 'text-slate-500 hover:text-slate-900'
                    }`}
                  >
                    Enter manually
                  </button>
                  <button
                    type="button"
                    disabled={busy}
                    onClick={handleSwitchToMap}
                    className={`rounded-xs px-2.5 py-1 font-medium transition-colors ${
                      locationMode === 'map'
                        ? 'bg-slate-900 text-white'
                        : 'text-slate-500 hover:text-slate-900'
                    }`}
                  >
                    Pick on map
                  </button>
                </div>
              </div>

              {locationMode === 'manual' ? (
                <div className="grid gap-5 sm:grid-cols-2">
                  <TextField
                    label="Latitude"
                    name="latitude"
                    type="number"
                    step="any"
                    min="-90"
                    max="90"
                    inputMode="decimal"
                    placeholder="6.9061"
                    value={form.latitude}
                    onChange={handleChange}
                    onBlur={handleBlur}
                    error={errors.latitude}
                    disabled={busy}
                  />

                  <TextField
                    label="Longitude"
                    name="longitude"
                    type="number"
                    step="any"
                    min="-180"
                    max="180"
                    inputMode="decimal"
                    placeholder="79.9696"
                    value={form.longitude}
                    onChange={handleChange}
                    onBlur={handleBlur}
                    error={errors.longitude}
                    disabled={busy}
                  />
                </div>
              ) : (
                <div>
                  <LocationPicker
                    key={mapResetKey}
                    value={pickedLocation}
                    onChange={handleLocationPicked}
                  />

                  {errors.latitude || errors.longitude ? (
                    <p className="mt-1.5 text-xs text-red-600">
                      {errors.latitude || errors.longitude}
                    </p>
                  ) : (
                    <p className="mt-1.5 text-xs leading-relaxed text-slate-400">
                      {pickedLocation
                        ? `Selected ${pickedLocation.lat.toFixed(4)}, ${pickedLocation.lng.toFixed(4)}. Click the map or drag the pin to adjust it.`
                        : 'Click the map to set the node’s location.'}
                    </p>
                  )}
                </div>
              )}
            </div>

            <div className="grid gap-5 sm:grid-cols-2">
              <TextField
                label="Capacity (kWh)"
                name="capacityKWh"
                type="number"
                step="0.1"
                min="0"
                inputMode="decimal"
                placeholder="250"
                value={form.capacityKWh}
                onChange={handleChange}
                onBlur={handleBlur}
                error={errors.capacityKWh}
                disabled={busy}
              />

              <TextField
                label="Battery slots"
                name="totalBatterySlots"
                type="number"
                step="1"
                min="1"
                inputMode="numeric"
                placeholder="12"
                value={form.totalBatterySlots}
                onChange={handleChange}
                onBlur={handleBlur}
                error={errors.totalBatterySlots}
                hint="Booking slots can only be created up to this number."
                disabled={busy}
              />
            </div>

            <div>
              <div className="mb-2 flex items-center justify-between gap-3">
                <span className="text-xs font-medium uppercase tracking-[0.08em] text-slate-600">
                  Operating schedule
                </span>

                {/* Switches how the text below is produced; the underlying
                    operatingSchedule value is the same either way. */}
                <div className="flex rounded-xs border border-slate-300 p-0.5 text-xs">
                  <button
                    type="button"
                    disabled={busy}
                    onClick={handleSwitchToBuildSchedule}
                    className={`rounded-xs px-2.5 py-1 font-medium transition-colors ${
                      scheduleMode === 'build'
                        ? 'bg-slate-900 text-white'
                        : 'text-slate-500 hover:text-slate-900'
                    }`}
                  >
                    Build schedule
                  </button>
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => setScheduleMode('manual')}
                    className={`rounded-xs px-2.5 py-1 font-medium transition-colors ${
                      scheduleMode === 'manual'
                        ? 'bg-slate-900 text-white'
                        : 'text-slate-500 hover:text-slate-900'
                    }`}
                  >
                    Type manually
                  </button>
                </div>
              </div>

              {scheduleMode === 'build' ? (
                <div>
                  <div className="grid gap-3 sm:grid-cols-2">
                    <label className="block">
                      <span className="mb-1 block text-[11px] text-slate-500">From day</span>
                      <select
                        name="fromDay"
                        value={schedule.fromDay}
                        onChange={handleScheduleFieldChange}
                        disabled={busy}
                        className={CONTROL_CLASSES}
                      >
                        {DAYS.map((day) => (
                          <option key={day.value} value={day.value}>
                            {day.label}
                          </option>
                        ))}
                      </select>
                    </label>

                    <label className="block">
                      <span className="mb-1 block text-[11px] text-slate-500">To day</span>
                      <select
                        name="toDay"
                        value={schedule.toDay}
                        onChange={handleScheduleFieldChange}
                        disabled={busy}
                        className={CONTROL_CLASSES}
                      >
                        {DAYS.map((day) => (
                          <option key={day.value} value={day.value}>
                            {day.label}
                          </option>
                        ))}
                      </select>
                    </label>

                    <label className="block">
                      <span className="mb-1 block text-[11px] text-slate-500">Opens</span>
                      <input
                        type="time"
                        name="openTime"
                        value={schedule.openTime}
                        onChange={handleScheduleFieldChange}
                        disabled={busy}
                        className={CONTROL_CLASSES}
                      />
                    </label>

                    <label className="block">
                      <span className="mb-1 block text-[11px] text-slate-500">Closes</span>
                      <input
                        type="time"
                        name="closeTime"
                        value={schedule.closeTime}
                        onChange={handleScheduleFieldChange}
                        disabled={busy}
                        className={CONTROL_CLASSES}
                      />
                    </label>
                  </div>

                  <p className="mt-1.5 text-xs leading-relaxed text-slate-400">
                    Saves as{' '}
                    <span className="font-medium text-slate-600">{form.operatingSchedule}</span>.
                  </p>
                </div>
              ) : (
                <TextField
                  label=""
                  name="operatingSchedule"
                  type="text"
                  maxLength={200}
                  placeholder="Mon-Sun 06:00-22:00"
                  value={form.operatingSchedule}
                  onChange={handleChange}
                  error={errors.operatingSchedule}
                  disabled={busy}
                />
              )}
            </div>
          </div>

          <div className="mt-7 flex justify-end gap-2 border-t border-slate-200 pt-5">
            <Button type="button" variant="secondary" onClick={onClose} disabled={busy}>
              Cancel
            </Button>
            <Button type="submit" disabled={busy}>
              {busy ? 'Saving...' : isEditing ? 'Save changes' : 'Register node'}
            </Button>
          </div>
        </form>
      )}
    </Modal>
  )
}
