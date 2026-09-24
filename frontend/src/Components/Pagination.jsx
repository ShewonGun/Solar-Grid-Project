/*
 * File: Pagination.jsx
 * Purpose: The footer strip under every data table - how many rows are on
 *          screen out of how many there are, a page-size chooser, and the page
 *          buttons. It is purely presentational: usePagination owns the state
 *          and hands this component everything it needs.
 * Author:  <your name>
 * Created: 2026
 */
import { IconChevronLeft, IconChevronRight } from './Icons'
import { PAGE_SIZE_OPTIONS } from '../hooks/usePagination'

/** How many numbered buttons to show before falling back to ellipses. */
const MAX_PAGE_BUTTONS = 7

/*
 * Works out which page numbers to show. Short lists get every page; longer ones
 * get the first, the last, the current page with a neighbour either side, and
 * an ellipsis where numbers were left out.
 */
function pageItemsFor(page, totalPages) {
  if (totalPages <= MAX_PAGE_BUTTONS) {
    return Array.from({ length: totalPages }, (_, index) => index + 1)
  }

  const pages = new Set([1, totalPages, page, page - 1, page + 1])

  // Keep the strip a steady width when the current page is near either end.
  if (page <= 3) {
    pages.add(2).add(3).add(4)
  }

  if (page >= totalPages - 2) {
    pages.add(totalPages - 1).add(totalPages - 2).add(totalPages - 3)
  }

  const sorted = [...pages].filter((value) => value >= 1 && value <= totalPages).sort((a, b) => a - b)

  // Insert an ellipsis marker wherever the run of numbers breaks.
  return sorted.flatMap((value, index) => {
    const previous = sorted[index - 1]
    return previous && value - previous > 1 ? [`gap-${value}`, value] : [value]
  })
}

const PAGE_BUTTON_BASE =
  'inline-flex h-7 min-w-7 items-center justify-center rounded-xs border px-1.5 text-xs font-medium tabular-nums transition-colors focus:outline-none focus-visible:ring-1 focus-visible:ring-slate-400 focus-visible:ring-offset-1 disabled:cursor-not-allowed'

/*
 * `noun` names what is being counted, so the summary reads "of 42 bookings"
 * rather than "of 42 items".
 */
export default function Pagination({
  page,
  pageSize,
  totalPages,
  total,
  rangeStart,
  rangeEnd,
  onPageChange,
  onPageSizeChange,
  noun = 'rows',
}) {
  // One page of results needs no controls, but the count is still worth saying.
  const showControls = totalPages > 1

  return (
    <nav
      aria-label="Pagination"
      className="flex flex-col items-center gap-3 border-t border-slate-200 bg-slate-50/70 px-4 py-3 text-center sm:flex-row sm:flex-wrap sm:justify-between sm:py-2.5 sm:text-left"
    >
      <p className="text-xs text-slate-500">
        {total === 0 ? (
          `No ${noun}`
        ) : (
          <>
            Showing <span className="font-medium tabular-nums text-slate-700">{rangeStart}</span>
            {'–'}
            <span className="font-medium tabular-nums text-slate-700">{rangeEnd}</span> of{' '}
            <span className="font-medium tabular-nums text-slate-700">{total}</span> {noun}
          </>
        )}
      </p>

      <div className="flex flex-wrap items-center justify-center gap-3 sm:justify-start">
        <label className="flex items-center gap-1.5 text-xs text-slate-500">
          Rows
          <select
            value={pageSize}
            onChange={(event) => onPageSizeChange(Number(event.target.value))}
            className="h-7 rounded-xs border border-slate-300 bg-white px-1.5 text-xs tabular-nums text-slate-700 outline-none transition-colors hover:border-slate-400 focus:border-slate-900 focus:ring-1 focus:ring-slate-900"
          >
            {PAGE_SIZE_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
        </label>

        {showControls ? (
          <div className="flex flex-wrap items-center justify-center gap-1">
            <button
              type="button"
              onClick={() => onPageChange(page - 1)}
              disabled={page === 1}
              aria-label="Previous page"
              className={`${PAGE_BUTTON_BASE} border-slate-300 bg-white text-slate-600 hover:border-slate-400 hover:text-slate-900 disabled:border-slate-200 disabled:text-slate-300`}
            >
              <IconChevronLeft className="h-3.5 w-3.5" />
            </button>

            {pageItemsFor(page, totalPages).map((item) =>
              typeof item === 'number' ? (
                <button
                  key={item}
                  type="button"
                  onClick={() => onPageChange(item)}
                  aria-label={`Page ${item}`}
                  aria-current={item === page ? 'page' : undefined}
                  className={`${PAGE_BUTTON_BASE} ${
                    item === page
                      ? 'border-slate-900 bg-slate-900 text-white'
                      : 'border-slate-300 bg-white text-slate-600 hover:border-slate-400 hover:text-slate-900'
                  }`}
                >
                  {item}
                </button>
              ) : (
                <span key={item} className="px-0.5 text-xs text-slate-400" aria-hidden="true">
                  &hellip;
                </span>
              ),
            )}

            <button
              type="button"
              onClick={() => onPageChange(page + 1)}
              disabled={page === totalPages}
              aria-label="Next page"
              className={`${PAGE_BUTTON_BASE} border-slate-300 bg-white text-slate-600 hover:border-slate-400 hover:text-slate-900 disabled:border-slate-200 disabled:text-slate-300`}
            >
              <IconChevronRight className="h-3.5 w-3.5" />
            </button>
          </div>
        ) : null}
      </div>
    </nav>
  )
}
