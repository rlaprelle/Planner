import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { getTasks, createTask, updateTask, deleteTask, getUsers, getProjects } from '@/api/admin'
import { useAdminCrud } from './hooks/useAdminCrud'
import { AdminCrudPage } from './components/AdminCrudPage'

export default function AdminTasksTable() {
  const { t } = useTranslation('admin')
  const { data: users = [] } = useQuery({ queryKey: ['admin', 'users'], queryFn: getUsers })
  const { data: projects = [] } = useQuery({ queryKey: ['admin', 'projects'], queryFn: getProjects })

  const userOptions = users.map(u => ({ value: u.id, label: u.email }))
  const projectOptions = projects.map(p => ({ value: p.id, label: `${p.name} (${p.userEmail})` }))

  const statusOptions = [
    { value: 'OPEN', label: t('statusOpen') },
    { value: 'COMPLETED', label: t('statusCompleted') },
    { value: 'CANCELLED', label: t('statusCancelled') },
  ]

  const energyOptions = [
    { value: '', label: t('none') },
    { value: 'LOW', label: t('priorityLow') },
    { value: 'MEDIUM', label: t('priorityMedium') },
    { value: 'HIGH', label: t('priorityHigh') },
  ]

  const columns = [
    { key: 'userEmail', label: t('user') },
    { key: 'projectName', label: t('project') },
    { key: 'title', label: t('title') },
    { key: 'status', label: t('status') },
    { key: 'priority', label: t('priority') },
    { key: 'dueDate', label: t('due') },
  ]

  const formFields = [
    { name: 'userId', label: t('user'), type: 'select', options: userOptions, required: true },
    { name: 'projectId', label: t('project'), type: 'select', options: projectOptions, required: true },
    { name: 'title', label: t('title'), required: true },
    { name: 'description', label: t('description'), type: 'textarea' },
    { name: 'status', label: t('status'), type: 'select', options: statusOptions, defaultValue: 'OPEN' },
    { name: 'priority', label: t('priority'), type: 'number', defaultValue: 3 },
    { name: 'pointsEstimate', label: t('pointsEstimate'), type: 'number' },
    { name: 'energyLevel', label: t('energyLevel'), type: 'select', options: energyOptions },
    { name: 'dueDate', label: t('dueDate'), type: 'date' },
    { name: 'sortOrder', label: t('sortOrder'), type: 'number', defaultValue: 0 },
  ]

  const crud = useAdminCrud({
    queryKey: ['admin', 'tasks'],
    listFn: getTasks, createFn: createTask, updateFn: updateTask, deleteFn: deleteTask,
  })

  return <AdminCrudPage title={t('tasks')} entityName={t('task')} columns={columns} fields={formFields} crud={crud} />
}
