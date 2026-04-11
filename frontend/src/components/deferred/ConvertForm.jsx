import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { getProjects } from '@/api/projects'
import { convertDeferredItem } from '@/api/deferred'

export function ConvertForm({ item, onDone, onCancel }) {
  const { t } = useTranslation('deferred')
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
        <label className="block text-xs font-medium text-ink-secondary mb-1">{t('projectRequired')}</label>
        <select
          className="w-full rounded-md border border-edge px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-edge-focus"
          value={projectId}
          onChange={(e) => setProjectId(e.target.value)}
          required
        >
          <option value="">{t('selectAProject')}</option>
          {projects.map((p) => (
            <option key={p.id} value={p.id}>{p.name}</option>
          ))}
        </select>
      </div>

      <div>
        <label className="block text-xs font-medium text-ink-secondary mb-1">{t('titleRequired')}</label>
        <input
          className="w-full rounded-md border border-edge px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-edge-focus"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
        />
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="block text-xs font-medium text-ink-secondary mb-1">{t('common:dueDate')}</label>
          <input
            type="date"
            className="w-full rounded-md border border-edge px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-edge-focus"
            value={dueDate}
            onChange={(e) => setDueDate(e.target.value)}
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-ink-secondary mb-1">{t('priorityRange')}</label>
          <input
            type="number" min="1" max="5"
            className="w-full rounded-md border border-edge px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-edge-focus"
            value={priority}
            onChange={(e) => setPriority(e.target.value)}
          />
        </div>
      </div>

      <div>
        <label className="block text-xs font-medium text-ink-secondary mb-1">{t('pointsEstimate')}</label>
        <input
          type="number" min="1"
          className="w-full rounded-md border border-edge px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-edge-focus"
          value={points}
          onChange={(e) => setPoints(e.target.value)}
        />
      </div>

      <div>
        <label className="block text-xs font-medium text-ink-secondary mb-1">{t('common:description')}</label>
        <textarea
          rows={2}
          className="w-full rounded-md border border-edge px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-edge-focus resize-none"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
      </div>

      {mutation.isError && (
        <p className="text-xs text-error">{t('common:tryAgainError')}</p>
      )}

      <div className="flex gap-2 justify-end">
        <button
          type="button"
          onClick={onCancel}
          className="px-3 py-1.5 text-sm rounded-md text-ink-secondary hover:bg-surface-soft transition-colors"
        >
          {t('common:cancel')}
        </button>
        <button
          type="submit"
          disabled={mutation.isPending}
          className="px-3 py-1.5 text-sm rounded-md bg-primary-500 text-white hover:bg-primary-600 disabled:opacity-50 transition-colors"
        >
          {mutation.isPending ? t('creating') : t('createTask')}
        </button>
      </div>
    </form>
  )
}
