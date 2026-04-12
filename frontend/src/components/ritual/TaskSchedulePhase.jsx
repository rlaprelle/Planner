import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { getActiveTasks, rescheduleTask } from '@/api/tasks'
import { getPreferences } from '@/api/preferences'

// Maps API day names (MONDAY, SUNDAY, etc.) to JS Date.getDay() values (0=Sun, 1=Mon, ...)
const DAY_TO_JS = { SUNDAY: 0, MONDAY: 1, TUESDAY: 2, WEDNESDAY: 3, THURSDAY: 4, FRIDAY: 5, SATURDAY: 6 }

function getWeeksInMonth(year, month, i18n, weekStartDay = 'MONDAY') {
  const startDayJs = DAY_TO_JS[weekStartDay] ?? 1
  const weeks = []
  const first = new Date(year, month, 1)
  // Find the configured week-start day of the week containing the 1st
  let weekStart = new Date(first)
  const day = weekStart.getDay()
  const diff = (day - startDayJs + 7) % 7
  weekStart.setDate(weekStart.getDate() - diff)

  const lastDay = new Date(year, month + 1, 0)
  const fmt = new Intl.DateTimeFormat(i18n.language, { month: 'numeric', day: 'numeric' })

  while (weekStart <= lastDay) {
    const weekEnd = new Date(weekStart)
    weekEnd.setDate(weekEnd.getDate() + 6)
    weeks.push({
      start: new Date(weekStart),
      label: `${fmt.format(weekStart)} – ${fmt.format(weekEnd)}`,
      isoStart: weekStart.toISOString().split('T')[0],
    })
    weekStart = new Date(weekStart)
    weekStart.setDate(weekStart.getDate() + 7)
  }
  return weeks
}

function getDaysInWeek(t, i18n, weekStartDay = 'MONDAY') {
  const startDayJs = DAY_TO_JS[weekStartDay] ?? 1
  // Day name keys ordered Sun(0)..Sat(6) to match JS getDay()
  const nameKeys = ['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat']

  const today = new Date()
  const currentDayJs = today.getDay()
  const diff = (currentDayJs - startDayJs + 7) % 7
  const weekStart = new Date(today)
  weekStart.setDate(weekStart.getDate() - diff)

  const days = []
  const fmt = new Intl.DateTimeFormat(i18n.language, { month: 'numeric', day: 'numeric' })
  for (let i = 0; i < 7; i++) {
    const d = new Date(weekStart)
    d.setDate(d.getDate() + i)
    days.push({
      label: t(nameKeys[d.getDay()]),
      dateLabel: fmt.format(d),
      iso: d.toISOString().split('T')[0],
    })
  }
  return days
}

export function TaskSchedulePhase({ mode, onPhaseComplete }) {
  const { t, i18n } = useTranslation('ritual')
  const queryClient = useQueryClient()
  const [currentIndex, setCurrentIndex] = useState(0)

  const { data: prefs } = useQuery({
    queryKey: ['preferences'],
    queryFn: getPreferences,
  })
  const weekStartDay = prefs?.weekStartDay || 'MONDAY'

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
    return <div className="p-8 text-ink-muted text-sm">{t('loadingTasks')}</div>
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
  const weeks = mode === 'month' ? getWeeksInMonth(now.getFullYear(), now.getMonth(), i18n, weekStartDay) : []
  const days = mode === 'week' ? getDaysInWeek(t, i18n, weekStartDay) : []

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <span className="text-sm text-ink-muted">
          {t('scheduleProgress', { current: currentIndex + 1, total: tasks.length })}
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
                {t('dueLabel', { date: task.dueDate })}
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Schedule options */}
      {mode === 'month' && (
        <div className="space-y-3 mb-4">
          <p className="text-sm font-medium text-ink-body">{t('assignToWeek')}</p>
          <div className="grid grid-cols-2 gap-2">
            {weeks.map((week) => (
              <button
                key={week.isoStart}
                onClick={() => rescheduleMutation.mutate({
                  taskId: task.id,
                  visibleFrom: week.isoStart,
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
          <p className="text-sm font-medium text-ink-body">{t('assignToDay')}</p>
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
        {t('leaveForNow')}
      </button>

      {tasks.length - currentIndex > 2 && (
        <div className="mt-6 pt-4 border-t border-edge-subtle">
          <button
            onClick={onPhaseComplete}
            className="text-xs text-ink-muted hover:text-ink-secondary transition-colors"
          >
            {t('skipRemaining', { count: tasks.length - currentIndex })}
          </button>
        </div>
      )}

      {rescheduleMutation.isError && (
        <p className="mt-4 text-sm text-error">{t('common:tryAgainError')}</p>
      )}
    </div>
  )
}
