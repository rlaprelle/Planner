import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams, Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { confirmEmailChange } from '@/api/account'
import { useAuth } from '@/auth/useAuth'
import { EchelLogo } from '@/components/EchelLogo'

// Reached from the verification link emailed to the new address. Redeems the
// token, then sends the user to log in again — confirming an email change
// revokes all sessions on the backend, so they must re-authenticate.
export function ConfirmEmailChangePage() {
  const { t } = useTranslation('auth')
  const { logout } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')

  const [status, setStatus] = useState(token ? 'verifying' : 'error')
  const [newEmail, setNewEmail] = useState(null)
  const ran = useRef(false)

  useEffect(() => {
    if (!token || ran.current) return
    // Guard against double-invocation (React 18 StrictMode) consuming the
    // single-use token twice and surfacing a spurious "already used" error.
    ran.current = true
    confirmEmailChange(token)
      .then((data) => {
        setNewEmail(data?.email ?? null)
        setStatus('success')
        // Sessions were revoked server-side; clear any stale client auth state.
        logout()
      })
      .catch(() => setStatus('error'))
  }, [token, logout])

  return (
    <div className="min-h-screen bg-surface flex items-center justify-center px-4">
      <div className="w-full max-w-sm bg-surface-raised rounded-2xl shadow-modal p-8 text-center">
        <div className="flex items-center justify-center gap-2.5 mb-6">
          <EchelLogo size={36} />
          <h1 className="text-2xl font-bold text-ink-heading">{t('appName')}</h1>
        </div>

        {status === 'verifying' && (
          <p className="text-sm text-ink-secondary" role="status">{t('verifyingEmail')}</p>
        )}

        {status === 'success' && (
          <>
            <h2 className="text-lg font-semibold text-ink-heading mb-2">{t('emailUpdatedTitle')}</h2>
            <p className="text-sm text-ink-secondary mb-6">
              {newEmail ? t('emailUpdatedBody', { email: newEmail }) : t('emailUpdatedBodyGeneric')}
            </p>
            <button
              type="button"
              onClick={() => navigate('/login', { replace: true })}
              className="w-full py-2.5 px-4 bg-primary-500 hover:bg-primary-600
                text-white text-sm font-semibold rounded-lg shadow-soft
                focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-2
                transition-colors duration-150"
            >
              {t('logIn')}
            </button>
          </>
        )}

        {status === 'error' && (
          <>
            <h2 className="text-lg font-semibold text-ink-heading mb-2">{t('emailLinkInvalidTitle')}</h2>
            <p className="text-sm text-ink-secondary mb-6">{t('emailLinkInvalidBody')}</p>
            <Link
              to="/login"
              className="text-primary-500 hover:text-primary-700 font-medium text-sm"
            >
              {t('backToLogin')}
            </Link>
          </>
        )}
      </div>
    </div>
  )
}
