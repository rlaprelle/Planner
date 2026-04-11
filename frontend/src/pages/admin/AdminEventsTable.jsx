import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { getUsers, getProjects, getEvents, createAdminEvent, updateAdminEvent, deleteAdminEvent } from '@/api/admin'
import { useAdminCrud } from './hooks/useAdminCrud'
import { AdminCrudPage } from './components/AdminCrudPage'

export default function AdminEventsTable() {
  const { t } = useTranslation('admin')
  const { data: users = [] } = useQuery({ queryKey: ['admin', 'users'], queryFn: getUsers })
  const { data: projects = [] } = useQuery({ queryKey: ['admin', 'projects'], queryFn: getProjects })

  const userOptions = users.map(u => ({ value: u.id, label: u.email }))
  const projectOptions = projects.map(p => ({ value: p.id, label: p.name }))

  const columns = [
    { key: 'userEmail', label: t('user') },
    { key: 'projectName', label: t('project') },
    { key: 'title', label: t('title') },
    { key: 'blockDate', label: t('date') },
    { key: 'startTime', label: t('start') },
    { key: 'endTime', label: t('end') },
    { key: 'energyLevel', label: t('energy') },
  ]

  const formFields = [
    { name: 'userId', label: t('user'), type: 'select', options: userOptions, required: true },
    { name: 'projectId', label: t('project'), type: 'select', options: projectOptions },
    { name: 'title', label: t('title'), type: 'text', required: true },
    { name: 'description', label: t('description'), type: 'textarea' },
    { name: 'blockDate', label: t('date'), type: 'date', required: true },
    { name: 'startTime', label: t('startTime'), type: 'time', required: true },
    { name: 'endTime', label: t('endTime'), type: 'time', required: true },
    { name: 'energyLevel', label: t('energyLevel'), type: 'select', options: [
      { value: '', label: t('energyNotSet') },
      { value: 'LOW', label: t('energyLow') },
      { value: 'MEDIUM', label: t('energyMedium') },
      { value: 'HIGH', label: t('energyHigh') },
    ]},
  ]

  const crud = useAdminCrud({
    queryKey: ['admin', 'events'],
    listFn: getEvents, createFn: createAdminEvent, updateFn: updateAdminEvent, deleteFn: deleteAdminEvent,
  })

  return <AdminCrudPage title={t('events')} entityName={t('event')} columns={columns} fields={formFields} crud={crud} />
}
