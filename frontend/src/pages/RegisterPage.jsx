import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import * as Label from '@radix-ui/react-label'
import { register } from '@/api/auth'
import { useAuth } from '@/auth/useAuth'
import { EchelLogo } from '@/components/EchelLogo'

export function RegisterPage() {
  const { login } = useAuth()
  const navigate = useNavigate()

  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      const browserTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone
      await register(email, password, displayName, browserTimezone)
    } catch (err) {
      if (err.status === 409) {
        setError('An account with that email already exists.')
      } else if (err.status === 400) {
        setError(err.message || 'Please check your details and try again.')
      } else {
        setError('Something went wrong. Please try again.')
      }
      setIsSubmitting(false)
      return
    }

    // Registration succeeded — auto-login
    try {
      await login(email, password)
      navigate('/', { replace: true })
    } catch {
      // Registration worked but auto-login failed — redirect to login page
      navigate('/login')
    } finally {
      setIsSubmitting(false)
    }
  }

  const inputClass = (hasError) =>
    `w-full rounded-lg border px-3 py-2 text-ink-heading text-sm shadow-soft
     focus:outline-none focus:ring-2 focus:ring-edge-focus focus:border-edge-focus
     ${hasError ? 'border-error' : 'border-edge'}`

  return (
    <div className="min-h-screen bg-surface flex items-center justify-center px-4">
      <div className="w-full max-w-sm bg-surface-raised rounded-2xl shadow-modal p-8">
        <div className="flex items-center justify-center gap-2.5 mb-1">
          <EchelLogo size={36} />
          <h1 className="text-2xl font-bold text-ink-heading">Echel Planner</h1>
        </div>
        <p className="text-sm text-ink-muted text-center italic mb-6">
          Planning that works with your brain, not against it.
        </p>

        <form onSubmit={handleSubmit} noValidate className="space-y-5">
          <div className="flex flex-col gap-1.5">
            <Label.Root htmlFor="displayName" className="text-sm font-medium text-ink-body">
              Display name
            </Label.Root>
            <input
              id="displayName"
              type="text"
              autoComplete="name"
              required
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              className={inputClass(false)}
              placeholder="Your name"
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label.Root htmlFor="email" className="text-sm font-medium text-ink-body">
              Email
            </Label.Root>
            <input
              id="email"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className={inputClass(error?.includes('email'))}
              placeholder="you@example.com"
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label.Root htmlFor="password" className="text-sm font-medium text-ink-body">
              Password
            </Label.Root>
            <input
              id="password"
              type="password"
              autoComplete="new-password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className={inputClass(false)}
              placeholder="••••••••"
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
            {isSubmitting ? 'Creating account…' : 'Create account'}
          </button>
        </form>

        <p className="mt-6 text-sm text-center text-ink-secondary">
          Already have an account?{' '}
          <Link to="/login" className="text-primary-500 hover:text-primary-700 font-medium">
            Log in
          </Link>
        </p>
      </div>
    </div>
  )
}
