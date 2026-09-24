import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { Toaster } from 'react-hot-toast'

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

        {/* Prosumers use the mobile app only; Login turns them away before a
            session is ever created, so there is no prosumer route here. */}

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>

      {/* Toasts report the outcome of an action the user just took. Conditions
          that persist - a list that failed to load, an invalid form field -
          stay on the page itself, where they cannot time out unread.
          react-hot-toast ships no CSS of its own, so the console's own square
          corners and Outfit type are set here rather than in a stylesheet. */}
      <Toaster
        position="top-right"
        reverseOrder
        toastOptions={{
          duration: 4000,
          style: {
            fontFamily: 'var(--font-sans)',
            fontSize: '0.875rem',
            lineHeight: 1.5,
            color: '#0f172a',
            border: '1px solid #cbd5e1',
            borderRadius: 'var(--radius-xs)',
            boxShadow: '0 4px 12px rgb(15 23 42 / 0.1)',
            padding: '0.75rem 0.875rem',
          },
          success: { iconTheme: { primary: '#047857', secondary: '#ffffff' } },
          error: { iconTheme: { primary: '#b91c1c', secondary: '#ffffff' } },
        }}
      />
    </BrowserRouter>
  )
}
