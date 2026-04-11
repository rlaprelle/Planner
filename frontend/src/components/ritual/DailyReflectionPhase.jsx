import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { saveReflection } from '@/api/reflection'
import { getTodayCompletedTasks } from '@/api/tasks'
import { getDashboard } from '@/api/dashboard'

export function DailyReflectionPhase({ onPhaseComplete, reflectionType = 'DAILY' }) {
  const { t } = useTranslation('ritual')
  const [energy, setEnergy] = useState(3)
  const [mood, setMood] = useState(3)
  const [notes, setNotes] = useState('')

  const energyLabels = {
    1: t('energyDrained'),
    2: t('energyLow'),
    3: t('energyOkay'),
    4: t('energyGood'),
    5: t('energyEnergized'),
  }
  const moodLabels = {
    1: t('moodRough'),
    2: t('moodMeh'),
    3: t('moodOkay'),
    4: t('moodGood'),
    5: t('moodGreat'),
  }

  const { data: completedTasks = [] } = useQuery({
    queryKey: ['tasks', 'completed-today'],
    queryFn: getTodayCompletedTasks,
  })

  const { data: dashboardData } = useQuery({
    queryKey: ['dashboard'],
    queryFn: getDashboard,
  })

  const celebrations = dashboardData?.celebrationTasks ?? []

  const mutation = useMutation({
    mutationFn: () =>
      saveReflection({
        energyRating: energy,
        moodRating: mood,
        reflectionNotes: notes || null,
        isFinalized: true,
        reflectionType,
      }),
    onSuccess: () => onPhaseComplete({ energy, mood }),
  })

  return (
    <div>
      <h2 className="text-xl font-semibold text-ink-heading mb-6">{t('howDidTodayGo')}</h2>

      {completedTasks.length > 0 && (
        <div className="mb-6">
          <p className="text-xs font-medium text-ink-muted uppercase tracking-wider mb-2">
            {t('completedToday')}
          </p>
          <ul className="space-y-1">
            {completedTasks.map((task) => (
              <li key={task.id} className="text-sm text-ink-body flex items-center gap-2">
                <span className="text-primary-400">{t('checkmark')}</span>
                {task.title}
              </li>
            ))}
          </ul>
        </div>
      )}

      {celebrations.length > 0 && (
        <div className="mb-6">
          <p className="text-xs font-medium text-ink-muted uppercase tracking-wider mb-2">
            {t('notableToday')}
          </p>
          <div className="space-y-2">
            {celebrations.map((c) => (
              <div
                key={c.taskId}
                className="bg-primary-50/70 border border-primary-100 rounded-lg px-4 py-3"
              >
                <p className="text-sm font-medium text-ink-heading">{c.taskTitle}</p>
                <p className="text-xs text-ink-secondary mt-0.5">
                  {t('notableItem', { project: c.projectName, reason: c.reason })}
                </p>
              </div>
            ))}
          </div>
        </div>
      )}

      <form onSubmit={(e) => { e.preventDefault(); mutation.mutate() }} className="space-y-6">
        <div>
          <label className="block text-sm font-medium text-ink-body mb-2">
            {t('energyLabel', { level: energyLabels[energy] })}
          </label>
          <input
            type="range" min="1" max="5" step="1"
            value={energy}
            onChange={(e) => setEnergy(Number(e.target.value))}
            className="w-full accent-primary-500"
          />
          <div className="flex justify-between text-xs text-ink-muted mt-1">
            <span>{t('energyDrained')}</span><span>{t('energyEnergized')}</span>
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-ink-body mb-2">
            {t('moodLabel', { level: moodLabels[mood] })}
          </label>
          <input
            type="range" min="1" max="5" step="1"
            value={mood}
            onChange={(e) => setMood(Number(e.target.value))}
            className="w-full accent-primary-500"
          />
          <div className="flex justify-between text-xs text-ink-muted mt-1">
            <span>{t('moodRough')}</span><span>{t('moodGreat')}</span>
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-ink-body mb-2">
            {t('reflectionLabel')} <span className="text-ink-muted font-normal">{t('common:optional')}</span>
          </label>
          <textarea
            rows={3}
            placeholder={t('reflectionPlaceholder')}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            className="w-full rounded-md border border-edge px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-edge-focus resize-none"
          />
        </div>

        {mutation.isError && (
          <p className="text-sm text-error">{t('common:tryAgainError')}</p>
        )}

        <button
          type="submit"
          disabled={mutation.isPending}
          className="w-full py-2.5 text-sm rounded-md bg-primary-500 text-white hover:bg-primary-600 disabled:opacity-50 transition-colors font-medium"
        >
          {mutation.isPending ? t('common:saving') : t('continue')}
        </button>
      </form>
    </div>
  )
}
