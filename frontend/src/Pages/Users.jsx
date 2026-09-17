/*
 * File: Users.jsx
 * Purpose: User management for Back-office officers. Lists console users and
 *          solar prosumers with filters for role, status and a name/NIC/email
 *          search, and creates, edits, activates and deactivates accounts.
 *          Deactivating a prosumer also cancels the bookings they still hold,
 *          which the confirmation says before the action is taken.
 * Author:  <your name>
 * Created: 2026
 */
import { useEffect, useMemo, useState } from 'react'
import { toast } from 'react-toastify'

import { toApiError } from '../api/client'
import { activateUser, deactivateUser, getUsers } from '../api/usersApi'
import useSession from '../auth/useSession'
import { IconPlus, IconSearch } from '../Components/Icons'
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
import UserFormModal from '../Components/UserFormModal'
import usePagination from '../hooks/usePagination'
import { newestFirst } from '../utils/sorting'

/** Roles the API can return, for the filter control. */
const ROLES = [
  { value: 'Backoffice', label: 'Back-office officer' },
  { value: 'GridOperator', label: 'Grid operator' },
  { value: 'Prosumer', label: 'Solar prosumer' },
]

/** Account statuses the API can return, for the filter control. */
const STATUSES = [
  { value: 'PendingActivation', label: 'Pending activation' },
  { value: 'Active', label: 'Active' },
  { value: 'DeactivationRequested', label: 'Deactivation requested' },
  { value: 'Deactivated', label: 'Deactivated' },
]

/*
 * Status colours are a secondary cue only - every pill also spells the status
 * out, because several of these are hard to tell apart for a reader with
 * red-green colour blindness.
 */
const STATUS_TONES = {
  Active: 'active',
  PendingActivation: 'warning',
  DeactivationRequested: 'warning',
  Deactivated: 'danger',
}

const STATUS_LABELS = {
  Active: 'Active',
  PendingActivation: 'Pending activation',
  DeactivationRequested: 'Deactivation requested',
  Deactivated: 'Deactivated',
}

const ROLE_LABELS = {
  Backoffice: 'Back-office officer',
  GridOperator: 'Grid operator',
  Prosumer: 'Solar prosumer',
}

const EMPTY_FILTERS = { role: '', status: '', search: '' }

/* One row of the users table. */
function UserRow({ user, isSelf, busy, onEdit, onActivate, onDeactivate }) {
  const isActive = user.status === 'Active'
  const canActivate = !isActive
  // The API refuses to let an officer deactivate their own account.
  const canDeactivate = user.status !== 'Deactivated' && !isSelf

  return (
    <TR>
      <TD className="text-slate-900">
        <span className="block font-medium">{user.fullName}</span>
        <span className="mt-0.5 block text-xs tabular-nums text-slate-400">NIC {user.nic}</span>
      </TD>
      <TD>
        <span className="block">{user.email}</span>
        {user.phone ? (
          <span className="mt-0.5 block text-xs tabular-nums text-slate-400">{user.phone}</span>
        ) : null}
      </TD>
      <TD>{ROLE_LABELS[user.role]}</TD>
      <TD>
        <StatusPill tone={STATUS_TONES[user.status]}>{STATUS_LABELS[user.status]}</StatusPill>
      </TD>
      <TD>
        <div className="flex flex-col items-end gap-1">
          <RowActions>
            <Button variant="secondary" size="sm" onClick={() => onEdit(user)}>
              Edit
            </Button>

            {canActivate ? (
              <Button size="sm" disabled={busy} onClick={() => onActivate(user)}>
                Activate
              </Button>
            ) : null}

            {canDeactivate ? (
              <Button
                variant="danger"
                size="sm"
                disabled={busy}
                onClick={() => onDeactivate(user)}
              >
                Deactivate
              </Button>
            ) : null}
          </RowActions>

          {isSelf ? (
            <span className="text-[11px] text-slate-400">Your own account</span>
          ) : null}
        </div>
      </TD>
    </TR>
  )
}

