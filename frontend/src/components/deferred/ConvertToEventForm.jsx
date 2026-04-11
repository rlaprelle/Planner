import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { getProjects } from '@/api/projects'
import { convertDeferredToEvent } from '@/api/deferred'

/**
 * Form for converting a deferred item into a scheduled event (time block).
 * Collects project, title, date/time range, energy level, and description.
 */
export function ConvertToEventForm({ item, onDone, onCancel }) {
  const [title, setTitle] = useState(item.rawText)
  const [projectId, setProjectId] = useState('')
  const [description, setDescription] = useState('')
  const [blockDate, setBlockDate] = useState('')
  const [startTime, setStartTime] = useState('')
  const [endTime, setEndTime] = useState('')
  const [energyLevel, setEnergyLevel] = useState('')

  const { data: projects = [] } = useQuery({
    queryKey: ['projects'],
    queryFn: getProjects,
  })

  const mutation = useMutation({
    mutationFn: () =>
      convertDeferredToEvent(item.id, {
        projectId,
        title,
        description: description || null,
        blockDate,
        startTime: startTime + ':00',
        endTime: endTime + ':00',
        energyLevel: energyLevel || null,
      }),
    onSuccess: () => onDone(),
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

      <div className="grid grid-cols-3 gap-3">
        <div>
          <label className="block text-xs font-medium text-ink-secondary mb-1">Date *</label>
          <input
            type="date"
            className="w-full rounded-md border border-edge px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-edge-focus"
            value={blockDate}
            onChange={(e) => setBlockDate(e.target.value)}
            required
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-ink-secondary mb-1">Start *</label>
          <input
            type="time"
            step="900"
            className="w-full rounded-md border border-edge px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-edge-focus"
            value={startTime}
            onChange={(e) => setStartTime(e.target.value)}
            required
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-ink-secondary mb-1">End *</label>
          <input
            type="time"
            step="900"
            className="w-full rounded-md border border-edge px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-edge-focus"
            value={endTime}
            onChange={(e) => setEndTime(e.target.value)}
            required
          />
        </div>
      </div>

      <div>
        <label className="block text-xs font-medium text-ink-secondary mb-1">Energy level</label>
        <select
          className="w-full rounded-md border border-edge px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-edge-focus"
          value={energyLevel}
          onChange={(e) => setEnergyLevel(e.target.value)}
        >
          <option value="">Any</option>
          <option value="LOW">Low</option>
          <option value="MEDIUM">Medium</option>
          <option value="HIGH">High</option>
        </select>
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
          {mutation.isPending ? 'Creating…' : 'Create event'}
        </button>
      </div>
    </form>
  )
}
