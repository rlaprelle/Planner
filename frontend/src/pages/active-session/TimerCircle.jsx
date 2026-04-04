import { useState, useEffect } from 'react'

const RADIUS = 70
const CIRCUMFERENCE = 2 * Math.PI * RADIUS

export default function TimerCircle({ endTime, totalMinutes, onTimeUp }) {
  const [remainingMs, setRemainingMs] = useState(() => endTime - Date.now())
  const [hasChimed, setHasChimed] = useState(false)

  useEffect(() => {
    const interval = setInterval(() => {
      const ms = endTime - Date.now()
      setRemainingMs(ms)
      if (ms <= 0 && !hasChimed) {
        setHasChimed(true)
        onTimeUp?.()
      }
    }, 1000)
    return () => clearInterval(interval)
  }, [endTime, hasChimed, onTimeUp])

  const totalMs = totalMinutes * 60 * 1000
  const elapsed = totalMs - remainingMs
  const progress = Math.min(elapsed / totalMs, 1)
  const offset = CIRCUMFERENCE * (1 - progress)
  const isOvertime = remainingMs < 0

  const absMs = Math.abs(remainingMs)
  const minutes = Math.floor(absMs / 60000)
  const seconds = Math.floor((absMs % 60000) / 1000)
  const timeStr = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`

  return (
    <div className="flex flex-col items-center">
      <div className="relative">
        <svg width="180" height="180" className="transform -rotate-90">
          {/* Background circle */}
          <circle
            cx="90" cy="90" r={RADIUS}
            fill="none" stroke="#D4C8E2" strokeWidth="8"
          />
          {/* Progress arc */}
          <circle
            cx="90" cy="90" r={RADIUS}
            fill="none" stroke="#7C6B9E" strokeWidth="8"
            strokeDasharray={CIRCUMFERENCE}
            strokeDashoffset={offset}
            strokeLinecap="round"
            className={`transition-all duration-1000 ${isOvertime ? 'animate-pulse' : ''}`}
          />
        </svg>
        {/* Timer text centered */}
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          {isOvertime ? (
            <>
              <span className="text-3xl font-light text-ink-muted">+{timeStr}</span>
              <span className="text-sm text-primary-400 mt-1 font-medium">
                Time&apos;s up. Good work!
              </span>
            </>
          ) : (
            <span className="text-4xl font-light text-ink-heading tracking-wide">{timeStr}</span>
          )}
        </div>
      </div>
      <span className="text-sm text-ink-muted mt-3">of {totalMinutes} min</span>
    </div>
  )
}
