import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { saveReflection } from '@/api/reflection'
import { getTodayCompletedTasks } from '@/api/tasks'
import { getDashboard } from '@/api/dashboard'

const ENERGY_LABELS = { 1: 'Drained', 2: 'Low', 3: 'Okay', 4: 'Good', 5: 'Energized' }
const MOOD_LABELS = { 1: 'Rough', 2: 'Meh', 3: 'Okay', 4: 'Good', 5: 'Great' }

export function DailyReflectionPhase({ onPhaseComplete, reflectionType = 'DAILY' }) {
  const [energy, setEnergy] = useState(3)
  const [mood, setMood] = useState(3)
  const [notes, setNotes] = useState('')

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
      <h2 className="text-xl font-semibold text-ink-heading mb-6">How did today go?</h2>

      {completedTasks.length > 0 && (
        <div className="mb-6">
          <p className="text-xs font-medium text-ink-muted uppercase tracking-wider mb-2">
            Completed today
          </p>
          <ul className="space-y-1">
            {completedTasks.map((t) => (
              <li key={t.id} className="text-sm text-ink-body flex items-center gap-2">
                <span className="text-primary-400">✓</span>
                {t.title}
              </li>
            ))}
          </ul>
        </div>
      )}

      {celebrations.length > 0 && (
        <div className="mb-6">
          <p className="text-xs font-medium text-ink-muted uppercase tracking-wider mb-2">
            Notable today
          </p>
          <div className="space-y-2">
            {celebrations.map((c) => (
              <div
                key={c.taskId}
                className="bg-primary-50/70 border border-primary-100 rounded-lg px-4 py-3"
              >
                <p className="text-sm font-medium text-ink-heading">{c.taskTitle}</p>
                <p className="text-xs text-ink-secondary mt-0.5">
                  {c.projectName} — {c.reason}
                </p>
              </div>
            ))}
          </div>
        </div>
      )}

      <form onSubmit={(e) => { e.preventDefault(); mutation.mutate() }} className="space-y-6">
        <div>
          <label className="block text-sm font-medium text-ink-body mb-2">
            Energy — <span className="text-primary-500">{ENERGY_LABELS[energy]}</span>
          </label>
          <input
            type="range" min="1" max="5" step="1"
            value={energy}
            onChange={(e) => setEnergy(Number(e.target.value))}
            className="w-full accent-primary-500"
          />
          <div className="flex justify-between text-xs text-ink-muted mt-1">
            <span>Drained</span><span>Energized</span>
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-ink-body mb-2">
            Mood — <span className="text-primary-500">{MOOD_LABELS[mood]}</span>
          </label>
          <input
            type="range" min="1" max="5" step="1"
            value={mood}
            onChange={(e) => setMood(Number(e.target.value))}
            className="w-full accent-primary-500"
          />
          <div className="flex justify-between text-xs text-ink-muted mt-1">
            <span>Rough</span><span>Great</span>
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-ink-body mb-2">
            Anything on your mind? <span className="text-ink-muted font-normal">(optional)</span>
          </label>
          <textarea
            rows={3}
            placeholder="Anything on your mind?"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            className="w-full rounded-md border border-edge px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-edge-focus resize-none"
          />
        </div>

        {mutation.isError && (
          <p className="text-sm text-error">Something went wrong. Try again.</p>
        )}

        <button
          type="submit"
          disabled={mutation.isPending}
          className="w-full py-2.5 text-sm rounded-md bg-primary-500 text-white hover:bg-primary-600 disabled:opacity-50 transition-colors font-medium"
        >
          {mutation.isPending ? 'Saving…' : 'Continue'}
        </button>
      </form>
    </div>
  )
}
