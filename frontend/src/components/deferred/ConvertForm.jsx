import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { getProjects } from '@/api/projects'
import { convertDeferredItem } from '@/api/deferred'

export function ConvertForm({ item, onDone, onCancel }) {
  const [title, setTitle] = useState(item.rawText)
  const [projectId, setProjectId] = useState('')
  const [description, setDescription] = useState('')
  const [dueDate, setDueDate] = useState('')
  const [priority, setPriority] = useState('')
  const [points, setPoints] = useState('')

  const { data: projects = [] } = useQuery({
    queryKey: ['projects'],
    queryFn: getProjects,
  })

  const mutation = useMutation({
    mutationFn: () =>
      convertDeferredItem(item.id, {
        projectId,
        title,
        description: description || null,
        dueDate: dueDate || null,
        priority: priority ? Number(priority) : null,
        pointsEstimate: points ? Number(points) : null,
      }),
    onSuccess: (task) => onDone(task),
  })

  return (
    <form
      className="mt-3 space-y-3 border-t border-edge-subtle pt-3"
      onSubmit={(e) => { e.preventDefault(); mutation.mutate() }}
    >
      <div>
        <label className="block text-xs font-medium text-ink-secondary mb-1">Project *</label>
        <select
          className="w-full rounded-md border border-edge px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-edge-focus"
          value={projectId}
          onChange={(e) => setProjectId(e.target.value)}
          required
        >
          <option value="">Select a project…</option>
          {projects.map((p) => (
            <option key={p.id} value={p.id}>{p.name}</option>
          ))}
        </select>
      </div>

      <div>
        <label className="block text-xs font-medium text-ink-secondary mb-1">Title *</label>
        <input
          className="w-full rounded-md border border-edge px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-edge-focus"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
        />
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="block text-xs font-medium text-ink-secondary mb-1">Due date</label>
          <input
            type="date"
            className="w-full rounded-md border border-edge px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-edge-focus"
            value={dueDate}
            onChange={(e) => setDueDate(e.target.value)}
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-ink-secondary mb-1">Priority (1–5)</label>
          <input
            type="number" min="1" max="5"
            className="w-full rounded-md border border-edge px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-edge-focus"
            value={priority}
            onChange={(e) => setPriority(e.target.value)}
          />
        </div>
      </div>

      <div>
        <label className="block text-xs font-medium text-ink-secondary mb-1">Points estimate</label>
        <input
          type="number" min="1"
          className="w-full rounded-md border border-edge px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-edge-focus"
          value={points}
          onChange={(e) => setPoints(e.target.value)}
        />
      </div>

      <div>
        <label className="block text-xs font-medium text-ink-secondary mb-1">Description</label>
        <textarea
          rows={2}
          className="w-full rounded-md border border-edge px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-edge-focus resize-none"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
      </div>

      {mutation.isError && (
        <p className="text-xs text-error">Something went wrong. Try again.</p>
      )}

      <div className="flex gap-2 justify-end">
        <button
          type="button"
          onClick={onCancel}
          className="px-3 py-1.5 text-sm rounded-md text-ink-secondary hover:bg-surface-soft transition-colors"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={mutation.isPending}
          className="px-3 py-1.5 text-sm rounded-md bg-primary-500 text-white hover:bg-primary-600 disabled:opacity-50 transition-colors"
        >
          {mutation.isPending ? 'Creating…' : 'Create task'}
        </button>
      </div>
    </form>
  )
}
