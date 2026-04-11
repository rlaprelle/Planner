import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { deferDeferredItem, dismissDeferredItem } from '@/api/deferred'
import { ConvertForm } from './ConvertForm'
import { ConvertToEventForm } from './ConvertToEventForm'
import { formatDistanceToNow } from 'date-fns'

export function DeferredItemActions({ item, onDone }) {
  const { t } = useTranslation('deferred')
  const [mode, setMode] = useState(null) // null | 'convert' | 'convert-event' | 'defer' | 'dismiss-confirm'

  const deferMutation = useMutation({
    mutationFn: (deferFor) => deferDeferredItem(item.id, deferFor),
    onSuccess: () => onDone(),
  })

  const dismissMutation = useMutation({
    mutationFn: () => dismissDeferredItem(item.id),
    onSuccess: () => onDone(),
  })

  const capturedAgo = formatDistanceToNow(new Date(item.capturedAt), { addSuffix: true })

  return (
    <div>
      <p className="text-sm text-ink-muted mb-1">{capturedAgo}</p>
      <p className="text-ink-heading mb-3">{item.rawText}</p>

      {mode === null && (
        <div className="flex gap-2">
          <button
            onClick={() => setMode('convert')}
            className="px-3 py-1.5 text-sm rounded-md bg-primary-100 text-primary-700 hover:bg-primary-200 transition-colors font-medium"
          >
            {t('task')}
          </button>
          <button
            onClick={() => setMode('convert-event')}
            className="px-3 py-1.5 text-sm rounded-md bg-primary-100 text-primary-700 hover:bg-primary-200 transition-colors font-medium"
          >
            {t('event')}
          </button>
          <button
            onClick={() => setMode('defer')}
            className="px-3 py-1.5 text-sm rounded-md border border-edge text-ink-secondary hover:bg-surface-soft transition-colors"
          >
            {t('defer')}
          </button>
          <button
            onClick={() => setMode('dismiss-confirm')}
            className="px-3 py-1.5 text-sm rounded-md border border-edge text-ink-muted hover:bg-surface-soft transition-colors"
          >
            {t('dismissAction')}
          </button>
        </div>
      )}

      {mode === 'convert' && (
        <ConvertForm item={item} onDone={() => onDone()} onCancel={() => setMode(null)} />
      )}

      {mode === 'convert-event' && (
        <ConvertToEventForm item={item} onDone={() => onDone()} onCancel={() => setMode(null)} />
      )}

      {mode === 'defer' && (
        <div className="flex gap-2 mt-1">
          <span className="text-sm text-ink-muted self-center mr-1">{t('deferFor')}</span>
          {[
            { label: t('oneDay'), value: 'ONE_DAY' },
            { label: t('oneWeek'), value: 'ONE_WEEK' },
            { label: t('oneMonth'), value: 'ONE_MONTH' },
          ].map(({ label, value }) => (
            <button
              key={value}
              onClick={() => deferMutation.mutate(value)}
              disabled={deferMutation.isPending}
              className="px-3 py-1.5 text-sm rounded-md border border-edge text-ink-secondary hover:bg-surface-soft disabled:opacity-50 transition-colors"
            >
              {label}
            </button>
          ))}
          <button
            onClick={() => setMode(null)}
            className="px-2 py-1.5 text-sm text-ink-muted hover:text-ink-secondary"
          >
            ✕
          </button>
        </div>
      )}

      {mode === 'dismiss-confirm' && (
        <div className="flex gap-2 mt-1 items-center">
          <span className="text-sm text-ink-muted">{t('sure')}</span>
          <button
            onClick={() => dismissMutation.mutate()}
            disabled={dismissMutation.isPending}
            className="px-3 py-1.5 text-sm rounded-md bg-red-50 text-red-700 border border-red-200 hover:bg-red-100 disabled:opacity-50 transition-colors"
          >
            {t('yesDismiss')}
          </button>
          <button
            onClick={() => setMode(null)}
            className="px-2 py-1.5 text-sm text-ink-muted hover:text-ink-secondary"
          >
            {t('common:cancel')}
          </button>
        </div>
      )}
    </div>
  )
}
