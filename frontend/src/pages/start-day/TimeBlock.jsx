import { useState, useCallback } from 'react'
import { useDraggable } from '@dnd-kit/core'
import { useNavigate } from 'react-router-dom'

/**
 * A single draggable, resizable time block on the calendar grid.
 *
 * Adapts its layout based on available width:
 *   - Wide (>= 140px): title and time side by side
 *   - Medium (>= 70px): title stacked above time
 *   - Narrow (< 70px): title only, no time range
 */
export function TimeBlock({
  block,
  blockIndex,
  allBlocks,
  onBlocksChange,
  onRemove,
  minutesToPercent,
  durationToPercent,
  startResize,
  showStartButton = true,
}) {
  const isCompleted = block.task?.status === 'COMPLETED'
  const navigate = useNavigate()
  const [blockWidth, setBlockWidth] = useState(200)

  const measuredRef = useCallback((node) => {
    if (!node) return
    const observer = new ResizeObserver(([entry]) => {
      setBlockWidth(entry.contentRect.width)
    })
    observer.observe(node)
    return () => observer.disconnect()
  }, [])

  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: `calendar-block-${block.id}`,
    data: { type: 'calendar-block', block, blockIndex },
    disabled: isCompleted,
  })

  const mergedRef = (el) => {
    setNodeRef(el)
    measuredRef(el)
  }

  const left = minutesToPercent(block.startMinutes)
  const width = durationToPercent(block.endMinutes - block.startMinutes)

  const style = {
    position: 'absolute',
    left: `${left}%`,
    width: `${Math.max(width, 1)}%`,
    top: '3px',
    bottom: '3px',
    transform: transform ? `translateX(${transform.x}px)` : undefined,
    opacity: isDragging ? 0.4 : 1,
    zIndex: isDragging ? 20 : 1,
    cursor: isCompleted ? 'default' : isDragging ? 'grabbing' : 'grab',
    userSelect: 'none',
  }

  const startLabel = `${Math.floor(block.startMinutes / 60)}:${String(block.startMinutes % 60).padStart(2, '0')}`
  const endLabel = `${Math.floor(block.endMinutes / 60)}:${String(block.endMinutes % 60).padStart(2, '0')}`

  const isWide = blockWidth >= 140
  const isMedium = blockWidth >= 70 && blockWidth < 140
  const isNarrow = blockWidth < 70

  const timeSpan = (
    <span className={`text-[10px] leading-tight shrink-0 ${isCompleted ? 'text-ink-muted' : 'text-primary-200'}`}>
      {startLabel}–{endLabel}
    </span>
  )

  return (
    <div
      ref={mergedRef}
      style={style}
      className={`rounded select-none border group overflow-hidden ${
        isCompleted
          ? 'bg-surface-soft border-edge text-ink-muted'
          : 'bg-primary-500 border-primary-600 text-white'
      } ${isWide ? 'flex items-center' : 'flex flex-col justify-center'}`}
      {...(isCompleted ? {} : { ...listeners, ...attributes })}
    >
      {/* Block body — adapts layout based on width */}
      {isWide ? (
        <div className="flex-1 flex items-center gap-1 px-2 overflow-hidden min-w-0">
          <span className="text-xs font-medium truncate">
            {block.task?.title ?? 'Untitled'}
          </span>
          {timeSpan}
        </div>
      ) : (
        <div className="flex flex-col px-1.5 overflow-hidden min-w-0">
          <span className="text-[11px] font-medium truncate leading-tight">
            {block.task?.title ?? 'Untitled'}
          </span>
          {isMedium && timeSpan}
        </div>
      )}

      {/* Start button — appears on hover for incomplete blocks (hidden during planning) */}
      {showStartButton && block.task?.status !== 'COMPLETED' && isWide && (
        <button
          onClick={(e) => {
            e.stopPropagation()
            navigate(`/session/${block.id}`)
          }}
          onPointerDown={(e) => e.stopPropagation()}
          className="opacity-0 group-hover:opacity-100 transition-opacity text-white/80 hover:text-white text-xs font-medium bg-primary-600/50 hover:bg-primary-600/80 rounded px-2 py-0.5 shrink-0"
        >
          Start
        </button>
      )}

      {/* Remove button — upper right */}
      {!isCompleted && onRemove && (
        <button
          className="absolute top-0 right-0 w-4 h-4 rounded-bl bg-black/20 hover:bg-red-500 text-white/80 hover:text-white flex items-center justify-center text-[10px] leading-none opacity-0 group-hover:opacity-100 transition-all z-10"
          onPointerDown={(e) => e.stopPropagation()}
          onClick={(e) => {
            e.stopPropagation()
            onRemove(block.id)
          }}
          aria-label={`Remove ${block.task?.title ?? 'block'} from plan`}
        >
          ×
        </button>
      )}

      {/* Resize handle — right edge */}
      {!isCompleted && (
        <div
          className="absolute right-0 top-0 bottom-0 w-2 cursor-ew-resize hover:bg-white/20 rounded-r"
          onPointerDown={(e) => e.stopPropagation()}
          onMouseDown={(e) => startResize(e, block, blockIndex, allBlocks, onBlocksChange)}
        />
      )}
    </div>
  )
}
