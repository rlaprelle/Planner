import { NavLink, Outlet, Navigate } from 'react-router-dom'

const NAV_ITEMS = [
  { to: '/admin/users', label: 'Users' },
  { to: '/admin/projects', label: 'Projects' },
  { to: '/admin/tasks', label: 'Tasks' },
  { to: '/admin/deferred', label: 'Deferred Items' },
  { to: '/admin/reflections', label: 'Reflections' },
  { to: '/admin/time-blocks', label: 'Time Blocks' },
  { to: '/admin/events', label: 'Events' },
]

export default function AdminPage() {
  return (
    <div className="flex h-screen bg-gray-50">
      {/* Sidebar */}
      <nav className="w-56 bg-gray-900 text-gray-300 flex flex-col shrink-0">
        <div className="px-4 py-4 text-white font-bold text-lg border-b border-gray-700">
          Admin
        </div>
        <div className="flex-1 py-2">
          {NAV_ITEMS.map(item => (
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
          <a href="/" className="text-xs text-gray-500 hover:text-gray-300">&larr; Back to app</a>
        </div>
      </nav>

      {/* Main content */}
      <main className="flex-1 overflow-auto p-6">
        <Outlet />
      </main>
    </div>
  )
}
