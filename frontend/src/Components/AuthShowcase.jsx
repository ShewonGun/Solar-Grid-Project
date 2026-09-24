/*
 * File: AuthShowcase.jsx
 * Purpose: The image panel that fills the left 70% of the Login and Signup
 *          pages. It cross-fades through the three solar array photos,
 *          lays a dark scrim over them so the branding stays readable on a
 *          bright sky, and lets the user step between slides. Falls back to a
 *          single still image when the visitor prefers reduced motion.
 * Author:  <your name>
 * Created: 2026
 */
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'

import solarOne from '../assets/Solar-Image-1.jpg'
import solarTwo from '../assets/Solar-Image-2.jpg'
import solarThree from '../assets/Solar-Image-3.jpg'
import { BrandLockup } from './Brand'

/* The slides, in the order they are shown. */
const SLIDES = [
  { src: solarOne, alt: 'Solar panel array across open farmland at sunset.' },
  { src: solarTwo, alt: 'Wide solar farm under an orange evening sky.' },
  { src: solarThree, alt: 'Rooftop solar panels catching the setting sun.' },
]

/** How long each slide is held before the next one fades in. */
const SLIDE_DURATION_MS = 6000

/*
 * Reports whether the visitor has asked for reduced motion, and keeps up with
 * the setting if they change it while the page is open.
 */
function usePrefersReducedMotion() {
  const [prefersReduced, setPrefersReduced] = useState(
    () => window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false,
  )

  useEffect(() => {
    const query = window.matchMedia('(prefers-reduced-motion: reduce)')
    const handleChange = (event) => setPrefersReduced(event.matches)

    query.addEventListener('change', handleChange)
    return () => query.removeEventListener('change', handleChange)
  }, [])

  return prefersReduced
}

/*
 * Advances the slideshow on a timer. The timer is skipped entirely when the
 * visitor prefers reduced motion, leaving the first photo on screen.
 */
function useSlideshow(paused) {
  const [index, setIndex] = useState(0)

  useEffect(() => {
    if (paused) {
      return
    }

    const timer = window.setInterval(
      () => setIndex((current) => (current + 1) % SLIDES.length),
      SLIDE_DURATION_MS,
    )

    return () => window.clearInterval(timer)
  }, [paused])

  return [index, setIndex]
}

/*
 * Renders the photo panel with the branding overlay. `headline` lets each page
 * put its own line over the image.
 */
export default function AuthShowcase({ headline }) {
  const prefersReducedMotion = usePrefersReducedMotion()
  const [index, setIndex] = useSlideshow(prefersReducedMotion)

  return (
    <section className="relative hidden h-screen w-[70%] overflow-hidden bg-slate-900 lg:block">
      {SLIDES.map((slide, slideIndex) => (
        <img
          key={slide.src}
          src={slide.src}
          alt={slideIndex === index ? slide.alt : ''}
          aria-hidden={slideIndex === index ? undefined : 'true'}
          className={`absolute inset-0 h-full w-full object-cover transition-opacity duration-1000 ease-linear ${
            slideIndex === index ? 'opacity-100' : 'opacity-0'
          }`}
        />
      ))}

      {/* Scrim - keeps the white overlay text readable over a bright sky. */}
      <div
        className="absolute inset-0 bg-linear-to-t from-slate-950/85 via-slate-950/25 to-slate-950/40"
        aria-hidden="true"
      />

      <div className="relative flex h-full flex-col justify-between p-12">
        <Link to="/" className="flex w-fit items-center text-white">
          <BrandLockup markClassName="h-12 w-12" textClassName="text-xl text-white" />
        </Link>

        <div>
          <p className="text-xs font-medium uppercase tracking-[0.18em] text-amber-400">
            Trading console
          </p>
          <h2 className="mt-4 max-w-2xl text-[2.75rem] font-light leading-[1.08] tracking-tight text-white">
            {headline}
          </h2>

          <div className="mt-10 flex items-center justify-between border-t border-white/15 pt-5">
            <p className="text-xs tracking-wide text-slate-300">
              Smart Solar Microgrid &middot; 2026
            </p>

            {/* Slide indicators, square to match the rest of the UI. */}
            <div className="flex gap-2">
              {SLIDES.map((slide, slideIndex) => (
                <button
                  key={slide.src}
                  type="button"
                  onClick={() => setIndex(slideIndex)}
                  aria-label={`Show image ${slideIndex + 1}`}
                  aria-current={slideIndex === index ? 'true' : undefined}
                  className={`h-0.5 w-8 transition-colors ${
                    slideIndex === index ? 'bg-amber-400' : 'bg-white/30 hover:bg-white/60'
                  }`}
                />
              ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
