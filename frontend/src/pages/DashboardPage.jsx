import { useEffect, useState } from 'react'
import { useNavigate, useLocation, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getDashboard } from '@/api/dashboard'

function ProgressBar({ value, max }) {
  const pct = max > 0 ? Math.round((value / max) * 100) : 0
  return (
    <div className="mt-2 h-2 bg-gray-100 rounded-full overflow-hidden">
      <div
        data-testid="progress-fill"
        className="h-full bg-indigo-500 rounded-full transition-all"
        style={{ width: `${pct}%` }}
      />
    </div>
  )
}

function Card({ children, className = '', ...rest }) {
  return (
    <div className={`bg-white border border-gray-200 rounded-xl p-5 ${className}`} {...rest}>
      {children}
    </div>
  )
}

function CardLabel({ children }) {
  return (
    <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">{children}</p>
  )
}

const DEADLINE_BADGE = {
  TODAY: 'bg-red-100 text-red-700',
  THIS_WEEK: 'bg-amber-100 text-amber-700',
}

export function DashboardPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [toast, setToast] = useState(location.state?.successMessage ?? null)

  useEffect(() => {
    if (toast) {
      window.history.replaceState({}, '')
      const t = setTimeout(() => setToast(null), 3500)
      return () => clearTimeout(t)
    }
  }, [toast])

  const { data, isLoading } = useQuery({
    queryKey: ['dashboard'],
    queryFn: getDashboard,
  })

  if (isLoading) {
    return <div className="p-8 text-gray-400 text-sm">Loading…</div>
  }

  const { todayBlockCount, todayCompletedCount, streakDays, upcomingDeadlines, deferredItemCount } = data ?? {}

  return (
    <div className="p-6 max-w-4xl mx-auto">

      {/* Toast */}
      {toast && (
        <div className="mb-4 px-4 py-3 bg-indigo-50 border border-indigo-200 text-indigo-800 text-sm rounded-lg">
          {toast}
        </div>
      )}

      <h1 className="text-2xl font-semibold text-gray-900 mb-6">Dashboard</h1>

      <div className="grid grid-cols-2 gap-4 mb-6">

        {/* Card 1: Today at a glance */}
        <Card>
          <CardLabel>Today at a Glance</CardLabel>
          {todayBlockCount > 0 ? (
            <>
              <p className="text-2xl font-bold text-gray-900">
                {todayCompletedCount} / {todayBlockCount}
                <span className="text-sm font-normal text-gray-500 ml-2">tasks done</span>
              </p>
              <ProgressBar value={todayCompletedCount} max={todayBlockCount} />
            </>
          ) : (
            <>
              <p className="text-gray-500 text-sm">No plan yet.</p>
              <Link
                to="/start-day"
                className="inline-block mt-2 text-sm text-indigo-600 hover:text-indigo-800 font-medium"
              >
                Start planning →
              </Link>
            </>
          )}
        </Card>

        {/* Card 2: Streak */}
        <Card>
          <CardLabel>Planning Streak</CardLabel>
          {streakDays > 0 ? (
            <>
              <p className="text-2xl font-bold text-gray-900">
                {streakDays}
                <span className="text-sm font-normal text-gray-500 ml-2">
                  {streakDays === 1 ? 'day' : 'days'} in a row
                </span>
              </p>
              <p className="mt-1 text-xs text-gray-400">Keep it going — finish tonight's reflection.</p>
            </>
          ) : (
            <p className="text-sm text-gray-500">Start your streak tonight — finish today's reflection.</p>
          )}
        </Card>

        {/* Card 3: Upcoming deadlines */}
        <Card>
          <CardLabel>Upcoming Deadlines</CardLabel>
          {upcomingDeadlines?.length > 0 ? (
            <ul className="space-y-2">
              {upcomingDeadlines.map((d) => (
                <li
                  key={d.taskId}
                  className="flex items-center gap-2 cursor-pointer hover:bg-gray-50 -mx-1 px-1 rounded"
                  onClick={() => navigate('/start-day')}
                >
                  {d.projectColor && (
                    <span
                      className="w-2.5 h-2.5 rounded-full shrink-0"
                      style={{ background: d.projectColor }}
                    />
                  )}
                  <span className="text-sm text-gray-800 truncate flex-1">{d.taskTitle}</span>
                  {DEADLINE_BADGE[d.deadlineGroup] && (
                    <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded ${DEADLINE_BADGE[d.deadlineGroup]}`}>
                      {d.deadlineGroup === 'TODAY' ? 'TODAY' : d.projectName}
                    </span>
                  )}
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-gray-400">No upcoming deadlines. Nice.</p>
          )}
        </Card>

        {/* Card 4: Inbox */}
        <Card
          className={deferredItemCount > 0 ? 'cursor-pointer hover:border-indigo-300 transition-colors' : ''}
          onClick={deferredItemCount > 0 ? () => navigate('/inbox') : undefined}
        >
          <CardLabel>Inbox</CardLabel>
          {deferredItemCount > 0 ? (
            <>
              <p className="text-2xl font-bold text-gray-900">
                {deferredItemCount}
                <span className="text-sm font-normal text-gray-500 ml-2">
                  {deferredItemCount === 1 ? 'item' : 'items'} waiting
                </span>
              </p>
              <p className="mt-1 text-xs text-indigo-500">Click to review →</p>
            </>
          ) : (
            <p className="text-sm text-gray-400">Inbox clear.</p>
          )}
        </Card>
      </div>

      {/* Quick actions */}
      <div className="flex gap-3">
        <Link
          to="/start-day"
          className="px-4 py-2 text-sm rounded-md bg-indigo-600 text-white hover:bg-indigo-700 transition-colors font-medium"
        >
          Start Morning Planning
        </Link>
        <Link
          to="/end-day"
          className="px-4 py-2 text-sm rounded-md border border-gray-300 text-gray-700 hover:bg-gray-50 transition-colors"
        >
          Evening Clean-up
        </Link>
      </div>
    </div>
  )
}
