import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getStreak } from '@/api/reflection'

export function CompletionPhase({ level }) {
  const navigate = useNavigate()
  const [streak, setStreak] = useState(null)

  useEffect(() => {
    getStreak().then(data => setStreak(data.streak)).catch(() => setStreak(0))
  }, [])

  const streakMessage = streak === 1
    ? 'Day 1 — you showed up.'
    : streak > 1
    ? `${streak} days in a row. Keep it going.`
    : 'Good work today.'

  const wrapMessage = {
    day: "That's a wrap for today.",
    week: "That's a wrap for the week.",
    month: "That's a wrap for the month.",
  }[level] || "That's a wrap."

  return (
    <div className="text-center py-8">
      <div className="text-4xl mb-3">✨</div>
      <p className="text-xl font-semibold text-ink-heading">{streakMessage}</p>
      <p className="mt-2 text-ink-secondary text-sm">{wrapMessage}</p>
      <button
        onClick={() => navigate('/')}
        className="mt-8 px-5 py-2 text-sm rounded-md bg-primary-500 text-white hover:bg-primary-600 transition-colors"
      >
        Done
      </button>
    </div>
  )
}
