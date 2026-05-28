import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { I18nextProvider } from 'react-i18next'
import i18n from '../i18n'
import { ErrorBoundary } from './ErrorBoundary'

function Boom() {
  throw new Error('kaboom')
}

function Innocent() {
  return <div>I am fine</div>
}

function renderWithI18n(ui) {
  return render(<I18nextProvider i18n={i18n}>{ui}</I18nextProvider>)
}

describe('ErrorBoundary', () => {
  let errorSpy
  const swallowWindowError = (e) => e.preventDefault()

  beforeEach(() => {
    // React intentionally re-logs caught render errors via console.error in dev;
    // silence them so test output stays readable. The boundary's own
    // console.error call is also captured by this spy.
    errorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    // jsdom dispatches the original throw as a window 'error' event before the
    // boundary catches it, which prints a stack trace. Preventing default
    // suppresses the noisy default logging without affecting test outcomes.
    window.addEventListener('error', swallowWindowError)
  })

  afterEach(() => {
    errorSpy.mockRestore()
    window.removeEventListener('error', swallowWindowError)
  })

  it('renders the fallback UI when a child throws', () => {
    renderWithI18n(
      <ErrorBoundary>
        <Boom />
      </ErrorBoundary>,
    )

    expect(screen.getByRole('alert')).toBeInTheDocument()
    expect(screen.getByText(/something went wrong/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /refresh/i })).toBeInTheDocument()
  })

  it('renders children unchanged when nothing throws', () => {
    renderWithI18n(
      <ErrorBoundary>
        <Innocent />
      </ErrorBoundary>,
    )

    expect(screen.getByText('I am fine')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('honors a custom fallback render prop', () => {
    renderWithI18n(
      <ErrorBoundary fallback={({ error }) => <div>custom: {error.message}</div>}>
        <Boom />
      </ErrorBoundary>,
    )

    expect(screen.getByText('custom: kaboom')).toBeInTheDocument()
  })

  it('logs the error to the console', () => {
    renderWithI18n(
      <ErrorBoundary>
        <Boom />
      </ErrorBoundary>,
    )

    const loggedOurMessage = errorSpy.mock.calls.some(args =>
      args.some(arg => typeof arg === 'string' && arg.includes('ErrorBoundary caught')),
    )
    expect(loggedOurMessage).toBe(true)
  })
})
