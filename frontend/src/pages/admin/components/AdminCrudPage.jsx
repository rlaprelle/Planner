import { useTranslation } from 'react-i18next'
import { AdminTable } from './AdminTable'
import { AdminFormModal } from './AdminFormModal'
import { DeleteConfirmDialog } from './DeleteConfirmDialog'

export function AdminCrudPage({
  title, entityName, columns, fields, crud,
  dependentCounts, children,
}) {
  const { t } = useTranslation('admin')
  const {
    data, isLoading,
    formOpen, editItem, deleteItem,
    openCreate, openEdit, openDelete,
    handleFormClose, handleDeleteClose,
    handleSubmit, confirmDelete,
    isSaving, isDeleting,
    saveError,
  } = crud

  if (isLoading) return <div className="text-gray-400 py-8 text-center">{t('common:loading')}</div>

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-xl font-bold text-gray-900">{title}</h1>
        <button
          onClick={openCreate}
          className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700"
        >
          {t('createEntity', { entity: entityName })}
        </button>
      </div>

      <AdminTable columns={columns} data={data} onEdit={openEdit} onDelete={openDelete} entityName={title.toLowerCase()} />

      <AdminFormModal
        open={formOpen}
        onOpenChange={handleFormClose}
        title={editItem ? t('editEntity', { entity: entityName }) : t('createEntityHeading', { entity: entityName })}
        fields={fields}
        initialValues={editItem}
        onSubmit={handleSubmit}
        isPending={isSaving}
        saveError={saveError}
      />

      <DeleteConfirmDialog
        open={!!deleteItem}
        onOpenChange={handleDeleteClose}
        entityName={entityName.toLowerCase()}
        dependentCounts={dependentCounts}
        onConfirm={confirmDelete}
        isPending={isDeleting}
      />

      {children}
    </div>
  )
}
