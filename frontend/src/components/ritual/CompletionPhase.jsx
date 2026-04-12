import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { getStreak } from '@/api/reflection'

export function CompletionPhase({ level }) {
  const navigate = useNavigate()
  const { t } = useTranslation('ritual')
  const [streak, setStreak] = useState(null)

  useEffect(() => {
    getStreak().then(data => setStreak(data.streak)).catch(() => setStreak(0))
  }, [])

  const streakMessage = streak === 1
    ? t('streakDay1')
    : streak > 1
    ? t('streakKeepGoing', { count: streak })
    : t('goodWorkToday')

  const wrapMessage = {
    day: t('wrapDay'),
    week: t('wrapWeek'),
    month: t('wrapMonth'),
  }[level] || t('wrapGeneric')

  return (
    <div className="text-center py-8">
      <div className="text-4xl mb-3">✨</div>
      <p className="text-xl font-semibold text-ink-heading">{streakMessage}</p>
      <p className="mt-2 text-ink-secondary text-sm">{wrapMessage}</p>
      <button
        onClick={() => navigate('/')}
        className="mt-8 px-5 py-2 text-sm rounded-md bg-primary-500 text-white hover:bg-primary-600 transition-colors"
      >
        {t('done')}
      </button>
    </div>
  )
}
