import { useDraggable } from '@dnd-kit/core'
import { useTranslation } from 'react-i18next'

const DEADLINE_BADGE_CLASS = {
  TODAY: 'bg-deadline-today-bg text-deadline-today-text',
  THIS_WEEK: 'bg-deadline-week-bg text-deadline-week-text',
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
export function TaskCard({ task, isSelected, isScheduled, onToggle, section = 'default' }) {
  const { t } = useTranslation('timeBlocking')
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
    id: `task-card-${section}-${task.id}`,
    data: { type: 'task-card', task },
    disabled: isScheduled,
  })

  const DEADLINE_BADGE_LABEL = {
    TODAY: t('deadlineToday'),
    THIS_WEEK: t('deadlineThisWeek'),
  }

  const badgeClassName = DEADLINE_BADGE_CLASS[task.deadlineGroup]
  const badgeLabel = DEADLINE_BADGE_LABEL[task.deadlineGroup]
  const badge = badgeClassName ? { label: badgeLabel, className: badgeClassName } : undefined

  return (
    <div
      ref={setNodeRef}
      className={`flex items-center gap-2 bg-white rounded px-2 py-1.5 border text-xs
        ${isDragging ? 'opacity-40 border-primary-300' : 'border-edge'}
        ${isScheduled ? 'opacity-50' : 'cursor-grab hover:border-primary-300 hover:shadow-card'}
        transition-all`}
      {...(isScheduled ? {} : { ...listeners, ...attributes })}
    >
      <input
        type="checkbox"
        checked={isSelected || isScheduled}
        disabled={isScheduled}
        onChange={() => onToggle(task.id)}
        onClick={(e) => e.stopPropagation()}
        className="shrink-0 accent-primary-500"
      />
      <span className="truncate flex-1 text-ink-body">{task.title}</span>
      {badge && (
        <span className={`shrink-0 rounded px-1 py-0.5 text-[10px] font-semibold ${badge.className}`}>
          {badge.label}
        </span>
      )}
      {task.pointsEstimate != null && (
        <span className="shrink-0 text-ink-muted">{task.pointsEstimate}{t('pointsSuffix')}</span>
      )}
    </div>
  )
}
