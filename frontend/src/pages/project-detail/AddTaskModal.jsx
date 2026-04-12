import { useState } from 'react'
import * as Dialog from '@radix-ui/react-dialog'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { createTask } from '@/api/tasks'
import { PRIORITY_OPTIONS } from './constants'
import { PlusIcon } from './icons'

export function AddTaskModal({ open, onOpenChange, projectId, parentTaskId = null, parentTitle = null }) {
  const { t } = useTranslation('tasks')
  const [title, setTitle] = useState('')
  const [dueDate, setDueDate] = useState('')
  const [priority, setPriority] = useState('')
  const [titleError, setTitleError] = useState('')

  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (data) => createTask(projectId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks', projectId] })
      queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'tasks'] })
      onOpenChange(false)
      resetForm()
    },
  })

  function resetForm() {
    setTitle('')
    setDueDate('')
    setPriority('')
    setTitleError('')
    mutation.reset()
  }

  function handleOpenChange(val) {
    if (!val) resetForm()
    onOpenChange(val)
  }

  function handleSubmit(e) {
    e.preventDefault()
    if (mutation.isPending) return
    if (!title.trim()) {
      setTitleError(t('titleRequired'))
      return
    }
    setTitleError('')
    const data = {
      title: title.trim(),
      ...(dueDate ? { dueDate } : {}),
      ...(priority ? { priority: Number(priority) } : {}),
      ...(parentTaskId ? { parentTaskId } : {}),
    }
    mutation.mutate(data)
  }

  const modalTitle = parentTitle ? t('addSubtaskTo', { title: parentTitle }) : t('addTask')

  return (
    <Dialog.Root open={open} onOpenChange={handleOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/40 z-40 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0" />
        <Dialog.Content className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-50 w-full max-w-md bg-surface-raised rounded-xl shadow-modal p-6 focus:outline-none data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95">
          <Dialog.Title className="text-lg font-semibold text-ink-heading mb-4">
            {modalTitle}
          </Dialog.Title>

          <form onSubmit={handleSubmit} noValidate className="space-y-4">
            {/* Title */}
            <div>
              <label htmlFor="task-title" className="block text-sm font-medium text-ink-body mb-1">
                {t('title')} <span className="text-error">*</span>
              </label>
              <input
                id="task-title"
                type="text"
                value={title}
                onChange={(e) => { setTitle(e.target.value); setTitleError('') }}
                placeholder={t('whatNeedsToBeDone')}
                className={[
                  'w-full px-3 py-2 text-sm border rounded-md focus:outline-none focus:ring-2 focus:ring-edge-focus focus:border-transparent transition-colors',
                  titleError ? 'border-error' : 'border-edge',
                ].join(' ')}
                autoFocus
              />
              {titleError && <p className="mt-1 text-xs text-error">{titleError}</p>}
            </div>

            {/* Due date */}
            <div>
              <label htmlFor="task-due-date" className="block text-sm font-medium text-ink-body mb-1">
                {t('common:dueDate')} <span className="text-ink-muted font-normal">{t('common:optional')}</span>
              </label>
              <input
                id="task-due-date"
                type="date"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                className="w-full px-3 py-2 text-sm border border-edge rounded-md focus:outline-none focus:ring-2 focus:ring-edge-focus focus:border-transparent transition-colors"
              />
            </div>

            {/* Priority */}
            <div>
              <label htmlFor="task-priority" className="block text-sm font-medium text-ink-body mb-1">
                {t('priority')} <span className="text-ink-muted font-normal">{t('common:optional')}</span>
              </label>
              <select
                id="task-priority"
                value={priority}
                onChange={(e) => setPriority(e.target.value)}
                className="w-full px-3 py-2 text-sm border border-edge rounded-md focus:outline-none focus:ring-2 focus:ring-edge-focus focus:border-transparent transition-colors bg-surface-raised"
              >
                <option value="">{t('common:noneSelected')}</option>
                {PRIORITY_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
              </select>
            </div>

            {mutation.isError && (
              <p className="text-sm text-error">
                {mutation.error?.message || t('common:genericError')}
              </p>
            )}

            <div className="flex justify-end gap-3 pt-2">
              <Dialog.Close asChild>
                <button
                  type="button"
                  className="px-4 py-2 text-sm font-medium text-ink-body bg-surface-raised border border-edge rounded-md hover:bg-surface-soft focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1 transition-colors"
                >
                  {t('common:cancel')}
                </button>
              </Dialog.Close>
              <button
                type="submit"
                disabled={mutation.isPending}
                className="px-4 py-2 text-sm font-medium text-white bg-primary-500 rounded-md hover:bg-primary-600 focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1 disabled:opacity-60 disabled:cursor-not-allowed transition-colors"
              >
                {mutation.isPending ? t('adding') : t('addTask')}
              </button>
            </div>
          </form>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
