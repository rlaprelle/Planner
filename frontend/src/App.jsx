import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider } from '@/auth/AuthContext'
import { ProtectedRoute } from '@/auth/ProtectedRoute'
import { AppLayout } from '@/layouts/AppLayout'
import { LoginPage } from '@/pages/LoginPage'
import { RegisterPage } from '@/pages/RegisterPage'
import { DashboardPage } from '@/pages/DashboardPage'
import { TodayPage } from '@/pages/TodayPage'
import { ProjectsPage } from '@/pages/ProjectsPage'
import { InboxPage } from '@/pages/InboxPage'
import { EndRitualPage } from '@/pages/EndRitualPage'
import { StartDayPage } from '@/pages/StartDayPage'
import { StartWeekPage } from '@/pages/StartWeekPage'
import { StartMonthPage } from '@/pages/StartMonthPage'
import { SettingsPage } from '@/pages/SettingsPage'
import { ActiveSessionProvider } from '@/contexts/ActiveSessionContext'
import ActiveSessionPage from '@/pages/ActiveSessionPage'
import AdminPage from '@/pages/admin/AdminPage'
import AdminUsersTable from '@/pages/admin/AdminUsersTable'
import AdminProjectsTable from '@/pages/admin/AdminProjectsTable'
import AdminTasksTable from '@/pages/admin/AdminTasksTable'
import AdminDeferredTable from '@/pages/admin/AdminDeferredTable'
import AdminReflectionsTable from '@/pages/admin/AdminReflectionsTable'
import AdminTimeBlocksTable from '@/pages/admin/AdminTimeBlocksTable'
import AdminEventsTable from '@/pages/admin/AdminEventsTable'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
    },
  },
})

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ActiveSessionProvider>
        <BrowserRouter>
          <AuthProvider>
            <Routes>
              {/* Public routes — no sidebar */}
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />

              {/* Admin routes — no auth, own layout */}
              <Route path="/admin" element={<AdminPage />}>
                <Route index element={<Navigate to="/admin/users" replace />} />
                <Route path="users" element={<AdminUsersTable />} />
                <Route path="projects" element={<AdminProjectsTable />} />
                <Route path="tasks" element={<AdminTasksTable />} />
                <Route path="deferred" element={<AdminDeferredTable />} />
                <Route path="reflections" element={<AdminReflectionsTable />} />
                <Route path="time-blocks" element={<AdminTimeBlocksTable />} />
                <Route path="events" element={<AdminEventsTable />} />
              </Route>

              {/* Protected routes */}
              <Route element={<ProtectedRoute />}>
                {/* App routes — wrapped in AppLayout (sidebar + main area) */}
                <Route element={<AppLayout />}>
                  <Route path="/session/:blockId" element={<ActiveSessionPage />} />
                  <Route path="/" element={<DashboardPage />} />
                  <Route path="/today" element={<TodayPage />} />
                  <Route path="/projects" element={<ProjectsPage />} />
                  <Route path="/inbox" element={<InboxPage />} />
                  <Route path="/end-day" element={<EndRitualPage level="day" />} />
                  <Route path="/end-week" element={<EndRitualPage level="week" />} />
                  <Route path="/end-month" element={<EndRitualPage level="month" />} />
                  <Route path="/start-day" element={<StartDayPage />} />
                  <Route path="/start-week" element={<StartWeekPage />} />
                  <Route path="/start-month" element={<StartMonthPage />} />
                  <Route path="/settings" element={<SettingsPage />} />
                </Route>
              </Route>

              {/* Fallback */}
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </AuthProvider>
        </BrowserRouter>
      </ActiveSessionProvider>
    </QueryClientProvider>
  )
}

export default App
