/*
 * File: AppShell.jsx
 * Purpose: Frame around every signed-in screen - a grouped side navigation, a
 *          top bar carrying the breadcrumb trail and the signed-in user, and
 *          the content area. The navigation only lists the areas the user's
 *          role may open, which matches the roles the Web API enforces on the
 *          matching endpoints. The sidebar and top bar are pinned to the
 *          viewport - the outer frame never scrolls, only the content pane
 *          under <main> does - which is the standard fixed-shell layout of an
 *          enterprise console (the sidebar and header never leave view no
 *          matter how long a table gets).
 * Author:  <your name>
 * Created: 2026
 */
import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'

import useSession from '../auth/useSession'
import BrandMark, { BrandLockup } from './Brand'
import {
  IconCalendar,
  IconChevronRight,
  IconClock,
  IconDashboard,
  IconNode,
  IconSignOut,
  IconUserCircle,
  IconUsers,
} from './Icons'

/*
 * The navigation, grouped into sections. Each entry names the roles allowed to
 * see it, so adding a screen is one edit in one place.
 */
const NAV_SECTIONS = [
  {
    label: 'Overview',
    items: [
      { to: '/backoffice', label: 'Dashboard', icon: IconDashboard, roles: ['Backoffice'], end: true },
      { to: '/operator', label: 'Dashboard', icon: IconDashboard, roles: ['GridOperator'], end: true },
    ],
  },
  {
    label: 'Operations',
    items: [
      {
        to: '/stations',
        label: 'Microgrid nodes',
        icon: IconNode,
        roles: ['Backoffice', 'GridOperator'],
      },
      {
        to: '/reservations',
        label: 'Reservations',
        icon: IconCalendar,
        roles: ['Backoffice', 'GridOperator'],
      },
    ],
  },
  {
    label: 'Administration',
    items: [
      { to: '/users', label: 'Users', icon: IconUsers, roles: ['Backoffice'] },
      {
        to: '/pending-activations',
        label: 'Pending activations',
        icon: IconClock,
        roles: ['Backoffice'],
      },
    ],
  },
]

/** Human-readable labels for the role names the API returns. */
const ROLE_LABELS = {
  Backoffice: 'Back-office officer',
  GridOperator: 'Grid operator',
  Prosumer: 'Solar prosumer',
}

/** Breadcrumb labels for the routes the console has. */
const CRUMB_LABELS = {
  backoffice: 'Dashboard',
  operator: 'Dashboard',
  stations: 'Microgrid nodes',
  reservations: 'Reservations',
  users: 'Users',
  'pending-activations': 'Pending activations',
  slots: 'Battery slots',
}

/* Returns the user's initials for the identity block. */
function initialsOf(fullName) {
  return (fullName ?? '')
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('')
}

/*
 * Turns the current path into breadcrumb labels. Path segments that are ids
 * rather than names are skipped, so the trail reads as words.
 */
function crumbsFor(pathname) {
  return pathname
    .split('/')
    .filter(Boolean)
    .map((segment) => CRUMB_LABELS[segment])
    .filter(Boolean)
}

/* One navigation row, with its icon and active treatment. */
function NavItem({ item }) {
  const ItemIcon = item.icon

  return (
    <NavLink
      to={item.to}
      end={item.end}
      className={({ isActive }) =>
        `flex items-center gap-2.5 border-l-2 py-2.5 pl-3.5 pr-3 text-sm transition-colors ${
          isActive
            ? 'border-amber-400 bg-slate-800 font-medium text-white'
            : 'border-transparent text-slate-400 hover:bg-slate-800/50 hover:text-slate-100'
        }`
      }
    >
      <ItemIcon className="h-4 w-4 shrink-0" />
      <span className="truncate">{item.label}</span>
    </NavLink>
  )
}

/* The signed-in user, with a menu holding the sign-out action. */
function UserMenu({ user, role, onSignOut }) {
  const [open, setOpen] = useState(false)
  const containerRef = useRef(null)

  useEffect(() => {
    /* Closes the menu on a click anywhere outside it. */
    function handlePointerDown(event) {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setOpen(false)
      }
    }

    document.addEventListener('mousedown', handlePointerDown)
    return () => document.removeEventListener('mousedown', handlePointerDown)
  }, [])

  return (
    <div className="relative" ref={containerRef}>
      <button
        type="button"
        onClick={() => setOpen((current) => !current)}
        aria-expanded={open}
        aria-haspopup="menu"
        className="flex items-center gap-2.5 rounded-xs border border-transparent py-1 pl-1 pr-2 transition-colors hover:border-slate-200 hover:bg-slate-50"
      >
        <span className="grid h-7 w-7 shrink-0 place-items-center rounded-xs bg-slate-900 text-[11px] font-semibold text-white">
          {initialsOf(user?.fullName)}
        </span>
        <span className="hidden text-left sm:block">
          <span className="block text-xs font-medium leading-tight text-slate-900">
            {user?.fullName}
          </span>
          <span className="block text-[11px] leading-tight text-slate-400">
            {ROLE_LABELS[role]}
          </span>
        </span>
      </button>

      {open ? (
        <div
          role="menu"
          className="absolute right-0 top-full z-30 mt-1 w-56 border border-slate-200 bg-white py-1 shadow-lg"
        >
          <div className="border-b border-slate-100 px-3 py-2">
            <p className="truncate text-xs font-medium text-slate-900">{user?.fullName}</p>
            <p className="mt-0.5 truncate text-[11px] tabular-nums text-slate-400">
              NIC {user?.nic}
            </p>
          </div>
          <Link
            to="/profile"
            role="menuitem"
            onClick={() => setOpen(false)}
            className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-slate-600 transition-colors hover:bg-slate-50 hover:text-slate-900"
          >
            <IconUserCircle className="h-4 w-4" />
            My account
          </Link>
          <button
            type="button"
            role="menuitem"
            onClick={onSignOut}
            className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-slate-600 transition-colors hover:bg-slate-50 hover:text-slate-900"
          >
            <IconSignOut className="h-4 w-4" />
            Sign out
          </button>
        </div>
      ) : null}
    </div>
  )
}

