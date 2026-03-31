/**
 * Convert "HH:MM" or "HH:MM:SS" string to minutes from midnight.
 * @param {string} timeStr
 * @returns {number}
 */
export function timeToMinutes(timeStr) {
  const parts = timeStr.split(':').map(Number)
  return parts[0] * 60 + parts[1]
}

/**
 * Convert minutes from midnight to "HH:MM" string.
 * @param {number} minutes
 * @returns {string}
 */
export function minutesToTime(minutes) {
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

/**
 * Snap a minute value to the nearest 15-minute increment.
 * @param {number} minutes
 * @returns {number}
 */
export function snapTo15(minutes) {
  return Math.round(minutes / 15) * 15
}

/**
 * Pure function. Given a sorted array of grid blocks (sorted by startMinutes)
 * and the index of the block that just changed, pushes subsequent overlapping
 * blocks forward to eliminate overlap.
 *
 * Returns a new array (does not mutate input).
 * Returns null if the push chain would cause the last block to exceed dayEndMinutes.
 *
 * @param {Array<{id: string, startMinutes: number, endMinutes: number}>} blocks - sorted by startMinutes
 * @param {number} changedIndex - index of the block that triggered the push
 * @param {number} dayEndMinutes - e.g. 17 * 60 = 1020 for 5 PM
 * @returns {Array|null}
 */
export function pushBlocks(blocks, changedIndex, dayEndMinutes) {
  const result = blocks.map(b => ({ ...b }))

  for (let i = changedIndex + 1; i < result.length; i++) {
    const prev = result[i - 1]
    const curr = result[i]
    if (curr.startMinutes < prev.endMinutes) {
      const shift = prev.endMinutes - curr.startMinutes
      result[i] = {
        ...curr,
        startMinutes: curr.startMinutes + shift,
        endMinutes: curr.endMinutes + shift,
      }
    } else {
      break // gap exists, no further pushing needed
    }
  }

  // Reject if the last block exceeds the day end
  if (result.length > 0 && result[result.length - 1].endMinutes > dayEndMinutes) {
    return null
  }

  return result
}

/**
 * Convert a server TimeBlockResponse to a grid block with minute fields.
 * @param {Object} serverBlock - has startTime "HH:MM:SS", endTime "HH:MM:SS"
 * @returns {Object}
 */
export function toGridBlock(serverBlock) {
  return {
    ...serverBlock,
    startMinutes: timeToMinutes(serverBlock.startTime),
    endMinutes: timeToMinutes(serverBlock.endTime),
  }
}
