/*
 * File: Stations.jsx
 * Purpose: Microgrid node management. Lists every solar grid hub with its GPS
 *          location, capacity, battery slots and schedule, and lets a
 *          Back-office officer register, edit, activate and deactivate them.
 *          Deactivation is refused by the Web API while a node still has active
 *          energy reservations; that refusal is shown as the service sends it.
 * Author:  <your name>
 * Created: 2026
 */
import { useEffect, useMemo, useState } from 'react'
import toast from 'react-hot-toast'

import { toApiError } from '../api/client'
import { deleteStation, getStations, setStationActive } from '../api/stationsApi'
import useSession from '../auth/useSession'
import { IconPlus, IconSearch } from '../Components/Icons'
import {
  Banner,
  Button,
  ButtonLink,
  EmptyState,
  FilterField,
  LoadingState,
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
import ConfirmDialog from '../Components/ConfirmDialog'
import Pagination from '../Components/Pagination'
import StationFormModal from '../Components/StationFormModal'
import usePagination from '../hooks/usePagination'
import { newestFirst } from '../utils/sorting'

/* Formats a coordinate pair for the table. */
function formatLocation(station) {
  return `${station.latitude.toFixed(4)}, ${station.longitude.toFixed(4)}`
}

/* One row of the stations table. */
function StationRow({ station, canManage, busy, onRequestDeactivate, onActivate, onEdit, onDelete }) {
  return (
    <TR>
      <TD className="text-slate-900">
        <span className="block font-medium">{station.stationName}</span>
        <span className="mt-0.5 block text-xs tabular-nums text-slate-400">
          {formatLocation(station)}
        </span>
      </TD>
      <TD numeric>{station.capacityKWh} kWh</TD>
      <TD numeric>{station.totalBatterySlots}</TD>
      <TD>
        {station.operatingSchedule || <span className="text-slate-300">Not set</span>}
      </TD>
      <TD>
        <StatusPill tone={station.isActive ? 'active' : 'inactive'}>
          {station.isActive ? 'Active' : 'Inactive'}
        </StatusPill>
      </TD>
      <TD>
        <RowActions>
          <ButtonLink to={`/stations/${station.id}/slots`} variant="secondary" size="sm">
            Slots
          </ButtonLink>

          {canManage ? (
            <>
              <Button variant="secondary" size="sm" onClick={() => onEdit(station)}>
                Edit
              </Button>
              {/* Deactivating needs confirming; reactivating does not, since it
                  is the reversible, low-risk direction. */}
              <Button
                variant={station.isActive ? 'danger' : 'secondary'}
                size="sm"
                disabled={busy}
                onClick={() =>
                  station.isActive ? onRequestDeactivate(station) : onActivate(station)
                }
              >
                {station.isActive ? 'Deactivate' : 'Activate'}
              </Button>
              <Button
                variant="danger"
                size="sm"
                disabled={busy}
                onClick={() => onDelete(station)}
              >
                Delete
              </Button>
            </>
          ) : null}
        </RowActions>
      </TD>
    </TR>
  )
}

export default function Stations() {
  const { role } = useSession()
  const canManage = role === 'Backoffice'

  const [stations, setStations] = useState([])
  const [loading, setLoading] = useState(true)
  // Only the load failure lives on the page; action outcomes go to a toast.
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)
  const [search, setSearch] = useState('')
  const [activeOnly, setActiveOnly] = useState(false)
  // Which node the dialog is editing: an id, 'new' to register one, or null.
  const [editing, setEditing] = useState(null)
  // The node awaiting delete confirmation.
  const [deleting, setDeleting] = useState(null)
  // The node awaiting deactivate confirmation.
  const [deactivating, setDeactivating] = useState(null)
  // Bumped after a save so the list reloads with the change in it.
  const [reloadToken, setReloadToken] = useState(0)

  useEffect(() => {
    let cancelled = false

    /*
     * Loads the node list for the current active-only filter. State is only
     * touched after the request settles, and skipped entirely when the filter
     * changed again while this request was still in flight.
     */
    async function loadStations() {
      try {
        const data = await getStations(activeOnly)

        if (!cancelled) {
          setStations(data)
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

    loadStations()

    return () => {
      cancelled = true
    }
  }, [activeOnly, reloadToken])

  /* Reloads the list and reports what changed after the dialog saves. */
  function handleSaved(message) {
    toast.success(message)
    setReloadToken((current) => current + 1)
  }

  /* Reactivates a node. This direction needs no confirmation - it only ever
   * puts capability back, never takes it away. */
  async function handleActivate(station) {
    setBusyId(station.id)

    try {
      const updated = await setStationActive(station.id, true)

      setStations((current) =>
        current.map((item) => (item.id === updated.id ? updated : item)),
      )
      toast.success(`${updated.stationName} is now active.`)
    } catch (failure) {
      toast.error(toApiError(failure).message)
    } finally {
      setBusyId(null)
    }
  }

  /* Deactivates the node chosen in the dialog. */
  async function handleConfirmDeactivate() {
    const station = deactivating

    setBusyId(station.id)

    try {
      const updated = await setStationActive(station.id, false)

      setStations((current) =>
        current.map((item) => (item.id === updated.id ? updated : item)),
      )
      toast.success(`${updated.stationName} is now inactive.`)
      setDeactivating(null)
    } catch (failure) {
      toast.error(toApiError(failure).message)
      setDeactivating(null)
    } finally {
      setBusyId(null)
    }
  }

  /*
   * Removes the node chosen in the dialog. The API refuses to delete one that
   * any reservation still references and asks for it to be deactivated
   * instead; that refusal is shown to the user as the service sends it.
   */
  async function handleConfirmDelete() {
    const station = deleting

    setBusyId(station.id)

    try {
      await deleteStation(station.id)

      setStations((current) => current.filter((item) => item.id !== station.id))
      toast.success(`${station.stationName} removed.`)
      setDeleting(null)
    } catch (failure) {
      toast.error(toApiError(failure).message)
      setDeleting(null)
    } finally {
      setBusyId(null)
    }
  }

  // Filtering by name happens in the browser: the list endpoint has no search
  // parameter, and a site's node list is small enough to filter client-side.
  // The API returns nodes alphabetically; the table shows newest first.
  const visibleStations = useMemo(() => {
    const term = search.trim().toLowerCase()
    const matches = term
      ? stations.filter((station) => station.stationName.toLowerCase().includes(term))
      : stations

    return newestFirst(matches)
  }, [stations, search])

  // Paged in the browser: no endpoint on the Web API takes a page parameter.
  const pagination = usePagination(visibleStations)

  return (
    <>
      <PageHeader
        title="Microgrid nodes"
        description="Solar grid hubs with their GPS location, capacity and battery storage slots."
      >
        {canManage ? (
          <Button onClick={() => setEditing('new')}>
            <IconPlus />
            Register node
          </Button>
        ) : null}
      </PageHeader>

      <Banner tone="error">{error}</Banner>

      {loading ? (
        <LoadingState label="Loading microgrid nodes..." />
      ) : (
        <Panel flush>
          <Toolbar>
            <FilterField
              label="Search"
              type="search"
              icon={IconSearch}
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Node name"
              className="w-56"
            />

            <label className="flex h-9 items-center gap-2 text-sm text-slate-600">
              <input
                type="checkbox"
                checked={activeOnly}
                onChange={(event) => setActiveOnly(event.target.checked)}
                className="h-3.5 w-3.5 rounded-xs border-slate-300 text-slate-900 focus:ring-slate-900"
              />
              Active only
            </label>

            <span className="ml-auto flex h-9 items-center text-xs tabular-nums text-slate-400">
              {visibleStations.length} of {stations.length} nodes
            </span>
          </Toolbar>

          {visibleStations.length === 0 ? (
            <EmptyState
              title={
                stations.length === 0 ? 'No microgrid nodes yet' : 'No nodes match that search'
              }
              description={
                stations.length === 0
                  ? 'Register a solar grid hub to start scheduling battery slots.'
                  : 'Try a different name, or clear the filters.'
              }
            >
              {canManage && stations.length === 0 ? (
                <Button onClick={() => setEditing('new')}>
                  <IconPlus />
                  Register node
                </Button>
              ) : null}
            </EmptyState>
          ) : (
            <TableWrap minWidth="58rem">
              <thead>
                <tr>
                  <TH>Node</TH>
                  <TH align="right">Capacity</TH>
                  <TH align="right">Slots</TH>
                  <TH>Schedule</TH>
                  <TH>Status</TH>
                  <TH align="right">Actions</TH>
                </tr>
              </thead>
              <tbody>
                {pagination.pageItems.map((station) => (
                  <StationRow
                    key={station.id}
                    station={station}
                    canManage={canManage}
                    busy={busyId === station.id}
                    onRequestDeactivate={setDeactivating}
                    onActivate={handleActivate}
                    onEdit={(item) => setEditing(item.id)}
                    onDelete={setDeleting}
                  />
                ))}
              </tbody>
            </TableWrap>
          )}

          {visibleStations.length > 0 ? (
            <Pagination
              {...pagination}
              onPageChange={pagination.setPage}
              onPageSizeChange={pagination.setPageSize}
              noun="nodes"
            />
          ) : null}
        </Panel>
      )}

      {deleting ? (
        <ConfirmDialog
          title="Delete this microgrid node?"
          description={`${deleting.stationName} and its battery slots are removed. This cannot be undone.`}
          confirmLabel="Delete node"
          pendingLabel="Deleting..."
          cancelLabel="Keep node"
          busy={busyId === deleting.id}
          onConfirm={handleConfirmDelete}
          onClose={() => setDeleting(null)}
        >
          <Banner tone="info">
            A node that any reservation still refers to cannot be deleted. Deactivate it instead to
            take it out of service while keeping its booking history.
          </Banner>
        </ConfirmDialog>
      ) : null}

      {deactivating ? (
        <ConfirmDialog
          title="Deactivate this node?"
          description={`${deactivating.stationName}'s battery slots stop taking bookings until it is activated again. Existing reservations are not affected.`}
          confirmLabel="Deactivate node"
          pendingLabel="Deactivating..."
          cancelLabel="Keep active"
          tone="primary"
          busy={busyId === deactivating.id}
          onConfirm={handleConfirmDeactivate}
          onClose={() => setDeactivating(null)}
        />
      ) : null}

      {editing ? (
        <StationFormModal
          stationId={editing === 'new' ? undefined : editing}
          onClose={() => setEditing(null)}
          onSaved={handleSaved}
        />
      ) : null}
    </>
  )
}
