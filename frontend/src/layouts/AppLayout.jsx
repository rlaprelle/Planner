import { useState, useEffect } from 'react'
import { NavLink, useNavigate, useLocation } from 'react-router-dom'
import { Outlet } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import * as Dialog from '@radix-ui/react-dialog'
import { useAuth } from '@/auth/useAuth'
import { getDeferredItems } from '@/api/deferred'
import { QuickCapture } from '@/components/QuickCapture'
import { useActiveSession } from '@/contexts/ActiveSessionContext'
import { EchelLogo } from '@/components/EchelLogo'
import { useMediaQuery } from '@/hooks/useMediaQuery'

const NAV_ITEMS = [
  {
    labelKey: 'dashboard',
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
    labelKey: 'projects',
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
    labelKey: 'inbox',
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
    labelKey: 'today',
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
    labelKey: 'daily',
    defaultOpen: true,
    items: [
      { labelKey: 'startDay', to: '/start-day' },
      { labelKey: 'endDay', to: '/end-day' },
    ],
  },
  {
    labelKey: 'weekly',
    defaultOpen: false,
    items: [
      { labelKey: 'startWeek', to: '/start-week' },
      { labelKey: 'endWeek', to: '/end-week' },
    ],
  },
  {
    labelKey: 'monthly',
    defaultOpen: false,
    items: [
      { labelKey: 'startMonth', to: '/start-month' },
      { labelKey: 'endMonth', to: '/end-month' },
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
  const { t } = useTranslation('common')
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
      {t(item.labelKey)}
      {badge > 0 && (
        <span className="ml-auto bg-primary-100 text-primary-700 text-xs font-semibold rounded-full px-1.5 py-0.5 min-w-[1.25rem] text-center">
          {badge > 99 ? t('badgeOverflow') : badge}
        </span>
      )}
    </NavLink>
  )
}

function RitualGroup({ group }) {
  const { t } = useTranslation('common')
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
        {t(group.labelKey)}
      </button>
      {open && (
        <div className="space-y-0.5 ml-7">
          {group.items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                [
                  'flex items-center gap-2 px-3 py-1 rounded-md text-xs font-normal transition-colors duration-100 focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1',
                  isActive
                    ? 'bg-primary-50 text-primary-700 font-medium'
                    : 'text-ink-secondary hover:bg-surface-soft hover:text-ink-heading',
                ].join(' ')
              }
            >
              {t(item.labelKey)}
            </NavLink>
          ))}
        </div>
      )}
    </div>
  )
}

/**
 * The sidebar's inner content, shared between the static sidebar (md+)
 * and the mobile drawer. `closeButton` is rendered in the brand row
 * (the drawer passes its Dialog.Close button).
 */
function SidebarContent({ closeButton = null }) {
  const { t } = useTranslation('common')
  const { user, logout, isAdmin } = useAuth()
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
    <>
      {/* Logo / Brand */}
      <div className="px-5 py-5 border-b border-edge-subtle flex items-center gap-2.5">
        <EchelLogo size={24} />
        <span className="text-lg font-semibold text-ink-heading tracking-tight">{t('appName')}</span>
        {closeButton}
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
          <p className="px-3 text-xs font-semibold text-ink-muted uppercase tracking-wider">{t('rituals')}</p>
        </div>

        <div className="space-y-1">
          {RITUAL_GROUPS.map((group) => (
            <RitualGroup key={group.labelKey} group={group} />
          ))}
        </div>
      </div>

      {/* Quick capture + settings + user + logout */}
      <div className="px-4 py-4 border-t border-edge-subtle space-y-2">
        <QuickCapture />
        <NavLink
          to="/settings"
          className={({ isActive }) =>
            [
              'flex items-center gap-2 px-3 py-2 rounded-md text-sm transition-colors duration-100 focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1',
              isActive
                ? 'bg-surface-accent text-primary-700 font-medium'
                : 'text-ink-secondary hover:bg-surface-soft hover:text-ink-heading',
            ].join(' ')
          }
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none"
            stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
            aria-hidden="true">
            <circle cx="12" cy="12" r="3" />
            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
          </svg>
          {t('settings')}
        </NavLink>
        {isAdmin && (
          <NavLink
            to="/admin"
            className={({ isActive }) =>
              [
                'flex items-center gap-2 px-3 py-2 rounded-md text-sm transition-colors duration-100 focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1',
                isActive
                  ? 'bg-surface-accent text-primary-700 font-medium'
                  : 'text-ink-secondary hover:bg-surface-soft hover:text-ink-heading',
              ].join(' ')
            }
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none"
              stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
              aria-hidden="true">
              <path d="M12 2l9 4v6c0 5.25-3.75 9.75-9 10-5.25-.25-9-4.75-9-10V6l9-4z" />
            </svg>
            {t('admin')}
          </NavLink>
        )}
        <p className="text-xs text-ink-muted truncate" title={displayName}>
          {displayName}
        </p>
        <button
          onClick={logout}
          className="w-full text-left px-3 py-2 rounded-md text-sm text-ink-secondary hover:bg-surface-soft hover:text-ink-heading transition-colors duration-100 focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1"
        >
          {t('logOut')}
        </button>
      </div>
    </>
  )
}

