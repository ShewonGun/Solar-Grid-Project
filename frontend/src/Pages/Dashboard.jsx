/*
 * File: Dashboard.jsx
 * Purpose: Home screen for Back-office officers and Grid Operators. Reads the
 *          pending and approved-upcoming reservation counts, the microgrid node
 *          list and the week's bookings live from the Web API, and shows the
 *          accounts waiting for activation to Back-office officers. Nothing on
 *          this screen is hard-coded - every figure comes from the service.
 * Author:  <your name>
 * Created: 2026
 */
import { useEffect, useState } from 'react'

import { toApiError } from '../api/client'
import { getDashboardCounts, searchReservations } from '../api/reservationsApi'
import { getStations } from '../api/stationsApi'
import { getPendingActivations } from '../api/usersApi'
import useSession from '../auth/useSession'
import { StatTile, WeekBookingsChart } from '../Components/DashboardParts'
import { IconCalendar, IconClock, IconNode } from '../Components/Icons'
import {
  Banner,
  ButtonLink,
  EmptyState,
  Panel,
  LoadingState,
  PageHeader,
  StatusPill,
} from '../Components/PageControls'

/** Days in the reservation window the API enforces (MaxDaysAhead). */
const BOOKING_WINDOW_DAYS = 7

/*
 * Status colours are never the only signal - every pill also carries its status
 * as text, because approved-green and cancelled-red are hard to tell apart for
 * a reader with deuteranopia.
 */
const STATUS_TONES = {
  Pending: 'warning',
  Approved: 'active',
  Completed: 'inactive',
  Cancelled: 'danger',
}

