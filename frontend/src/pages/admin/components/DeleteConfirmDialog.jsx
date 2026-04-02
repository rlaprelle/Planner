import * as Dialog from '@radix-ui/react-dialog'

const DEPENDENT_TYPES = [
  { key: 'projects', label: 'project' },
  { key: 'tasks', label: 'task' },
  { key: 'deferredItems', label: 'deferred item' },
  { key: 'reflections', label: 'reflection' },
  { key: 'timeBlocks', label: 'time block' },
]

function pluralize(count, singular) {
  return `${count} ${singular}${count !== 1 ? 's' : ''}`
}

export function DeleteConfirmDialog({ open, onOpenChange, entityName, dependentCounts, onConfirm, isPending }) {
  const hasDependents = dependentCounts && Object.values(dependentCounts).some(v => v > 0)

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/40 z-40" />
        <Dialog.Content className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-50 bg-white rounded-xl shadow-xl p-6 w-full max-w-md">
          <Dialog.Title className="text-lg font-semibold text-gray-900 mb-2">
            Delete {entityName}?
          </Dialog.Title>
          <Dialog.Description className="text-sm text-gray-600 mb-4">
            This action cannot be undone.
          </Dialog.Description>

          {hasDependents && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-3 mb-4 text-sm">
              <p className="font-medium text-red-800 mb-1">This will also delete:</p>
              <ul className="text-red-700 space-y-0.5">
                {DEPENDENT_TYPES
                  .filter(({ key }) => dependentCounts[key] > 0)
                  .map(({ key, label }) => (
                    <li key={key}>{'\u2022'} {pluralize(dependentCounts[key], label)}</li>
                  ))
                }
              </ul>
            </div>
          )}

          <div className="flex justify-end gap-2">
            <Dialog.Close asChild>
              <button className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">Cancel</button>
            </Dialog.Close>
            <button
              onClick={onConfirm}
              disabled={isPending}
              className="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
            >
              {isPending ? 'Deleting...' : 'Delete'}
            </button>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
