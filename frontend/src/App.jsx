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
import ProjectDetailPage from '@/pages/ProjectDetailPage'
import { InboxPage } from '@/pages/InboxPage'
import { EndDayPage } from '@/pages/EndDayPage'
import { StartDayPage } from '@/pages/StartDayPage'
import { ActiveSessionProvider } from '@/contexts/ActiveSessionContext'
import ActiveSessionPage from '@/pages/ActiveSessionPage'

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

              {/* Protected routes */}
              <Route element={<ProtectedRoute />}>
                {/* Session page — full-screen, no sidebar */}
                <Route path="/session/:blockId" element={<ActiveSessionPage />} />

                {/* App routes — wrapped in AppLayout (sidebar + main area) */}
                <Route element={<AppLayout />}>
                  <Route path="/" element={<DashboardPage />} />
                  <Route path="/today" element={<TodayPage />} />
                  <Route path="/projects" element={<ProjectsPage />} />
                  <Route path="/projects/:projectId" element={<ProjectDetailPage />} />
                  <Route path="/inbox" element={<InboxPage />} />
                  <Route path="/end-day" element={<EndDayPage />} />
                  <Route path="/start-day" element={<StartDayPage />} />
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
