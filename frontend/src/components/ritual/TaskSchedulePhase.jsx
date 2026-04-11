import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getActiveTasks, rescheduleTask } from '@/api/tasks'

function getWeeksInMonth(year, month) {
  const weeks = []
  const first = new Date(year, month, 1)
  // Find Monday of the week containing the 1st
  let monday = new Date(first)
  const day = monday.getDay()
  monday.setDate(monday.getDate() - (day === 0 ? 6 : day - 1))

  const lastDay = new Date(year, month + 1, 0)

  while (monday <= lastDay) {
    const sunday = new Date(monday)
    sunday.setDate(sunday.getDate() + 6)
    weeks.push({
      start: new Date(monday),
      label: `${monday.getMonth() + 1}/${monday.getDate()} – ${sunday.getMonth() + 1}/${sunday.getDate()}`,
      isoMonday: monday.toISOString().split('T')[0],
    })
    monday = new Date(monday)
    monday.setDate(monday.getDate() + 7)
  }
  return weeks
}

function getDaysInWeek() {
  const today = new Date()
  const day = today.getDay()
  const monday = new Date(today)
  monday.setDate(monday.getDate() - (day === 0 ? 6 : day - 1))

  const days = []
  const names = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
  for (let i = 0; i < 7; i++) {
    const d = new Date(monday)
    d.setDate(d.getDate() + i)
    days.push({
      label: names[i],
      dateLabel: `${d.getMonth() + 1}/${d.getDate()}`,
      iso: d.toISOString().split('T')[0],
    })
  }
  return days
}

export function TaskSchedulePhase({ mode, onPhaseComplete }) {
  const queryClient = useQueryClient()
  const [currentIndex, setCurrentIndex] = useState(0)

  const { data: tasks = [], isLoading } = useQuery({
    queryKey: ['tasks', 'active'],
    queryFn: getActiveTasks,
  })

  const rescheduleMutation = useMutation({
    mutationFn: ({ taskId, visibleFrom, schedulingScope }) =>
      rescheduleTask(taskId, { visibleFrom, schedulingScope }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      advance()
    },
  })

  function advance() {
    if (currentIndex + 1 >= tasks.length) {
      onPhaseComplete()
    } else {
      setCurrentIndex(currentIndex + 1)
    }
  }

  function handleSkip() {
    advance()
  }

  if (isLoading) {
    return <div className="p-8 text-ink-muted text-sm">Loading tasks…</div>
  }

  if (tasks.length === 0) {
    onPhaseComplete()
    return null
  }

  const task = tasks[currentIndex]
  if (!task) {
    onPhaseComplete()
    return null
  }

  const now = new Date()
  const weeks = mode === 'month' ? getWeeksInMonth(now.getFullYear(), now.getMonth()) : []
  const days = mode === 'week' ? getDaysInWeek() : []

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <span className="text-sm text-ink-muted">
          {currentIndex + 1} of {tasks.length}
        </span>
      </div>

      {/* Progress bar */}
      <div className="w-full bg-surface-soft rounded-full h-1.5 mb-6">
        <div
          className="bg-primary-400 h-1.5 rounded-full transition-all duration-300"
          style={{ width: `${(currentIndex / tasks.length) * 100}%` }}
        />
      </div>

      {/* Task card */}
      <div className="bg-surface-raised border border-edge rounded-xl p-6 shadow-card mb-6">
        <div className="flex items-start gap-3">
          <div
            className="w-3 h-3 rounded-full mt-1 flex-shrink-0"
            style={{ backgroundColor: task.projectColor || '#6366f1' }}
          />
          <div>
            <h3 className="text-lg font-medium text-ink-heading">{task.title}</h3>
            <p className="text-sm text-ink-muted mt-0.5">{task.projectName}</p>
            {task.dueDate && (
              <span className="inline-block mt-2 text-xs font-medium text-orange-600 bg-orange-50 px-2 py-0.5 rounded">
                Due: {task.dueDate}
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Schedule options */}
      {mode === 'month' && (
        <div className="space-y-3 mb-4">
          <p className="text-sm font-medium text-ink-body">Assign to a week:</p>
          <div className="grid grid-cols-2 gap-2">
            {weeks.map((week) => (
              <button
                key={week.isoMonday}
                onClick={() => rescheduleMutation.mutate({
                  taskId: task.id,
                  visibleFrom: week.isoMonday,
                  schedulingScope: 'WEEK',
                })}
                disabled={rescheduleMutation.isPending}
                className="py-2.5 px-3 text-sm rounded-lg border border-edge hover:bg-surface-soft disabled:opacity-50 transition-colors text-ink-body"
              >
                {week.label}
              </button>
            ))}
          </div>
        </div>
      )}

      {mode === 'week' && (
        <div className="space-y-3 mb-4">
          <p className="text-sm font-medium text-ink-body">Assign to a day:</p>
          <div className="grid grid-cols-7 gap-1">
            {days.map((day) => (
              <button
                key={day.iso}
                onClick={() => rescheduleMutation.mutate({
                  taskId: task.id,
                  visibleFrom: day.iso,
                  schedulingScope: 'DAY',
                })}
                disabled={rescheduleMutation.isPending}
                className="py-3 text-center text-sm rounded-lg border border-edge hover:bg-surface-soft disabled:opacity-50 transition-colors"
              >
                <div className="font-medium text-ink-body">{day.label}</div>
                <div className="text-xs text-ink-muted">{day.dateLabel}</div>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Skip / leave as-is */}
      <button
        onClick={handleSkip}
        disabled={rescheduleMutation.isPending}
        className="w-full py-2.5 text-sm rounded-lg bg-primary-50 text-primary-600 hover:bg-primary-100 disabled:opacity-50 transition-colors font-medium"
      >
        Leave for now
      </button>

      {tasks.length - currentIndex > 2 && (
        <div className="mt-6 pt-4 border-t border-edge-subtle">
          <button
            onClick={onPhaseComplete}
            className="text-xs text-ink-muted hover:text-ink-secondary transition-colors"
          >
            Skip remaining {tasks.length - currentIndex} tasks →
          </button>
        </div>
      )}

      {rescheduleMutation.isError && (
        <p className="mt-4 text-sm text-error">Something went wrong. Try again.</p>
      )}
    </div>
  )
}
