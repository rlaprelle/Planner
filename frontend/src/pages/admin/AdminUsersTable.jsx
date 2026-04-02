import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getUsers, createUser, updateUser, deleteUser, getUserDependents } from '@/api/admin'
import { AdminTable } from './components/AdminTable'
import { AdminFormModal } from './components/AdminFormModal'
import { DeleteConfirmDialog } from './components/DeleteConfirmDialog'

const COLUMNS = [
  { key: 'email', label: 'Email' },
  { key: 'displayName', label: 'Display Name' },
  { key: 'timezone', label: 'Timezone' },
  { key: 'createdAt', label: 'Created' },
]

const FORM_FIELDS = [
  { name: 'email', label: 'Email', type: 'email', required: true },
  { name: 'password', label: 'Password', type: 'password', required: false, placeholder: 'Leave blank to keep current' },
  { name: 'displayName', label: 'Display Name', required: true },
  { name: 'timezone', label: 'Timezone', defaultValue: 'UTC' },
]

export default function AdminUsersTable() {
  const queryClient = useQueryClient()
  const [formOpen, setFormOpen] = useState(false)
  const [editItem, setEditItem] = useState(null)
  const [deleteItem, setDeleteItem] = useState(null)
  const [dependents, setDependents] = useState(null)

  const { data: users = [], isLoading } = useQuery({ queryKey: ['admin', 'users'], queryFn: getUsers })

  const createMutation = useMutation({
    mutationFn: (data) => createUser(data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }); setFormOpen(false) },
  })
  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => updateUser(id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }); setFormOpen(false); setEditItem(null) },
  })
  const deleteMutation = useMutation({
    mutationFn: (id) => deleteUser(id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }); setDeleteItem(null) },
  })

  const handleEdit = (row) => { setEditItem(row); setFormOpen(true) }
  const handleDelete = async (row) => {
    const counts = await getUserDependents(row.id)
    setDependents(counts)
    setDeleteItem(row)
  }
  const handleSubmit = (data) => {
    if (editItem) {
      updateMutation.mutate({ id: editItem.id, data })
    } else {
      createMutation.mutate(data)
    }
  }

  const formFields = editItem
    ? FORM_FIELDS
    : FORM_FIELDS.map(f => f.name === 'password' ? { ...f, required: true } : f)

  if (isLoading) return <div className="text-gray-400 py-8 text-center">Loading...</div>

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-xl font-bold text-gray-900">Users</h1>
        <button
          onClick={() => { setEditItem(null); setFormOpen(true) }}
          className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700"
        >
          + Create User
        </button>
      </div>

      <AdminTable columns={COLUMNS} data={users} onEdit={handleEdit} onDelete={handleDelete} entityName="users" />

      <AdminFormModal
        open={formOpen}
        onOpenChange={(open) => { setFormOpen(open); if (!open) setEditItem(null) }}
        title={editItem ? 'Edit User' : 'Create User'}
        fields={formFields}
        initialValues={editItem}
        onSubmit={handleSubmit}
        isPending={createMutation.isPending || updateMutation.isPending}
      />

      <DeleteConfirmDialog
        open={!!deleteItem}
        onOpenChange={(open) => { if (!open) setDeleteItem(null) }}
        entityName="user"
        item={deleteItem}
        dependentCounts={dependents}
        onConfirm={() => deleteMutation.mutate(deleteItem.id)}
        isPending={deleteMutation.isPending}
      />
    </div>
  )
}
