import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { getProjectTasks } from '@/api/tasks'

const MAX_VISIBLE_TASKS = 3

export function ProjectCardTasks({ projectId, onSelectTask }) {
  const { t } = useTranslation('tasks')
  const { data: tasks } = useQuery({
    queryKey: ['projects', projectId, 'tasks'],
    queryFn: () => getProjectTasks(projectId),
  })

  const today = new Date().toISOString().split('T')[0]
  const activeTasks = (tasks ?? [])
    .filter((t) => !t.parentTaskId && t.status === 'OPEN'
      && (!t.visibleFrom || t.visibleFrom <= today))

  if (activeTasks.length === 0) {
    return (
      <p className="text-xs text-ink-muted italic">{t('noActiveTasks')}</p>
    )
  }

  const visible = activeTasks.slice(0, MAX_VISIBLE_TASKS)
  const overflow = activeTasks.length - MAX_VISIBLE_TASKS

  return (
    <ul className="space-y-1.5">
      {visible.map((task) => (
        <li key={task.id} className="flex items-center gap-2 min-w-0">
          <span className="inline-block w-1.5 h-1.5 rounded-full flex-shrink-0 bg-primary-400" aria-hidden="true" />
          <button
            type="button"
            onClick={(e) => { e.preventDefault(); e.stopPropagation(); onSelectTask(task) }}
            className="text-xs text-ink-body truncate hover:text-primary-600 transition-colors text-left focus:outline-none focus:underline"
          >
            {task.title}
          </button>
        </li>
      ))}
      {overflow > 0 && (
        <li className="text-xs text-ink-muted">
          {t('overflowMore', { count: overflow })}
        </li>
      )}
    </ul>
  )
}
