import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getTasks, createTask, updateTask, deleteTask, getUsers, getProjects } from '@/api/admin'
import { AdminTable } from './components/AdminTable'
import { AdminFormModal } from './components/AdminFormModal'
import { DeleteConfirmDialog } from './components/DeleteConfirmDialog'

const COLUMNS = [
  { key: 'userEmail', label: 'User' },
  { key: 'projectName', label: 'Project' },
  { key: 'title', label: 'Title' },
  { key: 'status', label: 'Status' },
  { key: 'priority', label: 'Priority' },
  { key: 'dueDate', label: 'Due' },
]

const STATUS_OPTIONS = [
  { value: 'TODO', label: 'Todo' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'BLOCKED', label: 'Blocked' },
  { value: 'DONE', label: 'Done' },
  { value: 'SKIPPED', label: 'Skipped' },
]

const ENERGY_OPTIONS = [
  { value: '', label: 'None' },
  { value: 'LOW', label: 'Low' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'HIGH', label: 'High' },
]

export default function AdminTasksTable() {
  const queryClient = useQueryClient()
  const [formOpen, setFormOpen] = useState(false)
  const [editItem, setEditItem] = useState(null)
  const [deleteItem, setDeleteItem] = useState(null)

  const { data: tasks = [], isLoading } = useQuery({ queryKey: ['admin', 'tasks'], queryFn: getTasks })
  const { data: users = [] } = useQuery({ queryKey: ['admin', 'users'], queryFn: getUsers })
  const { data: projects = [] } = useQuery({ queryKey: ['admin', 'projects'], queryFn: getProjects })

  const userOptions = users.map(u => ({ value: u.id, label: u.email }))
  const projectOptions = projects.map(p => ({ value: p.id, label: `${p.name} (${p.userEmail})` }))

  const formFields = [
    { name: 'userId', label: 'User', type: 'select', options: userOptions, required: true },
    { name: 'projectId', label: 'Project', type: 'select', options: projectOptions, required: true },
    { name: 'title', label: 'Title', required: true },
    { name: 'description', label: 'Description', type: 'textarea' },
    { name: 'status', label: 'Status', type: 'select', options: STATUS_OPTIONS, defaultValue: 'TODO' },
    { name: 'priority', label: 'Priority', type: 'number', defaultValue: 3 },
    { name: 'pointsEstimate', label: 'Points Estimate', type: 'number' },
    { name: 'energyLevel', label: 'Energy Level', type: 'select', options: ENERGY_OPTIONS },
    { name: 'dueDate', label: 'Due Date', type: 'date' },
    { name: 'sortOrder', label: 'Sort Order', type: 'number', defaultValue: 0 },
  ]

  const createMutation = useMutation({
    mutationFn: (data) => createTask(data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'tasks'] }); setFormOpen(false) },
  })
  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => updateTask(id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'tasks'] }); setFormOpen(false); setEditItem(null) },
  })
  const deleteMutation = useMutation({
    mutationFn: (id) => deleteTask(id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'tasks'] }); setDeleteItem(null) },
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
        <h1 className="text-xl font-bold text-gray-900">Tasks</h1>
        <button onClick={() => { setEditItem(null); setFormOpen(true) }}
                className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700">
          + Create Task
        </button>
      </div>
      <AdminTable columns={COLUMNS} data={tasks} onEdit={handleEdit} onDelete={handleDelete} entityName="tasks" />
      <AdminFormModal open={formOpen} onOpenChange={(open) => { setFormOpen(open); if (!open) setEditItem(null) }}
        title={editItem ? 'Edit Task' : 'Create Task'} fields={formFields} initialValues={editItem}
        onSubmit={handleSubmit} isPending={createMutation.isPending || updateMutation.isPending} />
      <DeleteConfirmDialog open={!!deleteItem} onOpenChange={(open) => { if (!open) setDeleteItem(null) }}
        entityName="task" onConfirm={() => deleteMutation.mutate(deleteItem.id)}
        isPending={deleteMutation.isPending} />
    </div>
  )
}
