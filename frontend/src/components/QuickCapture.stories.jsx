import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { I18nextProvider } from 'react-i18next'
import { MemoryRouter } from 'react-router-dom'
import { http, HttpResponse, delay } from 'msw'
import i18n from '../i18n'
import { QuickCapture } from './QuickCapture'

/**
 * Integrated story for the Quick Capture flow.
 *
 * Unlike the FlyAwayCard story (which tests one animation in isolation
 * against labeled scaffolding), THIS story renders the REAL QuickCapture
 * component — the same one AppLayout uses — and drives it through a mocked
 * backend via MSW. Click "+ Quick capture" to open the modal, type, and
 * submit; MSW intercepts the POST and returns a fake success so the full
 * animation chain (confirmation checkmark → FlyAwayCard → modal dismiss)
 * plays out against the real component's state machine.
 *
 * The `<a href="/inbox">` element is required because QuickCapture's
 * FlyAwayCard targets it via document.querySelector. In the real app this
 * anchor lives in AppLayout's sidebar; here it's provided by the decorator.
 */

function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, staleTime: Infinity },
      mutations: { retry: false },
    },
  })
}

function Providers({ children }) {
  return (
    <QueryClientProvider client={makeQueryClient()}>
      <I18nextProvider i18n={i18n}>
        <MemoryRouter>{children}</MemoryRouter>
      </I18nextProvider>
    </QueryClientProvider>
  )
}

export default {
  title: 'Flows/QuickCapture',
  component: QuickCapture,
  parameters: {
    layout: 'fullscreen',
  },
  decorators: [
    Story => (
      <Providers>
        <div
          style={{
            display: 'flex',
            minHeight: '100vh',
            background: '#FAF8F6',
          }}
        >
          {/*
            Stand-in for AppLayout's sidebar Inbox link. The real QuickCapture's
            FlyAwayCard queries `a[href="/inbox"]` to compute the animation's
            endpoint — any anchor with that href will do.
          */}
          <aside
            style={{
              width: 240,
              background: '#FFFFFF',
              borderRight: '1px solid #E5DFE8',
              padding: 16,
              display: 'flex',
              flexDirection: 'column',
              gap: 8,
            }}
          >
            <div
              style={{
                fontSize: 11,
                color: '#9B95A3',
                textTransform: 'uppercase',
                letterSpacing: '0.06em',
                marginBottom: 8,
              }}
            >
              decorator scaffolding
            </div>
            <a
              href="/inbox"
              style={{
                display: 'block',
                padding: '8px 12px',
                borderRadius: 6,
                fontSize: 14,
                color: '#6B6573',
                textDecoration: 'none',
              }}
            >
              📥 Inbox
            </a>
            <div style={{ marginTop: 'auto', borderTop: '1px solid #F0ECF2', paddingTop: 12 }}>
              <Story />
            </div>
          </aside>
          <main style={{ flex: 1, padding: 24 }} />
        </div>
      </Providers>
    ),
  ],
}

export const HappyPath = {
  name: 'Happy path (Capture & close)',
  parameters: {
    msw: {
      handlers: [
        http.post('/api/v1/deferred', async ({ request }) => {
          const body = await request.json()
          await delay(150)
          return HttpResponse.json({
            id: 'story-mock-1',
            rawText: body.rawText,
            createdAt: new Date().toISOString(),
          })
        }),
      ],
    },
  },
}

export const ServerError = {
  name: 'Server error ("Could not save")',
  parameters: {
    msw: {
      handlers: [
        http.post('/api/v1/deferred', async () => {
          await delay(150)
          return HttpResponse.json({ message: 'Database unavailable' }, { status: 500 })
        }),
      ],
    },
  },
}
