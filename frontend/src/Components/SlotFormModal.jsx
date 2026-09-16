/*
 * File: SlotFormModal.jsx
 * Purpose: Dialog that opens a bookable window on one of a node's battery
 *          slots, and edits an existing one - passing a slotId switches it to
 *          edit. The node supplies the limits shown here (how many battery
 *          slots it has, how much it holds); the Web API re-checks them, along
 *          with the overlap rule it alone can see.
 * Author:  <your name>
 * Created: 2026
 */
import { useEffect, useState } from 'react'

import { toApiError } from '../api/client'
import { createSlot, getSlot, updateSlot } from '../api/slotsApi'
import { TextField } from './FormControls'
import { Banner, Button, Modal } from './PageControls'
import { isFormValid } from '../utils/validation'
import { fromLocalInputValue, nowInputValue, toLocalInputValue } from '../utils/datetime'

const EMPTY_FORM = {
  batterySlotNumber: '',
  startTime: '',
  endTime: '',
  capacityKWh: '',
  isAvailable: true,
}

/*
 * Checks the form against the rules the API applies to a slot: a battery number
 * within the node's count, a capacity within the node's own, and a window that
 * ends after it starts and has not already begun.
 */
function validateForm(form, station) {
  const batteryNumber = Number(form.batterySlotNumber)
  const capacity = Number(form.capacityKWh)
  const start = form.startTime ? new Date(form.startTime) : null
  const end = form.endTime ? new Date(form.endTime) : null

  return {
    batterySlotNumber: !form.batterySlotNumber.trim()
      ? 'Battery slot number is required.'
      : !Number.isInteger(batteryNumber) ||
          batteryNumber < 1 ||
          batteryNumber > station.totalBatterySlots
        ? `Enter a number between 1 and ${station.totalBatterySlots}.`
        : '',

    startTime: !form.startTime
      ? 'Start time is required.'
      : start <= new Date()
        ? 'Slots must start in the future.'
        : '',

    endTime: !form.endTime
      ? 'End time is required.'
      : start && end <= start
        ? 'End time must be after the start time.'
        : '',

    capacityKWh: !form.capacityKWh.trim()
      ? 'Capacity is required.'
      : !Number.isFinite(capacity) || capacity <= 0
        ? 'Capacity must be greater than 0.'
        : capacity > station.capacityKWh
          ? `This node holds at most ${station.capacityKWh} kWh.`
          : '',
  }
}

/*
 * `slotId` selects the slot to edit, or is left out to open a new one.
 * `station` supplies the limits the fields are checked against. `onSaved` is
 * called after the service accepts the change; `onClose` dismisses the dialog.
 */
