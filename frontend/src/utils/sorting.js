/*
 * File: sorting.js
 * Purpose: Row ordering shared by the console's data tables. The Web API sorts
 *          its lists for its own purposes - stations and users by name, slots
 *          by start time - so the console reorders them here to put the most
 *          recently created record at the top of every table.
 * Author:  <your name>
 * Created: 2026
 */

/*
 * Returns a new array ordered newest first by createdAt. The input is left
 * alone, because it is React state and sorting in place would mutate it.
 * Records with a missing or unparseable date sink to the bottom rather than
 * scrambling the order around them.
 */
export function newestFirst(items) {
  return [...items].sort((a, b) => timeOf(b.createdAt) - timeOf(a.createdAt))
}

/* Milliseconds for a date the API sent, or -Infinity when it cannot be read. */
function timeOf(value) {
  const time = Date.parse(value)
  return Number.isNaN(time) ? -Infinity : time
}
