import { useSyncExternalStore } from 'react'

/**
 * Reactively track a CSS media query, e.g. useMediaQuery('(min-width: 1024px)').
 * Returns false during SSR / non-browser environments.
 */
export function useMediaQuery(query) {
  return useSyncExternalStore(
    (onChange) => {
      const mql = window.matchMedia(query)
      mql.addEventListener('change', onChange)
      return () => mql.removeEventListener('change', onChange)
    },
    () => window.matchMedia(query).matches,
    () => false
  )
}
