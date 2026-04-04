import { useState, useRef } from 'react'
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { getDeferredItems } from '@/api/deferred'
import { DeferredItemActions } from '@/components/deferred/DeferredItemActions'
import { saveReflection, getStreak } from '@/api/reflection'
import { getTodayCompletedTasks } from '@/api/tasks'
import { getDashboard } from '@/api/dashboard'

const ENERGY_LABELS = { 1: 'Drained', 2: 'Low', 3: 'Okay', 4: 'Good', 5: 'Energized' }
const MOOD_LABELS = { 1: 'Rough', 2: 'Meh', 3: 'Okay', 4: 'Good', 5: 'Great' }

function Phase2() {
  const [energy, setEnergy] = useState(3)
  const [mood, setMood] = useState(3)
  const [notes, setNotes] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [streak, setStreak] = useState(null)

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
      saveReflection({ energyRating: energy, moodRating: mood, reflectionNotes: notes || null, isFinalized: true }),
    onSuccess: async () => {
      const data = await getStreak()
      setStreak(data.streak)
      setSubmitted(true)
    },
  })

  const navigate = useNavigate()

  if (submitted) {
    const streakMessage = streak === 1
      ? 'Day 1 — you showed up.'
      : streak > 1
      ? `${streak} days in a row. Keep it going.`
      : 'Good work today.'

    return (
      <div className="text-center py-8">
        <div className="text-4xl mb-3">✨</div>
        <p className="text-xl font-semibold text-ink-heading">{streakMessage}</p>
        <p className="mt-2 text-ink-secondary text-sm">That's a wrap for today.</p>
        <button
          onClick={() => navigate('/')}
          className="mt-8 px-5 py-2 text-sm rounded-md bg-primary-500 text-white hover:bg-primary-600 transition-colors"
        >
          Done
        </button>
      </div>
    )
  }

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
          {mutation.isPending ? 'Saving…' : 'Wrap up the day'}
        </button>
      </form>
    </div>
  )
}

export function EndDayPage() {
  const queryClient = useQueryClient()
  const { data: items = [], isLoading } = useQuery({
    queryKey: ['deferred'],
    queryFn: getDeferredItems,
  })

  const [processedCount, setProcessedCount] = useState(0)
  const [phase, setPhase] = useState(1)
  const [showCelebration, setShowCelebration] = useState(false)
  // Capture total at load time so progress counter stays stable as items are removed
  const totalItemsRef = useRef(null)
  if (!isLoading && totalItemsRef.current === null) {
    totalItemsRef.current = items.length
  }

  function handleItemDone() {
    queryClient.invalidateQueries({ queryKey: ['deferred'] })
    const newCount = processedCount + 1
    setProcessedCount(newCount)
    if (newCount >= totalItemsRef.current) {
      setShowCelebration(true)
      setTimeout(() => {
        setShowCelebration(false)
        setPhase(2)
      }, 2000)
    }
  }

  if (isLoading) {
    return <div className="p-8 text-ink-muted">Loading…</div>
  }

  if (phase === 2) {
    return (
      <div className="min-h-screen bg-surface flex items-start justify-center pt-16 px-4">
        <div className="w-full max-w-lg">
          <Phase2 />
        </div>
      </div>
    )
  }

  const noItems = totalItemsRef.current === 0
  const currentItem = items[0]

  if (showCelebration) {
    return (
      <div className="min-h-screen bg-surface flex items-center justify-center">
        <div className="text-center">
          <div className="text-5xl mb-4">🎉</div>
          <h2 className="text-2xl font-semibold text-ink-heading">Inbox Zero!</h2>
          <p className="mt-2 text-ink-secondary">All caught up.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-surface flex items-start justify-center pt-16 px-4">
      <div className="w-full max-w-lg">
        <h1 className="text-2xl font-semibold text-ink-heading mb-2">End of Day</h1>

        {noItems ? (
          <div className="mb-8">
            <p className="text-ink-secondary text-sm mb-6">Inbox is clear.</p>
          </div>
        ) : (
          <div className="mb-8">
            <p className="text-sm text-ink-muted mb-4">
              {processedCount + 1} of {totalItemsRef.current}
            </p>
            <div className="bg-surface-raised border border-edge rounded-lg p-6 shadow-card">
              {currentItem && (
                <DeferredItemActions key={currentItem.id} item={currentItem} onDone={handleItemDone} />
              )}
            </div>
          </div>
        )}

        {noItems && (
          <button
            onClick={() => setPhase(2)}
            className="px-4 py-2 text-sm rounded-md bg-primary-500 text-white hover:bg-primary-600 transition-colors"
          >
            Continue to reflection →
          </button>
        )}
      </div>
    </div>
  )
}
