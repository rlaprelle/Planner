import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { getProjects, archiveProject } from '@/api/projects'
import { TaskDetailModal } from './project-detail/TaskDetailModal'
import { AddTaskModal } from './project-detail/AddTaskModal'
import { PencilIcon, ArchiveIcon, PlusIcon, Spinner } from './projects/icons'
import { DEFAULT_COLOR } from './projects/constants'
import { ProjectFormModal } from './projects/ProjectFormModal'
import { ArchiveConfirmModal } from './projects/ArchiveConfirmModal'
import { ProjectCardTasks } from './projects/ProjectCardTasks'

function ProjectCard({ project, onEdit, onArchive, onSelectTask, onAddTask }) {
  const { t } = useTranslation('tasks')
  const color = project.color || DEFAULT_COLOR
  return (
    <div className="relative bg-surface-raised rounded-2xl border border-edge shadow-card hover:shadow-card-hover hover:border-primary-200 transition-all group overflow-hidden">
      {/* Top color accent */}
      <div className="h-1" style={{ backgroundColor: color }} aria-hidden="true" />

      <div className="p-5">
        {/* Header: Icon + Name */}
        <div className="flex items-center gap-2">
          {project.icon && (
            <span className="text-xl leading-none flex-shrink-0" aria-hidden="true">
              {project.icon}
            </span>
          )}
          <span className="font-semibold text-ink-heading text-[15px] truncate">
            {project.name}
          </span>
        </div>

        {/* Description */}
        {project.description && (
          <p className="mt-2 text-[13px] text-ink-secondary leading-relaxed line-clamp-2">
            {project.description}
          </p>
        )}

        {/* Active tasks */}
        <div className="mt-3 pt-3 border-t border-edge-subtle">
          <ProjectCardTasks projectId={project.id} onSelectTask={onSelectTask} />
        </div>

        {/* Actions */}
        <div className="flex items-center gap-1 mt-4 pt-3 border-t border-edge-subtle opacity-0 group-hover:opacity-100 focus-within:opacity-100 transition-opacity">
          <button
            type="button"
            onClick={() => onAddTask(project)}
            title={t('newTask')}
            className="p-1.5 text-ink-muted hover:text-primary-600 hover:bg-primary-50 rounded-lg focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1 transition-colors"
            aria-label={t('addTaskTo', { name: project.name })}
          >
            <PlusIcon />
          </button>
          <button
            type="button"
            onClick={() => onEdit(project)}
            title={t('editProject')}
            className="p-1.5 text-ink-muted hover:text-ink-body hover:bg-surface-soft rounded-lg focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1 transition-colors"
            aria-label={t('editProjectName', { name: project.name })}
          >
            <PencilIcon />
          </button>
          <button
            type="button"
            onClick={() => onArchive(project)}
            title={t('common:archive')}
            className="p-1.5 text-ink-muted hover:text-red-600 hover:bg-red-50 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-400 focus:ring-offset-1 transition-colors"
            aria-label={t('archiveProjectName', { name: project.name })}
          >
            <ArchiveIcon />
          </button>
        </div>
      </div>
    </div>
  )
}

export function ProjectsPage() {
  const { t } = useTranslation('tasks')
  const queryClient = useQueryClient()

  const { data: projects, isLoading, isError } = useQuery({
    queryKey: ['projects'],
    queryFn: getProjects,
  })

  // Form modal state
  const [formOpen, setFormOpen] = useState(false)
  const [editingProject, setEditingProject] = useState(null)

  // Task detail modal state
  const [selectedTask, setSelectedTask] = useState(null)
  const [selectedProjectId, setSelectedProjectId] = useState(null)
  const [selectedProjectName, setSelectedProjectName] = useState(null)

  // Add task modal state
  const [addTaskOpen, setAddTaskOpen] = useState(false)
  const [addTaskProjectId, setAddTaskProjectId] = useState(null)

  // Archive modal state
  const [archiveOpen, setArchiveOpen] = useState(false)
  const [archivingProject, setArchivingProject] = useState(null)

  const archiveMutation = useMutation({
    mutationFn: (id) => archiveProject(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] })
      setArchiveOpen(false)
      setArchivingProject(null)
    },
  })

  function handleNewProject() {
    setEditingProject(null)
    setFormOpen(true)
  }

  function handleEditProject(project) {
    setEditingProject(project)
    setFormOpen(true)
  }

  function handleAddTask(project) {
    setAddTaskProjectId(project.id)
    setAddTaskOpen(true)
  }

  function handleArchiveProject(project) {
    setArchivingProject(project)
    setArchiveOpen(true)
  }

  function handleSelectTask(task, project) {
    setSelectedTask(task)
    setSelectedProjectId(project.id)
    setSelectedProjectName(project.name)
  }

  function handleCloseTaskModal(open) {
    if (!open) {
      setSelectedTask(null)
      setSelectedProjectId(null)
      setSelectedProjectName(null)
    }
  }

  function handleConfirmArchive() {
    if (archivingProject) {
      archiveMutation.mutate(archivingProject.id)
    }
  }

  return (
    <div className="p-4 sm:p-8 max-w-4xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold text-ink-heading">{t('projects')}</h1>
        <button
          type="button"
          onClick={handleNewProject}
          className="inline-flex items-center gap-1.5 px-3 py-2 text-sm font-medium text-white bg-primary-500 rounded-md hover:bg-primary-600 focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1 transition-colors"
        >
          <PlusIcon />
          {t('newProject')}
        </button>
      </div>

      {/* Content */}
      {isLoading && <Spinner />}

      {isError && (
        <div className="py-10 text-center text-sm text-error">
          {t('loadProjectsFailed')}
        </div>
      )}

      {!isLoading && !isError && projects?.length === 0 && (
        <div className="py-16 text-center">
          <p className="text-ink-muted text-sm">{t('noProjectsYet')}</p>
          <button
            type="button"
            onClick={handleNewProject}
            className="mt-4 inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium text-primary-500 border border-primary-300 rounded-md hover:bg-primary-50 focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1 transition-colors"
          >
            <PlusIcon />
            {t('createAProject')}
          </button>
        </div>
      )}

      {!isLoading && !isError && projects?.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4" role="list">
          {projects.map((project) => (
            <div key={project.id} role="listitem">
              <ProjectCard
                project={project}
                onEdit={handleEditProject}
                onArchive={handleArchiveProject}
                onSelectTask={(task) => handleSelectTask(task, project)}
                onAddTask={handleAddTask}
              />
            </div>
          ))}
        </div>
      )}

      {/* Create / Edit modal */}
      <ProjectFormModal
        key={editingProject?.id ?? 'new'}
        open={formOpen}
        onOpenChange={setFormOpen}
        project={editingProject}
      />

      {/* Archive confirm modal */}
      <ArchiveConfirmModal
        open={archiveOpen}
        onOpenChange={setArchiveOpen}
        project={archivingProject}
        onConfirm={handleConfirmArchive}
        isPending={archiveMutation.isPending}
      />

      {/* Add task modal */}
      <AddTaskModal
        open={addTaskOpen}
        onOpenChange={setAddTaskOpen}
        projectId={addTaskProjectId}
      />

      {/* Task detail modal */}
      {selectedTask && (
        <TaskDetailModal
          key={selectedTask.id}
          open
          onOpenChange={handleCloseTaskModal}
          task={selectedTask}
          projectName={selectedProjectName}
          projectId={selectedProjectId}
        />
      )}
    </div>
  )
}
