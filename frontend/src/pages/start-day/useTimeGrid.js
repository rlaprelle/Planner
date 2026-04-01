import { useRef } from 'react'
import { pushBlocks, snapTo15 } from './pushBlocks'

export const DAY_START_MINUTES = 8 * 60   // 480 — 8 AM
export const DAY_END_MINUTES = 17 * 60    // 1020 — 5 PM
export const DAY_DURATION = DAY_END_MINUTES - DAY_START_MINUTES // 540

/**
 * Provides the grid ref, coordinate helpers, and resize handler.
 * Block state is owned by the caller (StartDayPage).
 */
export function useTimeGrid() {
  const gridRef = useRef(null)
  const resizeRef = useRef(null)

  /**
   * Convert minutes-from-midnight to a percentage left offset within the grid.
   */
  function minutesToPercent(minutes) {
    return ((minutes - DAY_START_MINUTES) / DAY_DURATION) * 100
  }

  /**
   * Convert a duration in minutes to a percentage width within the grid.
   */
  function durationToPercent(durationMinutes) {
    return (durationMinutes / DAY_DURATION) * 100
  }

  /**
   * Convert a pixel delta (e.g. from drag) to minutes.
   */
  function pixelDeltaToMinutes(deltaX) {
    if (!gridRef.current) return 0
    const { width } = gridRef.current.getBoundingClientRect()
    return (deltaX / width) * DAY_DURATION
  }

  /**
   * Convert an absolute clientX position to minutes-from-midnight,
   * relative to the grid's left edge.
   */
  function clientXToMinutes(clientX) {
    if (!gridRef.current) return DAY_START_MINUTES
    const { left, width } = gridRef.current.getBoundingClientRect()
    const ratio = Math.max(0, Math.min(1, (clientX - left) / width))
    return DAY_START_MINUTES + ratio * DAY_DURATION
  }

  /**
   * Begin a resize drag on a block's right edge.
   *
   * @param {MouseEvent} e - the mousedown event on the resize handle
   * @param {Object} block - the block being resized (must have startMinutes, endMinutes, id)
   * @param {number} blockIndex - index of the block in the `blocks` array
   * @param {Array} blocks - the current full blocks array
   * @param {Function} onBlocksChange - called with the new blocks array on each tick
   */
  function startResize(e, block, blockIndex, blocks, onBlocksChange) {
    e.preventDefault()
    e.stopPropagation()

    resizeRef.current = {
      blockIndex,
      initialClientX: e.clientX,
      initialEndMinutes: block.endMinutes,
      blocks,
      onBlocksChange,
    }

    function onMove(moveEvent) {
      const ref = resizeRef.current
      if (!ref) return

      const deltaX = moveEvent.clientX - ref.initialClientX
      const deltaMins = pixelDeltaToMinutes(deltaX)
      const rawEnd = ref.initialEndMinutes + deltaMins
      const minEnd = ref.blocks[ref.blockIndex].startMinutes + 15
      const snappedEnd = Math.max(minEnd, snapTo15(rawEnd))

      const newBlocks = ref.blocks.map((b, i) =>
        i === ref.blockIndex ? { ...b, endMinutes: snappedEnd } : { ...b }
      )

      const pushed = pushBlocks(newBlocks, ref.blockIndex, DAY_END_MINUTES)
      if (pushed) {
        ref.onBlocksChange(pushed)
      }
      // If pushed is null, the resize is capped — don't update
    }

    function onUp() {
      resizeRef.current = null
      window.removeEventListener('mousemove', onMove)
      window.removeEventListener('mouseup', onUp)
    }

    window.addEventListener('mousemove', onMove)
    window.addEventListener('mouseup', onUp)
  }

  return {
    gridRef,
    minutesToPercent,
    durationToPercent,
    pixelDeltaToMinutes,
    clientXToMinutes,
    startResize,
  }
}