export function AppLayout() {
  const { t } = useTranslation('common')
  const location = useLocation()
  const [mobileNavOpen, setMobileNavOpen] = useState(false)
  const isWide = useMediaQuery('(min-width: 768px)')

  // Close the drawer whenever navigation happens, and when the viewport
  // grows past the breakpoint (otherwise Radix's modal lock would linger
  // behind the hidden drawer).
  const [prevPathname, setPrevPathname] = useState(location.pathname)
  if (prevPathname !== location.pathname) {
    setPrevPathname(location.pathname)
    if (mobileNavOpen) setMobileNavOpen(false)
  }
  if (isWide && mobileNavOpen) setMobileNavOpen(false)

  return (
    <div className="flex h-screen flex-col md:flex-row bg-surface">
      {/* Mobile top bar */}
      <header className="md:hidden flex items-center gap-3 px-4 py-3 bg-surface-raised border-b border-edge flex-shrink-0">
        <Dialog.Root open={mobileNavOpen} onOpenChange={setMobileNavOpen}>
          <Dialog.Trigger asChild>
            <button
              type="button"
              aria-label={t('openMenu')}
              className="p-2 -ml-2 rounded-md text-ink-secondary hover:bg-surface-soft hover:text-ink-heading transition-colors focus:outline-none focus:ring-2 focus:ring-edge-focus"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none"
                stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
                aria-hidden="true">
                <line x1="3" y1="6" x2="21" y2="6" />
                <line x1="3" y1="12" x2="21" y2="12" />
                <line x1="3" y1="18" x2="21" y2="18" />
              </svg>
            </button>
          </Dialog.Trigger>
          <Dialog.Portal>
            <Dialog.Overlay className="fixed inset-0 z-40 bg-black/40 md:hidden data-[state=open]:animate-overlay-in data-[state=closed]:animate-overlay-out" />
            <Dialog.Content
              className="fixed inset-y-0 left-0 z-50 w-72 max-w-[85vw] md:hidden focus:outline-none data-[state=open]:animate-drawer-in data-[state=closed]:animate-drawer-out"
              aria-describedby={undefined}
            >
              <Dialog.Title className="sr-only">{t('mainNavigation')}</Dialog.Title>
              <nav
                className="flex flex-col h-full bg-surface-raised border-r border-edge shadow-modal"
                aria-label={t('mainNavigation')}
              >
                <SidebarContent
                  closeButton={
                    <Dialog.Close asChild>
                      <button
                        type="button"
                        aria-label={t('closeMenu')}
                        className="ml-auto p-2 -mr-2 rounded-md text-ink-muted hover:bg-surface-soft hover:text-ink-heading transition-colors focus:outline-none focus:ring-2 focus:ring-edge-focus"
                      >
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none"
                          stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
                          aria-hidden="true">
                          <line x1="18" y1="6" x2="6" y2="18" />
                          <line x1="6" y1="6" x2="18" y2="18" />
                        </svg>
                      </button>
                    </Dialog.Close>
                  }
                />
              </nav>
            </Dialog.Content>
          </Dialog.Portal>
        </Dialog.Root>
        <EchelLogo size={22} />
        <span className="text-lg font-semibold text-ink-heading tracking-tight">{t('appName')}</span>
      </header>

      {/* Static sidebar (md+) */}
      <nav
        className="hidden md:flex w-60 flex-shrink-0 flex-col bg-surface-raised border-r border-edge"
        aria-label={t('mainNavigation')}
      >
        <SidebarContent />
      </nav>

      {/* Main content */}
      <main className="flex-1 min-h-0 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  )
}
