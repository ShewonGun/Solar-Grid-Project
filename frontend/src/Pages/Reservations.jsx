/*
 * File: Reservations.jsx
 * Purpose: Energy slot reservation management. Lists power trading bookings
 *          with filters for status, node, prosumer and date range, and lets
 *          staff approve a pending booking, edit it or cancel it with a reason.
 *          The 12-hour notice rule is explained here before the user acts, but
 *          it is the Web API that enforces it on every request.
 * Author:  <your name>
 * Created: 2026
 */
import { useEffect, useMemo, useState } from 'react'
import { toast } from 'react-toastify'

import { toApiError } from '../api/client'
import { approveReservation, cancelReservation, searchReservations } from '../api/reservationsApi'
import { getStations } from '../api/stationsApi'
import { IconPlus } from '../Components/Icons'
import {
  Banner,
  Button,
  EmptyState,
  FilterField,
  LoadingState,
  Modal,
  PageHeader,
  Panel,
  RowActions,
  StatusPill,
  TableWrap,
  TD,
  TH,
  Toolbar,
  TR,
} from '../Components/PageControls'
import Pagination from '../Components/Pagination'
import ReservationFormModal from '../Components/ReservationFormModal'
import usePagination from '../hooks/usePagination'
import { MIN_NOTICE_HOURS, formatDateTime, modificationState } from '../utils/reservationRules'

/** The statuses the API can return, for the filter control. */
const STATUSES = ['Pending', 'Approved', 'Completed', 'Cancelled']

/*
 * Status colours are a secondary cue only - every pill also spells the status
 * out, because approved-green and cancelled-red are nearly identical to a
 * reader with deuteranopia.
 */
const STATUS_TONES = {
  Pending: 'warning',
  Approved: 'active',
  Completed: 'inactive',
  Cancelled: 'danger',
}

const EMPTY_FILTERS = { status: '', stationId: '', prosumerNic: '', from: '', to: '' }

/* One row of the reservations table. */
function ReservationRow({ reservation, stationName, busy, onApprove, onCancel, onEdit }) {
  const { allowed, reason } = modificationState(reservation)
  const isPending = reservation.status === 'Pending'

  return (
    <TR>
      <TD className="text-slate-900">
        <span className="block font-medium tabular-nums">
          {formatDateTime(reservation.reservationStart)}
        </span>
        <span className="mt-0.5 block text-xs text-slate-400">{stationName}</span>
      </TD>
      <TD className="tabular-nums">{reservation.prosumerNic}</TD>
      <TD>{reservation.type === 'DropOff' ? 'Drop-off' : 'Charging'}</TD>
      <TD numeric>{reservation.energyKWh} kWh</TD>
      <TD>
        <StatusPill tone={STATUS_TONES[reservation.status]}>{reservation.status}</StatusPill>
      </TD>
      <TD>
        <div className="flex flex-col items-end gap-1">
          <RowActions>
            {isPending ? (
              <Button size="sm" disabled={busy} onClick={() => onApprove(reservation)}>
                Approve
              </Button>
            ) : null}

            <Button
              variant="secondary"
              size="sm"
              disabled={busy || !allowed}
              onClick={() => onEdit(reservation)}
            >
              Edit
            </Button>

            <Button
              variant="danger"
              size="sm"
              disabled={busy || !allowed}
              onClick={() => onCancel(reservation)}
            >
              Cancel
            </Button>
          </RowActions>

          {/* Says why the actions are unavailable, rather than silently greying out. */}
          {allowed ? null : (
            <span className="max-w-xs text-right text-[11px] leading-snug text-slate-400">
              {reason}
            </span>
          )}
        </div>
      </TD>
    </TR>
  )
}

