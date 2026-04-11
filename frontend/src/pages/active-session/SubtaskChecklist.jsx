import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateTaskStatus } from '@/api/tasks'

export default function SubtaskChecklist({ subtasks, projectId }) {
  const queryClient = useQueryClient()

  const toggleMutation = useMutation({
    mutationFn: ({ taskId, isDone }) =>
      updateTaskStatus(taskId, isDone ? 'COMPLETED' : 'OPEN'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks', projectId] })
      queryClient.invalidateQueries({ queryKey: ['schedule'] })
    },
  })

  if (!subtasks || subtasks.length === 0) return null

  const sorted = [...subtasks].sort((a, b) => {
    if (a.status === 'COMPLETED' && b.status !== 'COMPLETED') return 1
    if (a.status !== 'COMPLETED' && b.status === 'COMPLETED') return -1
    return (a.sortOrder ?? 0) - (b.sortOrder ?? 0)
  })

  return (
    <div className="bg-surface-raised rounded-2xl p-4 w-full max-w-sm">
      <div className="text-xs font-semibold text-ink-muted uppercase tracking-wider mb-3">
        Subtasks
      </div>
      <div className="space-y-1">
        {sorted.map((child) => {
          const isDone = child.status === 'COMPLETED'
          return (
            <label
              key={child.id}
              className="flex items-center gap-3 py-2 px-1 rounded-lg hover:bg-surface-soft cursor-pointer transition-colors"
            >
              <input
                type="checkbox"
                checked={isDone}
                onChange={() =>
                  toggleMutation.mutate({ taskId: child.id, isDone: !isDone })
                }
                className="w-4 h-4 rounded border-primary-300 text-primary-500 focus:ring-edge-focus"
              />
              <span className={`text-sm ${isDone ? 'text-ink-muted line-through' : 'text-ink-body'}`}>
                {child.title}
              </span>
            </label>
          )
        })}
      </div>
    </div>
  )
}
