import { useQuery } from '@tanstack/react-query'
import { getTasks, createTask, updateTask, deleteTask, getUsers, getProjects } from '@/api/admin'
import { useAdminCrud } from './hooks/useAdminCrud'
import { AdminCrudPage } from './components/AdminCrudPage'

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

  const crud = useAdminCrud({
    queryKey: ['admin', 'tasks'],
    listFn: getTasks, createFn: createTask, updateFn: updateTask, deleteFn: deleteTask,
  })

  return <AdminCrudPage title="Tasks" entityName="Task" columns={COLUMNS} fields={formFields} crud={crud} />
}
