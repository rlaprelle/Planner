import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { TaskTriagePhase } from '@/components/ritual/TaskTriagePhase'
import { InboxPhase } from '@/components/ritual/InboxPhase'
import { DailyReflectionPhase } from '@/components/ritual/DailyReflectionPhase'
import { CompletionPhase } from '@/components/ritual/CompletionPhase'

function buildPhases(level) {
  const phases = ['triage', 'inbox', 'reflection']
  if (level === 'week' || level === 'month') phases.push('weeklyReflection')
  if (level === 'month') phases.push('monthlyReflection')
  phases.push('done')
  return phases
}

function PhaseIndicator({ phases, currentPhase }) {
  const currentIndex = phases.indexOf(currentPhase)
  // Don't show indicator on the done screen
  if (currentPhase === 'done') return null

  return (
    <div className="flex items-center gap-2 mb-8">
      {phases.filter(p => p !== 'done').map((phase, i) => (
        <div key={phase} className="flex items-center gap-2">
          <div
            className={`h-1.5 rounded-full transition-all duration-300 ${
              i < currentIndex ? 'bg-primary-400 w-8'
              : i === currentIndex ? 'bg-primary-500 w-12'
              : 'bg-surface-soft w-8'
            }`}
          />
        </div>
      ))}
    </div>
  )
}

export function EndRitualPage({ level = 'day' }) {
  const { t } = useTranslation('ritual')
  const phases = buildPhases(level)
  const [currentPhase, setCurrentPhase] = useState(phases[0])

  function advancePhase() {
    const currentIndex = phases.indexOf(currentPhase)
    if (currentIndex < phases.length - 1) {
      setCurrentPhase(phases[currentIndex + 1])
    }
  }

  const title = {
    day: t('endOfDay'),
    week: t('endOfWeek'),
    month: t('endOfMonth'),
  }[level]

  return (
    <div className="min-h-screen bg-surface flex items-start justify-center pt-16 px-4">
      <div className="w-full max-w-lg">
        {currentPhase !== 'done' && (
          <>
            <h1 className="text-2xl font-semibold text-ink-heading mb-2">{title}</h1>
            <PhaseIndicator phases={phases} currentPhase={currentPhase} />
          </>
        )}

        {currentPhase === 'triage' && (
          <TaskTriagePhase onPhaseComplete={advancePhase} />
        )}

        {currentPhase === 'inbox' && (
          <InboxPhase onPhaseComplete={advancePhase} />
        )}

        {currentPhase === 'reflection' && (
          <DailyReflectionPhase onPhaseComplete={advancePhase} />
        )}

        {currentPhase === 'weeklyReflection' && (
          <div>
            <h2 className="text-xl font-semibold text-ink-heading mb-6">{t('howWasYourWeek')}</h2>
            <p className="text-sm text-ink-secondary mb-6">
              {t('weeklyReflectionComingSoon')}
            </p>
            <button
              onClick={advancePhase}
              className="w-full py-2.5 text-sm rounded-md bg-primary-500 text-white hover:bg-primary-600 transition-colors font-medium"
            >
              {t('continue')}
            </button>
          </div>
        )}

        {currentPhase === 'monthlyReflection' && (
          <div>
            <h2 className="text-xl font-semibold text-ink-heading mb-6">{t('howWasYourMonth')}</h2>
            <p className="text-sm text-ink-secondary mb-6">
              {t('monthlyReflectionComingSoon')}
            </p>
            <button
              onClick={advancePhase}
              className="w-full py-2.5 text-sm rounded-md bg-primary-500 text-white hover:bg-primary-600 transition-colors font-medium"
            >
              {t('continue')}
            </button>
          </div>
        )}

        {currentPhase === 'done' && (
          <CompletionPhase level={level} />
        )}
      </div>
    </div>
  )
}
