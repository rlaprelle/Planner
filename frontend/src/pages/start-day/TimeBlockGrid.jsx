import { useDroppable } from '@dnd-kit/core'
import { TimeBlock } from './TimeBlock'
import { DAY_START_MINUTES, DAY_END_MINUTES } from './useTimeGrid'

// Hours to display: 8 AM through 5 PM
const HOURS = Array.from(
  { length: DAY_END_MINUTES / 60 - DAY_START_MINUTES / 60 + 1 },
  (_, i) => i + DAY_START_MINUTES / 60
) // [8, 9, 10, 11, 12, 13, 14, 15, 16, 17]

function formatHour(h) {
  if (h === 12) return '12 PM'
  if (h < 12) return `${h} AM`
  return `${h - 12} PM`
}

/**
 * The horizontal planner grid.
 *
 * Props:
 *   blocks          - array of grid blocks (with startMinutes, endMinutes)
 *   onBlocksChange  - called with new blocks array
 *   gridRef         - ref from useTimeGrid (attach to the body area)
 *   minutesToPercent, durationToPercent, startResize - from useTimeGrid
 */
export function TimeBlockGrid({
  blocks,
  onBlocksChange,
  gridRef,
  minutesToPercent,
  durationToPercent,
  startResize,
}) {
  const { setNodeRef, isOver } = useDroppable({ id: 'time-block-grid' })

  // Merge the droppable ref and the grid measurement ref
  const mergedRef = (el) => {
    setNodeRef(el)
    gridRef.current = el
  }

  return (
    <div
      className={`relative border border-gray-200 rounded-lg overflow-hidden transition-colors ${
        isOver ? 'bg-indigo-50' : 'bg-white'
      }`}
    >
      {/* Hour columns with time labels and grid lines */}
      <div className="flex">
        {HOURS.map((hour, idx) => (
          <div
            key={hour}
            className={`flex-1 border-r border-gray-200 ${idx === HOURS.length - 1 ? 'border-r-0' : ''}`}
          >
            <div className="text-xs text-gray-400 px-1 py-1 border-b border-gray-100 whitespace-nowrap">
              {formatHour(hour)}
            </div>
            {/* 15-min sub-lines inside the body */}
            <div className="h-14 relative">
              <div className="absolute left-1/4 top-0 bottom-0 border-l border-gray-100" />
              <div className="absolute left-2/4 top-0 bottom-0 border-l border-gray-200" />
              <div className="absolute left-3/4 top-0 bottom-0 border-l border-gray-100" />
            </div>
          </div>
        ))}
      </div>

      {/* Absolute overlay for time blocks — uses gridRef for pixel↔time conversion */}
      <div
        ref={mergedRef}
        className="absolute left-0 right-0 bottom-0"
        style={{ top: '1.75rem' }} /* height of the time label row */
      >
        {blocks.map((block, i) => (
          <TimeBlock
            key={block.id}
            block={block}
            blockIndex={i}
            allBlocks={blocks}
            onBlocksChange={onBlocksChange}
            minutesToPercent={minutesToPercent}
            durationToPercent={durationToPercent}
            startResize={startResize}
          />
        ))}
      </div>
    </div>
  )
}
