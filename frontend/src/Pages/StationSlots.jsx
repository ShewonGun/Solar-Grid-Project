/*
 * File: StationSlots.jsx
 * Purpose: Battery slot management for one microgrid node. Grid Operators open
 *          bookable windows on the node's battery slots, change availability
 *          and remove slots. A slot that a prosumer has already reserved is
 *          locked by the Web API - it cannot be edited, held or deleted until
 *          the reservation is cancelled, and this screen says so.
 * Author:  <your name>
 * Created: 2026
 */
import { useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { toast } from 'react-toastify'

import { toApiError } from '../api/client'
import { deleteSlot, getStationSlots, setSlotAvailability } from '../api/slotsApi'
import { getStation } from '../api/stationsApi'
import { IconPlus } from '../Components/Icons'
import {
  Banner,
  Button,
  ButtonLink,
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
import SlotFormModal from '../Components/SlotFormModal'
import usePagination from '../hooks/usePagination'
import { newestFirst } from '../utils/sorting'
import { formatDateTime, formatTimeRange } from '../utils/reservationRules'

/** Slot statuses the API can return, for the filter control. */
const STATUSES = [
  { value: 'Available', label: 'Available' },
  { value: 'Reserved', label: 'Reserved' },
  { value: 'Unavailable', label: 'Unavailable' },
]

/*
 * Status colours are a secondary cue only - every pill also spells the status
 * out, so the table never depends on telling two colours apart.
 */
const STATUS_TONES = {
  Available: 'active',
  Reserved: 'warning',
  Unavailable: 'inactive',
}

const EMPTY_FILTERS = { status: '', from: '', to: '' }

/* One row of the battery slot table. */
function SlotRow({ slot, busy, onEdit, onToggleAvailability, onDelete }) {
  // A reserved slot is locked by the service until its booking is cancelled.
  const isReserved = slot.status === 'Reserved'
  const isAvailable = slot.status === 'Available'

  return (
    <TR>
      <TD numeric className="font-medium text-slate-900">
        {slot.batterySlotNumber}
      </TD>
      <TD className="text-slate-900">
        <span className="block font-medium tabular-nums">{formatDateTime(slot.startTime)}</span>
        <span className="mt-0.5 block text-xs tabular-nums text-slate-400">
          {formatTimeRange(slot.startTime, slot.endTime)}
        </span>
      </TD>
      <TD numeric>{slot.capacityKWh} kWh</TD>
      <TD>
        <StatusPill tone={STATUS_TONES[slot.status]}>{slot.status}</StatusPill>
      </TD>
      <TD className="tabular-nums">
        {slot.updatedBy || <span className="text-slate-300">-</span>}
      </TD>
      <TD>
        <div className="flex flex-col items-end gap-1">
          <RowActions>
            <Button
              variant="secondary"
              size="sm"
              disabled={busy || isReserved}
              onClick={() => onEdit(slot)}
            >
              Edit
            </Button>

            <Button
              variant="secondary"
              size="sm"
              disabled={busy || isReserved}
              onClick={() => onToggleAvailability(slot)}
            >
              {isAvailable ? 'Hold' : 'Release'}
            </Button>

            <Button
              variant="danger"
              size="sm"
              disabled={busy || isReserved}
              onClick={() => onDelete(slot)}
            >
              Delete
            </Button>
          </RowActions>

          {isReserved ? (
            <span className="text-[11px] leading-snug text-slate-400">
              Reserved - cancel the booking first
            </span>
          ) : null}
        </div>
      </TD>
    </TR>
  )
}

export default function StationSlots() {
  const { id } = useParams()

  const [station, setStation] = useState(null)
  const [slots, setSlots] = useState([])
  const [filters, setFilters] = useState(EMPTY_FILTERS)
  const [applied, setApplied] = useState(EMPTY_FILTERS)
  const [loading, setLoading] = useState(true)
  // Only the load failure lives on the page; action outcomes go to a toast.
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)
  // Which slot the dialog is editing: an id, 'new' to open one, or null.
  const [editing, setEditing] = useState(null)
  // The slot awaiting delete confirmation.
  const [deleting, setDeleting] = useState(null)
  // Bumped after a change so the list reloads with it in.
  const [reloadToken, setReloadToken] = useState(0)

  useEffect(() => {
    let cancelled = false

    /* Loads the node, so its name and limits can head the screen. */
    async function loadStation() {
      try {
        const data = await getStation(id)

        if (!cancelled) {
          setStation(data)
        }
      } catch (failure) {
        if (!cancelled) {
          setError(toApiError(failure).message)
        }
      }
    }

    loadStation()

    return () => {
      cancelled = true
    }
  }, [id])

  useEffect(() => {
    let cancelled = false

    /* Loads this node's slots for the applied filters. */
    async function loadSlots() {
      try {
        const data = await getStationSlots(id, {
          status: applied.status || undefined,
          from: applied.from ? new Date(applied.from) : undefined,
          to: applied.to ? new Date(applied.to) : undefined,
        })

        if (!cancelled) {
          setSlots(data)
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

    loadSlots()

    return () => {
      cancelled = true
    }
  }, [id, applied, reloadToken])

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
   * Takes a slot out of service or puts it back. The API refuses either on a
   * slot a prosumer has already reserved.
   */
  async function handleToggleAvailability(slot) {
    setBusyId(slot.id)

    try {
      const updated = await setSlotAvailability(slot.id, slot.status !== 'Available')

      setSlots((current) => current.map((item) => (item.id === updated.id ? updated : item)))
      toast.success(
        `Battery slot ${updated.batterySlotNumber} is now ${
          updated.status === 'Available' ? 'available' : 'held out of service'
        }.`,
      )
    } catch (failure) {
      toast.error(toApiError(failure).message)
    } finally {
      setBusyId(null)
    }
  }

  /* Removes the slot chosen in the dialog. */
  async function handleConfirmDelete() {
    const slot = deleting

    setBusyId(slot.id)

    try {
      await deleteSlot(slot.id)

      setSlots((current) => current.filter((item) => item.id !== slot.id))
      toast.success(`Battery slot ${slot.batterySlotNumber} removed.`)
      setDeleting(null)
    } catch (failure) {
      toast.error(toApiError(failure).message)
      setDeleting(null)
    } finally {
      setBusyId(null)
    }
  }

  // The API returns slots in schedule order; the table shows the most recently
  // opened slot first, so a slot just created is the first row.
  const orderedSlots = useMemo(() => newestFirst(slots), [slots])

  // Paged in the browser: no endpoint on the Web API takes a page parameter.
  const pagination = usePagination(orderedSlots)

  return (
    <>
      <PageHeader
        title={station ? `${station.stationName} - battery slots` : 'Battery slots'}
        description={
          station
            ? `${station.totalBatterySlots} battery slots, up to ${station.capacityKWh} kWh. Open a window on a slot to make it bookable.`
            : 'Bookable windows on this node’s battery slots.'
        }
      >
        <ButtonLink to="/stations" variant="secondary">
          All nodes
        </ButtonLink>
        {station ? (
          <Button onClick={() => setEditing('new')}>
            <IconPlus />
            Open slot
          </Button>
        ) : null}
      </PageHeader>

      <Banner tone="error">{error}</Banner>

      {/* A deactivated node refuses new slots, so say it once at the top. */}
      {station && !station.isActive ? (
        <Banner tone="info">
          This node is deactivated. Its slots cannot be booked and new ones cannot be opened until
          it is activated again.
        </Banner>
      ) : null}

      {loading ? (
        <LoadingState label="Loading battery slots..." />
      ) : (
        <Panel flush>
          <Toolbar onSubmit={handleApplyFilters}>
            <FilterField
              label="Status"
              as="select"
              name="status"
              value={filters.status}
              onChange={handleFilterChange}
              className="w-40"
            >
              <option value="">Any status</option>
              {STATUSES.map((status) => (
                <option key={status.value} value={status.value}>
                  {status.label}
                </option>
              ))}
            </FilterField>

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
              {slots.length} slots
            </span>
          </Toolbar>

          {slots.length === 0 ? (
            <EmptyState
              title="No battery slots"
              description="Open a window on one of this node's battery slots so prosumers can book it."
            >
              {station ? (
                <Button onClick={() => setEditing('new')}>
                  <IconPlus />
                  Open slot
                </Button>
              ) : null}
            </EmptyState>
          ) : (
            <TableWrap minWidth="58rem">
              <thead>
                <tr>
                  <TH align="right">Battery</TH>
                  <TH>Window</TH>
                  <TH align="right">Capacity</TH>
                  <TH>Status</TH>
                  <TH>Updated by</TH>
                  <TH align="right">Actions</TH>
                </tr>
              </thead>
              <tbody>
                {pagination.pageItems.map((slot) => (
                  <SlotRow
                    key={slot.id}
                    slot={slot}
                    busy={busyId === slot.id}
                    onEdit={(item) => setEditing(item.id)}
                    onToggleAvailability={handleToggleAvailability}
                    onDelete={setDeleting}
                  />
                ))}
              </tbody>
            </TableWrap>
          )}

          {slots.length > 0 ? (
            <Pagination
              {...pagination}
              onPageChange={pagination.setPage}
              onPageSizeChange={pagination.setPageSize}
              noun="slots"
            />
          ) : null}
        </Panel>
      )}

      {deleting ? (
        <Modal
          title="Delete this battery slot?"
          description={`Battery ${deleting.batterySlotNumber}, ${formatDateTime(
            deleting.startTime,
          )}. This cannot be undone.`}
          onClose={() => setDeleting(null)}
        >
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setDeleting(null)}>
              Keep slot
            </Button>
            <Button
              variant="danger"
              disabled={busyId === deleting.id}
              onClick={handleConfirmDelete}
            >
              {busyId === deleting.id ? 'Deleting...' : 'Delete slot'}
            </Button>
          </div>
        </Modal>
      ) : null}

      {editing && station ? (
        <SlotFormModal
          station={station}
          slotId={editing === 'new' ? undefined : editing}
          onClose={() => setEditing(null)}
          onSaved={handleSaved}
        />
      ) : null}
    </>
  )
}
