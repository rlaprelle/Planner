import { useQuery, useQueryClient } from '@tanstack/react-query'
import { getDeferredItems } from '@/api/deferred'
import { DeferredItemActions } from '@/components/deferred/DeferredItemActions'

export function InboxPage() {
  const queryClient = useQueryClient()
  const { data: items = [], isLoading } = useQuery({
    queryKey: ['deferred'],
    queryFn: getDeferredItems,
  })

  function handleDone() {
    queryClient.invalidateQueries({ queryKey: ['deferred'] })
  }

  if (isLoading) {
    return <div className="p-8 text-ink-muted">Loading…</div>
  }

  return (
    <div className="p-8 max-w-2xl">
      <h1 className="text-2xl font-semibold text-ink-heading mb-6">Inbox</h1>

      {items.length === 0 ? (
        <p className="text-ink-muted">All clear. Nothing waiting for you.</p>
      ) : (
        <ul className="space-y-4">
          {items.map((item) => (
            <li key={item.id} className="bg-surface-raised border border-edge rounded-lg p-4 shadow-card">
              <DeferredItemActions item={item} onDone={handleDone} />
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
