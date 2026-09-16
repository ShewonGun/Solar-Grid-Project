/*
 * File: App.jsx
 * Purpose: Root component and route table for the Smart Solar Microgrid web
 *          console. Declares the public authentication routes, then the
 *          signed-in routes, which sit behind ProtectedRoute and inside the
 *          AppShell frame. Routes that only Back-office officers may open name
 *          that role, matching the roles the Web API enforces.
 * Author:  <your name>
 * Created: 2026
 */
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'

import AppShell from './Components/AppShell'
import ProtectedRoute from './Components/ProtectedRoute'
import Dashboard from './Pages/Dashboard'
import Login from './Pages/Login'
import PendingActivations from './Pages/PendingActivations'
import Profile from './Pages/Profile'
import Reservations from './Pages/Reservations'
import Signup from './Pages/Signup'
import StationSlots from './Pages/StationSlots'
import Stations from './Pages/Stations'
import Users from './Pages/Users'
import { homeRouteForRole, readSession } from './auth/session'

/** Roles that may reach the day-to-day operational screens. */
const STAFF = ['Backoffice', 'GridOperator']

/** Only Back-office officers administer the system. */
const BACKOFFICE = ['Backoffice']

/*
 * Sends visitors to the right starting page: the home screen for their role
 * when a session exists, otherwise the login page.
 */
function LandingRedirect() {
  const session = readSession()

  return <Navigate to={session ? homeRouteForRole(session.user.role) : '/login'} replace />
}

/*
 * Temporary placeholder for screens that are not built yet, so navigation and
 * the role redirects work end to end while the rest is written.
 */
function ComingSoon({ title }) {
  return (
    <div className="border-l-2 border-amber-400 pl-4">
      <h1 className="text-xl font-medium tracking-tight text-slate-900">{title}</h1>
      <p className="mt-1 text-sm text-slate-400">This screen is not built yet.</p>
    </div>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public routes. */}
        <Route path="/" element={<LandingRedirect />} />
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} />

        {/* Signed-in routes, framed by the app shell. */}
        <Route element={<ProtectedRoute roles={STAFF} />}>
          <Route element={<AppShell />}>
            {/* Both roles get the same dashboard; it adapts to what they may see. */}
            <Route path="/backoffice" element={<Dashboard />} />
            <Route path="/operator" element={<Dashboard />} />

            {/* Any signed-in user may read and edit their own account. */}
            <Route path="/profile" element={<Profile />} />

            <Route path="/stations" element={<Stations />} />
            <Route path="/stations/:id/slots" element={<StationSlots />} />
            <Route path="/reservations" element={<Reservations />} />

            {/* Administration - Back-office officers only. */}
            <Route element={<ProtectedRoute roles={BACKOFFICE} />}>
              <Route path="/users" element={<Users />} />
              <Route path="/pending-activations" element={<PendingActivations />} />
            </Route>
          </Route>
        </Route>

        {/* Prosumers use the mobile app; the web console has nothing for them. */}
        <Route element={<ProtectedRoute />}>
          <Route path="/prosumer" element={<ComingSoon title="Prosumer home" />} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
