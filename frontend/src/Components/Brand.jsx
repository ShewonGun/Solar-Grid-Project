/*
 * File: Brand.jsx
 * Purpose: The VoltShare logo mark, used everywhere the console shows its
 *          identity - the sidebar, the mobile header and the auth screens. The
 *          source artwork has a transparent background, so it drops straight
 *          onto any surface, light or dark, with no backing tile needed.
 * Author:  <your name>
 * Created: 2026
 */
import voltshareMark from '../assets/voltshare-mark.png'

/* `className` sizes the mark, e.g. "h-7 w-7". */
export default function BrandMark({ className = 'h-7 w-7' }) {
  return (
    <span className={`inline-flex shrink-0 items-center justify-center ${className}`}>
      <img src={voltshareMark} alt="" className="h-full w-full object-contain" />
    </span>
  )
}

/*
 * The mark paired with the "VoltShare" wordmark, in the console's own type
 * rather than the logo file's raster text, so it stays crisp at every size and
 * follows the surrounding text colour on both light and dark surfaces.
 */
export function BrandLockup({ markClassName = 'h-7 w-7', textClassName = '' }) {
  return (
    <span className="flex min-w-0 items-center gap-2.5">
      <BrandMark className={markClassName} />
      <span className={`truncate font-semibold tracking-tight ${textClassName}`}>VoltShare</span>
    </span>
  )
}
