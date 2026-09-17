/*
 * File: Profile.jsx
 * Purpose: The signed-in user's own account - their details, role and status
 *          read live from GET api/auth/me, with actions to edit the profile and
 *          change the password. Any user of the console can reach this screen,
 *          because the API lets everyone edit their own profile whatever their
 *          role.
 * Author:  <your name>
 * Created: 2026
 */
import { useEffect, useState } from 'react'
import { toast } from 'react-toastify'

import { getCurrentUser } from '../api/authApi'
import { toApiError } from '../api/client'
import useSession from '../auth/useSession'
import ChangePasswordModal from '../Components/ChangePasswordModal'
import {
  Banner,
  Button,
  LoadingState,
  PageHeader,
  Panel,
  StatusPill,
} from '../Components/PageControls'
import UserFormModal from '../Components/UserFormModal'

const ROLE_LABELS = {
  Backoffice: 'Back-office officer',
  GridOperator: 'Grid operator',
  Prosumer: 'Solar prosumer',
}

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

/* One label-and-value pair in the details list. */
function DetailRow({ label, value, numeric = false }) {
  return (
    <div className="flex flex-wrap items-baseline justify-between gap-3 px-4 py-3">
      <span className="text-[11px] font-semibold uppercase tracking-[0.07em] text-slate-500">
        {label}
      </span>
      <span className={`text-sm text-slate-900 ${numeric ? 'tabular-nums' : ''}`}>
        {value || <span className="text-slate-300">Not given</span>}
      </span>
    </div>
  )
}

/* Formats a date for the account details. */
function formatDate(value) {
  return new Date(value).toLocaleDateString(undefined, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

export default function Profile() {
  const { user: sessionUser } = useSession()

  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  // Only the load failure lives on the page; action outcomes go to a toast.
  const [error, setError] = useState('')
  const [editing, setEditing] = useState(false)
  const [changingPassword, setChangingPassword] = useState(false)
  // Bumped after a save so the details reload with the change in them.
  const [reloadToken, setReloadToken] = useState(0)

  useEffect(() => {
    let cancelled = false

    /* Reads the signed-in user's own profile from the service. */
    async function loadProfile() {
      try {
        const data = await getCurrentUser()

        if (!cancelled) {
          setUser(data)
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

    loadProfile()

    return () => {
      cancelled = true
    }
  }, [reloadToken])

  /* Reloads the profile and reports what changed after a dialog saves. */
  function handleSaved(message) {
    toast.success(message)
    setReloadToken((current) => current + 1)
  }

  if (loading) {
    return <LoadingState label="Loading your profile..." />
  }

  if (!user) {
    return (
      <>
        <PageHeader title="My account" />
        <Banner tone="error">{error}</Banner>
      </>
    )
  }

  return (
    <>
      <PageHeader
        title="My account"
        description="Your own details, as the Smart Microgrid service holds them."
      >
        <Button variant="secondary" onClick={() => setChangingPassword(true)}>
          Change password
        </Button>
        <Button onClick={() => setEditing(true)}>Edit profile</Button>
      </PageHeader>

      <Banner tone="error">{error}</Banner>

      <div className="grid gap-5 lg:grid-cols-2">
        <Panel title="Profile" flush>
          <div className="divide-y divide-slate-100">
            <DetailRow label="Full name" value={user.fullName} />
            <DetailRow label="Email" value={user.email} />
            <DetailRow label="Phone" value={user.phone} numeric />
            <DetailRow label="Address" value={user.address} />
            {user.role === 'Prosumer' ? (
              <DetailRow
                label="Solar capacity"
                value={
                  user.solarCapacityKW === null || user.solarCapacityKW === undefined
                    ? ''
                    : `${user.solarCapacityKW} kW`
                }
                numeric
              />
            ) : null}
          </div>
        </Panel>

        <Panel title="Account" flush>
          <div className="divide-y divide-slate-100">
            <DetailRow label="NIC" value={user.nic} numeric />
            <DetailRow label="Role" value={ROLE_LABELS[user.role]} />
            <div className="flex flex-wrap items-baseline justify-between gap-3 px-4 py-3">
              <span className="text-[11px] font-semibold uppercase tracking-[0.07em] text-slate-500">
                Status
              </span>
              <StatusPill tone={STATUS_TONES[user.status]}>
                {STATUS_LABELS[user.status]}
              </StatusPill>
            </div>
            <DetailRow label="Registered" value={formatDate(user.createdAt)} numeric />
            <DetailRow label="Last updated" value={formatDate(user.updatedAt)} numeric />
          </div>
        </Panel>
      </div>

      {editing ? (
        <UserFormModal
          nic={sessionUser?.nic}
          onClose={() => setEditing(false)}
          onSaved={handleSaved}
        />
      ) : null}

      {changingPassword ? (
        <ChangePasswordModal
          onClose={() => setChangingPassword(false)}
          onSaved={handleSaved}
        />
      ) : null}
    </>
  )
}
