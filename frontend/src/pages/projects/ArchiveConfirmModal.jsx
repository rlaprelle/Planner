import * as Dialog from '@radix-ui/react-dialog'
import { useTranslation } from 'react-i18next'

export function ArchiveConfirmModal({ open, onOpenChange, project, onConfirm, isPending }) {
  const { t } = useTranslation('tasks')
  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/40 z-40" />
        <Dialog.Content className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-50 w-[calc(100vw-2rem)] max-w-sm bg-surface-raised rounded-xl shadow-modal p-6 focus:outline-none">
          <Dialog.Title className="text-base font-semibold text-ink-heading mb-2">
            {t('archiveThisProject')}
          </Dialog.Title>
          <Dialog.Description className="text-sm text-ink-secondary mb-5">
            {t('archiveProjectConfirm', { name: project?.name })}
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
