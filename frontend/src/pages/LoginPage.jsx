import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import * as Label from '@radix-ui/react-label'
import { useAuth } from '@/auth/useAuth'
import { EchelLogo } from '@/components/EchelLogo'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const { t } = useTranslation('auth')

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      await login(email, password)
      navigate('/', { replace: true })
    } catch (err) {
      if (err.status === 401) {
        setError(t('invalidCredentials'))
      } else {
        setError(t('common:genericError'))
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen bg-surface flex items-center justify-center px-4">
      <div className="w-full max-w-sm bg-surface-raised rounded-2xl shadow-modal p-8">
        <div className="flex items-center justify-center gap-2.5 mb-1">
          <EchelLogo size={36} />
          <h1 className="text-2xl font-bold text-ink-heading">{t('appName')}</h1>
        </div>
        <p className="text-sm text-ink-muted text-center italic mb-6">
          {t('tagline')}
        </p>

        <form onSubmit={handleSubmit} noValidate className="space-y-5">
          <div className="flex flex-col gap-1.5">
            <Label.Root htmlFor="email" className="text-sm font-medium text-ink-body">
              {t('email')}
            </Label.Root>
            <input
              id="email"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className={`w-full rounded-lg border px-3 py-2 text-ink-heading text-sm shadow-soft
                focus:outline-none focus:ring-2 focus:ring-edge-focus focus:border-edge-focus
                ${error ? 'border-error' : 'border-edge'}`}
              placeholder={t('emailPlaceholder')}
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label.Root htmlFor="password" className="text-sm font-medium text-ink-body">
              {t('password')}
            </Label.Root>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className={`w-full rounded-lg border px-3 py-2 text-ink-heading text-sm shadow-soft
                focus:outline-none focus:ring-2 focus:ring-edge-focus focus:border-edge-focus
                ${error ? 'border-error' : 'border-edge'}`}
              placeholder={t('passwordPlaceholder')}
            />
          </div>

          {error && (
            <p className="text-sm text-error font-medium" role="alert">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full py-2.5 px-4 bg-primary-500 hover:bg-primary-600 disabled:bg-primary-400
              text-white text-sm font-semibold rounded-lg shadow-soft
              focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-2
              transition-colors duration-150"
          >
            {isSubmitting ? t('loggingIn') : t('logIn')}
          </button>
        </form>

        <p className="mt-6 text-sm text-center text-ink-secondary">
          {t('noAccount')}{' '}
          <Link to="/register" className="text-primary-500 hover:text-primary-700 font-medium">
            {t('createOne')}
          </Link>
        </p>
      </div>
    </div>
  )
}
