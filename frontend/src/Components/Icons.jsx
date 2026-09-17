/*
 * File: Icons.jsx
 * Purpose: The console's icon set, drawn from react-icons' Lucide collection.
 *          Every icon in the app is named and sized here rather than imported
 *          piecemeal, so a toolbar of mixed icons keeps one stroke weight and
 *          one default size, and swapping an icon is a one-line change that
 *          every screen picks up.
 * Author:  <your name>
 * Created: 2026
 */
import {
  LuBatteryCharging,
  LuCalendar,
  LuChevronLeft,
  LuChevronRight,
  LuCircleUser,
  LuClock,
  LuInbox,
  LuLayoutDashboard,
  LuLoaderCircle,
  LuLogOut,
  LuPlus,
  LuSearch,
  LuSun,
  LuSunMedium,
  LuUsers,
  LuX,
} from 'react-icons/lu'

/*
 * Shared frame for every icon. Lucide draws at stroke-width 2 by default, which
 * reads heavy beside this UI's thin borders and small type, so it is lightened
 * here in one place. Icons are hidden from screen readers because one always
 * sits beside a text label.
 */
function Glyph({ as: Component, className = 'h-4 w-4', ...props }) {
  return <Component className={className} strokeWidth={1.6} aria-hidden="true" {...props} />
}

/* Dashboard - a panel grid. */
export function IconDashboard(props) {
  return <Glyph as={LuLayoutDashboard} {...props} />
}

/* Microgrid node. Lucide has no solar-panel glyph; the sun carries the idea. */
export function IconNode(props) {
  return <Glyph as={LuSunMedium} {...props} />
}

/* Reservations - a calendar. */
export function IconCalendar(props) {
  return <Glyph as={LuCalendar} {...props} />
}

/* Users - a pair of figures. */
export function IconUsers(props) {
  return <Glyph as={LuUsers} {...props} />
}

/* Pending - a clock. */
export function IconClock(props) {
  return <Glyph as={LuClock} {...props} />
}

/* Battery slot. */
export function IconBattery(props) {
  return <Glyph as={LuBatteryCharging} {...props} />
}

/* Search. */
export function IconSearch(props) {
  return <Glyph as={LuSearch} {...props} />
}

/* Plus, for a create action. */
export function IconPlus(props) {
  return <Glyph as={LuPlus} {...props} />
}

/* Close. */
export function IconClose(props) {
  return <Glyph as={LuX} {...props} />
}

/* Chevron, pointing right - breadcrumb separators and the next page. */
export function IconChevronRight(props) {
  return <Glyph as={LuChevronRight} {...props} />
}

/* Chevron, pointing left - the previous page. */
export function IconChevronLeft(props) {
  return <Glyph as={LuChevronLeft} {...props} />
}

/* Sign out. */
export function IconSignOut(props) {
  return <Glyph as={LuLogOut} {...props} />
}

/* A single user, for the account menu. */
export function IconUserCircle(props) {
  return <Glyph as={LuCircleUser} {...props} />
}

/* Inbox, for an empty list. */
export function IconInbox(props) {
  return <Glyph as={LuInbox} {...props} />
}

/* The sun used as the product logo on the auth screens and in the shell. */
export function IconSun(props) {
  return <Glyph as={LuSun} {...props} />
}

/* Spinner for a button that is waiting on the Web API; pair with animate-spin. */
export function IconSpinner(props) {
  return <Glyph as={LuLoaderCircle} {...props} />
}