export default function Users() {
  const { user: signedInUser } = useSession()

  const [users, setUsers] = useState([])
  const [filters, setFilters] = useState(EMPTY_FILTERS)
  const [applied, setApplied] = useState(EMPTY_FILTERS)
  const [loading, setLoading] = useState(true)
  // Only the load failure lives on the page; action outcomes go to a toast.
  const [error, setError] = useState('')
  const [busyNic, setBusyNic] = useState(null)
  // Which account the dialog is editing: a NIC, 'new' to create one, or null.
  const [editing, setEditing] = useState(null)
  // The account awaiting deactivation confirmation.
  const [deactivating, setDeactivating] = useState(null)
  // Bumped after a change so the list reloads with it in.
  const [reloadToken, setReloadToken] = useState(0)

  useEffect(() => {
    let cancelled = false

    /*
     * Runs the search with the filters the user applied. The role, status and
     * search terms all go to the Web API rather than filtering a cached array,
     * so the list always reflects the service's own view.
     */
    async function loadUsers() {
      try {
        const data = await getUsers({
          role: applied.role || undefined,
          status: applied.status || undefined,
          search: applied.search.trim() || undefined,
        })

        if (!cancelled) {
          setUsers(data)
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

    loadUsers()

    return () => {
      cancelled = true
    }
  }, [applied, reloadToken])

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
   * Activates or reactivates an account. Only a Back-office officer may do
   * this, which the API enforces; it also refuses an account that is already
   * active.
   */
  async function handleActivate(user) {
    setBusyNic(user.nic)

    try {
      const updated = await activateUser(user.nic)

      setUsers((current) => current.map((item) => (item.nic === updated.nic ? updated : item)))
      toast.success(`${updated.fullName} is now active.`)
    } catch (failure) {
      toast.error(toApiError(failure).message)
    } finally {
      setBusyNic(null)
    }
  }

  /*
   * Deactivates the account chosen in the dialog. The service cancels any
   * bookings the prosumer still holds as part of the same call.
   */
  async function handleConfirmDeactivate() {
    const user = deactivating

    setBusyNic(user.nic)

    try {
      const updated = await deactivateUser(user.nic)

      setUsers((current) => current.map((item) => (item.nic === updated.nic ? updated : item)))
      toast.success(`${updated.fullName} has been deactivated.`)
      setDeactivating(null)
    } catch (failure) {
      toast.error(toApiError(failure).message)
      setDeactivating(null)
    } finally {
      setBusyNic(null)
    }
  }

  // The API returns accounts alphabetically; the table shows newest first.
  const orderedUsers = useMemo(() => newestFirst(users), [users])

  // Paged in the browser: no endpoint on the Web API takes a page parameter.
  const pagination = usePagination(orderedUsers)

  return (
    <>
      <PageHeader
        title="Users"
        description="Console users and solar prosumers. Only a back-office officer can reactivate a deactivated account."
      >
        <Button onClick={() => setEditing('new')}>
          <IconPlus />
          Create user
        </Button>
      </PageHeader>

      <Banner tone="error">{error}</Banner>

      {loading ? (
        <LoadingState label="Loading users..." />
      ) : (
        <Panel flush>
          <Toolbar onSubmit={handleApplyFilters}>
            <FilterField
              label="Search"
              type="search"
              name="search"
              icon={IconSearch}
              value={filters.search}
              onChange={handleFilterChange}
              placeholder="Name, NIC or email"
              className="w-56"
            />

            <FilterField
              label="Role"
              as="select"
              name="role"
              value={filters.role}
              onChange={handleFilterChange}
              className="w-44"
            >
              <option value="">Any role</option>
              {ROLES.map((role) => (
                <option key={role.value} value={role.value}>
                  {role.label}
                </option>
              ))}
            </FilterField>

            <FilterField
              label="Status"
              as="select"
              name="status"
              value={filters.status}
              onChange={handleFilterChange}
              className="w-48"
            >
              <option value="">Any status</option>
              {STATUSES.map((status) => (
                <option key={status.value} value={status.value}>
                  {status.label}
                </option>
              ))}
            </FilterField>

            <div className="flex h-9 items-center gap-1.5">
              <Button type="submit" size="sm">
                Apply
              </Button>
              <Button type="button" variant="ghost" size="sm" onClick={handleClearFilters}>
                Clear
              </Button>
            </div>

            <span className="ml-auto flex h-9 items-center text-xs tabular-nums text-slate-400">
              {users.length} accounts
            </span>
          </Toolbar>

          {users.length === 0 ? (
            <EmptyState
              title="No users found"
              description="Nothing matches these filters. Try a different search or clear them."
            >
              <Button onClick={() => setEditing('new')}>
                <IconPlus />
                Create user
              </Button>
            </EmptyState>
          ) : (
            <TableWrap minWidth="58rem">
              <thead>
                <tr>
                  <TH>Name</TH>
                  <TH>Contact</TH>
                  <TH>Role</TH>
                  <TH>Status</TH>
                  <TH align="right">Actions</TH>
                </tr>
              </thead>
              <tbody>
                {pagination.pageItems.map((user) => (
                  <UserRow
                    key={user.nic}
                    user={user}
                    isSelf={user.nic === signedInUser?.nic}
                    busy={busyNic === user.nic}
                    onEdit={(item) => setEditing(item.nic)}
                    onActivate={handleActivate}
                    onDeactivate={setDeactivating}
                  />
                ))}
              </tbody>
            </TableWrap>
          )}

          {users.length > 0 ? (
            <Pagination
              {...pagination}
              onPageChange={pagination.setPage}
              onPageSizeChange={pagination.setPageSize}
              noun="accounts"
            />
          ) : null}
        </Panel>
      )}

      {deactivating ? (
        <Modal
          title="Deactivate this account?"
          description={`${deactivating.fullName} will not be able to sign in. Only a back-office officer can reactivate the account afterwards.`}
          onClose={() => setDeactivating(null)}
        >
          {deactivating.role === 'Prosumer' ? (
            <Banner tone="info">
              Any pending or approved bookings this prosumer still holds are cancelled, and their
              battery slots released back for booking.
            </Banner>
          ) : null}

          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setDeactivating(null)}>
              Keep active
            </Button>
            <Button
              variant="danger"
              disabled={busyNic === deactivating.nic}
              onClick={handleConfirmDeactivate}
            >
              {busyNic === deactivating.nic ? 'Deactivating...' : 'Deactivate account'}
            </Button>
          </div>
        </Modal>
      ) : null}

      {editing ? (
        <UserFormModal
          nic={editing === 'new' ? undefined : editing}
          onClose={() => setEditing(null)}
          onSaved={handleSaved}
        />
      ) : null}
    </>
  )
}