export default function AppShell() {
  const { user, role, signOut } = useSession()
  const navigate = useNavigate()
  const location = useLocation()

  /* Signs the user out and returns them to the login page. */
  function handleSignOut() {
    signOut()
    navigate('/login', { replace: true })
  }

  // Only the sections that still have a visible item for this role.
  const sections = NAV_SECTIONS.map((section) => ({
    ...section,
    items: section.items.filter((item) => item.roles.includes(role)),
  })).filter((section) => section.items.length > 0)

  const crumbs = crumbsFor(location.pathname)

  return (
    // h-screen + overflow-hidden locks the whole frame to the viewport, so
    // nothing here scrolls as a page. Only <main> below is left free to grow,
    // via min-h-0 (flex children default to min-height: auto, which would
    // otherwise stretch this container instead of letting main scroll inside it).
    <div className="flex h-screen overflow-hidden bg-slate-50 text-slate-900">
      {/* Side navigation - fixed height, never scrolls as a unit; only its own
          nav list scrolls if the items ever outgrow the viewport. */}
      <aside className="hidden h-screen w-60 shrink-0 flex-col border-r border-slate-800 bg-slate-900 lg:flex">
        <Link
          to="/"
          className="flex h-14 shrink-0 items-center border-b border-slate-800 px-4 text-white"
        >
          <BrandLockup markClassName="h-8 w-8" textClassName="text-base text-white" />
        </Link>

        {/* One flat list, no section headings - the icons and labels carry
            enough meaning on their own. */}
        <nav className="min-h-0 flex-1 overflow-y-auto py-5">
          {sections.flatMap((section) => section.items).map((item) => (
            <NavItem key={item.to} item={item} />
          ))}
        </nav>

        {/* Sign out sits at the very foot of the sidebar, styled like a nav row
            so it reads as part of the same list rather than a bolted-on extra. */}
        <div className="shrink-0 border-t border-slate-800 py-2">
          <button
            type="button"
            onClick={handleSignOut}
            className="flex w-full items-center gap-2.5 border-l-2 border-transparent py-2.5 pl-3.5 pr-3 text-sm text-slate-400 transition-colors hover:bg-slate-800/50 hover:text-slate-100"
          >
            <IconSignOut className="h-4 w-4 shrink-0" />
            Sign out
          </button>
        </div>
      </aside>

      <div className="flex h-screen min-w-0 flex-1 flex-col">
        {/* Top bar - fixed: it sits above main's scroll area rather than
            scrolling with the page, so it stays put without needing "sticky". */}
        <header className="flex h-14 shrink-0 items-center justify-between gap-4 border-b border-slate-200 bg-white px-4 sm:px-6">
          <div className="flex min-w-0 items-center gap-2.5">
            {/* Compact logo for small screens, where the sidebar is hidden. */}
            <Link to="/" className="flex items-center lg:hidden">
              <BrandMark className="h-9 w-9" />
            </Link>

            <nav aria-label="Breadcrumb" className="flex min-w-0 items-center gap-1.5">
              <span className="hidden text-sm text-slate-400 sm:inline">Console</span>
              {crumbs.map((crumb, index) => (
                <span key={crumb} className="flex min-w-0 items-center gap-1.5">
                  <IconChevronRight className="hidden h-3.5 w-3.5 shrink-0 text-slate-300 sm:block" />
                  <span
                    className={`truncate text-sm ${
                      index === crumbs.length - 1
                        ? 'font-medium text-slate-900'
                        : 'text-slate-400'
                    }`}
                    aria-current={index === crumbs.length - 1 ? 'page' : undefined}
                  >
                    {crumb}
                  </span>
                </span>
              ))}
            </nav>
          </div>

          <UserMenu user={user} role={role} onSignOut={handleSignOut} />
        </header>

        {/* Navigation as a fixed scrolling strip on small screens. */}
        <nav className="flex shrink-0 gap-1 overflow-x-auto border-b border-slate-200 bg-white px-3 py-2 lg:hidden">
          {sections.flatMap((section) =>
            section.items.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  `flex items-center gap-1.5 whitespace-nowrap rounded-xs px-2.5 py-1.5 text-xs transition-colors ${
                    isActive
                      ? 'bg-slate-900 font-medium text-white'
                      : 'text-slate-500 hover:bg-slate-100 hover:text-slate-900'
                  }`
                }
              >
                <item.icon className="h-3.5 w-3.5" />
                {item.label}
              </NavLink>
            )),
          )}
        </nav>

        {/* The one scrolling region in the shell. */}
        <main className="min-h-0 flex-1 overflow-y-auto px-4 py-6 sm:px-6 lg:px-8">
          <div className="mx-auto max-w-7xl">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
