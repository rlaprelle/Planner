import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { updateTaskStatus } from '@/api/tasks'
import {
  PRIORITY_DOT_COLORS,
  DEADLINE_BADGE_COLORS,
} from './constants'
import { ChevronRightIcon, ChevronDownIcon } from './icons'

function PriorityDot({ priority, label }) {
  const color = PRIORITY_DOT_COLORS[priority] || 'bg-gray-300'
  return (
    <span
      className={`inline-block w-2 h-2 rounded-full flex-shrink-0 ${color}`}
      title={label}
    />
  )
}

function StatusCheckbox({ status, onChange, isPending, doneLabel, openLabel }) {
  const isDone = status === 'COMPLETED'
  return (
    <button
      type="button"
      onClick={onChange}
      disabled={isPending}
      title={isDone ? openLabel : doneLabel}
      className={[
        'flex-shrink-0 w-5 h-5 rounded border-2 flex items-center justify-center transition-colors focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1',
        isDone
          ? 'bg-success border-success text-white'
          : 'border-primary-300 hover:border-primary-400',
        isPending ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer',
      ].join(' ')}
      aria-label={isDone ? openLabel : doneLabel}
    >
      {isDone && (
        <svg width="10" height="10" viewBox="0 0 12 12" fill="none" aria-hidden="true">
          <polyline points="2,6 5,9 10,3" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      )}
    </button>
  )
}

export function TaskRow({ task, projectId, onSelect, selectedTaskId, depth = 0 }) {
  const { t, i18n } = useTranslation('tasks')
  const [childrenExpanded, setChildrenExpanded] = useState(false)
  const queryClient = useQueryClient()

  const statusMutation = useMutation({
    mutationFn: ({ taskId, status }) => updateTaskStatus(taskId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks', projectId] })
    },
  })

  function handleStatusToggle(e) {
    e.stopPropagation()
    const newStatus = task.status === 'COMPLETED' ? 'OPEN' : 'COMPLETED'
    statusMutation.mutate({ taskId: task.id, status: newStatus })
  }

  const hasChildren = task.children && task.children.length > 0
  const isDone = task.status === 'COMPLETED'
  const isSelected = selectedTaskId === task.id
  const deadlineBadgeColor = DEADLINE_BADGE_COLORS[task.deadlineGroup] || ''

  const formatDate = (dateStr) => {
    if (!dateStr) return null
    return new Intl.DateTimeFormat(i18n.language, { month: 'numeric', day: 'numeric', year: '2-digit' }).format(new Date(dateStr + 'T00:00:00'))
  }

  return (
    <div>
      <div
        className={[
          'flex items-center gap-2 py-2 rounded-lg group transition-colors',
          isSelected ? 'bg-surface-accent border border-primary-200' : 'hover:bg-surface-soft border border-transparent',
        ].join(' ')}
        style={{ paddingLeft: `${12 + depth * 20}px`, paddingRight: '8px' }}
      >
        {/* Children toggle arrow */}
        <button
          type="button"
          onClick={(e) => { e.stopPropagation(); setChildrenExpanded((v) => !v) }}
          className={[
            'flex-shrink-0 w-4 h-4 flex items-center justify-center text-ink-muted hover:text-ink-secondary focus:outline-none rounded transition-colors',
            hasChildren ? 'visible' : 'invisible',
          ].join(' ')}
          aria-label={childrenExpanded ? t('collapseSubtasks') : t('expandSubtasks')}
          tabIndex={hasChildren ? 0 : -1}
        >
          {childrenExpanded ? <ChevronDownIcon size={14} /> : <ChevronRightIcon size={14} />}
        </button>

        {/* Status checkbox */}
        <StatusCheckbox
          status={task.status}
          onChange={handleStatusToggle}
          isPending={statusMutation.isPending}
          doneLabel={t('common:markAsCompleted')}
          openLabel={t('common:markAsOpen')}
        />

        {/* Priority dot */}
        <PriorityDot priority={task.priority} label={t('priorityLabel', { priority: task.priority })} />

        {/* Title — click opens detail panel */}
        <button
          type="button"
          className={[
            'flex-1 min-w-0 text-sm truncate text-left cursor-pointer select-none bg-transparent border-0 p-0 focus:outline-none',
            isDone ? 'line-through text-ink-muted' : 'text-ink-body',
          ].join(' ')}
          onClick={() => onSelect(task)}
        >
          {task.title}
        </button>

        {/* Due date badge */}
        {task.dueDate && task.deadlineGroup !== 'NO_DEADLINE' && (
          <span className={`flex-shrink-0 text-xs px-1.5 py-0.5 rounded font-medium ${deadlineBadgeColor}`}>
            {formatDate(task.dueDate)}
          </span>
        )}
        {task.dueDate && task.deadlineGroup === 'NO_DEADLINE' && (
          <span className="flex-shrink-0 text-xs text-ink-muted">
            {formatDate(task.dueDate)}
          </span>
        )}

        {/* Open detail panel arrow */}
        <button
          type="button"
          onClick={() => onSelect(task)}
          className="flex-shrink-0 p-0.5 text-ink-faint hover:text-ink-secondary focus:outline-none rounded opacity-0 group-hover:opacity-100 focus:opacity-100 transition-opacity"
          aria-label={t('openDetails', { title: task.title })}
        >
          <ChevronRightIcon size={14} />
        </button>
      </div>

      {/* Children */}
      {hasChildren && childrenExpanded && (
        <div>
          {task.children.map((child) => (
            <TaskRow
              key={child.id}
              task={child}
              projectId={projectId}
              onSelect={onSelect}
              selectedTaskId={selectedTaskId}
              depth={depth + 1}
            />
          ))}
        </div>
      )}
    </div>
  )
}
