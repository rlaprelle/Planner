import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { deferDeferredItem, dismissDeferredItem } from '@/api/deferred'
import { ConvertForm } from './ConvertForm'
import { formatDistanceToNow } from 'date-fns'

export function DeferredItemActions({ item, onDone }) {
  const [mode, setMode] = useState(null) // null | 'convert' | 'defer' | 'dismiss-confirm'

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
      <p className="text-sm text-gray-500 mb-1">{capturedAgo}</p>
      <p className="text-gray-900 mb-3">{item.rawText}</p>

      {mode === null && (
        <div className="flex gap-2">
          <button
            onClick={() => setMode('convert')}
            className="px-3 py-1.5 text-sm rounded-md bg-indigo-600 text-white hover:bg-indigo-700 transition-colors"
          >
            Convert
          </button>
          <button
            onClick={() => setMode('defer')}
            className="px-3 py-1.5 text-sm rounded-md border border-gray-300 text-gray-700 hover:bg-gray-50 transition-colors"
          >
            Defer
          </button>
          <button
            onClick={() => setMode('dismiss-confirm')}
            className="px-3 py-1.5 text-sm rounded-md border border-gray-300 text-gray-500 hover:bg-gray-50 transition-colors"
          >
            Dismiss
          </button>
        </div>
      )}

      {mode === 'convert' && (
        <ConvertForm item={item} onDone={() => onDone()} onCancel={() => setMode(null)} />
      )}

      {mode === 'defer' && (
        <div className="flex gap-2 mt-1">
          <span className="text-sm text-gray-500 self-center mr-1">Defer for:</span>
          {[
            { label: '1 day', value: 'ONE_DAY' },
            { label: '1 week', value: 'ONE_WEEK' },
            { label: '1 month', value: 'ONE_MONTH' },
          ].map(({ label, value }) => (
            <button
              key={value}
              onClick={() => deferMutation.mutate(value)}
              disabled={deferMutation.isPending}
              className="px-3 py-1.5 text-sm rounded-md border border-gray-300 text-gray-700 hover:bg-gray-50 disabled:opacity-50 transition-colors"
            >
              {label}
            </button>
          ))}
          <button
            onClick={() => setMode(null)}
            className="px-2 py-1.5 text-sm text-gray-400 hover:text-gray-600"
          >
            ✕
          </button>
        </div>
      )}

      {mode === 'dismiss-confirm' && (
        <div className="flex gap-2 mt-1 items-center">
          <span className="text-sm text-gray-500">Sure?</span>
          <button
            onClick={() => dismissMutation.mutate()}
            disabled={dismissMutation.isPending}
            className="px-3 py-1.5 text-sm rounded-md bg-red-50 text-red-700 border border-red-200 hover:bg-red-100 disabled:opacity-50 transition-colors"
          >
            Yes, dismiss
          </button>
          <button
            onClick={() => setMode(null)}
            className="px-2 py-1.5 text-sm text-gray-400 hover:text-gray-600"
          >
            Cancel
          </button>
        </div>
      )}
    </div>
  )
}