/* Formats a reservation's start time for the upcoming list. */
function formatDateTime(value) {
  return new Date(value).toLocaleString(undefined, {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/*
 * Buckets reservations into one entry per day of the booking window, starting
 * today, so the chart always shows seven columns even on a quiet week.
 */
function bucketByDay(reservations, windowStart) {
  const days = []

  for (let offset = 0; offset < BOOKING_WINDOW_DAYS; offset += 1) {
    const day = new Date(windowStart)
    day.setDate(day.getDate() + offset)
    day.setHours(0, 0, 0, 0)

    days.push({
      iso: day.toISOString(),
      date: day,
      label: day.toLocaleDateString(undefined, { weekday: 'short' }),
      count: 0,
    })
  }

  reservations.forEach((reservation) => {
    const start = new Date(reservation.reservationStart)
    start.setHours(0, 0, 0, 0)

    const bucket = days.find((day) => day.date.getTime() === start.getTime())

    if (bucket) {
      bucket.count += 1
    }
  })

  return days
}

export default function Dashboard() {
  const { user, role } = useSession()
  const isBackoffice = role === 'Backoffice'

  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false

    /*
     * Loads everything the dashboard shows in one pass. The pending-activations
     * list is Back-office only, so it is requested only for that role - a Grid
     * Operator asking for it would be refused by the API.
     */
    async function loadDashboard() {
      const windowStart = new Date()
      const windowEnd = new Date(windowStart)
      windowEnd.setDate(windowEnd.getDate() + BOOKING_WINDOW_DAYS)

      try {
        const [counts, stations, upcoming, pendingAccounts] = await Promise.all([
          getDashboardCounts(),
          getStations(),
          searchReservations({ from: windowStart, to: windowEnd }),
          isBackoffice ? getPendingActivations() : Promise.resolve([]),
        ])

        if (!cancelled) {
          setData({
            counts,
            stations,
            upcoming,
            pendingAccounts,
            days: bucketByDay(upcoming, windowStart),
          })
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

    loadDashboard()

    return () => {
      cancelled = true
    }
  }, [isBackoffice])

  if (loading) {
    return <LoadingState label="Loading dashboard..." />
  }

  if (!data) {
    return (
      <>
        <PageHeader title="Dashboard" />
        <Banner tone="error">{error}</Banner>
      </>
    )
  }

  const { counts, stations, upcoming, pendingAccounts, days } = data
  const activeStations = stations.filter((station) => station.isActive).length

  // The soonest few bookings; the reservations screen carries the full list.
  const nextBookings = [...upcoming]
    .sort((a, b) => new Date(a.reservationStart) - new Date(b.reservationStart))
    .slice(0, 5)

  const stationNames = new Map(stations.map((station) => [station.id, station.stationName]))

  return (
    <>
      <PageHeader
        title={`Good day, ${user?.fullName?.split(' ')[0] ?? 'there'}`}
        description={
          isBackoffice
            ? 'Microgrid nodes, prosumer accounts and energy trading at a glance.'
            : 'Battery slot availability and energy trading bookings at a glance.'
        }
      >
        <ButtonLink to="/reservations" variant="secondary">
          All reservations
        </ButtonLink>
      </PageHeader>

      <Banner tone="error">{error}</Banner>

      {/* Headline counts. A handful of numbers belongs in tiles, not a chart. */}
      <div className="mb-5 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatTile
          label="Pending reservations"
          value={counts.pending}
          caption="Waiting for approval"
          to="/reservations"
          icon={IconCalendar}
          tone={counts.pending > 0 ? 'attention' : 'default'}
        />
        <StatTile
          label="Approved upcoming"
          value={counts.approvedUpcoming}
          caption="Confirmed future bookings"
          to="/reservations"
          icon={IconCalendar}
        />
        <StatTile
          label="Active nodes"
          value={activeStations}
          caption={`${stations.length} registered in total`}
          to="/stations"
          icon={IconNode}
        />
        {isBackoffice ? (
          <StatTile
            label="Pending activations"
            value={pendingAccounts.length}
            caption="Prosumer accounts awaiting approval"
            to="/pending-activations"
            icon={IconClock}
            tone={pendingAccounts.length > 0 ? 'attention' : 'default'}
          />
        ) : (
          <StatTile
            label="Booked this week"
            value={upcoming.length}
            caption="Across the 7-day window"
            icon={IconCalendar}
          />
        )}
      </div>

      <div className="grid gap-5 lg:grid-cols-2">
        <WeekBookingsChart days={days} />

        {/* Next bookings. */}
        <Panel title="Next bookings" meta="Soonest first" flush>
          {nextBookings.length === 0 ? (
            <EmptyState
              title="Nothing booked yet"
              description="Reservations inside the next 7 days will appear here."
            />
          ) : (
            <ul className="divide-y divide-slate-100">
              {nextBookings.map((reservation) => (
                <li
                  key={reservation.id}
                  className="flex items-center justify-between gap-4 px-4 py-3 transition-colors hover:bg-slate-50/80"
                >
                  <span className="min-w-0">
                    <span className="block truncate text-sm font-medium text-slate-900">
                      {stationNames.get(reservation.stationId) ?? 'Unknown node'}
                    </span>
                    <span className="mt-0.5 block text-xs tabular-nums text-slate-400">
                      {formatDateTime(reservation.reservationStart)} &middot; NIC{' '}
                      {reservation.prosumerNic} &middot; {reservation.energyKWh} kWh
                    </span>
                  </span>

                  <StatusPill tone={STATUS_TONES[reservation.status]}>
                    {reservation.status}
                  </StatusPill>
                </li>
              ))}
            </ul>
          )}
        </Panel>
      </div>

      {/* Accounts waiting for a Back-office officer to activate them. */}
      {isBackoffice && pendingAccounts.length > 0 ? (
        <div className="mt-5">
          <Panel
            title="Accounts awaiting activation"
            meta={`${pendingAccounts.length} waiting`}
            actions={
              <ButtonLink to="/pending-activations" variant="secondary" size="sm">
                Review all
              </ButtonLink>
            }
            flush
          >
            <ul className="divide-y divide-slate-100">
              {pendingAccounts.slice(0, 5).map((account) => (
                <li
                  key={account.nic}
                  className="flex items-center justify-between gap-4 px-4 py-3 transition-colors hover:bg-slate-50/80"
                >
                  <span className="min-w-0">
                    <span className="block truncate text-sm font-medium text-slate-900">
                      {account.fullName}
                    </span>
                    <span className="mt-0.5 block text-xs text-slate-400">
                      NIC {account.nic} &middot; {account.email}
                    </span>
                  </span>
                  <StatusPill tone="warning">Pending activation</StatusPill>
                </li>
              ))}
            </ul>
          </Panel>
        </div>
      ) : null}
    </>
  )
}
