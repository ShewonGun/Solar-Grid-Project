/*
 * File: Icons.jsx
 * Purpose: The console's icon set, drawn inline as SVG so they inherit the
 *          surrounding text colour and need no icon font or extra request.
 *          All of them share one grid, stroke weight and square cap, which is
 *          what keeps a toolbar of mixed icons looking like one set.
 * Author:  <your name>
 * Created: 2026
 */

/*
 * Shared frame for every icon. Children are drawn on a 24x24 grid with a
 * 1.6 stroke; `aria-hidden` because an icon here always sits beside a label.
 */
function Icon({ children, className = 'h-4 w-4' }) {
  return (
    <svg
      viewBox="0 0 24 24"
      className={className}
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="square"
      strokeLinejoin="miter"
      aria-hidden="true"
    >
      {children}
    </svg>
  )
}

/* Dashboard - a panel grid. */
export function IconDashboard(props) {
  return (
    <Icon {...props}>
      <rect x="3" y="3" width="7.5" height="7.5" />
      <rect x="13.5" y="3" width="7.5" height="4.5" />
      <rect x="3" y="13.5" width="7.5" height="7.5" />
      <rect x="13.5" y="10.5" width="7.5" height="10.5" />
    </Icon>
  )
}

/* Microgrid node - a solar panel. */
export function IconNode(props) {
  return (
    <Icon {...props}>
      <path d="M3 14h18l-2.5-9h-13L3 14Z" />
      <path d="M12 5v9M6.8 9.5h10.4M12 14v5M9 19h6" />
    </Icon>
  )
}

/* Reservations - a calendar. */
export function IconCalendar(props) {
  return (
    <Icon {...props}>
      <rect x="3" y="5" width="18" height="16" />
      <path d="M3 10h18M8 3v4M16 3v4" />
    </Icon>
  )
}

/* Users - two figures. */
export function IconUsers(props) {
  return (
    <Icon {...props}>
      <circle cx="9" cy="8" r="3.2" />
      <path d="M3 20c0-3.3 2.7-5.5 6-5.5s6 2.2 6 5.5" />
      <path d="M16 5.2A3.2 3.2 0 0 1 16 11M17.5 14.8c2.1.6 3.5 2.4 3.5 4.8" />
    </Icon>
  )
}

/* Pending - a clock. */
export function IconClock(props) {
  return (
    <Icon {...props}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5.5l3.5 2" />
    </Icon>
  )
}

/* Battery slot. */
export function IconBattery(props) {
  return (
    <Icon {...props}>
      <rect x="2" y="7" width="17" height="10" />
      <path d="M22 10.5v3M6 10.5v3M10 10.5v3" />
    </Icon>
  )
}

/* Search. */
export function IconSearch(props) {
  return (
    <Icon {...props}>
      <circle cx="10.5" cy="10.5" r="6.5" />
      <path d="m15.5 15.5 5 5" />
    </Icon>
  )
}

/* Plus, for a create action. */
export function IconPlus(props) {
  return (
    <Icon {...props}>
      <path d="M12 5v14M5 12h14" />
    </Icon>
  )
}

/* Close. */
export function IconClose(props) {
  return (
    <Icon {...props}>
      <path d="m6 6 12 12M18 6 6 18" />
    </Icon>
  )
}

/* Chevron, pointing right - used for breadcrumb separators. */
export function IconChevronRight(props) {
  return (
    <Icon {...props}>
      <path d="m9 5 7 7-7 7" />
    </Icon>
  )
}

/* Sign out. */
export function IconSignOut(props) {
  return (
    <Icon {...props}>
      <path d="M15 4h5v16h-5" />
      <path d="M3 12h12m0 0-4-4m4 4-4 4" />
    </Icon>
  )
}

/* A single user, for the account menu. */
export function IconUserCircle(props) {
  return (
    <Icon {...props}>
      <circle cx="12" cy="9" r="3.4" />
      <circle cx="12" cy="12" r="9" />
      <path d="M5.9 19a6.6 6.6 0 0 1 12.2 0" />
    </Icon>
  )
}

/* Inbox, for an empty list. */
export function IconInbox(props) {
  return (
    <Icon {...props}>
      <path d="M3 13h5l1.5 3h5L16 13h5" />
      <path d="M5.5 4h13L21 13v7H3v-7L5.5 4Z" />
    </Icon>
  )
}
