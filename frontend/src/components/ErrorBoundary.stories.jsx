import { useState } from 'react'
import { I18nextProvider } from 'react-i18next'
import i18n from '../i18n'
import { ErrorBoundary } from './ErrorBoundary'

/**
 * The ErrorBoundary is a UI primitive — no router, no query client needed.
 * Only i18n is required for the default fallback's translated strings.
 */
function Providers({ children }) {
  return <I18nextProvider i18n={i18n}>{children}</I18nextProvider>
}

function ThrowOnRender() {
  throw new Error('Storybook: simulated render crash')
}

/**
 * A "throw on demand" child for the happy-path story so the user can
 * trigger the error state interactively without re-mounting the boundary.
 */
function ToggleableChild() {
  const [shouldThrow, setShouldThrow] = useState(false)
  if (shouldThrow) throw new Error('Storybook: user-triggered crash')
  return (
    <div className="p-8 text-center">
      <p className="text-ink-body mb-4">
        Boundary is mounted, child is rendering happily.
      </p>
      <button
        type="button"
        onClick={() => setShouldThrow(true)}
        className="px-4 py-2 rounded-lg border border-edge bg-surface-raised text-ink-body hover:bg-surface-soft transition-colors"
      >
        Throw an error
      </button>
    </div>
  )
}

export default {
  title: 'Components/ErrorBoundary',
  component: ErrorBoundary,
  parameters: {
    layout: 'fullscreen',
  },
  decorators: [
    Story => (
      <Providers>
        <Story />
      </Providers>
    ),
  ],
}

export const FallbackUI = {
  name: 'Fallback (after a crash)',
  render: () => (
    <ErrorBoundary>
      <ThrowOnRender />
    </ErrorBoundary>
  ),
}

export const HappyPath = {
  name: 'Happy path (children render normally)',
  render: () => (
    <ErrorBoundary>
      <ToggleableChild />
    </ErrorBoundary>
  ),
}
