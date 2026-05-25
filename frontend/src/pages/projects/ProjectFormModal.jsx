import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import * as Dialog from '@radix-ui/react-dialog'
import { useTranslation } from 'react-i18next'
import { createProject, updateProject } from '@/api/projects'
import { ColorPicker } from './ColorPicker'
import { DEFAULT_COLOR } from './constants'

export function ProjectFormModal({ open, onOpenChange, project, onSuccess }) {
  const { t } = useTranslation('tasks')
  const isEdit = Boolean(project)
  const [name, setName] = useState(project?.name ?? '')
  const [description, setDescription] = useState(project?.description ?? '')
  const [color, setColor] = useState(project?.color ?? DEFAULT_COLOR)
  const [icon, setIcon] = useState(project?.icon ?? '')
  const [nameError, setNameError] = useState('')

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
      setNameError(t('nameRequired'))
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
            {isEdit ? t('editProject') : t('newProject')}
          </Dialog.Title>

          <form onSubmit={handleSubmit} noValidate className="space-y-4">
            {/* Name */}
            <div>
              <label htmlFor="project-name" className="block text-sm font-medium text-ink-body mb-1">
                {t('name')} <span className="text-error">*</span>
              </label>
              <input
                id="project-name"
                type="text"
                value={name}
                onChange={(e) => { setName(e.target.value); setNameError('') }}
                placeholder={t('namePlaceholder')}
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
                {t('common:description')} <span className="text-ink-muted font-normal">{t('common:optional')}</span>
              </label>
              <textarea
                id="project-description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder={t('whatIsThisAbout')}
                rows={2}
                className="w-full px-3 py-2 text-sm border border-edge rounded-md focus:outline-none focus:ring-2 focus:ring-edge-focus focus:border-transparent transition-colors resize-none"
              />
            </div>

            {/* Color */}
            <div>
              <label className="block text-sm font-medium text-ink-body mb-2">
                {t('color')} <span className="text-ink-muted font-normal">{t('common:optional')}</span>
              </label>
              <ColorPicker value={color} onChange={setColor} />
            </div>

            {/* Icon */}
            <div>
              <label htmlFor="project-icon" className="block text-sm font-medium text-ink-body mb-1">
                {t('icon')} <span className="text-ink-muted font-normal">{t('common:optional')}</span>
              </label>
              <input
                id="project-icon"
                type="text"
                value={icon}
                onChange={(e) => setIcon(e.target.value)}
                placeholder={t('iconPlaceholder')}
                className="w-full px-3 py-2 text-sm border border-edge rounded-md focus:outline-none focus:ring-2 focus:ring-edge-focus focus:border-transparent transition-colors"
              />
            </div>

            {/* Server error */}
            {mutation.isError && (
              <p className="text-sm text-error">
                {mutation.error?.message || t('common:genericError')}
              </p>
            )}

            {/* Actions */}
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
                {mutation.isPending ? t('common:saving') : t('common:save')}
              </button>
            </div>
          </form>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
