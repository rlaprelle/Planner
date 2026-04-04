import { useQuery } from '@tanstack/react-query'
import { getTimeBlocks, createTimeBlock, updateTimeBlock, deleteTimeBlock, getUsers, getTasks } from '@/api/admin'
import { useAdminCrud } from './hooks/useAdminCrud'
import { AdminCrudPage } from './components/AdminCrudPage'

const COLUMNS = [
  { key: 'userEmail', label: 'User' },
  { key: 'blockDate', label: 'Date' },
  { key: 'taskTitle', label: 'Task' },
  { key: 'startTime', label: 'Start' },
  { key: 'endTime', label: 'End' },
  { key: 'wasCompleted', label: 'Completed' },
]

export default function AdminTimeBlocksTable() {
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

  const crud = useAdminCrud({
    queryKey: ['admin', 'time-blocks'],
    listFn: getTimeBlocks, createFn: createTimeBlock, updateFn: updateTimeBlock, deleteFn: deleteTimeBlock,
  })

  return <AdminCrudPage title="Time Blocks" entityName="Time Block" columns={COLUMNS} fields={formFields} crud={crud} />
}
