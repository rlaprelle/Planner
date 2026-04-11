import { useQuery } from '@tanstack/react-query'
import { getUsers, getProjects, getEvents, createAdminEvent, updateAdminEvent, deleteAdminEvent } from '@/api/admin'
import { useAdminCrud } from './hooks/useAdminCrud'
import { AdminCrudPage } from './components/AdminCrudPage'

const COLUMNS = [
  { key: 'userEmail', label: 'User' },
  { key: 'projectName', label: 'Project' },
  { key: 'title', label: 'Title' },
  { key: 'blockDate', label: 'Date' },
  { key: 'startTime', label: 'Start' },
  { key: 'endTime', label: 'End' },
  { key: 'energyLevel', label: 'Energy' },
]

export default function AdminEventsTable() {
  const { data: users = [] } = useQuery({ queryKey: ['admin', 'users'], queryFn: getUsers })
  const { data: projects = [] } = useQuery({ queryKey: ['admin', 'projects'], queryFn: getProjects })

  const userOptions = users.map(u => ({ value: u.id, label: u.email }))
  const projectOptions = projects.map(p => ({ value: p.id, label: p.name }))
  const formFields = [
    { name: 'userId', label: 'User', type: 'select', options: userOptions, required: true },
    { name: 'projectId', label: 'Project', type: 'select', options: projectOptions },
    { name: 'title', label: 'Title', type: 'text', required: true },
    { name: 'description', label: 'Description', type: 'textarea' },
    { name: 'blockDate', label: 'Date', type: 'date', required: true },
    { name: 'startTime', label: 'Start Time', type: 'time', required: true },
    { name: 'endTime', label: 'End Time', type: 'time', required: true },
    { name: 'energyLevel', label: 'Energy Level', type: 'select', options: [
      { value: '', label: 'Not set' },
      { value: 'LOW', label: 'LOW' },
      { value: 'MEDIUM', label: 'MEDIUM' },
      { value: 'HIGH', label: 'HIGH' },
    ]},
  ]

  const crud = useAdminCrud({
    queryKey: ['admin', 'events'],
    listFn: getEvents, createFn: createAdminEvent, updateFn: updateAdminEvent, deleteFn: deleteAdminEvent,
  })

  return <AdminCrudPage title="Events" entityName="Event" columns={COLUMNS} fields={formFields} crud={crud} />
}
