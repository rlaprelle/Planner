import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getReflections, createReflection, updateReflection, deleteReflection, getUsers } from '@/api/admin'
import { AdminTable } from './components/AdminTable'
import { AdminFormModal } from './components/AdminFormModal'
import { DeleteConfirmDialog } from './components/DeleteConfirmDialog'

const COLUMNS = [
  { key: 'userEmail', label: 'User' },
  { key: 'reflectionDate', label: 'Date' },
  { key: 'energyRating', label: 'Energy' },
  { key: 'moodRating', label: 'Mood' },
  { key: 'reflectionNotes', label: 'Notes' },
  { key: 'isFinalized', label: 'Finalized' },
]

export default function AdminReflectionsTable() {
  const queryClient = useQueryClient()
  const [formOpen, setFormOpen] = useState(false)
  const [editItem, setEditItem] = useState(null)
  const [deleteItem, setDeleteItem] = useState(null)

  const { data: reflections = [], isLoading } = useQuery({ queryKey: ['admin', 'reflections'], queryFn: getReflections })
  const { data: users = [] } = useQuery({ queryKey: ['admin', 'users'], queryFn: getUsers })

  const userOptions = users.map(u => ({ value: u.id, label: u.email }))
  const formFields = [
    { name: 'userId', label: 'User', type: 'select', options: userOptions, required: true },
    { name: 'reflectionDate', label: 'Date', type: 'date', required: true },
    { name: 'energyRating', label: 'Energy Rating (1-5)', type: 'number', required: true },
    { name: 'moodRating', label: 'Mood Rating (1-5)', type: 'number', required: true },
    { name: 'reflectionNotes', label: 'Notes', type: 'textarea' },
    { name: 'isFinalized', label: 'Finalized', type: 'checkbox' },
  ]

  const createMutation = useMutation({
    mutationFn: (data) => createReflection(data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'reflections'] }); setFormOpen(false) },
  })
  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => updateReflection(id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'reflections'] }); setFormOpen(false); setEditItem(null) },
  })
  const deleteMutation = useMutation({
    mutationFn: (id) => deleteReflection(id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'reflections'] }); setDeleteItem(null) },
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
        <h1 className="text-xl font-bold text-gray-900">Reflections</h1>
        <button onClick={() => { setEditItem(null); setFormOpen(true) }}
                className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700">
          + Create Reflection
        </button>
      </div>
      <AdminTable columns={COLUMNS} data={reflections} onEdit={handleEdit} onDelete={handleDelete} entityName="reflections" />
      <AdminFormModal open={formOpen} onOpenChange={(open) => { setFormOpen(open); if (!open) setEditItem(null) }}
        title={editItem ? 'Edit Reflection' : 'Create Reflection'} fields={formFields} initialValues={editItem}
        onSubmit={handleSubmit} isPending={createMutation.isPending || updateMutation.isPending} />
      <DeleteConfirmDialog open={!!deleteItem} onOpenChange={(open) => { if (!open) setDeleteItem(null) }}
        entityName="reflection" onConfirm={() => deleteMutation.mutate(deleteItem.id)}
        isPending={deleteMutation.isPending} />
    </div>
  )
}
