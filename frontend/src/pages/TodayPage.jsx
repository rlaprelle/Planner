import { useTranslation } from 'react-i18next'

export function TodayPage() {
  const { t } = useTranslation('tasks')
  return (
    <div className="p-8">
      <h1 className="text-2xl font-semibold text-ink-heading">{t('todayHeading')}</h1>
      <p className="mt-2 text-ink-secondary">{t('comingSoon')}</p>
    </div>
  )
}
