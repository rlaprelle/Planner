import { useState, useEffect } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { getDeferredItems } from '@/api/deferred'
import { DeferredItemActions } from '@/components/deferred/DeferredItemActions'

function InboxInner({ totalItems, onPhaseComplete }) {
  const queryClient = useQueryClient()
  const [processedCount, setProcessedCount] = useState(0)
  const [showCelebration, setShowCelebration] = useState(false)

  const { data: items = [] } = useQuery({
    queryKey: ['deferred'],
    queryFn: getDeferredItems,
  })

  function handleItemDone() {
    queryClient.invalidateQueries({ queryKey: ['deferred'] })
    const newCount = processedCount + 1
    setProcessedCount(newCount)
    if (newCount >= totalItems) {
      setShowCelebration(true)
      setTimeout(() => {
        setShowCelebration(false)
        onPhaseComplete()
      }, 2000)
    }
  }

  if (showCelebration) {
    return (
      <div className="text-center py-12">
        <div className="text-5xl mb-4">🎉</div>
        <h2 className="text-2xl font-semibold text-ink-heading">Inbox Zero!</h2>
        <p className="mt-2 text-ink-secondary">All caught up.</p>
      </div>
    )
  }

  const currentItem = items[0]

  return (
    <div>
      <h2 className="text-xl font-semibold text-ink-heading mb-2">Process your inbox</h2>
      <p className="text-sm text-ink-muted mb-6">
        {processedCount + 1} of {totalItems}
      </p>

      <div className="w-full bg-surface-soft rounded-full h-1.5 mb-6">
        <div
          className="bg-primary-400 h-1.5 rounded-full transition-all duration-300"
          style={{ width: `${(processedCount / totalItems) * 100}%` }}
        />
      </div>

      <div className="bg-surface-raised border border-edge rounded-lg p-6 shadow-card">
        {currentItem && (
          <DeferredItemActions key={currentItem.id} item={currentItem} onDone={handleItemDone} />
        )}
      </div>
    </div>
  )
}

export function InboxPhase({ onPhaseComplete }) {
  const { data: items = [], isLoading } = useQuery({
    queryKey: ['deferred'],
    queryFn: getDeferredItems,
  })

  useEffect(() => {
    if (!isLoading && items.length === 0) {
      onPhaseComplete()
    }
  }, [isLoading, items.length, onPhaseComplete])

  if (isLoading) {
    return <div className="p-8 text-ink-muted text-sm">Loading…</div>
  }

  if (items.length === 0) return null

  return <InboxInner totalItems={items.length} onPhaseComplete={onPhaseComplete} />
}
