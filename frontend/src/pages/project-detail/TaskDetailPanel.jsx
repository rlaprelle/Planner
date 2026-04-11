import { useState, useRef, useEffect } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import * as Dialog from '@radix-ui/react-dialog'
import { useTranslation } from 'react-i18next'
import { updateTask, archiveTask, updateTaskStatus } from '@/api/tasks'
import {
  STATUS_OPTIONS,
  PRIORITY_OPTIONS,
  POINTS_OPTIONS,
  ENERGY_OPTIONS,
} from './constants'
import { XIcon, ArchiveIcon, PlusIcon } from './icons'
import { AddTaskModal } from './AddTaskModal'

// ─── Field components ─────────────────────────────────────────────────────────

function FieldLabel({ children }) {
  return <span className="block text-xs font-medium text-ink-muted mb-1">{children}</span>
}

function SelectField({ label, value, onChange, options, emptyLabel }) {
  return (
    <div>
      <FieldLabel>{label}</FieldLabel>
      <select
        value={value ?? ''}
        onChange={(e) => {
          const v = e.target.value
          onChange(v === '' ? null : v)
        }}
        className="w-full px-2 py-1.5 text-sm border border-edge-subtle rounded-md focus:outline-none focus:ring-2 focus:ring-edge-focus focus:border-transparent bg-surface-raised transition-colors"
      >
        {emptyLabel && <option value="">{emptyLabel}</option>}
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>
    </div>
  )
}

// ─── Archive Confirm Modal ────────────────────────────────────────────────────

function ArchiveConfirmModal({ open, onOpenChange, taskTitle, onConfirm, isPending }) {
  const { t } = useTranslation('tasks')
  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/40 z-40" />
        <Dialog.Content className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-50 w-full max-w-sm bg-surface-raised rounded-xl shadow-modal p-6 focus:outline-none">
          <Dialog.Title className="text-base font-semibold text-ink-heading mb-2">
            {t('archiveThisTask')}
          </Dialog.Title>
          <Dialog.Description className="text-sm text-ink-secondary mb-5">
            {t('archiveTaskConfirm', { title: taskTitle })}
          </Dialog.Description>
          <div className="flex justify-end gap-3">
            <Dialog.Close asChild>
              <button
                type="button"
                className="px-4 py-2 text-sm font-medium text-ink-body bg-surface-raised border border-edge rounded-md hover:bg-surface-soft focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1 transition-colors"
              >
                {t('common:cancel')}
              </button>
            </Dialog.Close>
            <button
              type="button"
              onClick={onConfirm}
              disabled={isPending}
              className="px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-md hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-1 disabled:opacity-60 disabled:cursor-not-allowed transition-colors"
            >
              {isPending ? t('common:archiving') : t('common:archive')}
            </button>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}

// ─── Child task row inside detail panel ──────────────────────────────────────

function ChildTaskItem({ child, projectId }) {
  const { t } = useTranslation('tasks')
  const isDone = child.status === 'COMPLETED'
  const queryClient = useQueryClient()

  const statusMutation = useMutation({
    mutationFn: ({ taskId, status }) => updateTaskStatus(taskId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks', projectId] })
    },
  })

  function handleToggle() {
    const newStatus = isDone ? 'OPEN' : 'COMPLETED'
    statusMutation.mutate({ taskId: child.id, status: newStatus })
  }

  return (
    <div className="flex items-center gap-2 py-1">
      <button
        type="button"
        onClick={handleToggle}
        disabled={statusMutation.isPending}
        className={[
          'flex-shrink-0 w-4 h-4 rounded border-2 flex items-center justify-center transition-colors focus:outline-none',
          isDone ? 'bg-success border-success text-white' : 'border-primary-300 hover:border-primary-400',
          statusMutation.isPending ? 'opacity-50' : 'cursor-pointer',
        ].join(' ')}
        aria-label={isDone ? t('common:markAsOpen') : t('common:markAsCompleted')}
      >
        {isDone && (
          <svg width="8" height="8" viewBox="0 0 12 12" fill="none" aria-hidden="true">
            <polyline points="2,6 5,9 10,3" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        )}
      </button>
      <span className={`text-sm flex-1 min-w-0 truncate ${isDone ? 'line-through text-ink-muted' : 'text-ink-body'}`}>
        {child.title}
      </span>
    </div>
  )
}

