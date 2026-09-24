/*
 * File: PendingActivations.jsx
 * Purpose: The queue of solar prosumer accounts that have registered from the
 *          mobile app and are waiting for a Back-office officer to activate
 *          them. Until an account is activated the Web API refuses its sign-in,
 *          so this screen is what lets a new prosumer start using the system.
 * Author:  <your name>
 * Created: 2026
 */
import { useEffect, useMemo, useState } from 'react'
import toast from 'react-hot-toast'

import { toApiError } from '../api/client'
import { activateUser, getPendingActivations } from '../api/usersApi'
import {
  Banner,
  ButtonLink,
  EmptyState,
  LoadingState,
  PageHeader,
  Panel,
  TableWrap,
  TH,
} from '../Components/PageControls'
import { PendingCard, PendingRow } from '../Components/PendingRow'
import Pagination from '../Components/Pagination'
import usePagination from '../hooks/usePagination'
import { newestFirst } from '../utils/sorting'

export default function PendingActivations() {
  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(true)
  // Only the load failure lives on the page; action outcomes go to a toast.
  const [error, setError] = useState('')
  const [busyNic, setBusyNic] = useState(null)

  useEffect(() => {
    let cancelled = false

    /* Loads the accounts the service reports as waiting for activation. */
    async function loadPending() {
      try {
        const data = await getPendingActivations()

        if (!cancelled) {
          setAccounts(data)
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

    loadPending()

    return () => {
      cancelled = true
    }
  }, [])

  /*
   * Activates one account and drops it from the queue, since it is no longer
   * pending once the service accepts the change.
   */
  async function handleActivate(account) {
    setBusyNic(account.nic)

    try {
      const updated = await activateUser(account.nic)

      setAccounts((current) => current.filter((item) => item.nic !== updated.nic))
      toast.success(`${updated.fullName} can now sign in.`)
    } catch (failure) {
      toast.error(toApiError(failure).message)
    } finally {
      setBusyNic(null)
    }
  }

  // Newest registration first, so the accounts waiting longest sink to the
  // bottom and a prosumer who just signed up appears at the top of the queue.
  const orderedAccounts = useMemo(() => newestFirst(accounts), [accounts])

  // Paged in the browser: no endpoint on the Web API takes a page parameter.
  const pagination = usePagination(orderedAccounts)

  return (
    <>
      <PageHeader
        title="Pending activations"
        description="Prosumers who registered from the mobile app. They cannot sign in until an officer activates the account."
      >
        <ButtonLink to="/users" variant="secondary">
          All users
        </ButtonLink>
      </PageHeader>

      <Banner tone="error">{error}</Banner>

      {loading ? (
        <LoadingState label="Loading pending activations..." />
      ) : (
        <Panel
          title="Awaiting approval"
          meta={`${accounts.length} ${accounts.length === 1 ? 'account' : 'accounts'}`}
          flush
        >
          {accounts.length === 0 ? (
            <EmptyState
              title="Nothing waiting"
              description="Every registered prosumer account has been activated."
            >
              <ButtonLink to="/users" variant="secondary">
                Go to all users
              </ButtonLink>
            </EmptyState>
          ) : (
            <>
              {/* Table at "lg" and above; a purpose-built card list below it -
                  see PendingRow.jsx for why this page gets its own card rather
                  than the generic label/value stacking every other table
                  falls back to. */}
              <div className="hidden lg:block">
                <TableWrap minWidth="56rem">
                  <thead>
                    <tr>
                      <TH>Prosumer</TH>
                      <TH>Contact</TH>
                      <TH align="right">Solar array</TH>
                      <TH>Registered</TH>
                      <TH>Status</TH>
                      <TH align="right">Actions</TH>
                    </tr>
                  </thead>
                  <tbody>
                    {pagination.pageItems.map((account) => (
                      <PendingRow
                        key={account.nic}
                        account={account}
                        busy={busyNic === account.nic}
                        onActivate={handleActivate}
                      />
                    ))}
                  </tbody>
                </TableWrap>
              </div>

              <div className="grid gap-3 p-4 lg:hidden">
                {pagination.pageItems.map((account) => (
                  <PendingCard
                    key={account.nic}
                    account={account}
                    busy={busyNic === account.nic}
                    onActivate={handleActivate}
                  />
                ))}
              </div>
            </>
          )}

          {accounts.length > 0 ? (
            <Pagination
              {...pagination}
              onPageChange={pagination.setPage}
              onPageSizeChange={pagination.setPageSize}
              noun="accounts"
            />
          ) : null}
        </Panel>
      )}
    </>
  )
}
