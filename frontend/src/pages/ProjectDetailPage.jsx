import { useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getProject } from '@/api/projects'
import { getProjectTasks } from '@/api/tasks'
import { TaskList } from './project-detail/TaskList'
import { TaskDetailPanel } from './project-detail/TaskDetailPanel'
import { AddTaskModal } from './project-detail/AddTaskModal'
import { PlusIcon, Spinner } from './project-detail/icons'

export default function ProjectDetailPage() {
  const { projectId } = useParams()
  const [selectedTaskId, setSelectedTaskId] = useState(null)
  const [addTaskOpen, setAddTaskOpen] = useState(false)

  const {
    data: project,
    isLoading: projectLoading,
  } = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => getProject(projectId),
    enabled: Boolean(projectId),
  })

  const {
    data: tasks,
    isLoading: tasksLoading,
    isError: tasksError,
  } = useQuery({
    queryKey: ['tasks', projectId],
    queryFn: () => getProjectTasks(projectId),
    enabled: Boolean(projectId),
  })

  const projectName = project?.name ?? '…'
  const isLoading = projectLoading || tasksLoading

  // Find the selected task in the (possibly refreshed) task tree
  const resolvedSelectedTask = selectedTaskId
    ? findTask(tasks ?? [], selectedTaskId)
    : null

  function handleSelectTask(task) {
    setSelectedTaskId(task.id)
  }

  function handleClosePanel() {
    setSelectedTaskId(null)
  }

  return (
    <div className="flex h-full overflow-hidden">
      {/* Left panel — task list */}
      <div className="flex flex-col flex-1 min-w-0 overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-edge bg-surface-raised flex-shrink-0">
          <div>
            <Link
              to="/projects"
              className="text-xs text-primary-500 hover:underline focus:outline-none focus:underline"
            >
              ← Projects
            </Link>
            <h1 className="text-xl font-semibold text-ink-heading mt-0.5">
              {projectLoading ? '…' : (project?.name ?? 'Project')}
            </h1>
          </div>
          <button
            type="button"
            onClick={() => setAddTaskOpen(true)}
            className="inline-flex items-center gap-1.5 px-3 py-2 text-sm font-medium text-white bg-primary-500 rounded-md hover:bg-primary-600 focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1 transition-colors flex-shrink-0"
          >
            <PlusIcon size={15} />
            Add task
          </button>
        </div>

        {/* Task list body */}
        <div className="flex-1 overflow-y-auto px-4 py-4">
          {isLoading && <Spinner />}

          {!isLoading && tasksError && (
            <div className="py-10 text-center text-sm text-error">
              Failed to load tasks. Please try again.
            </div>
          )}

          {!isLoading && !tasksError && (
            <TaskList
              tasks={tasks ?? []}
              projectId={projectId}
              selectedTask={resolvedSelectedTask}
              onSelectTask={handleSelectTask}
            />
          )}
        </div>
      </div>

      {/* Right panel — task details */}
      {resolvedSelectedTask && (
        <div className="w-96 flex-shrink-0 border-l border-edge overflow-hidden">
          <TaskDetailPanel
            key={resolvedSelectedTask.id}
            task={resolvedSelectedTask}
            projectName={projectName}
            projectId={projectId}
            onClose={handleClosePanel}
          />
        </div>
      )}

      {/* Add task modal */}
      <AddTaskModal
        open={addTaskOpen}
        onOpenChange={setAddTaskOpen}
        projectId={projectId}
      />
    </div>
  )
}

// Walk the nested task tree to find a task by id
function findTask(tasks, id) {
  for (const task of tasks) {
    if (task.id === id) return task
    if (task.children?.length) {
      const found = findTask(task.children, id)
      if (found) return found
    }
  }
  return null
}
