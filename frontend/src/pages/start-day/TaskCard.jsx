import { useDraggable } from '@dnd-kit/core'

const DEADLINE_BADGE = {
  TODAY: { label: 'TODAY', className: 'bg-red-100 text-red-700' },
  THIS_WEEK: { label: 'THIS WK', className: 'bg-amber-100 text-amber-700' },
}

/**
 * A single task card shown in the task browser rows.
 *
 * Props:
 *   task        - TaskResponse from server
 *   isSelected  - whether the checkbox is checked
 *   isScheduled - task is already on the calendar (checkbox disabled)
 *   onToggle(taskId) - called when checkbox changes
 */
export function TaskCard({ task, isSelected, isScheduled, onToggle }) {
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
    id: `task-card-${task.id}`,
    data: { type: 'task-card', task },
    disabled: isScheduled,
  })

  const badge = DEADLINE_BADGE[task.deadlineGroup]

  return (
    <div
      ref={setNodeRef}
      className={`flex items-center gap-2 bg-white rounded px-2 py-1.5 border text-xs
        ${isDragging ? 'opacity-40 border-indigo-300' : 'border-gray-200'}
        ${isScheduled ? 'opacity-50' : 'cursor-grab hover:border-indigo-300 hover:shadow-sm'}
        transition-all`}
      {...(isScheduled ? {} : { ...listeners, ...attributes })}
    >
      <input
        type="checkbox"
        checked={isSelected || isScheduled}
        disabled={isScheduled}
        onChange={() => onToggle(task.id)}
        onClick={(e) => e.stopPropagation()}
        className="shrink-0 accent-indigo-600"
      />
      <span className="truncate flex-1 text-gray-800">{task.title}</span>
      {badge && (
        <span className={`shrink-0 rounded px-1 py-0.5 text-[10px] font-semibold ${badge.className}`}>
          {badge.label}
        </span>
      )}
      {task.pointsEstimate != null && (
        <span className="shrink-0 text-gray-400">{task.pointsEstimate}pt</span>
      )}
    </div>
  )
}
