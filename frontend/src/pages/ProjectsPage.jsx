import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import * as Dialog from '@radix-ui/react-dialog'
import { getProjects, createProject, updateProject, archiveProject } from '@/api/projects'

// ─── Constants ───────────────────────────────────────────────────────────────

const PRESET_COLORS = [
  { hex: '#6366f1', label: 'Indigo' },
  { hex: '#8b5cf6', label: 'Violet' },
  { hex: '#ec4899', label: 'Pink' },
  { hex: '#f43f5e', label: 'Rose' },
  { hex: '#f97316', label: 'Orange' },
  { hex: '#eab308', label: 'Yellow' },
  { hex: '#22c55e', label: 'Green' },
  { hex: '#14b8a6', label: 'Teal' },
  { hex: '#3b82f6', label: 'Blue' },
  { hex: '#64748b', label: 'Slate' },
]

const DEFAULT_COLOR = '#6366f1'

// ─── Icons ────────────────────────────────────────────────────────────────────

function PencilIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24"
      fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
      aria-hidden="true">
      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
    </svg>
  )
}

function ArchiveIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24"
      fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
      aria-hidden="true">
      <polyline points="21 8 21 21 3 21 3 8" />
      <rect x="1" y="3" width="22" height="5" />
      <line x1="10" y1="12" x2="14" y2="12" />
    </svg>
  )
}

function PlusIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24"
      fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"
      aria-hidden="true">
      <line x1="12" y1="5" x2="12" y2="19" />
      <line x1="5" y1="12" x2="19" y2="12" />
    </svg>
  )
}

function Spinner() {
  return (
    <div className="flex items-center justify-center py-16">
      <div className="w-8 h-8 border-2 border-primary-100 border-t-primary-500 rounded-full animate-spin" />
    </div>
  )
}

// ─── Color Picker ─────────────────────────────────────────────────────────────

function ColorPicker({ value, onChange }) {
  const selected = value || DEFAULT_COLOR
  return (
    <div className="flex flex-wrap gap-2">
      {PRESET_COLORS.map(({ hex, label }) => (
        <button
          key={hex}
          type="button"
          title={label}
          onClick={() => onChange(hex)}
          className={[
            'w-7 h-7 rounded-full transition-transform focus:outline-none focus:ring-2 focus:ring-offset-1 focus:ring-edge-focus',
            selected === hex ? 'ring-2 ring-offset-1 ring-gray-600 scale-110' : 'hover:scale-110',
          ].join(' ')}
          style={{ backgroundColor: hex }}
          aria-label={label}
          aria-pressed={selected === hex}
        />
      ))}
    </div>
  )
}

// ─── Project Form Modal ───────────────────────────────────────────────────────

