/*
 * File: ReservationFormModal.jsx
 * Purpose: Dialog that books a power trading reservation and edits an existing
 *          one - passing a reservationId switches it to edit. The slot picker
 *          is filled from api/slots/bookable, which the service already limits
 *          to available slots at active nodes inside the 7-day window, so that
 *          rule is honoured by construction rather than re-checked here.
 * Author:  <your name>
 * Created: 2026
 */
import { useEffect, useMemo, useState } from 'react'

import { toApiError } from '../api/client'
import {
  createReservation,
  getReservation,
  updateReservation,
} from '../api/reservationsApi'
import { getBookableSlots } from '../api/slotsApi'
import { getStations } from '../api/stationsApi'
import { TextField } from './FormControls'
import { Banner, Button, Modal } from './PageControls'
import { isFormValid, validateNic } from '../utils/validation'
import {
  BOOKING_WINDOW_DAYS,
  MIN_NOTICE_HOURS,
  formatDateTime,
  formatTimeRange,
  modificationState,
  slotAcceptsReschedule,
} from '../utils/reservationRules'

const EMPTY_FORM = { prosumerNic: '', slotId: '', type: 'DropOff', energyKWh: '' }

const CONTROL_CLASSES =
  'w-full rounded-xs border border-slate-300 bg-white px-3 py-2.5 text-sm outline-none transition-colors hover:border-slate-400 focus:border-slate-900 focus:ring-1 focus:ring-slate-900 disabled:bg-slate-50 disabled:text-slate-400'

/*
 * Checks the form against the rules the API applies. `selectedSlot` caps the
 * energy amount, which the service limits to the slot's own capacity.
 */
function validateForm(form, selectedSlot, isEditing) {
  const energy = Number(form.energyKWh)

  return {
    // The prosumer is fixed once a booking exists, so it is only checked on create.
    prosumerNic: isEditing ? '' : validateNic(form.prosumerNic),
    slotId: form.slotId ? '' : 'Choose a battery slot.',
    energyKWh: !form.energyKWh.trim()
      ? 'Energy is required.'
      : !Number.isFinite(energy) || energy <= 0
        ? 'Energy must be greater than 0.'
        : selectedSlot && energy > selectedSlot.capacityKWh
          ? `This slot holds at most ${selectedSlot.capacityKWh} kWh.`
          : '',
  }
}

/*
 * `reservationId` selects the booking to edit, or is left out to create one.
 * `onSaved` is called after the service accepts the change so the list behind
 * the dialog can reload; `onClose` dismisses it.
 */
