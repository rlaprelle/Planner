/**
 * clientX of the pointer that started a dnd-kit drag.
 *
 * MouseEvent/PointerEvent activators carry clientX directly; TouchEvent
 * activators carry it on touches[0] (or changedTouches[0] for touchend).
 * Falls back to 0 with a warning if the event has no usable coordinate,
 * so a regression surfaces in the console instead of failing silently.
 */
export function activatorClientX(event) {
  const ae = event.activatorEvent
  const x = ae?.clientX ?? ae?.touches?.[0]?.clientX ?? ae?.changedTouches?.[0]?.clientX
  if (x == null) {
    console.warn('activatorClientX: activator event has no usable clientX; defaulting to 0', ae?.type)
    return 0
  }
  return x
}
