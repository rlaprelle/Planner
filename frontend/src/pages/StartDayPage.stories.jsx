import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { I18nextProvider } from 'react-i18next'
import { MemoryRouter } from 'react-router-dom'
import { http, HttpResponse } from 'msw'
import { format } from 'date-fns'
import i18n from '../i18n'
import { StartDayPage } from './StartDayPage'
import {
  fakeSuggestedTasks,
  emptySchedule,
  emptyEvents,
} from '../testing/fixtures/schedule'

/**
 * Integrated story for the Start Day planning page.
 *
 * This renders the REAL StartDayPage against MSW-mocked API responses. Use
 * it to iterate on the drag-and-drop scheduling experience — dragging task
 * cards from the browser rows at the bottom onto the time grid, seeing the
 * 15-minute snap, the drop preview, and (in variants with existing blocks)
 * the push-forward overlap behavior.
 *
 * Shared mock data lives in src/testing/fixtures/schedule.js. Keep this
 * story focused on the empty-day flow; variants that prepopulate the grid
 * with existing blocks or events belong in separate stories with their own
 * fixture data.
 */

const TODAY = format(new Date(), 'yyyy-MM-dd')

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
  title: 'Flows/StartDayPage',
  component: StartDayPage,
  parameters: {
    layout: 'fullscreen',
  },
  decorators: [
    Story => (
      <Providers>
        <div style={{ minHeight: '100vh', background: '#FAF8F6' }}>
          {/*
            Banner identifying this as Storybook scaffolding. The real page is
            rendered inside AppLayout's main area (with a sidebar); here we
            render it standalone since sidebar context isn't needed for the
            drag-and-drop flow under test.
          */}
          <div
            style={{
              padding: '6px 16px',
              background: '#F3EEF8',
              borderBottom: '1px solid #E5DFE8',
              fontSize: 11,
              color: '#6B6573',
              textTransform: 'uppercase',
              letterSpacing: '0.06em',
            }}
          >
            Storybook scaffolding — real page below, mocked backend
          </div>
          <Story />
        </div>
      </Providers>
    ),
  ],
}

export const EmptyDay = {
  name: 'Empty day — plan from scratch',
  parameters: {
    msw: {
      handlers: [
        // Task browser populated with fixture data
        http.get('/api/v1/tasks/suggested', () => HttpResponse.json(fakeSuggestedTasks)),
        // No existing plan for today
        http.get('/api/v1/schedule/today', () => HttpResponse.json(emptySchedule)),
        // No events on the calendar
        http.get('/api/v1/events/for-date', () => HttpResponse.json(emptyEvents)),
        // Save-plan echoes back what it received — success shape
        http.post('/api/v1/schedule/today/plan', async ({ request }) => {
          const body = await request.json()
          return HttpResponse.json({
            blockDate: TODAY,
            blocks: body.blocks,
            startHour: body.startHour,
            endHour: body.endHour,
          })
        }),
      ],
    },
  },
}
