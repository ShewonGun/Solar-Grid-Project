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
import toast from 'react-hot-toast'

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
  PageHeader,
  Panel,
  TableWrap,
  TH,
  Toolbar,
} from '../Components/PageControls'
import ConfirmDialog from '../Components/ConfirmDialog'
import Pagination from '../Components/Pagination'
import UserFormModal from '../Components/UserFormModal'
import { UserCard, UserRow } from '../Components/UserRow'
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

const EMPTY_FILTERS = { role: '', status: '', search: '' }

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
              className="sm:w-56"
            />

            <FilterField
              label="Role"
              as="select"
              name="role"
              value={filters.role}
              onChange={handleFilterChange}
              className="sm:w-44"
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
              className="sm:w-48"
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
            <>
              {/* Table at "lg" and above; a purpose-built card list below it -
                  see UserRow.jsx for why this page gets its own card rather
                  than the generic label/value stacking every other table
                  falls back to. */}
              <div className="hidden lg:block">
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
              </div>

              <div className="grid gap-3 p-4 lg:hidden">
                {pagination.pageItems.map((user) => (
                  <UserCard
                    key={user.nic}
                    user={user}
                    isSelf={user.nic === signedInUser?.nic}
                    busy={busyNic === user.nic}
                    onEdit={(item) => setEditing(item.nic)}
                    onActivate={handleActivate}
                    onDeactivate={setDeactivating}
                  />
                ))}
              </div>
            </>
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
        <ConfirmDialog
          title="Deactivate this account?"
          description={`${deactivating.fullName} will not be able to sign in. Only a back-office officer can reactivate the account afterwards.`}
          confirmLabel="Deactivate account"
          pendingLabel="Deactivating..."
          cancelLabel="Keep active"
          busy={busyNic === deactivating.nic}
          onConfirm={handleConfirmDeactivate}
          onClose={() => setDeactivating(null)}
        >
          {deactivating.role === 'Prosumer' ? (
            <Banner tone="info">
              Any pending or approved bookings this prosumer still holds are cancelled, and their
              battery slots released back for booking.
            </Banner>
          ) : null}
        </ConfirmDialog>
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
