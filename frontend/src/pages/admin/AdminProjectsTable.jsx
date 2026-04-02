import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getProjects, createProject, updateProject, deleteProject, getUsers } from '@/api/admin'
import { AdminTable } from './components/AdminTable'
import { AdminFormModal } from './components/AdminFormModal'
import { DeleteConfirmDialog } from './components/DeleteConfirmDialog'

const COLUMNS = [
  { key: 'userEmail', label: 'User' },
  { key: 'name', label: 'Name' },
  { key: 'description', label: 'Description' },
  { key: 'color', label: 'Color', render: (v) => v ? <span className="inline-flex items-center gap-1"><span className="w-3 h-3 rounded-full inline-block" style={{ background: v }} />{v}</span> : '\u2014' },
  { key: 'isActive', label: 'Active' },
  { key: 'createdAt', label: 'Created' },
]

export default function AdminProjectsTable() {
  const queryClient = useQueryClient()
  const [formOpen, setFormOpen] = useState(false)
  const [editItem, setEditItem] = useState(null)
  const [deleteItem, setDeleteItem] = useState(null)

  const { data: projects = [], isLoading } = useQuery({ queryKey: ['admin', 'projects'], queryFn: getProjects })
  const { data: users = [] } = useQuery({ queryKey: ['admin', 'users'], queryFn: getUsers })

  const userOptions = users.map(u => ({ value: u.id, label: u.email }))
  const formFields = [
    { name: 'userId', label: 'User', type: 'select', options: userOptions, required: true },
    { name: 'name', label: 'Name', required: true },
    { name: 'description', label: 'Description', type: 'textarea' },
    { name: 'color', label: 'Color (hex)', placeholder: '#6b4c9a' },
    { name: 'icon', label: 'Icon' },
    { name: 'isActive', label: 'Active', type: 'checkbox', defaultValue: true },
    { name: 'sortOrder', label: 'Sort Order', type: 'number', defaultValue: 0 },
  ]

  const createMutation = useMutation({
    mutationFn: (data) => createProject(data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'projects'] }); setFormOpen(false) },
  })
  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => updateProject(id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'projects'] }); setFormOpen(false); setEditItem(null) },
  })
  const deleteMutation = useMutation({
    mutationFn: (id) => deleteProject(id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'projects'] }); setDeleteItem(null) },
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
        <h1 className="text-xl font-bold text-gray-900">Projects</h1>
        <button onClick={() => { setEditItem(null); setFormOpen(true) }}
                className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700">
          + Create Project
        </button>
      </div>
      <AdminTable columns={COLUMNS} data={projects} onEdit={handleEdit} onDelete={handleDelete} entityName="projects" />
      <AdminFormModal open={formOpen} onOpenChange={(open) => { setFormOpen(open); if (!open) setEditItem(null) }}
        title={editItem ? 'Edit Project' : 'Create Project'} fields={formFields} initialValues={editItem}
        onSubmit={handleSubmit} isPending={createMutation.isPending || updateMutation.isPending} />
      <DeleteConfirmDialog open={!!deleteItem} onOpenChange={(open) => { if (!open) setDeleteItem(null) }}
        entityName="project" item={deleteItem} onConfirm={() => deleteMutation.mutate(deleteItem.id)}
        isPending={deleteMutation.isPending} />
    </div>
  )
}
