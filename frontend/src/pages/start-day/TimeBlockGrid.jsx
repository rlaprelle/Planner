import { useDroppable } from '@dnd-kit/core'
import { TimeBlock } from './TimeBlock'
import { EventBlock } from './EventBlock'

function formatHour(h) {
  if (h === 12) return '12 PM'
  if (h < 12) return `${h} AM`
  return `${h - 12} PM`
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
  dayStartMinutes,
  dayEndMinutes,
}) {
  const { setNodeRef, isOver } = useDroppable({ id: 'time-block-grid' })

  const dayDuration = dayEndMinutes - dayStartMinutes
  const totalHours = dayDuration / 60

  const hourMarks = Array.from(
    { length: totalHours + 1 },
    (_, i) => dayStartMinutes / 60 + i
  )

  function hourToPercent(h) {
    return ((h * 60 - dayStartMinutes) / dayDuration) * 100
  }

  const mergedRef = (el) => {
    setNodeRef(el)
    gridRef.current = el
  }

  return (
    <div
      className={`relative border border-edge rounded-lg overflow-hidden transition-colors ${
        isOver ? 'bg-primary-50' : 'bg-surface-raised'
      }`}
    >
      {/* Hour labels — positioned at true hour boundaries */}
      <div className="relative h-7 border-b border-edge-subtle">
        {hourMarks.map((hour, idx) => {
          const isFirst = idx === 0
          const isLast = idx === hourMarks.length - 1
          return (
            <span
              key={hour}
              className="absolute top-1 text-xs text-ink-muted whitespace-nowrap"
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
        {hourMarks.slice(1, -1).map((hour) => (
          <div
            key={`h-${hour}`}
            className="absolute top-0 bottom-0 border-l border-edge"
            style={{ left: `${hourToPercent(hour)}%` }}
          />
        ))}

        {/* 15-min sub-lines within each hour span */}
        {Array.from({ length: totalHours }, (_, i) => {
          const spanStartPct = (i / totalHours) * 100
          const spanWidthPct = 100 / totalHours
          return [0.25, 0.5, 0.75].map((frac) => (
            <div
              key={`s-${i}-${frac}`}
              className={`absolute top-0 bottom-0 border-l ${
                frac === 0.5 ? 'border-edge' : 'border-edge-subtle'
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
        {blocks.map((block, i) =>
          block.isEvent ? (
            <EventBlock
              key={block.id}
              block={block}
              minutesToPercent={minutesToPercent}
              durationToPercent={durationToPercent}
            />
          ) : (
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
          )
        )}

        {/* Ghost preview while dragging a task card over the grid */}
        {dropPreview && (
          <div
            className="absolute rounded border-2 border-dashed border-primary-400 bg-primary-200/40 pointer-events-none"
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
