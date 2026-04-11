import { useEffect, useState } from 'react'
import { useNavigate, useLocation, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { getDashboard, getWeeklySummary } from '@/api/dashboard'
import { getScheduleToday } from '@/api/schedule'
import { Card } from '@/components/ui/Card'
import { CardLabel } from '@/components/ui/CardLabel'
import { ProgressBar } from '@/components/ui/ProgressBar'
import { useAuth } from '@/auth/useAuth'

function formatFocusTime(minutes) {
  if (!minutes || minutes === 0) return '0m'
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  if (h === 0) return `${m}m`
  if (m === 0) return `${h}h`
  return `${h}h ${m}m`
}

function WeeklyBanner({ summary, t }) {
  if (!summary) return null

  if (!summary.hasActivity) {
    return (
      <Card className="mb-4">
        <CardLabel>{t('yourWeek')}</CardLabel>
        <p className="text-sm text-ink-secondary">{t('noActivityThisWeek')}</p>
      </Card>
    )
  }

  const trendParts = []
  if (summary.streakDays > 0) {
    trendParts.push(t('dayStreak', { count: summary.streakDays }))
  }
  if (summary.energyTrend) {
    trendParts.push(t('energyTrend', { trend: summary.energyTrend }))
  }
  if (summary.moodTrend) {
    trendParts.push(t('moodTrend', { trend: summary.moodTrend }))
  }

  return (
    <Card className="mb-4">
      <CardLabel>{t('yourWeek')}</CardLabel>
      <p className="text-lg font-semibold text-ink-heading">
        {t('weekSummary', {
          tasks: summary.tasksCompleted,
          taskWord: t('task', { count: summary.tasksCompleted }),
          points: summary.totalPoints,
          pointWord: t('point', { count: summary.totalPoints }),
          minutes: formatFocusTime(summary.totalFocusMinutes),
        })}
      </p>
      {trendParts.length > 0 && (
        <p className="mt-1 text-sm text-ink-secondary">{trendParts.join(' · ')}</p>
      )}
    </Card>
  )
}

const DEADLINE_BADGE = {
  TODAY: 'bg-deadline-today-bg text-deadline-today-text',
  THIS_WEEK: 'bg-deadline-week-bg text-deadline-week-text',
}

export function DashboardPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [toast, setToast] = useState(location.state?.successMessage ?? null)
  const { user } = useAuth()
  const firstName = user?.displayName?.split(' ')[0]
  const { t } = useTranslation('dashboard')

  function getGreetingKey() {
    const hour = new Date().getHours()
    if (hour < 12) return 'goodMorning'
    if (hour < 17) return 'goodAfternoon'
    return 'goodEvening'
  }

  useEffect(() => {
    if (toast) {
      window.history.replaceState({}, '')
      const timer = setTimeout(() => setToast(null), 3500)
      return () => clearTimeout(timer)
    }
  }, [toast])

  const { data, isLoading } = useQuery({
    queryKey: ['dashboard'],
    queryFn: getDashboard,
  })

  const TODAY = new Date().toISOString().slice(0, 10)
  const { data: todayBlocks } = useQuery({
    queryKey: ['schedule', TODAY],
    queryFn: getScheduleToday,
  })
  const { data: weeklySummary } = useQuery({
    queryKey: ['stats', 'weekly-summary'],
    queryFn: getWeeklySummary,
  })

  const nextBlock = todayBlocks?.find(
    (b) => b.task && b.task.status !== 'COMPLETED' && !b.actualEnd
  )

  if (isLoading) {
    return <div className="p-8 text-ink-muted text-sm">{t('common:loading')}</div>
  }

  const { todayBlockCount, todayCompletedCount, streakDays, upcomingDeadlines, deferredItemCount } = data ?? {}

  return (
    <div className="p-6 max-w-4xl mx-auto">

      {/* Toast */}
      {toast && (
        <div className="mb-4 px-4 py-3 bg-primary-50 border border-primary-200 text-primary-800 text-sm rounded-lg">
          {toast}
        </div>
      )}

      <h1 className="text-2xl font-semibold text-ink-heading mb-6">
        {firstName
          ? t('greeting', { greeting: t(getGreetingKey()), name: firstName })
          : t(getGreetingKey())}
      </h1>

      <WeeklyBanner summary={weeklySummary} t={t} />

      <div className="grid grid-cols-2 gap-4 mb-6">

        {/* Card 1: Today at a glance */}
        <Card>
          <CardLabel>{t('todayAtAGlance')}</CardLabel>
          {todayBlockCount > 0 ? (
            <>
              <p className="text-2xl font-bold text-ink-heading">
                {todayCompletedCount} / {todayBlockCount}
                <span className="text-sm font-normal text-ink-secondary ml-2">{t('tasksDone')}</span>
              </p>
              <ProgressBar value={todayCompletedCount} max={todayBlockCount} />
              {nextBlock && (
                <button
                  onClick={() => navigate(`/session/${nextBlock.id}`)}
                  className="mt-3 w-full px-4 py-2 bg-primary-500 text-white rounded-lg text-sm font-medium hover:bg-primary-600 transition-colors"
                >
                  Start: {nextBlock.task.title}
                </button>
              )}
            </>
          ) : (
            <>
              <p className="text-ink-secondary text-sm">{t('noPlanYet')}</p>
              <Link
                to="/start-day"
                className="inline-block mt-2 text-sm text-primary-500 hover:text-primary-700 font-medium"
              >
                {t('startPlanning')}
              </Link>
            </>
          )}
        </Card>

        {/* Card 2: Streak */}
        <Card>
          <CardLabel>{t('planningStreak')}</CardLabel>
          {streakDays > 0 ? (
            <>
              <p className="text-2xl font-bold text-ink-heading">
                {streakDays}
                <span className="text-sm font-normal text-ink-secondary ml-2">
                  {t('streakDays', { count: streakDays })}
                </span>
              </p>
              <p className="mt-1 text-xs text-ink-muted">{t('keepItGoing')}</p>
            </>
          ) : (
            <p className="text-sm text-ink-secondary">{t('startYourStreak')}</p>
          )}
        </Card>

        {/* Card 3: Upcoming deadlines */}
        <Card>
          <CardLabel>{t('upcomingDeadlines')}</CardLabel>
          {upcomingDeadlines?.length > 0 ? (
            <ul className="space-y-2">
              {upcomingDeadlines.map((d) => (
                <li
                  key={d.taskId}
                  className="flex items-center gap-2 cursor-pointer hover:bg-surface-soft -mx-1 px-1 rounded"
                  onClick={() => navigate('/start-day')}
                >
                  {d.projectColor && (
                    <span
                      className="w-2.5 h-2.5 rounded-full shrink-0"
                      style={{ background: d.projectColor }}
                    />
                  )}
                  <span className="text-sm text-ink-body truncate flex-1">{d.taskTitle}</span>
                  {DEADLINE_BADGE[d.deadlineGroup] && (
                    <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded ${DEADLINE_BADGE[d.deadlineGroup]}`}>
                      {d.deadlineGroup === 'TODAY' ? t('deadlineToday') : d.projectName}
                    </span>
                  )}
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-ink-muted">{t('noDeadlinesNice')}</p>
          )}
        </Card>

        {/* Card 4: Inbox */}
        <Card
          className={deferredItemCount > 0 ? 'cursor-pointer hover:border-primary-300 transition-colors' : ''}
          onClick={deferredItemCount > 0 ? () => navigate('/inbox') : undefined}
        >
          <CardLabel>{t('inboxHeading')}</CardLabel>
          {deferredItemCount > 0 ? (
            <>
              <p className="text-2xl font-bold text-ink-heading">
                {deferredItemCount}
                <span className="text-sm font-normal text-ink-secondary ml-2">
                  {t('inboxWaiting', { count: deferredItemCount })}
                </span>
              </p>
              <p className="mt-1 text-xs text-primary-500">{t('clickToReview')}</p>
            </>
          ) : (
            <p className="text-sm text-ink-muted">{t('inboxClear')}</p>
          )}
        </Card>
      </div>

      {/* Quick actions */}
      <div className="flex gap-3">
        <Link
          to="/start-day"
          className="px-4 py-2 text-sm rounded-md bg-primary-500 text-white hover:bg-primary-600 transition-colors font-medium"
        >
          {t('startMorningPlanning')}
        </Link>
        <Link
          to="/end-day"
          className="px-4 py-2 text-sm rounded-md border border-edge text-ink-secondary hover:bg-surface-soft transition-colors"
        >
          {t('eveningCleanup')}
        </Link>
      </div>
    </div>
  )
}