export default function SlotFormModal({ station, slotId, onClose, onSaved }) {
  const isEditing = Boolean(slotId)

  const [form, setForm] = useState(EMPTY_FORM)
  const [errors, setErrors] = useState({})
  const [apiError, setApiError] = useState('')
  const [loading, setLoading] = useState(isEditing)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    // Nothing to load when opening a new slot.
    if (!isEditing) {
      return
    }

    let cancelled = false

    /* Loads the slot being edited and fills the form with its window. */
    async function loadSlot() {
      try {
        const slot = await getSlot(slotId)

        if (!cancelled) {
          setForm({
            batterySlotNumber: String(slot.batterySlotNumber),
            startTime: toLocalInputValue(slot.startTime),
            endTime: toLocalInputValue(slot.endTime),
            capacityKWh: String(slot.capacityKWh),
            isAvailable: slot.status !== 'Unavailable',
          })
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

    loadSlot()

    return () => {
      cancelled = true
    }
  }, [slotId, isEditing])

  /* Keeps one field in state and clears its error as soon as the user edits it. */
  function handleChange(event) {
    const { name, value, type, checked } = event.target

    setForm((current) => ({ ...current, [name]: type === 'checkbox' ? checked : value }))
    setErrors((current) => ({ ...current, [name]: '' }))
    setApiError('')
  }

  /* Re-checks a single field once the user leaves it. */
  function handleBlur(event) {
    const { name } = event.target
    setErrors((current) => ({ ...current, [name]: validateForm(form, station)[name] }))
  }

  /*
   * Validates the form and saves the slot. Times are sent as UTC instants; the
   * overlap check against other slots on the same battery is the service's,
   * because only it can see the node's whole schedule.
   */
  async function handleSubmit(event) {
    event.preventDefault()

    const nextErrors = validateForm(form, station)
    setErrors(nextErrors)

    if (!isFormValid(nextErrors)) {
      return
    }

    setBusy(true)
    setApiError('')

    const payload = {
      batterySlotNumber: Number(form.batterySlotNumber),
      startTime: fromLocalInputValue(form.startTime),
      endTime: fromLocalInputValue(form.endTime),
      capacityKWh: Number(form.capacityKWh),
      isAvailable: form.isAvailable,
    }

    try {
      if (isEditing) {
        await updateSlot(slotId, payload)
      } else {
        await createSlot({ ...payload, stationId: station.id })
      }

      onSaved(isEditing ? 'Battery slot updated.' : 'Battery slot opened for booking.')
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
      title={isEditing ? 'Edit battery slot' : 'Open battery slot'}
      description={`${station.stationName} - ${station.totalBatterySlots} battery slots, up to ${station.capacityKWh} kWh.`}
      size="lg"
      onClose={onClose}
    >
      <Banner tone="error">{apiError}</Banner>

      {loading ? (
        <p className="py-8 text-center text-sm text-slate-400">Loading slot...</p>
      ) : (
        <form onSubmit={handleSubmit} noValidate>
          <div className="space-y-5">
            <div className="grid gap-5 sm:grid-cols-2">
              <TextField
                label="Battery slot number"
                name="batterySlotNumber"
                type="number"
                step="1"
                min="1"
                max={station.totalBatterySlots}
                inputMode="numeric"
                autoFocus
                placeholder="1"
                value={form.batterySlotNumber}
                onChange={handleChange}
                onBlur={handleBlur}
                error={errors.batterySlotNumber}
                hint={`This node has ${station.totalBatterySlots} battery slots.`}
                disabled={busy}
              />

              <TextField
                label="Capacity (kWh)"
                name="capacityKWh"
                type="number"
                step="0.1"
                min="0"
                max={station.capacityKWh}
                inputMode="decimal"
                placeholder="25"
                value={form.capacityKWh}
                onChange={handleChange}
                onBlur={handleBlur}
                error={errors.capacityKWh}
                hint={`At most ${station.capacityKWh} kWh, the node's own capacity.`}
                disabled={busy}
              />
            </div>

            <div className="grid gap-5 sm:grid-cols-2">
              <TextField
                label="Starts"
                name="startTime"
                type="datetime-local"
                min={nowInputValue()}
                value={form.startTime}
                onChange={handleChange}
                onBlur={handleBlur}
                error={errors.startTime}
                disabled={busy}
              />

              <TextField
                label="Ends"
                name="endTime"
                type="datetime-local"
                min={form.startTime || nowInputValue()}
                value={form.endTime}
                onChange={handleChange}
                onBlur={handleBlur}
                error={errors.endTime}
                disabled={busy}
              />
            </div>

            <label className="flex items-start gap-2.5 border border-slate-200 bg-slate-50 px-3 py-2.5">
              <input
                type="checkbox"
                name="isAvailable"
                checked={form.isAvailable}
                onChange={handleChange}
                disabled={busy}
                className="mt-0.5 h-3.5 w-3.5 rounded-xs border-slate-300 text-slate-900 focus:ring-slate-900"
              />
              <span>
                <span className="block text-sm font-medium text-slate-800">
                  Available for booking
                </span>
                <span className="mt-0.5 block text-xs text-slate-500">
                  Clear this to hold the slot out of service, for maintenance.
                </span>
              </span>
            </label>
          </div>

          <div className="mt-7 flex justify-end gap-2 border-t border-slate-200 pt-5">
            <Button type="button" variant="secondary" onClick={onClose} disabled={busy}>
              Cancel
            </Button>
            <Button type="submit" disabled={busy}>
              {busy ? 'Saving...' : isEditing ? 'Save changes' : 'Open slot'}
            </Button>
          </div>
        </form>
      )}
    </Modal>
  )
}
