/*
 * File: usePagination.js
 * Purpose: Splits a list into pages for the console's data tables. The Web API
 *          returns whole result sets - none of its endpoints take a page or
 *          skip parameter - so paging happens in the browser over the rows the
 *          service already sent. The current page is clamped during render
 *          rather than corrected in an effect, so filtering a list down can
 *          never leave the table stranded on a page that no longer exists.
 * Author:  <your name>
 * Created: 2026
 */
import { useMemo, useState } from 'react'

/**
 * Page sizes offered in the pagination control. The first entry is the default
 * every table starts on, so the two can never disagree.
 */
export const PAGE_SIZE_OPTIONS = [5, 10, 25, 50, 100]

/*
 * Returns the slice of `items` for the current page along with everything the
 * Pagination component needs to describe and change it.
 */
export default function usePagination(items, initialPageSize = PAGE_SIZE_OPTIONS[0]) {
  const [requestedPage, setRequestedPage] = useState(1)
  const [pageSize, setRequestedPageSize] = useState(initialPageSize)

  const total = items.length
  const totalPages = Math.max(1, Math.ceil(total / pageSize))

  // Clamping here, rather than resetting through an effect, means a filter that
  // shrinks the list shows page 1 on the very same render.
  const page = Math.min(Math.max(requestedPage, 1), totalPages)

  const pageItems = useMemo(
    () => items.slice((page - 1) * pageSize, page * pageSize),
    [items, page, pageSize],
  )

  /* Moves to a page, ignoring anything outside the range. */
  function setPage(next) {
    setRequestedPage(Math.min(Math.max(next, 1), totalPages))
  }

  /*
   * Changes the page size and returns to the first page, so the user is not
   * dropped somewhere unrecognisable in the middle of the list.
   */
  function setPageSize(next) {
    setRequestedPageSize(next)
    setRequestedPage(1)
  }

  return {
    page,
    pageSize,
    totalPages,
    total,
    pageItems,
    setPage,
    setPageSize,
    // 1-based inclusive range of the rows on screen, for "showing X to Y".
    rangeStart: total === 0 ? 0 : (page - 1) * pageSize + 1,
    rangeEnd: Math.min(page * pageSize, total),
  }
}