export default function Reservations() {
  const [reservations, setReservations] = useState([])
  const [stations, setStations] = useState([])
  const [filters, setFilters] = useState(EMPTY_FILTERS)
  const [applied, setApplied] = useState(EMPTY_FILTERS)
  const [loading, setLoading] = useState(true)
  // Only the load failure lives on the page; action outcomes go to a toast.
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)
  const [cancelling, setCancelling] = useState(null)
  const [cancelReason, setCancelReason] = useState('')
  // Which booking the dialog is editing: an id, 'new' to create one, or null.
  const [editing, setEditing] = useState(null)
  // Bumped after a save so the list reloads with the change in it.
  const [reloadToken, setReloadToken] = useState(0)

  useEffect(() => {
    let cancelled = false

    /* Loads the node list once, to show node names beside each booking. */
    async function loadStations() {
      try {
        const data = await getStations()

        if (!cancelled) {
          setStations(data)
        }
      } catch {
        // A failure here only costs the node names; the bookings still load.
      }
    }

    loadStations()

    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    let cancelled = false

    /*
     * Runs the search with the filters the user applied. Every filter is passed
     * to the Web API rather than applied in the browser, so the list always
     * reflects the service's own view of the data.
     */
    async function loadReservations() {
      try {
        const data = await searchReservations({
          status: applied.status || undefined,
          stationId: applied.stationId || undefined,
          prosumerNic: applied.prosumerNic.trim() || undefined,
          from: applied.from ? new Date(applied.from) : undefined,
          to: applied.to ? new Date(applied.to) : undefined,
        })

        if (!cancelled) {
          setReservations(data)
          setError('')
        }
      } catch (failure) {
        if (!cancelled) {
          setError(toApiError(failure).message)
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    loadReservations()

    return () => {
      cancelled = true
    }
  }, [applied, reloadToken])

  const stationNames = useMemo(
    () => new Map(stations.map((station) => [station.id, station.stationName])),
    [stations],
  )

  /* Reloads the list and reports what changed after the dialog saves. */
  function handleSaved(message) {
    toast.success(message)
    setReloadToken((current) => current + 1)
  }

  /* Keeps one filter control in state without running the search yet. */
  function handleFilterChange(event) {
    const { name, value } = event.target
    setFilters((current) => ({ ...current, [name]: value }))
  }

  /* Runs the search with the current filters. */
  function handleApplyFilters(event) {
    event.preventDefault()
    setApplied(filters)
  }

  /* Clears every filter and reloads the full list. */
  function handleClearFilters() {
    setFilters(EMPTY_FILTERS)
    setApplied(EMPTY_FILTERS)
  }

  /*
   * Approves a pending booking. The API issues the QR token on approval and
   * refuses anything that is not still pending.
   */
  async function handleApprove(reservation) {
    setBusyId(reservation.id)

    try {
      const updated = await approveReservation(reservation.id)

      setReservations((current) =>
        current.map((item) => (item.id === updated.id ? updated : item)),
      )
      toast.success(`Reservation for NIC ${updated.prosumerNic} approved.`)
    } catch (failure) {
      toast.error(toApiError(failure).message)
    } finally {
      setBusyId(null)
    }
  }

  /*
   * Cancels the booking chosen in the dialog. The API applies the 12-hour
   * notice rule again and its refusal is shown as sent.
   */
  async function handleConfirmCancel() {
    const reservation = cancelling

    setBusyId(reservation.id)

    try {
      const updated = await cancelReservation(reservation.id, cancelReason.trim() || undefined)

      setReservations((current) =>
        current.map((item) => (item.id === updated.id ? updated : item)),
      )
      toast.success(`Reservation for NIC ${updated.prosumerNic} cancelled.`)
      setCancelling(null)
      setCancelReason('')
    } catch (failure) {
      toast.error(toApiError(failure).message)
      setCancelling(null)
    } finally {
      setBusyId(null)
    }
  }

  // Paged in the browser: no endpoint on the Web API takes a page parameter.
  const pagination = usePagination(reservations)

  return (
    <>
      <PageHeader
        title="Energy reservations"
        description={`Power trading bookings. Changes and cancellations need at least ${MIN_NOTICE_HOURS} hours' notice.`}
      >
        <Button onClick={() => setEditing('new')}>
          <IconPlus />
          New reservation
        </Button>
      </PageHeader>

      <Banner tone="error">{error}</Banner>

      {loading ? (
        <LoadingState label="Loading reservations..." />
      ) : (
        <Panel flush>
          <Toolbar onSubmit={handleApplyFilters}>
            <FilterField
              label="Status"
              as="select"
              name="status"
              value={filters.status}
              onChange={handleFilterChange}
              className="w-36"
            >
              <option value="">Any status</option>
              {STATUSES.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </FilterField>

            <FilterField
              label="Node"
              as="select"
              name="stationId"
              value={filters.stationId}
              onChange={handleFilterChange}
              className="w-44"
            >
              <option value="">Any node</option>
              {stations.map((station) => (
                <option key={station.id} value={station.id}>
                  {station.stationName}
                </option>
              ))}
            </FilterField>

            <FilterField
              label="Prosumer NIC"
              type="search"
              name="prosumerNic"
              value={filters.prosumerNic}
              onChange={handleFilterChange}
              placeholder="200012345678"
              className="w-40"
            />

            <FilterField
              label="From"
              type="date"
              name="from"
              value={filters.from}
              onChange={handleFilterChange}
              className="w-36"
            />

            <FilterField
              label="To"
              type="date"
              name="to"
              value={filters.to}
              onChange={handleFilterChange}
              className="w-36"
            />

            <div className="flex h-9 items-center gap-1.5">
              <Button type="submit" size="sm">
                Apply
              </Button>
              <Button type="button" variant="ghost" size="sm" onClick={handleClearFilters}>
                Clear
              </Button>
            </div>

            <span className="ml-auto flex h-9 items-center text-xs tabular-nums text-slate-400">
              {reservations.length} bookings
            </span>
          </Toolbar>

          {reservations.length === 0 ? (
            <EmptyState
              title="No reservations found"
              description="Nothing matches these filters. Try widening the date range or clearing them."
            >
              <Button onClick={() => setEditing('new')}>
                <IconPlus />
                New reservation
              </Button>
            </EmptyState>
          ) : (
            <TableWrap minWidth="62rem">
              <thead>
                <tr>
                  <TH>Starts</TH>
                  <TH>Prosumer</TH>
                  <TH>Type</TH>
                  <TH align="right">Energy</TH>
                  <TH>Status</TH>
                  <TH align="right">Actions</TH>
                </tr>
              </thead>
              <tbody>
                {pagination.pageItems.map((reservation) => (
                  <ReservationRow
                    key={reservation.id}
                    reservation={reservation}
                    stationName={stationNames.get(reservation.stationId) ?? 'Unknown node'}
                    busy={busyId === reservation.id}
                    onApprove={handleApprove}
                    onCancel={(item) => {
                      setCancelReason('')
                      setCancelling(item)
                    }}
                    onEdit={(item) => setEditing(item.id)}
                  />
                ))}
              </tbody>
            </TableWrap>
          )}

          {reservations.length > 0 ? (
            <Pagination
              {...pagination}
              onPageChange={pagination.setPage}
              onPageSizeChange={pagination.setPageSize}
              noun="bookings"
            />
          ) : null}
        </Panel>
      )}

      {cancelling ? (
        <Modal
          title="Cancel this reservation?"
          description={`Booking for NIC ${cancelling.prosumerNic} starting ${formatDateTime(
            cancelling.reservationStart,
          )}. The battery slot is released back for booking.`}
          onClose={() => setCancelling(null)}
        >
          <label className="block">
            <span className="text-[11px] font-medium uppercase tracking-[0.07em] text-slate-500">
              Reason
            </span>
            <textarea
              value={cancelReason}
              onChange={(event) => setCancelReason(event.target.value)}
              rows={3}
              placeholder="Optional - recorded against the cancellation"
              className="mt-1 w-full rounded-xs border border-slate-300 px-2.5 py-2 text-sm outline-none transition-colors placeholder:text-slate-400 hover:border-slate-400 focus:border-slate-900 focus:ring-1 focus:ring-slate-900"
            />
          </label>

          <div className="mt-5 flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setCancelling(null)}>
              Keep booking
            </Button>
            <Button
              variant="danger"
              disabled={busyId === cancelling.id}
              onClick={handleConfirmCancel}
            >
              {busyId === cancelling.id ? 'Cancelling...' : 'Cancel reservation'}
            </Button>
          </div>
        </Modal>
      ) : null}

      {editing ? (
        <ReservationFormModal
          reservationId={editing === 'new' ? undefined : editing}
          onClose={() => setEditing(null)}
          onSaved={handleSaved}
        />
      ) : null}
    </>
  )
}
