import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getDeferredItems, createDeferredItem, updateDeferredItem, deleteDeferredItem, getUsers } from '@/api/admin'
import { AdminTable } from './components/AdminTable'
import { AdminFormModal } from './components/AdminFormModal'
import { DeleteConfirmDialog } from './components/DeleteConfirmDialog'

const COLUMNS = [
  { key: 'userEmail', label: 'User' },
  { key: 'rawText', label: 'Text' },
  { key: 'isProcessed', label: 'Processed' },
  { key: 'deferralCount', label: 'Deferrals' },
  { key: 'deferredUntilDate', label: 'Deferred Until' },
  { key: 'capturedAt', label: 'Captured' },
]

export default function AdminDeferredTable() {
  const queryClient = useQueryClient()
  const [formOpen, setFormOpen] = useState(false)
  const [editItem, setEditItem] = useState(null)
  const [deleteItem, setDeleteItem] = useState(null)

  const { data: items = [], isLoading } = useQuery({ queryKey: ['admin', 'deferred-items'], queryFn: getDeferredItems })
  const { data: users = [] } = useQuery({ queryKey: ['admin', 'users'], queryFn: getUsers })

  const userOptions = users.map(u => ({ value: u.id, label: u.email }))
  const formFields = [
    { name: 'userId', label: 'User', type: 'select', options: userOptions, required: true },
    { name: 'rawText', label: 'Text', type: 'textarea', required: true },
    { name: 'isProcessed', label: 'Processed', type: 'checkbox' },
    { name: 'deferredUntilDate', label: 'Deferred Until', type: 'date' },
    { name: 'deferralCount', label: 'Deferral Count', type: 'number', defaultValue: 0 },
  ]

  const createMutation = useMutation({
    mutationFn: (data) => createDeferredItem(data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'deferred-items'] }); setFormOpen(false) },
  })
  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => updateDeferredItem(id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'deferred-items'] }); setFormOpen(false); setEditItem(null) },
  })
  const deleteMutation = useMutation({
    mutationFn: (id) => deleteDeferredItem(id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'deferred-items'] }); setDeleteItem(null) },
  })

  const handleEdit = (row) => { setEditItem(row); setFormOpen(true) }
  const handleDelete = (row) => setDeleteItem(row)
  const handleSubmit = (data) => {
    if (editItem) {
      updateMutation.mutate({ id: editItem.id, data })
    } else {
      createMutation.mutate(data)
    }
  }

  if (isLoading) return <div className="text-gray-400 py-8 text-center">Loading...</div>

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-xl font-bold text-gray-900">Deferred Items</h1>
        <button onClick={() => { setEditItem(null); setFormOpen(true) }}
                className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700">
          + Create Item
        </button>
      </div>
      <AdminTable columns={COLUMNS} data={items} onEdit={handleEdit} onDelete={handleDelete} entityName="deferred items" />
      <AdminFormModal open={formOpen} onOpenChange={(open) => { setFormOpen(open); if (!open) setEditItem(null) }}
        title={editItem ? 'Edit Deferred Item' : 'Create Deferred Item'} fields={formFields} initialValues={editItem}
        onSubmit={handleSubmit} isPending={createMutation.isPending || updateMutation.isPending} />
      <DeleteConfirmDialog open={!!deleteItem} onOpenChange={(open) => { if (!open) setDeleteItem(null) }}
        entityName="deferred item" item={deleteItem} onConfirm={() => deleteMutation.mutate(deleteItem.id)}
        isPending={deleteMutation.isPending} />
    </div>
  )
}
