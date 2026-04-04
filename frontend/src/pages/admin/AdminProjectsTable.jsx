import { useQuery } from '@tanstack/react-query'
import { getProjects, createProject, updateProject, deleteProject, getUsers } from '@/api/admin'
import { useAdminCrud } from './hooks/useAdminCrud'
import { AdminCrudPage } from './components/AdminCrudPage'

function renderColorSwatch(color) {
  if (!color) return '\u2014'
  return (
    <span className="inline-flex items-center gap-1">
      <span className="w-3 h-3 rounded-full inline-block" style={{ background: color }} />
      {color}
    </span>
  )
}

const COLUMNS = [
  { key: 'userEmail', label: 'User' },
  { key: 'name', label: 'Name' },
  { key: 'description', label: 'Description' },
  { key: 'color', label: 'Color', render: renderColorSwatch },
  { key: 'isActive', label: 'Active' },
  { key: 'createdAt', label: 'Created' },
]

export default function AdminProjectsTable() {
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

  const crud = useAdminCrud({
    queryKey: ['admin', 'projects'],
    listFn: getProjects, createFn: createProject, updateFn: updateProject, deleteFn: deleteProject,
  })

  return <AdminCrudPage title="Projects" entityName="Project" columns={COLUMNS} fields={formFields} crud={crud} />
}
