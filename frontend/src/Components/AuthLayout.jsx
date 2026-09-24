/*
 * File: AuthLayout.jsx
 * Purpose: Shell for the console's sign-in screen - the solar photo panel
 *          takes the left 70% of the screen and the form column the right 30%,
 *          scrolling on its own so a long form never pushes the image out of
 *          view. Below the lg breakpoint the photo is dropped and the form
 *          takes the full width. Kept generic (title/subtitle/footer as props)
 *          rather than folded into Login itself, in case another public screen
 *          - a password reset, say - ever needs the same shell.
 * Author:  <your name>
 * Created: 2026
 */
import { Link } from 'react-router-dom'

import AuthShowcase from './AuthShowcase'
import { BrandLockup } from './Brand'

/*
 * Renders the photo panel beside the given form. `title` and `subtitle` head
 * the form column, `headline` is the line laid over the photo, and `footer` is
 * an optional slot underneath the form for a supporting link or note.
 */
export default function AuthLayout({ title, subtitle, headline, footer, children }) {
  return (
    <div className="flex min-h-screen bg-white text-slate-900 lg:h-screen lg:overflow-hidden">
      <AuthShowcase headline={headline} />

      {/* Form column - just under a third of the screen on large displays. The
          flex/auto-margin pair centres the form vertically, and still lets it
          scroll from the top if it ever grows taller than the viewport. */}
      <main className="flex w-full overflow-y-auto px-6 py-12 sm:px-10 lg:w-[30%] lg:px-10">
        <div className="m-auto w-full max-w-sm lg:max-w-none">
          {/* Compact logo for small screens, where the photo panel is hidden. */}
          <Link to="/" className="mb-10 flex items-center lg:hidden">
            <BrandLockup markClassName="h-10 w-10" textClassName="text-base text-slate-900" />
          </Link>

          <h1 className="text-2xl font-medium leading-tight tracking-tight text-slate-900">
            {title}
          </h1>
          <p className="mt-2 text-sm leading-relaxed text-slate-500">{subtitle}</p>

          <div className="mt-8">{children}</div>

          {footer ? (
            <div className="mt-7 border-t border-slate-200 pt-5 text-sm text-slate-500">
              {footer}
            </div>
          ) : null}
        </div>
      </main>
    </div>
  )
}