// ─── Task Detail Panel ────────────────────────────────────────────────────────

export function TaskDetailPanel({ task, projectName, projectId, onClose }) {
  const { t } = useTranslation('tasks')
  const queryClient = useQueryClient()
  const [archiveOpen, setArchiveOpen] = useState(false)
  const [addChildOpen, setAddChildOpen] = useState(false)

  // Local form state — sync when task changes
  const [title, setTitle] = useState(task.title)
  const [description, setDescription] = useState(task.description ?? '')
  const [status, setStatus] = useState(task.status)
  const [priority, setPriority] = useState(task.priority)
  const [pointsEstimate, setPointsEstimate] = useState(task.pointsEstimate ?? '')
  const [energyLevel, setEnergyLevel] = useState(task.energyLevel ?? '')
  const [dueDate, setDueDate] = useState(task.dueDate ?? '')

  const fieldsRef = useRef({})
  useEffect(() => {
    fieldsRef.current = { title, description, status, priority, pointsEstimate, energyLevel, dueDate }
  }, [title, description, status, priority, pointsEstimate, energyLevel, dueDate])

  const updateMutation = useMutation({
    mutationFn: (data) => updateTask(task.id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks', projectId] })
    },
  })

  const archiveMutation = useMutation({
    mutationFn: () => archiveTask(task.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks', projectId] })
      setArchiveOpen(false)
      onClose()
    },
  })

  // Build the full payload for a save — we always send the full task shape
  function buildPayload(overrides = {}) {
    const f = fieldsRef.current
    return {
      title: f.title?.trim() || task.title,
      description: f.description || null,
      status: f.status,
      priority: f.priority,
      pointsEstimate: f.pointsEstimate || null,
      energyLevel: f.energyLevel || null,
      dueDate: f.dueDate || null,
      ...overrides,
    }
  }

  function saveField(overrides) {
    updateMutation.mutate(buildPayload(overrides))
  }

  // Title editable inline
  function handleTitleBlur() {
    const trimmed = title.trim()
    if (!trimmed) {
      setTitle(task.title)
      return
    }
    if (trimmed !== task.title) {
      saveField({ title: trimmed })
    }
  }

  function handleDescriptionBlur() {
    if ((description || null) !== (task.description || null)) {
      saveField({ description: description || null })
    }
  }

  function handleStatusChange(val) {
    setStatus(val)
    saveField({ status: val })
  }

  function handlePriorityChange(val) {
    const num = val ? Number(val) : null
    setPriority(num)
    saveField({ priority: num })
  }

  function handlePointsChange(val) {
    const num = val ? Number(val) : null
    setPointsEstimate(num ?? '')
    saveField({ pointsEstimate: num })
  }

  function handleEnergyChange(val) {
    setEnergyLevel(val ?? '')
    saveField({ energyLevel: val || null })
  }

  function handleDueDateBlur() {
    if ((dueDate || null) !== (task.dueDate || null)) {
      saveField({ dueDate: dueDate || null })
    }
  }

  const children = task.children ?? []

  return (
    <div className="h-full flex flex-col bg-surface-raised border-l border-edge overflow-hidden">
      {/* Header */}
      <div className="flex items-start gap-2 px-5 py-4 border-b border-edge-subtle flex-shrink-0">
        <div className="flex-1 min-w-0">
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            onBlur={handleTitleBlur}
            className="w-full text-base font-semibold text-ink-heading bg-transparent border-0 border-b border-transparent hover:border-edge focus:border-edge-focus focus:outline-none px-0 py-0.5 transition-colors"
            aria-label={t('taskTitle')}
          />
          <p className="text-xs text-ink-muted mt-0.5">{projectName}</p>
        </div>
        <button
          type="button"
          onClick={onClose}
          className="flex-shrink-0 p-1 text-ink-muted hover:text-ink-secondary rounded focus:outline-none focus:ring-2 focus:ring-edge-focus transition-colors mt-0.5"
          aria-label={t('closePanel')}
        >
          <XIcon size={16} />
        </button>
      </div>

      {/* Fields */}
      <div className="flex-1 overflow-y-auto px-5 py-4 space-y-4">
        {/* Status */}
        <SelectField
          label={t('status')}
          value={status}
          onChange={handleStatusChange}
          options={STATUS_OPTIONS}
        />

        {/* Priority */}
        <SelectField
          label={t('priority')}
          value={priority}
          onChange={handlePriorityChange}
          options={PRIORITY_OPTIONS}
        />

        {/* Points Estimate */}
        <SelectField
          label={t('pointsEstimate')}
          value={pointsEstimate}
          onChange={handlePointsChange}
          options={POINTS_OPTIONS}
          emptyLabel={t('common:noneSelected')}
        />

        {/* Energy Level */}
        <SelectField
          label={t('energyLevel')}
          value={energyLevel}
          onChange={handleEnergyChange}
          options={ENERGY_OPTIONS}
          emptyLabel={t('common:noneSelected')}
        />

        {/* Due date */}
        <div>
          <FieldLabel>{t('common:dueDate')}</FieldLabel>
          <input
            type="date"
            value={dueDate}
            onChange={(e) => setDueDate(e.target.value)}
            onBlur={handleDueDateBlur}
            className="w-full px-2 py-1.5 text-sm border border-edge-subtle rounded-md focus:outline-none focus:ring-2 focus:ring-edge-focus focus:border-transparent transition-colors"
          />
        </div>

        {/* Description */}
        <div>
          <FieldLabel>{t('common:description')}</FieldLabel>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            onBlur={handleDescriptionBlur}
            placeholder={t('addSomeDetails')}
            rows={4}
            className="w-full px-2 py-1.5 text-sm border border-edge-subtle rounded-md focus:outline-none focus:ring-2 focus:ring-edge-focus focus:border-transparent transition-colors resize-none"
          />
        </div>

        {/* Child tasks */}
        <div>
          <div className="flex items-center justify-between mb-2">
            <FieldLabel>{t('subtasks')}</FieldLabel>
            <button
              type="button"
              onClick={() => setAddChildOpen(true)}
              className="inline-flex items-center gap-1 text-xs text-primary-500 hover:text-primary-700 focus:outline-none focus:underline transition-colors"
            >
              <PlusIcon size={12} />
              {t('addSubtask')}
            </button>
          </div>
          {children.length === 0 ? (
            <p className="text-xs text-ink-muted italic">{t('noSubtasksYet')}</p>
          ) : (
            <div className="space-y-0.5">
              {children.map((child) => (
                <ChildTaskItem
                  key={child.id}
                  child={child}
                  projectId={projectId}
                />
              ))}
            </div>
          )}
        </div>

        {updateMutation.isError && (
          <p className="text-xs text-error">
            {updateMutation.error?.message
              ? t('saveFailed', { message: updateMutation.error.message })
              : t('saveFailedGeneric')}
          </p>
        )}
      </div>

      {/* Archive button */}
      <div className="px-5 py-4 border-t border-edge-subtle flex-shrink-0">
        <button
          type="button"
          onClick={() => setArchiveOpen(true)}
          className="inline-flex items-center gap-2 text-sm text-ink-muted hover:text-error focus:outline-none focus:ring-2 focus:ring-error focus:ring-offset-1 rounded transition-colors"
        >
          <ArchiveIcon size={14} />
          {t('archiveTask')}
        </button>
      </div>

      {/* Modals */}
      <ArchiveConfirmModal
        open={archiveOpen}
        onOpenChange={setArchiveOpen}
        taskTitle={task.title}
        onConfirm={() => archiveMutation.mutate()}
        isPending={archiveMutation.isPending}
      />

      <AddTaskModal
        open={addChildOpen}
        onOpenChange={setAddChildOpen}
        projectId={projectId}
        parentTaskId={task.id}
        parentTitle={task.title}
      />
    </div>
  )
}
