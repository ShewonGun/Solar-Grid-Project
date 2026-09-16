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
import { Banner, Button, Modal } from './PageControls'
import { isFormValid, validateRequired } from '../utils/validation'

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

  const [form, setForm] = useState(EMPTY_FORM)
  const [errors, setErrors] = useState({})
  const [apiError, setApiError] = useState('')
  const [loading, setLoading] = useState(isEditing)
  const [busy, setBusy] = useState(false)

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

            <TextField
              label="Operating schedule"
              name="operatingSchedule"
              type="text"
              maxLength={200}
              placeholder="Mon-Sun 06:00-22:00"
              value={form.operatingSchedule}
              onChange={handleChange}
              error={errors.operatingSchedule}
              optional
              disabled={busy}
            />
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
