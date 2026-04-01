import { useDraggable } from '@dnd-kit/core'

/**
 * A single draggable, resizable time block on the calendar grid.
 *
 * Props:
 *   block          - { id, startMinutes, endMinutes, task: { title, status } }
 *   blockIndex     - index in the blocks array
 *   allBlocks      - the full blocks array (passed to startResize)
 *   onBlocksChange - called when blocks change via resize
 *   minutesToPercent(n)    - grid helper from useTimeGrid
 *   durationToPercent(n)   - grid helper from useTimeGrid
 *   startResize(e, block, index, blocks, onChange) - from useTimeGrid
 */
export function TimeBlock({
  block,
  blockIndex,
  allBlocks,
  onBlocksChange,
  minutesToPercent,
  durationToPercent,
  startResize,
}) {
  const isCompleted = block.task?.status === 'DONE'

  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: `calendar-block-${block.id}`,
    data: { type: 'calendar-block', block, blockIndex },
    disabled: isCompleted,
  })

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

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={`rounded flex items-center overflow-hidden select-none border ${
        isCompleted
          ? 'bg-gray-100 border-gray-200 text-gray-400'
          : 'bg-indigo-500 border-indigo-600 text-white'
      }`}
      {...(isCompleted ? {} : { ...listeners, ...attributes })}
    >
      {/* Block body — shows title and time */}
      <div className="flex-1 flex items-center gap-1 px-2 overflow-hidden min-w-0">
        <span className="text-xs font-medium truncate">
          {block.task?.title ?? 'Untitled'}
        </span>
        <span className={`text-xs shrink-0 ${isCompleted ? 'text-gray-400' : 'text-indigo-200'}`}>
          {startLabel}–{endLabel}
        </span>
      </div>

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
