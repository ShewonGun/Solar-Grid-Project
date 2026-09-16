/*
 * File: ProtectedRoute.jsx
 * Purpose: Route guard for the pages behind sign-in. It sends anonymous
 *          visitors to the login page, remembering where they were headed, and
 *          turns away signed-in users whose role does not cover the route. The
 *          Web API enforces the same roles on every endpoint - this guard only
 *          keeps the user out of screens that would fail for them anyway.
 * Author:  <your name>
 * Created: 2026
 */
import { Navigate, Outlet, useLocation } from 'react-router-dom'

import useSession from '../auth/useSession'
import { homeRouteForRole } from '../auth/session'

/*
 * Renders the nested routes when the user may see them. `roles` lists the roles
 * allowed through; leaving it out allows any signed-in user.
 */
export default function ProtectedRoute({ roles }) {
  const { session, role } = useSession()
  const location = useLocation()

  // Not signed in: go to login and remember the page for after sign-in.
  if (!session) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  // Signed in but not allowed here: fall back to the user's own home screen.
  if (roles && !roles.includes(role)) {
    return <Navigate to={homeRouteForRole(role)} replace />
  }

  return <Outlet />
}
