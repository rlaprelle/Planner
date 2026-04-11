import { useTranslation } from 'react-i18next'
import * as Dialog from '@radix-ui/react-dialog'

const DEPENDENT_KEY_MAP = {
  projects: 'dependentProjects',
  tasks: 'dependentTasks',
  deferredItems: 'dependentDeferredItems',
  reflections: 'dependentReflections',
  timeBlocks: 'dependentTimeBlocks',
}

export function DeleteConfirmDialog({ open, onOpenChange, entityName, dependentCounts, onConfirm, isPending }) {
  const { t } = useTranslation('admin')
  const hasDependents = dependentCounts && Object.values(dependentCounts).some(v => v > 0)

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/40 z-40" />
        <Dialog.Content className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-50 bg-white rounded-xl shadow-xl p-6 w-full max-w-md">
          <Dialog.Title className="text-lg font-semibold text-gray-900 mb-2">
            {t('deleteEntity', { entity: entityName })}
          </Dialog.Title>
          <Dialog.Description className="text-sm text-gray-600 mb-4">
            {t('cannotBeUndone')}
          </Dialog.Description>

          {hasDependents && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-3 mb-4 text-sm">
              <p className="font-medium text-red-800 mb-1">{t('willAlsoDelete')}</p>
              <ul className="text-red-700 space-y-0.5">
                {Object.entries(DEPENDENT_KEY_MAP)
                  .filter(([key]) => dependentCounts[key] > 0)
                  .map(([key, i18nKey]) => (
                    <li key={key}>{'\u2022'} {t(i18nKey, { count: dependentCounts[key] })}</li>
                  ))
                }
              </ul>
            </div>
          )}

          <div className="flex justify-end gap-2">
            <Dialog.Close asChild>
              <button className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">{t('common:cancel')}</button>
            </Dialog.Close>
            <button
              onClick={onConfirm}
              disabled={isPending}
              className="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
            >
              {isPending ? t('deleting') : t('common:delete')}
            </button>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
