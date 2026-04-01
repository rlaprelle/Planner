import { useDroppable } from '@dnd-kit/core'
import { TimeBlock } from './TimeBlock'
import { DAY_START_MINUTES, DAY_DURATION } from './useTimeGrid'

const TOTAL_HOURS = DAY_DURATION / 60 // 9

// Hour boundary marks for labels and grid lines: 8 AM through 5 PM
const HOUR_MARKS = Array.from(
  { length: TOTAL_HOURS + 1 },
  (_, i) => DAY_START_MINUTES / 60 + i
) // [8, 9, 10, 11, 12, 13, 14, 15, 16, 17]

function formatHour(h) {
  if (h === 12) return '12 PM'
  if (h < 12) return `${h} AM`
  return `${h - 12} PM`
}

function hourToPercent(h) {
  return ((h * 60 - DAY_START_MINUTES) / DAY_DURATION) * 100
}

/**
 * The horizontal planner grid.
 *
 * Labels and grid lines are absolutely positioned using the same
 * percentage math as the time blocks (minutesToPercent), so visual
 * positions always match the logical time positions.
 */
export function TimeBlockGrid({
  blocks,
  onBlocksChange,
  onRemoveBlock,
  dropPreview,
  gridRef,
  minutesToPercent,
  durationToPercent,
  startResize,
}) {
  const { setNodeRef, isOver } = useDroppable({ id: 'time-block-grid' })

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
      {/* Hour labels — positioned at true hour boundaries */}
      <div className="relative h-7 border-b border-gray-100">
        {HOUR_MARKS.map((hour, idx) => {
          const isFirst = idx === 0
          const isLast = idx === HOUR_MARKS.length - 1
          return (
            <span
              key={hour}
              className="absolute top-1 text-xs text-gray-400 whitespace-nowrap"
              style={{
                left: isLast ? undefined : `${hourToPercent(hour)}%`,
                right: isLast ? 0 : undefined,
                paddingLeft: isFirst ? 4 : undefined,
                paddingRight: isLast ? 4 : undefined,
                transform: isFirst || isLast ? undefined : 'translateX(-50%)',
              }}
            >
              {formatHour(hour)}
            </span>
          )
        })}
      </div>

      {/* Grid body with hour lines and 15-min sub-lines */}
      <div className="relative h-14">
        {/* Hour boundary lines (container border handles the outer edges) */}
        {HOUR_MARKS.slice(1, -1).map((hour) => (
          <div
            key={`h-${hour}`}
            className="absolute top-0 bottom-0 border-l border-gray-200"
            style={{ left: `${hourToPercent(hour)}%` }}
          />
        ))}

        {/* 15-min sub-lines within each hour span */}
        {Array.from({ length: TOTAL_HOURS }, (_, i) => {
          const spanStartPct = (i / TOTAL_HOURS) * 100
          const spanWidthPct = 100 / TOTAL_HOURS
          return [0.25, 0.5, 0.75].map((frac) => (
            <div
              key={`s-${i}-${frac}`}
              className={`absolute top-0 bottom-0 border-l ${
                frac === 0.5 ? 'border-gray-200' : 'border-gray-100'
              }`}
              style={{ left: `${spanStartPct + frac * spanWidthPct}%` }}
            />
          ))
        })}
      </div>

      {/* Absolute overlay for time blocks — uses gridRef for pixel-to-time conversion */}
      <div
        ref={mergedRef}
        className="absolute left-0 right-0 bottom-0"
        style={{ top: '1.75rem' }}
      >
        {blocks.map((block, i) => (
          <TimeBlock
            key={block.id}
            block={block}
            blockIndex={i}
            allBlocks={blocks}
            onBlocksChange={onBlocksChange}
            onRemove={onRemoveBlock}
            minutesToPercent={minutesToPercent}
            durationToPercent={durationToPercent}
            startResize={startResize}
          />
        ))}

        {/* Ghost preview while dragging a task card over the grid */}
        {dropPreview && (
          <div
            className="absolute rounded border-2 border-dashed border-indigo-400 bg-indigo-200/40 pointer-events-none"
            style={{
              left: `${minutesToPercent(dropPreview.startMinutes)}%`,
              width: `${durationToPercent(dropPreview.endMinutes - dropPreview.startMinutes)}%`,
              top: '3px',
              bottom: '3px',
            }}
          />
        )}
      </div>
    </div>
  )
}
