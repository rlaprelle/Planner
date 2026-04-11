import { useNavigate } from 'react-router-dom'
import { TaskSchedulePhase } from '@/components/ritual/TaskSchedulePhase'

export function StartWeekPage() {
  const navigate = useNavigate()

  return (
    <div className="min-h-screen bg-surface flex items-start justify-center pt-16 px-4">
      <div className="w-full max-w-lg">
        <h1 className="text-2xl font-semibold text-ink-heading mb-2">Start of Week</h1>
        <p className="text-sm text-ink-secondary mb-8">
          Spread your tasks across the week. Anything you skip will show up in Start Day.
        </p>
        <TaskSchedulePhase
          mode="week"
          onPhaseComplete={() => navigate('/', { state: { toast: 'Week planned. Let\'s go.' } })}
        />
      </div>
    </div>
  )
}
