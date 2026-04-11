import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { archiveEvent } from '@/api/events'
import { ENERGY_OPTIONS } from './constants'

const ENERGY_BADGE = {
  LOW: 'bg-green-100 text-green-700',
  MEDIUM: 'bg-yellow-100 text-yellow-700',
  HIGH: 'bg-red-100 text-red-700',
}

function isPast(dateStr) {
  return dateStr < new Date().toISOString().slice(0, 10)
}

function EventRow({ event, projectId }) {
  const { t, i18n } = useTranslation('tasks')
  const queryClient = useQueryClient()
  const past = isPast(event.blockDate)

  const formatDate = (dateStr) => {
    if (!dateStr) return ''
    return new Intl.DateTimeFormat(i18n.language, { month: 'numeric', day: 'numeric', year: '2-digit' }).format(new Date(dateStr + 'T00:00:00'))
  }

  const formatTime = (timeStr) => {
    if (!timeStr) return ''
    return new Intl.DateTimeFormat(i18n.language, { hour: 'numeric', minute: '2-digit' }).format(new Date(`2000-01-01T${timeStr}`))
  }

  const archiveMutation = useMutation({
    mutationFn: () => archiveEvent(event.id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['events', projectId] }),
  })

  const energyLabel = ENERGY_OPTIONS.find((o) => o.value === event.energyLevel)?.label
  const energyColor = ENERGY_BADGE[event.energyLevel] || ''

  return (
    <div
      className={[
        'flex items-center gap-2 py-2 px-3 rounded-lg group transition-colors hover:bg-surface-soft border border-transparent',
        past ? 'opacity-50' : '',
      ].join(' ')}
    >
      {/* Calendar icon */}
      <svg className="w-4 h-4 flex-shrink-0 text-amber-500" fill="currentColor" viewBox="0 0 20 20">
        <path fillRule="evenodd" d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z" clipRule="evenodd" />
      </svg>

      {/* Date */}
      <span className="flex-shrink-0 text-xs text-ink-muted w-16">
        {formatDate(event.blockDate)}
      </span>

      {/* Time range */}
      <span className="flex-shrink-0 text-xs text-ink-secondary w-28">
        {formatTime(event.startTime)} – {formatTime(event.endTime)}
      </span>

      {/* Title */}
      <span className="flex-1 min-w-0 text-sm text-ink-body truncate">
        {event.title}
      </span>

      {/* Energy badge */}
      {energyLabel && (
        <span className={`flex-shrink-0 text-xs px-1.5 py-0.5 rounded font-medium ${energyColor}`}>
          {energyLabel}
        </span>
      )}

      {/* Archive button */}
      <button
        type="button"
        onClick={() => archiveMutation.mutate()}
        disabled={archiveMutation.isPending}
        className="flex-shrink-0 text-xs text-ink-faint hover:text-red-500 opacity-0 group-hover:opacity-100 focus:opacity-100 transition-opacity focus:outline-none"
        aria-label={t('archiveEventName', { title: event.title })}
      >
        {archiveMutation.isPending ? '\u2026' : t('common:archive')}
      </button>
    </div>
  )
}

export function EventList({ events, projectId }) {
  const { t } = useTranslation('tasks')
  if (!events || events.length === 0) return null

  // Sort: future first (ascending), then past (descending)
  const sorted = [...events].sort((a, b) => {
    const aPast = isPast(a.blockDate)
    const bPast = isPast(b.blockDate)
    if (aPast !== bPast) return aPast ? 1 : -1
    if (!aPast) {
      // Both future: ascending by date then time
      return a.blockDate.localeCompare(b.blockDate) || a.startTime.localeCompare(b.startTime)
    }
    // Both past: descending by date then time
    return b.blockDate.localeCompare(a.blockDate) || b.startTime.localeCompare(a.startTime)
  })

  return (
    <section>
      <h3 className="text-xs font-semibold text-ink-muted uppercase tracking-wider px-3 mb-2">
        {t('events')}
      </h3>
      <div className="space-y-0.5">
        {sorted.map((event) => (
          <EventRow key={event.id} event={event} projectId={projectId} />
        ))}
      </div>
    </section>
  )
}
