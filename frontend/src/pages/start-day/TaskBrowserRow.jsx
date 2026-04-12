import { useTranslation } from 'react-i18next'
import { TaskCard } from './TaskCard'

/**
 * A horizontally scrollable row of project columns, each showing tasks for that project.
 *
 * Props:
 *   tasks           - array of TaskResponse (already filtered for this row)
 *   selectedTaskIds - Set<string>
 *   scheduledTaskIds - Set<string> (tasks already on calendar)
 *   onToggle(taskId) - toggle selection
 *   emptyMessage    - string shown when a project has no tasks matching this row's filter
 */
export function TaskBrowserRow({ tasks, selectedTaskIds, scheduledTaskIds, onToggle, emptyMessage, section = 'default' }) {
  const { t } = useTranslation('timeBlocking')
  const resolvedEmptyMessage = emptyMessage ?? t('nothingHere')

  // Group tasks by project, maintaining insertion order
  const projectMap = new Map()
  for (const task of tasks) {
    const key = task.projectId
    if (!projectMap.has(key)) {
      projectMap.set(key, { name: task.projectName ?? t('unknown'), color: task.projectColor, tasks: [] })
    }
    projectMap.get(key).tasks.push(task)
  }

  if (projectMap.size === 0) {
    return (
      <p className="text-xs text-ink-muted italic px-1">{resolvedEmptyMessage}</p>
    )
  }

  return (
    <div className="flex gap-3 overflow-x-auto pb-1">
      {[...projectMap.entries()].map(([projectId, { name, color, tasks: ptasks }]) => (
        <div
          key={projectId}
          className="min-w-[130px] max-w-[160px] flex-shrink-0 bg-primary-50 rounded-lg p-2"
        >
          {/* Project header */}
          <div className="flex items-center gap-1.5 mb-2">
            {color && (
              <span
                className="inline-block w-2.5 h-2.5 rounded-full shrink-0"
                style={{ background: color }}
              />
            )}
            <span className="text-xs font-bold text-primary-800 truncate">{name}</span>
          </div>

          {/* Task cards */}
          <div className="flex flex-col gap-1">
            {ptasks.map((task) => (
              <TaskCard
                key={task.id}
                task={task}
                isSelected={selectedTaskIds.has(task.id)}
                isScheduled={scheduledTaskIds.has(task.id)}
                onToggle={onToggle}
                section={section}
              />
            ))}
          </div>
        </div>
      ))}
      {/* Scroll affordance */}
      <div className="w-4 shrink-0" />
    </div>
  )
}
