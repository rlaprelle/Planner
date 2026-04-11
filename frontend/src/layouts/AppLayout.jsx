import { useState, useEffect } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { Outlet } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useAuth } from '@/auth/useAuth'
import { getDeferredItems } from '@/api/deferred'
import { QuickCapture } from '@/components/QuickCapture'
import { useActiveSession } from '@/contexts/ActiveSessionContext'

const NAV_ITEMS = [
  {
    label: 'Dashboard',
    to: '/',
    end: true,
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none"
        stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
        aria-hidden="true">
        <rect x="3" y="3" width="7" height="7" rx="1" />
        <rect x="14" y="3" width="7" height="7" rx="1" />
        <rect x="3" y="14" width="7" height="7" rx="1" />
        <rect x="14" y="14" width="7" height="7" rx="1" />
      </svg>
    ),
  },
  {
    label: 'Projects',
    to: '/projects',
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none"
        stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
        aria-hidden="true">
        <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
      </svg>
    ),
  },
  {
    label: 'Inbox',
    to: '/inbox',
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none"
        stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
        aria-hidden="true">
        <polyline points="22 12 16 12 14 15 10 15 8 12 2 12" />
        <path d="M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0-1.79 1.11z" />
      </svg>
    ),
  },
  {
    label: 'Today',
    to: '/today',
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none"
        stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
        aria-hidden="true">
        <circle cx="12" cy="12" r="4" />
        <line x1="12" y1="2" x2="12" y2="4" />
        <line x1="12" y1="20" x2="12" y2="22" />
        <line x1="2" y1="12" x2="4" y2="12" />
        <line x1="20" y1="12" x2="22" y2="12" />
        <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
        <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
        <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
        <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
      </svg>
    ),
  },
]

const RITUAL_GROUPS = [
  {
    label: 'Daily',
    defaultOpen: true,
    items: [
      { label: 'Start Day', to: '/start-day' },
      { label: 'End Day', to: '/end-day' },
    ],
  },
  {
    label: 'Weekly',
    defaultOpen: false,
    items: [
      { label: 'Start Week', to: '/start-week' },
      { label: 'End Week', to: '/end-week' },
    ],
  },
  {
    label: 'Monthly',
    defaultOpen: false,
    items: [
      { label: 'Start Month', to: '/start-month' },
      { label: 'End Month', to: '/end-month' },
    ],
  },
]

function HeaderTimer({ session, onClick }) {
  const [remaining, setRemaining] = useState('')

  useEffect(() => {
    const tick = () => {
      const ms = session.endTime - Date.now()
      const abs = Math.abs(ms)
      const m = Math.floor(abs / 60000)
      const s = Math.floor((abs % 60000) / 1000)
      const time = `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
      setRemaining(ms < 0 ? `+${time}` : time)
    }
    tick()
    const interval = setInterval(tick, 1000)
    return () => clearInterval(interval)
  }, [session.endTime])

  return (
    <button
      onClick={onClick}
      className="flex items-center gap-2 px-3 py-1.5 bg-primary-50 text-primary-700 rounded-lg text-sm font-medium hover:bg-primary-100 transition-colors mx-3 my-3"
    >
      <span className="truncate max-w-[150px]">{session.taskName}</span>
      <span className="font-mono tabular-nums">{remaining}</span>
    </button>
  )
}

function NavItem({ item, badge = 0 }) {
  return (
    <NavLink
      to={item.to}
      end={item.end}
      className={({ isActive }) =>
        [
          'flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium transition-colors duration-100 focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1',
          isActive
            ? 'bg-surface-accent text-primary-700'
            : 'text-ink-secondary hover:bg-surface-soft hover:text-ink-heading',
        ].join(' ')
      }
    >
      {item.icon}
      {item.label}
      {badge > 0 && (
        <span className="ml-auto bg-primary-100 text-primary-700 text-xs font-semibold rounded-full px-1.5 py-0.5 min-w-[1.25rem] text-center">
          {badge > 99 ? '99+' : badge}
        </span>
      )}
    </NavLink>
  )
}

function RitualGroup({ group }) {
  const [open, setOpen] = useState(group.defaultOpen)

  return (
    <div>
      <button
        type="button"
        onClick={() => setOpen(!open)}
        className="flex items-center gap-2 px-3 py-1.5 w-full text-left text-xs font-semibold text-ink-muted uppercase tracking-wider hover:text-ink-secondary transition-colors"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 24 24"
          fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
          className={`transition-transform ${open ? 'rotate-90' : ''}`}
          aria-hidden="true"
        >
          <polyline points="9 18 15 12 9 6" />
        </svg>
        {group.label}
      </button>
      {open && (
        <div className="space-y-0.5 ml-2">
          {group.items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                [
                  'flex items-center gap-2 px-3 py-1.5 rounded-md text-sm font-medium transition-colors duration-100 focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1',
                  isActive
                    ? 'bg-primary-100 text-primary-800'
                    : 'text-primary-400 hover:bg-primary-50 hover:text-primary-700',
                ].join(' ')
              }
            >
              {item.label}
            </NavLink>
          ))}
        </div>
      )}
    </div>
  )
}

export function AppLayout() {
  const { user, logout } = useAuth()
  const displayName = user?.displayName || user?.email || 'User'
  const { session } = useActiveSession()
  const navigate = useNavigate()

  const { data: deferredItems = [] } = useQuery({
    queryKey: ['deferred'],
    queryFn: getDeferredItems,
    staleTime: 60_000,
  })
  const inboxCount = deferredItems.length

  return (
    <div className="flex h-screen bg-surface">
      {/* Sidebar */}
      <nav
        className="w-60 flex-shrink-0 flex flex-col bg-surface-raised border-r border-edge"
        aria-label="Main navigation"
      >
        {/* Logo / Brand */}
        <div className="px-5 py-5 border-b border-edge-subtle">
          <span className="text-lg font-semibold text-ink-heading tracking-tight">Planner</span>
        </div>

        {/* Active session timer */}
        {session && (
          <HeaderTimer
            session={session}
            onClick={() => navigate(`/session/${session.blockId}`)}
          />
        )}

        {/* Nav links */}
        <div className="flex-1 overflow-y-auto px-3 py-4 space-y-1">
          {NAV_ITEMS.map((item) => (
            <NavItem
              key={item.to}
              item={item}
              badge={item.to === '/inbox' ? inboxCount : 0}
            />
          ))}

          <div className="pt-3 pb-1">
            <p className="px-3 text-xs font-semibold text-ink-muted uppercase tracking-wider">Rituals</p>
          </div>

          <div className="space-y-1">
            {RITUAL_GROUPS.map((group) => (
              <RitualGroup key={group.label} group={group} />
            ))}
          </div>
        </div>

        {/* Quick capture + user + logout */}
        <div className="px-4 py-4 border-t border-edge-subtle space-y-2">
          <QuickCapture />
          <p className="text-xs text-ink-muted truncate" title={displayName}>
            {displayName}
          </p>
          <button
            onClick={logout}
            className="w-full text-left px-3 py-2 rounded-md text-sm text-ink-secondary hover:bg-surface-soft hover:text-ink-heading transition-colors duration-100 focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1"
          >
            Log out
          </button>
        </div>
      </nav>

      {/* Main content */}
      <main className="flex-1 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  )
}
