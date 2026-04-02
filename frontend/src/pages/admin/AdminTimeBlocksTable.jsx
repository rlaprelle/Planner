import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getTimeBlocks, createTimeBlock, updateTimeBlock, deleteTimeBlock, getUsers, getTasks } from '@/api/admin'
import { AdminTable } from './components/AdminTable'
import { AdminFormModal } from './components/AdminFormModal'
import { DeleteConfirmDialog } from './components/DeleteConfirmDialog'

const COLUMNS = [
  { key: 'userEmail', label: 'User' },
  { key: 'blockDate', label: 'Date' },
  { key: 'taskTitle', label: 'Task' },
  { key: 'startTime', label: 'Start' },
  { key: 'endTime', label: 'End' },
  { key: 'wasCompleted', label: 'Completed' },
]

export default function AdminTimeBlocksTable() {
  const queryClient = useQueryClient()
  const [formOpen, setFormOpen] = useState(false)
  const [editItem, setEditItem] = useState(null)
  const [deleteItem, setDeleteItem] = useState(null)

  const { data: blocks = [], isLoading } = useQuery({ queryKey: ['admin', 'time-blocks'], queryFn: getTimeBlocks })
  const { data: users = [] } = useQuery({ queryKey: ['admin', 'users'], queryFn: getUsers })
  const { data: tasks = [] } = useQuery({ queryKey: ['admin', 'tasks'], queryFn: getTasks })

  const userOptions = users.map(u => ({ value: u.id, label: u.email }))
  const taskOptions = [{ value: '', label: 'None' }, ...tasks.map(t => ({ value: t.id, label: `${t.title} (${t.userEmail})` }))]

  const formFields = [
    { name: 'userId', label: 'User', type: 'select', options: userOptions, required: true },
    { name: 'blockDate', label: 'Date', type: 'date', required: true },
    { name: 'taskId', label: 'Task', type: 'select', options: taskOptions },
    { name: 'startTime', label: 'Start Time', type: 'time', required: true },
    { name: 'endTime', label: 'End Time', type: 'time', required: true },
    { name: 'sortOrder', label: 'Sort Order', type: 'number', defaultValue: 0 },
    { name: 'wasCompleted', label: 'Completed', type: 'checkbox' },
  ]

  const createMutation = useMutation({
    mutationFn: (data) => createTimeBlock(data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'time-blocks'] }); setFormOpen(false) },
  })
  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => updateTimeBlock(id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'time-blocks'] }); setFormOpen(false); setEditItem(null) },
  })
  const deleteMutation = useMutation({
    mutationFn: (id) => deleteTimeBlock(id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'time-blocks'] }); setDeleteItem(null) },
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
        <h1 className="text-xl font-bold text-gray-900">Time Blocks</h1>
        <button onClick={() => { setEditItem(null); setFormOpen(true) }}
                className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700">
          + Create Time Block
        </button>
      </div>
      <AdminTable columns={COLUMNS} data={blocks} onEdit={handleEdit} onDelete={handleDelete} entityName="time blocks" />
      <AdminFormModal open={formOpen} onOpenChange={(open) => { setFormOpen(open); if (!open) setEditItem(null) }}
        title={editItem ? 'Edit Time Block' : 'Create Time Block'} fields={formFields} initialValues={editItem}
        onSubmit={handleSubmit} isPending={createMutation.isPending || updateMutation.isPending} />
      <DeleteConfirmDialog open={!!deleteItem} onOpenChange={(open) => { if (!open) setDeleteItem(null) }}
        entityName="time block" onConfirm={() => deleteMutation.mutate(deleteItem.id)}
        isPending={deleteMutation.isPending} />
    </div>
  )
}
