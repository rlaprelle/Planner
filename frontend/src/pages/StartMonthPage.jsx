import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { TaskSchedulePhase } from '@/components/ritual/TaskSchedulePhase'

export function StartMonthPage() {
  const navigate = useNavigate()
  const { t } = useTranslation('ritual')

  return (
    <div className="min-h-screen bg-surface flex items-start justify-center pt-16 px-4">
      <div className="w-full max-w-lg">
        <h1 className="text-2xl font-semibold text-ink-heading mb-2">{t('startOfMonth')}</h1>
        <p className="text-sm text-ink-secondary mb-8">
          {t('spreadTasksMonth')}
        </p>
        <TaskSchedulePhase
          mode="month"
          onPhaseComplete={() => navigate('/', { state: { toast: t('monthPlanned') } })}
        />
      </div>
    </div>
  )
}
