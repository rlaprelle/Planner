import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
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

export default function AdminProjectsTable() {
  const { t } = useTranslation('admin')
  const { data: users = [] } = useQuery({ queryKey: ['admin', 'users'], queryFn: getUsers })

  const userOptions = users.map(u => ({ value: u.id, label: u.email }))

  const columns = [
    { key: 'userEmail', label: t('user') },
    { key: 'name', label: t('name') },
    { key: 'description', label: t('description') },
    { key: 'color', label: t('color'), render: renderColorSwatch },
    { key: 'isActive', label: t('active') },
    { key: 'createdAt', label: t('created') },
  ]

  const formFields = [
    { name: 'userId', label: t('user'), type: 'select', options: userOptions, required: true },
    { name: 'name', label: t('name'), required: true },
    { name: 'description', label: t('description'), type: 'textarea' },
    { name: 'color', label: t('colorHex'), placeholder: t('colorPlaceholder') },
    { name: 'icon', label: t('icon') },
    { name: 'isActive', label: t('active'), type: 'checkbox', defaultValue: true },
    { name: 'sortOrder', label: t('sortOrder'), type: 'number', defaultValue: 0 },
  ]

  const crud = useAdminCrud({
    queryKey: ['admin', 'projects'],
    listFn: getProjects, createFn: createProject, updateFn: updateProject, deleteFn: deleteProject,
  })

  return <AdminCrudPage title={t('projects')} entityName={t('project')} columns={columns} fields={formFields} crud={crud} />
}