function ProjectFormModal({ open, onOpenChange, project, onSuccess }) {
  const isEdit = Boolean(project)
  const [name, setName] = useState(project?.name ?? '')
  const [description, setDescription] = useState(project?.description ?? '')
  const [color, setColor] = useState(project?.color ?? DEFAULT_COLOR)
  const [icon, setIcon] = useState(project?.icon ?? '')
  const [nameError, setNameError] = useState('')

  useEffect(() => {
    setName(project?.name ?? '')
    setDescription(project?.description ?? '')
    setColor(project?.color ?? DEFAULT_COLOR)
    setIcon(project?.icon ?? '')
    setNameError('')
  }, [project])

  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (data) =>
      isEdit ? updateProject(project.id, data) : createProject(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] })
      onSuccess?.()
      onOpenChange(false)
    },
  })

  function handleSubmit(e) {
    e.preventDefault()
    mutation.reset()
    if (mutation.isPending) return
    if (!name.trim()) {
      setNameError('Name is required.')
      return
    }
    setNameError('')
    const data = {
      name: name.trim(),
      ...(description.trim() ? { description: description.trim() } : {}),
      ...(color ? { color } : {}),
      ...(icon.trim() ? { icon: icon.trim() } : {}),
    }
    mutation.mutate(data)
  }

  // Reset form when modal opens with new project context
  function handleOpenChange(val) {
    if (val) {
      setName(project?.name ?? '')
      setDescription(project?.description ?? '')
      setColor(project?.color ?? DEFAULT_COLOR)
      setIcon(project?.icon ?? '')
      setNameError('')
    }
    onOpenChange(val)
  }

  return (
    <Dialog.Root open={open} onOpenChange={handleOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/40 z-40 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0" />
        <Dialog.Content className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-50 w-full max-w-md bg-surface-raised rounded-xl shadow-modal p-6 focus:outline-none data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95">
          <Dialog.Title className="text-lg font-semibold text-ink-heading mb-4">
            {isEdit ? 'Edit project' : 'New project'}
          </Dialog.Title>

          <form onSubmit={handleSubmit} noValidate className="space-y-4">
            {/* Name */}
            <div>
              <label htmlFor="project-name" className="block text-sm font-medium text-ink-body mb-1">
                Name <span className="text-error">*</span>
              </label>
              <input
                id="project-name"
                type="text"
                value={name}
                onChange={(e) => { setName(e.target.value); setNameError('') }}
                placeholder="e.g. Website redesign"
                className={[
                  'w-full px-3 py-2 text-sm border rounded-md focus:outline-none focus:ring-2 focus:ring-edge-focus focus:border-transparent transition-colors',
                  nameError ? 'border-error' : 'border-edge',
                ].join(' ')}
                autoFocus
              />
              {nameError && (
                <p className="mt-1 text-xs text-error">{nameError}</p>
              )}
            </div>

            {/* Description */}
            <div>
              <label htmlFor="project-description" className="block text-sm font-medium text-ink-body mb-1">
                Description <span className="text-ink-muted font-normal">(optional)</span>
              </label>
              <textarea
                id="project-description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="What's this project about?"
                rows={2}
                className="w-full px-3 py-2 text-sm border border-edge rounded-md focus:outline-none focus:ring-2 focus:ring-edge-focus focus:border-transparent transition-colors resize-none"
              />
            </div>

            {/* Color */}
            <div>
              <label className="block text-sm font-medium text-ink-body mb-2">
                Color <span className="text-ink-muted font-normal">(optional)</span>
              </label>
              <ColorPicker value={color} onChange={setColor} />
            </div>

            {/* Icon */}
            <div>
              <label htmlFor="project-icon" className="block text-sm font-medium text-ink-body mb-1">
                Icon <span className="text-ink-muted font-normal">(optional)</span>
              </label>
              <input
                id="project-icon"
                type="text"
                value={icon}
                onChange={(e) => setIcon(e.target.value)}
                placeholder="e.g. 🚀 or any text"
                className="w-full px-3 py-2 text-sm border border-edge rounded-md focus:outline-none focus:ring-2 focus:ring-edge-focus focus:border-transparent transition-colors"
              />
            </div>

            {/* Server error */}
            {mutation.isError && (
              <p className="text-sm text-error">
                {mutation.error?.message || 'Something went wrong. Please try again.'}
              </p>
            )}

            {/* Actions */}
            <div className="flex justify-end gap-3 pt-2">
              <Dialog.Close asChild>
                <button
                  type="button"
                  className="px-4 py-2 text-sm font-medium text-ink-body bg-surface-raised border border-edge rounded-md hover:bg-surface-soft focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1 transition-colors"
                >
                  Cancel
                </button>
              </Dialog.Close>
              <button
                type="submit"
                disabled={mutation.isPending}
                className="px-4 py-2 text-sm font-medium text-white bg-primary-500 rounded-md hover:bg-primary-600 focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1 disabled:opacity-60 disabled:cursor-not-allowed transition-colors"
              >
                {mutation.isPending ? 'Saving…' : 'Save'}
              </button>
            </div>
          </form>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}

// ─── Archive Confirm Modal ────────────────────────────────────────────────────

function ArchiveConfirmModal({ open, onOpenChange, project, onConfirm, isPending }) {
  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/40 z-40" />
        <Dialog.Content className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-50 w-full max-w-sm bg-surface-raised rounded-xl shadow-modal p-6 focus:outline-none">
          <Dialog.Title className="text-base font-semibold text-ink-heading mb-2">
            Archive this project?
          </Dialog.Title>
          <Dialog.Description className="text-sm text-ink-secondary mb-5">
            <strong>{project?.name}</strong> will be archived and hidden from your active projects. You can restore it later.
          </Dialog.Description>
          <div className="flex justify-end gap-3">
            <Dialog.Close asChild>
              <button
                type="button"
                className="px-4 py-2 text-sm font-medium text-ink-body bg-surface-raised border border-edge rounded-md hover:bg-surface-soft focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1 transition-colors"
              >
                Cancel
              </button>
            </Dialog.Close>
            <button
              type="button"
              onClick={onConfirm}
              disabled={isPending}
              className="px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-md hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-1 disabled:opacity-60 disabled:cursor-not-allowed transition-colors"
            >
              {isPending ? 'Archiving…' : 'Archive'}
            </button>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}

// ─── Project Row ──────────────────────────────────────────────────────────────

function ProjectRow({ project, onEdit, onArchive }) {
  const color = project.color || DEFAULT_COLOR
  return (
    <div className="flex items-center gap-4 px-4 py-3 bg-surface-raised rounded-lg border border-edge hover:border-edge hover:shadow-card transition-all group">
      {/* Color swatch */}
      <div
        className="w-4 h-4 rounded flex-shrink-0"
        style={{ backgroundColor: color }}
        aria-hidden="true"
      />

      {/* Icon + Name + Description */}
      <div className="flex-1 min-w-0">
        <Link
          to={`/projects/${project.id}`}
          className="flex items-center gap-2 hover:text-primary-700 transition-colors focus:outline-none focus:underline"
        >
          {project.icon && (
            <span className="text-base leading-none flex-shrink-0" aria-hidden="true">
              {project.icon}
            </span>
          )}
          <span className="font-medium text-ink-heading text-sm truncate">
            {project.name}
          </span>
        </Link>
        {project.description && (
          <p className="mt-0.5 text-xs text-ink-muted truncate">{project.description}</p>
        )}
      </div>

      {/* Actions */}
      <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 focus-within:opacity-100 transition-opacity flex-shrink-0">
        <button
          type="button"
          onClick={() => onEdit(project)}
          title="Edit project"
          className="p-1.5 text-ink-muted hover:text-ink-body hover:bg-surface-soft rounded focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1 transition-colors"
          aria-label={`Edit ${project.name}`}
        >
          <PencilIcon />
        </button>
        <button
          type="button"
          onClick={() => onArchive(project)}
          title="Archive project"
          className="p-1.5 text-ink-muted hover:text-red-600 hover:bg-red-50 rounded focus:outline-none focus:ring-2 focus:ring-red-400 focus:ring-offset-1 transition-colors"
          aria-label={`Archive ${project.name}`}
        >
          <ArchiveIcon />
        </button>
      </div>
    </div>
  )
}

// ─── Projects Page ────────────────────────────────────────────────────────────

export function ProjectsPage() {
  const queryClient = useQueryClient()

  const { data: projects, isLoading, isError } = useQuery({
    queryKey: ['projects'],
    queryFn: getProjects,
  })

  // Form modal state
  const [formOpen, setFormOpen] = useState(false)
  const [editingProject, setEditingProject] = useState(null)

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

  function handleArchiveProject(project) {
    setArchivingProject(project)
    setArchiveOpen(true)
  }

  function handleConfirmArchive() {
    if (archivingProject) {
      archiveMutation.mutate(archivingProject.id)
    }
  }

  return (
    <div className="p-8 max-w-2xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold text-ink-heading">Projects</h1>
        <button
          type="button"
          onClick={handleNewProject}
          className="inline-flex items-center gap-1.5 px-3 py-2 text-sm font-medium text-white bg-primary-500 rounded-md hover:bg-primary-600 focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1 transition-colors"
        >
          <PlusIcon />
          New project
        </button>
      </div>

      {/* Content */}
      {isLoading && <Spinner />}

      {isError && (
        <div className="py-10 text-center text-sm text-error">
          Failed to load projects. Please try again.
        </div>
      )}

      {!isLoading && !isError && projects?.length === 0 && (
        <div className="py-16 text-center">
          <p className="text-ink-muted text-sm">No projects yet. Create your first project.</p>
          <button
            type="button"
            onClick={handleNewProject}
            className="mt-4 inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium text-primary-500 border border-primary-300 rounded-md hover:bg-primary-50 focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1 transition-colors"
          >
            <PlusIcon />
            Create a project
          </button>
        </div>
      )}

      {!isLoading && !isError && projects?.length > 0 && (
        <ul className="space-y-2" role="list">
          {projects.map((project) => (
            <li key={project.id}>
              <ProjectRow
                project={project}
                onEdit={handleEditProject}
                onArchive={handleArchiveProject}
              />
            </li>
          ))}
        </ul>
      )}

      {/* Create / Edit modal */}
      <ProjectFormModal
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
    </div>
  )
}
