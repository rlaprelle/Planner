import { DEADLINE_GROUP_LABELS } from './constants'
import { TaskRow } from './TaskRow'

const DEADLINE_GROUP_ORDER = ['TODAY', 'THIS_WEEK', 'NO_DEADLINE']

function groupTopLevelTasks(tasks) {
  const groups = { TODAY: [], THIS_WEEK: [], NO_DEADLINE: [] }
  for (const task of tasks) {
    const group = task.deadlineGroup || 'NO_DEADLINE'
    if (groups[group]) {
      groups[group].push(task)
    } else {
      groups.NO_DEADLINE.push(task)
    }
  }
  return groups
}

export function TaskList({ tasks, projectId, selectedTask, onSelectTask }) {
  // Only top-level tasks — children are nested inside each task object
  const topLevel = tasks.filter((t) => !t.parentTaskId)
  const selectedTaskId = selectedTask?.id ?? null

  if (topLevel.length === 0) {
    return (
      <div className="py-16 text-center">
        <p className="text-gray-400 text-sm">No tasks yet.</p>
      </div>
    )
  }

  const groups = groupTopLevelTasks(topLevel)

  return (
    <div className="space-y-6">
      {DEADLINE_GROUP_ORDER.map((groupKey) => {
        const groupTasks = groups[groupKey]
        if (!groupTasks || groupTasks.length === 0) return null

        return (
          <section key={groupKey}>
            <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider px-3 mb-2">
              {DEADLINE_GROUP_LABELS[groupKey]}
            </h3>
            <div className="space-y-0.5">
              {groupTasks.map((task) => (
                <TaskRow
                  key={task.id}
                  task={task}
                  projectId={projectId}
                  onSelect={onSelectTask}
                  selectedTaskId={selectedTaskId}
                />
              ))}
            </div>
          </section>
        )
      })}
    </div>
  )
}
