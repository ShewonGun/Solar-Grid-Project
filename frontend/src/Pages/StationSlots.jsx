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
import toast from 'react-hot-toast'

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
  PageHeader,
  Panel,
  TableWrap,
  TH,
  Toolbar,
} from '../Components/PageControls'
import ConfirmDialog from '../Components/ConfirmDialog'
import Pagination from '../Components/Pagination'
import { SlotCard, SlotRow } from '../Components/SlotRow'
import SlotFormModal from '../Components/SlotFormModal'
import usePagination from '../hooks/usePagination'
import { newestFirst } from '../utils/sorting'
import { formatDateTime } from '../utils/reservationRules'

/** Slot statuses the API can return, for the filter control. */
const STATUSES = [
  { value: 'Available', label: 'Available' },
  { value: 'Reserved', label: 'Reserved' },
  { value: 'Unavailable', label: 'Unavailable' },
]

const EMPTY_FILTERS = { status: '', from: '', to: '' }

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
  // The slot awaiting hold confirmation.
  const [holding, setHolding] = useState(null)
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

  /* Puts a held slot back into service. The API refuses this on a reserved slot. */
  async function handleRelease(slot) {
    setBusyId(slot.id)

    try {
      const updated = await setSlotAvailability(slot.id, true)

      setSlots((current) => current.map((item) => (item.id === updated.id ? updated : item)))
      toast.success(`Battery slot ${updated.batterySlotNumber} is available again.`)
    } catch (failure) {
      toast.error(toApiError(failure).message)
    } finally {
      setBusyId(null)
    }
  }

  /* Holds the slot chosen in the dialog, taking it out of service. */
  async function handleConfirmHold() {
    const slot = holding

    setBusyId(slot.id)

    try {
      const updated = await setSlotAvailability(slot.id, false)

      setSlots((current) => current.map((item) => (item.id === updated.id ? updated : item)))
      toast.success(`Battery slot ${updated.batterySlotNumber} held out of service.`)
      setHolding(null)
    } catch (failure) {
      toast.error(toApiError(failure).message)
      setHolding(null)
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
              className="sm:w-40"
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
              className="sm:w-36"
            />

            <FilterField
              label="To"
              type="date"
              name="to"
              value={filters.to}
              onChange={handleFilterChange}
              className="sm:w-36"
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
            <>
              {/* Table at "lg" and above; a purpose-built card list below it -
                  see SlotRow.jsx for why this page gets its own card rather
                  than the generic label/value stacking every other table
                  falls back to. */}
              <div className="hidden lg:block">
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
                        onRequestHold={setHolding}
                        onRelease={handleRelease}
                        onDelete={setDeleting}
                      />
                    ))}
                  </tbody>
                </TableWrap>
              </div>

              <div className="grid gap-3 p-4 lg:hidden">
                {pagination.pageItems.map((slot) => (
                  <SlotCard
                    key={slot.id}
                    slot={slot}
                    busy={busyId === slot.id}
                    onEdit={(item) => setEditing(item.id)}
                    onRequestHold={setHolding}
                    onRelease={handleRelease}
                    onDelete={setDeleting}
                  />
                ))}
              </div>
            </>
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
        <ConfirmDialog
          title="Delete this battery slot?"
          description={`Battery ${deleting.batterySlotNumber}, ${formatDateTime(
            deleting.startTime,
          )}. This cannot be undone.`}
          confirmLabel="Delete slot"
          pendingLabel="Deleting..."
          cancelLabel="Keep slot"
          busy={busyId === deleting.id}
          onConfirm={handleConfirmDelete}
          onClose={() => setDeleting(null)}
        />
      ) : null}

      {holding ? (
        <ConfirmDialog
          title="Hold this battery slot?"
          description={`Battery ${holding.batterySlotNumber}, ${formatDateTime(
            holding.startTime,
          )}. It will not be bookable until you release it again.`}
          confirmLabel="Hold slot"
          pendingLabel="Holding..."
          cancelLabel="Keep available"
          tone="primary"
          busy={busyId === holding.id}
          onConfirm={handleConfirmHold}
          onClose={() => setHolding(null)}
        />
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
