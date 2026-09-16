/*
 * File: PendingActivations.jsx
 * Purpose: The queue of solar prosumer accounts that have registered from the
 *          mobile app and are waiting for a Back-office officer to activate
 *          them. Until an account is activated the Web API refuses its sign-in,
 *          so this screen is what lets a new prosumer start using the system.
 * Author:  <your name>
 * Created: 2026
 */
import { useEffect, useState } from 'react'

import { toApiError } from '../api/client'
import { activateUser, getPendingActivations } from '../api/usersApi'
import {
  Banner,
  Button,
  ButtonLink,
  EmptyState,
  LoadingState,
  PageHeader,
  Panel,
  RowActions,
  StatusPill,
  TableWrap,
  TD,
  TH,
  TR,
} from '../Components/PageControls'

/* Formats the date an account registered. */
function formatDate(value) {
  return new Date(value).toLocaleDateString(undefined, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

/* One row of the pending-activation queue. */
function PendingRow({ account, busy, onActivate }) {
  return (
    <TR>
      <TD className="text-slate-900">
        <span className="block font-medium">{account.fullName}</span>
        <span className="mt-0.5 block text-xs tabular-nums text-slate-400">
          NIC {account.nic}
        </span>
      </TD>
      <TD>
        <span className="block">{account.email}</span>
        {account.phone ? (
          <span className="mt-0.5 block text-xs tabular-nums text-slate-400">
            {account.phone}
          </span>
        ) : null}
      </TD>
      <TD numeric>
        {account.solarCapacityKW === null || account.solarCapacityKW === undefined
          ? <span className="text-slate-300">Not given</span>
          : `${account.solarCapacityKW} kW`}
      </TD>
      <TD className="tabular-nums">{formatDate(account.createdAt)}</TD>
      <TD>
        <StatusPill tone="warning">Pending activation</StatusPill>
      </TD>
      <TD>
        <RowActions>
          <Button size="sm" disabled={busy} onClick={() => onActivate(account)}>
            {busy ? 'Activating...' : 'Activate'}
          </Button>
        </RowActions>
      </TD>
    </TR>
  )
}

export default function PendingActivations() {
  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
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
    setError('')
    setNotice('')

    try {
      const updated = await activateUser(account.nic)

      setAccounts((current) => current.filter((item) => item.nic !== updated.nic))
      setNotice(`${updated.fullName} can now sign in.`)
    } catch (failure) {
      setError(toApiError(failure).message)
    } finally {
      setBusyNic(null)
    }
  }

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
      <Banner tone="success">{notice}</Banner>

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
                {accounts.map((account) => (
                  <PendingRow
                    key={account.nic}
                    account={account}
                    busy={busyNic === account.nic}
                    onActivate={handleActivate}
                  />
                ))}
              </tbody>
            </TableWrap>
          )}
        </Panel>
      )}
    </>
  )
}