export default function ReservationFormModal({ reservationId, onClose, onSaved }) {
  const isEditing = Boolean(reservationId)

  const [form, setForm] = useState(EMPTY_FORM)
  const [reservation, setReservation] = useState(null)
  const [slots, setSlots] = useState([])
  const [stations, setStations] = useState([])
  const [errors, setErrors] = useState({})
  const [apiError, setApiError] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    let cancelled = false

    /*
     * Loads the bookable slots, the node names to label them with, and - when
     * editing - the booking being changed.
     */
    async function loadForm() {
      try {
        const [bookable, stationList, existing] = await Promise.all([
          getBookableSlots(),
          getStations(),
          isEditing ? getReservation(reservationId) : Promise.resolve(null),
        ])

        if (cancelled) {
          return
        }

        setSlots(bookable)
        setStations(stationList)

        if (existing) {
          setReservation(existing)
          setForm({
            prosumerNic: existing.prosumerNic,
            // The current slot stays selected until the user picks another.
            slotId: existing.slotId,
            type: existing.type,
            energyKWh: String(existing.energyKWh),
          })
        }

        setApiError('')
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

    loadForm()

    return () => {
      cancelled = true
    }
  }, [reservationId, isEditing])

  const stationNames = useMemo(
    () => new Map(stations.map((station) => [station.id, station.stationName])),
    [stations],
  )

  /*
   * The slots offered in the picker. When editing, a slot the booking could not
   * legally be moved to is left out, because the API applies the notice rule to
   * the destination slot as well as the booking itself.
   */
  const selectableSlots = useMemo(
    () => (isEditing ? slots.filter(slotAcceptsReschedule) : slots),
    [slots, isEditing],
  )

  const selectedSlot = useMemo(
    () => slots.find((slot) => slot.id === form.slotId) ?? null,
    [slots, form.slotId],
  )

  /* Keeps one field in state and clears its error as soon as the user edits it. */
  function handleChange(event) {
    const { name, value } = event.target

    setForm((current) => ({ ...current, [name]: value }))
    setErrors((current) => ({ ...current, [name]: '' }))
    setApiError('')
  }

  /*
   * Validates the form and saves the booking through the Web API. A new booking
   * is created as Pending; an edited one is reset to Pending by the service and
   * its QR code revoked, which the dialog says before the user saves.
   */
  async function handleSubmit(event) {
    event.preventDefault()

    const nextErrors = validateForm(form, selectedSlot, isEditing)
    setErrors(nextErrors)

    if (!isFormValid(nextErrors)) {
      return
    }

    setBusy(true)
    setApiError('')

    try {
      if (isEditing) {
        await updateReservation(reservationId, {
          slotId: form.slotId,
          type: form.type,
          energyKWh: Number(form.energyKWh),
        })
      } else {
        await createReservation({
          prosumerNic: form.prosumerNic.trim().toUpperCase(),
          slotId: form.slotId,
          type: form.type,
          energyKWh: Number(form.energyKWh),
        })
      }

      onSaved(isEditing ? 'Reservation updated and sent for approval.' : 'Reservation created.')
      onClose()
    } catch (failure) {
      const { message, fieldErrors } = toApiError(failure)

      setApiError(message)
      setErrors((current) => ({ ...current, ...fieldErrors }))
    } finally {
      setBusy(false)
    }
  }

  // A booking inside the notice window cannot be edited at all; say so rather
  // than showing a form whose every save would be refused.
  const blocked = isEditing && reservation ? modificationState(reservation) : { allowed: true }

  return (
    <Modal
      title={isEditing ? 'Edit reservation' : 'New reservation'}
      description={
        isEditing
          ? `Saving sends this booking back for approval and revokes its QR code. Changes need ${MIN_NOTICE_HOURS} hours' notice.`
          : `Book an available battery slot within the next ${BOOKING_WINDOW_DAYS} days.`
      }
      size="lg"
      onClose={onClose}
    >
      <Banner tone="error">{apiError}</Banner>

      {loading ? (
        <p className="py-8 text-center text-sm text-slate-400">Loading reservation...</p>
      ) : !blocked.allowed ? (
        <>
          <Banner tone="error">{blocked.reason}</Banner>
          <div className="flex justify-end">
            <Button variant="secondary" onClick={onClose}>
              Close
            </Button>
          </div>
        </>
      ) : (
        <form onSubmit={handleSubmit} noValidate>
          {selectableSlots.length === 0 ? (
            <Banner tone="info">
              No battery slots are bookable right now. Slots must be available, at an active node,
              and start within the next {BOOKING_WINDOW_DAYS} days.
            </Banner>
          ) : null}

          <div className="space-y-5">
            {isEditing ? (
              <div>
                <span className="text-xs font-medium uppercase tracking-[0.08em] text-slate-600">
                  Prosumer
                </span>
                <p className="mt-1.5 text-sm text-slate-900">NIC {form.prosumerNic}</p>
                <p className="mt-1 text-xs text-slate-400">
                  The prosumer on a booking cannot be changed. Cancel and rebook instead.
                </p>
              </div>
            ) : (
              <TextField
                label="Prosumer NIC"
                name="prosumerNic"
                type="text"
                autoFocus
                placeholder="200012345678"
                value={form.prosumerNic}
                onChange={handleChange}
                error={errors.prosumerNic}
                hint="The account must be an active prosumer."
                disabled={busy}
              />
            )}

            <div>
              <label
                htmlFor="slotId"
                className="mb-1.5 block text-xs font-medium uppercase tracking-[0.08em] text-slate-600"
              >
                Battery slot
              </label>
              <select
                id="slotId"
                name="slotId"
                value={form.slotId}
                onChange={handleChange}
                disabled={busy}
                className={CONTROL_CLASSES}
                aria-invalid={errors.slotId ? 'true' : undefined}
              >
                <option value="">Choose a slot</option>

                {/* When editing, the booking's current slot is no longer "bookable",
                    so it is listed separately to keep it selectable. */}
                {isEditing &&
                reservation &&
                !slots.some((slot) => slot.id === reservation.slotId) ? (
                  <option value={reservation.slotId}>
                    Current slot - {formatDateTime(reservation.reservationStart)}
                  </option>
                ) : null}

                {selectableSlots.map((slot) => (
                  <option key={slot.id} value={slot.id}>
                    {stationNames.get(slot.stationId) ?? 'Node'} - battery{' '}
                    {slot.batterySlotNumber} - {formatDateTime(slot.startTime)} (
                    {formatTimeRange(slot.startTime, slot.endTime)}) - up to {slot.capacityKWh} kWh
                  </option>
                ))}
              </select>

              {errors.slotId ? (
                <p className="mt-1.5 text-xs text-red-600">{errors.slotId}</p>
              ) : (
                <p className="mt-1.5 text-xs leading-relaxed text-slate-400">
                  Only slots that are free, at an active node and start within{' '}
                  {BOOKING_WINDOW_DAYS} days are listed.
                </p>
              )}
            </div>

            <div>
              <label
                htmlFor="type"
                className="mb-1.5 block text-xs font-medium uppercase tracking-[0.08em] text-slate-600"
              >
                Transfer type
              </label>
              <select
                id="type"
                name="type"
                value={form.type}
                onChange={handleChange}
                disabled={busy}
                className={CONTROL_CLASSES}
              >
                <option value="DropOff">Drop-off - deliver surplus energy to the grid</option>
                <option value="Charging">Charging - draw energy from the grid</option>
              </select>
            </div>

            <TextField
              label="Energy (kWh)"
              name="energyKWh"
              type="number"
              step="0.1"
              min="0"
              inputMode="decimal"
              placeholder="12.5"
              value={form.energyKWh}
              onChange={handleChange}
              error={errors.energyKWh}
              hint={
                selectedSlot
                  ? `This slot holds up to ${selectedSlot.capacityKWh} kWh.`
                  : 'Choose a slot to see how much it holds.'
              }
              disabled={busy}
            />
          </div>

          <div className="mt-7 flex justify-end gap-2 border-t border-slate-200 pt-5">
            <Button type="button" variant="secondary" onClick={onClose} disabled={busy}>
              Cancel
            </Button>
            <Button type="submit" disabled={busy}>
              {busy ? 'Saving...' : isEditing ? 'Save changes' : 'Create reservation'}
            </Button>
          </div>
        </form>
      )}
    </Modal>
  )
}
