import { NavLink, Outlet, Navigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

export default function AdminPage() {
  const { t } = useTranslation('admin')

  const navItems = [
    { to: '/admin/users', label: t('users') },
    { to: '/admin/projects', label: t('projects') },
    { to: '/admin/tasks', label: t('tasks') },
    { to: '/admin/deferred', label: t('deferredItems') },
    { to: '/admin/reflections', label: t('reflections') },
    { to: '/admin/time-blocks', label: t('timeBlocks') },
    { to: '/admin/events', label: t('events') },
  ]

  return (
    <div className="flex h-screen bg-gray-50">
      {/* Sidebar */}
      <nav className="w-56 bg-gray-900 text-gray-300 flex flex-col shrink-0">
        <div className="px-4 py-4 text-white font-bold text-lg border-b border-gray-700">
          {t('admin')}
        </div>
        <div className="flex-1 py-2">
          {navItems.map(item => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `block px-4 py-2 text-sm ${isActive ? 'bg-gray-700 text-white font-medium' : 'hover:bg-gray-800 hover:text-white'}`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </div>
        <div className="px-4 py-3 border-t border-gray-700">
          <a href="/" className="text-xs text-gray-500 hover:text-gray-300">{t('backToApp')}</a>
        </div>
      </nav>

      {/* Main content */}
      <main className="flex-1 overflow-auto p-6">
        <Outlet />
      </main>
    </div>
  )
}
