import { Component } from 'react'
import { useTranslation } from 'react-i18next'

/**
 * Class component is required: React 18 still has no hook equivalent for
 * getDerivedStateFromError / componentDidCatch.
 *
 * i18n note: hooks don't work in class components, so the translated fallback
 * UI lives in a small functional wrapper (DefaultFallback) that calls
 * useTranslation. The boundary itself stays render-only.
 *
 * External error tracking (Sentry, etc.) is out of scope for this component
 * — see issue #82.
 */
export class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { error: null }
  }

  static getDerivedStateFromError(error) {
    return { error }
  }

  componentDidCatch(error, info) {
    // Surfacing to dev console; production error tracking is #82's job.
    console.error('ErrorBoundary caught a render error:', error, info)
  }

  handleReload = () => {
    window.location.reload()
  }

  render() {
    if (!this.state.error) {
      return this.props.children
    }

    if (this.props.fallback) {
      return typeof this.props.fallback === 'function'
        ? this.props.fallback({ error: this.state.error, reload: this.handleReload })
        : this.props.fallback
    }

    return <DefaultFallback error={this.state.error} onReload={this.handleReload} />
  }
}

function DefaultFallback({ error, onReload }) {
  const { t } = useTranslation('common')
  const showDetails = import.meta.env.DEV

  return (
    <div
      role="alert"
      className="min-h-screen flex items-center justify-center bg-surface px-6 py-12"
    >
      <div className="bg-surface-raised border border-edge rounded-2xl shadow-card max-w-md w-full p-8 text-center">
        <h1 className="text-xl font-semibold text-ink-heading mb-3">
          {t('errorBoundary.headline')}
        </h1>
        <p className="text-ink-body mb-6 leading-relaxed">
          {t('errorBoundary.body')}
        </p>
        <button
          type="button"
          onClick={onReload}
          className="px-5 py-2.5 rounded-lg bg-primary-500 text-white font-medium hover:bg-primary-600 transition-colors"
        >
          {t('errorBoundary.reload')}
        </button>
        {showDetails && error && (
          <details className="mt-6 text-left text-sm text-ink-muted">
            <summary className="cursor-pointer select-none">
              {t('errorBoundary.detailsLabel')}
            </summary>
            <pre className="mt-2 whitespace-pre-wrap break-words font-mono text-xs bg-surface p-3 rounded-md border border-edge">
              {error?.stack || String(error)}
            </pre>
          </details>
        )}
      </div>
    </div>
  )
}

export default ErrorBoundary
