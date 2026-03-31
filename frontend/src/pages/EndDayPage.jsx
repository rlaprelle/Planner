import { useState, useRef } from 'react'
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { getDeferredItems } from '@/api/deferred'
import { DeferredItemActions } from '@/components/deferred/DeferredItemActions'
import { saveReflection, getStreak } from '@/api/reflection'
import { getTodayCompletedTasks } from '@/api/tasks'

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
        <p className="text-xl font-semibold text-gray-900">{streakMessage}</p>
        <p className="mt-2 text-gray-500 text-sm">That's a wrap for today.</p>
        <button
          onClick={() => navigate('/')}
          className="mt-8 px-5 py-2 text-sm rounded-md bg-indigo-600 text-white hover:bg-indigo-700 transition-colors"
        >
          Done
        </button>
      </div>
    )
  }

  return (
    <div>
      <h2 className="text-xl font-semibold text-gray-900 mb-6">How did today go?</h2>

      {completedTasks.length > 0 && (
        <div className="mb-6">
          <p className="text-xs font-medium text-gray-400 uppercase tracking-wider mb-2">
            Completed today
          </p>
          <ul className="space-y-1">
            {completedTasks.map((t) => (
              <li key={t.id} className="text-sm text-gray-700 flex items-center gap-2">
                <span className="text-indigo-400">✓</span>
                {t.title}
              </li>
            ))}
          </ul>
        </div>
      )}

      <form onSubmit={(e) => { e.preventDefault(); mutation.mutate() }} className="space-y-6">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Energy — <span className="text-indigo-600">{ENERGY_LABELS[energy]}</span>
          </label>
          <input
            type="range" min="1" max="5" step="1"
            value={energy}
            onChange={(e) => setEnergy(Number(e.target.value))}
            className="w-full accent-indigo-600"
          />
          <div className="flex justify-between text-xs text-gray-400 mt-1">
            <span>Drained</span><span>Energized</span>
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Mood — <span className="text-indigo-600">{MOOD_LABELS[mood]}</span>
          </label>
          <input
            type="range" min="1" max="5" step="1"
            value={mood}
            onChange={(e) => setMood(Number(e.target.value))}
            className="w-full accent-indigo-600"
          />
          <div className="flex justify-between text-xs text-gray-400 mt-1">
            <span>Rough</span><span>Great</span>
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Anything on your mind? <span className="text-gray-400 font-normal">(optional)</span>
          </label>
          <textarea
            rows={3}
            placeholder="Anything on your mind?"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 resize-none"
          />
        </div>

        {mutation.isError && (
          <p className="text-sm text-red-500">Something went wrong. Try again.</p>
        )}

        <button
          type="submit"
          disabled={mutation.isPending}
          className="w-full py-2.5 text-sm rounded-md bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50 transition-colors font-medium"
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

  const [currentIndex, setCurrentIndex] = useState(0)
  const [phase, setPhase] = useState(1)
  const [showCelebration, setShowCelebration] = useState(false)
  // Capture total at load time so stale query cache doesn't affect "remaining" calculation
  const totalItemsRef = useRef(null)
  if (!isLoading && totalItemsRef.current === null) {
    totalItemsRef.current = items.length
  }

  function handleItemDone() {
    queryClient.invalidateQueries({ queryKey: ['deferred'] })
    const remaining = totalItemsRef.current - (currentIndex + 1)
    if (remaining <= 0) {
      setShowCelebration(true)
      setTimeout(() => {
        setShowCelebration(false)
        setPhase(2)
      }, 2000)
    } else {
      setCurrentIndex((i) => i + 1)
    }
  }

  if (isLoading) {
    return <div className="p-8 text-gray-400">Loading…</div>
  }

  if (phase === 2) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-start justify-center pt-16 px-4">
        <div className="w-full max-w-lg">
          <Phase2 />
        </div>
      </div>
    )
  }

  const noItems = items.length === 0
  const currentItem = items[currentIndex]

  if (showCelebration) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="text-5xl mb-4">🎉</div>
          <h2 className="text-2xl font-semibold text-gray-900">Inbox Zero!</h2>
          <p className="mt-2 text-gray-500">All caught up.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50 flex items-start justify-center pt-16 px-4">
      <div className="w-full max-w-lg">
        <h1 className="text-2xl font-semibold text-gray-900 mb-2">End of Day</h1>

        {noItems ? (
          <div className="mb-8">
            <p className="text-gray-500 text-sm mb-6">Inbox is clear.</p>
          </div>
        ) : (
          <div className="mb-8">
            <p className="text-sm text-gray-400 mb-4">
              {currentIndex + 1} of {items.length}
            </p>
            <div className="bg-white border border-gray-200 rounded-lg p-6 shadow-sm">
              {currentItem && (
                <DeferredItemActions item={currentItem} onDone={handleItemDone} />
              )}
            </div>
          </div>
        )}

        {noItems && (
          <button
            onClick={() => setPhase(2)}
            className="px-4 py-2 text-sm rounded-md bg-indigo-600 text-white hover:bg-indigo-700 transition-colors"
          >
            Continue to reflection →
          </button>
        )}
      </div>
    </div>
  )
}
