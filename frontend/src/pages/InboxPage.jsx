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
    return <div className="p-8 text-gray-400">Loading…</div>
  }

  return (
    <div className="p-8 max-w-2xl">
      <h1 className="text-2xl font-semibold text-gray-900 mb-6">Inbox</h1>

      {items.length === 0 ? (
        <p className="text-gray-400">Nothing to process.</p>
      ) : (
        <ul className="space-y-4">
          {items.map((item) => (
            <li key={item.id} className="bg-white border border-gray-200 rounded-lg p-4">
              <DeferredItemActions item={item} onDone={handleDone} />
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
