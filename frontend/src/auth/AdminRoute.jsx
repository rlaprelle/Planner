import { Navigate, Outlet } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from './useAuth'

/**
 * Mirrors the backend authorization rule: anonymous users are bounced to
 * login, authenticated non-admin users see a friendly access-denied message
 * rather than a raw 401/403. The backend remains the actual enforcer — this
 * guard exists so the UI behaves sensibly before the server responds.
 */
export function AdminRoute() {
  const { token, isAdmin, isLoading } = useAuth()
  const { t } = useTranslation('admin')

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  if (!token) {
    return <Navigate to="/login" replace />
  }

  if (!isAdmin) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 px-6">
        <div className="max-w-md text-center">
          <h1 className="text-2xl font-semibold text-gray-900 mb-2">
            {t('accessDeniedTitle')}
          </h1>
          <p className="text-gray-600 mb-6">{t('accessDeniedMessage')}</p>
          <a
            href="/"
            className="inline-block px-4 py-2 bg-indigo-600 text-white rounded-md text-sm font-medium hover:bg-indigo-700"
          >
            {t('backToApp')}
          </a>
        </div>
      </div>
    )
  }

  return <Outlet />
}
