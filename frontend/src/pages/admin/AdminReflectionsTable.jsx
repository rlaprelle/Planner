import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { getReflections, createReflection, updateReflection, deleteReflection, getUsers } from '@/api/admin'
import { useAdminCrud } from './hooks/useAdminCrud'
import { AdminCrudPage } from './components/AdminCrudPage'

export default function AdminReflectionsTable() {
  const { t } = useTranslation('admin')
  const { data: users = [] } = useQuery({ queryKey: ['admin', 'users'], queryFn: getUsers })

  const userOptions = users.map(u => ({ value: u.id, label: u.email }))

  const columns = [
    { key: 'userEmail', label: t('user') },
    { key: 'reflectionDate', label: t('date') },
    { key: 'energyRating', label: t('energy') },
    { key: 'moodRating', label: t('mood') },
    { key: 'reflectionNotes', label: t('notes') },
    { key: 'isFinalized', label: t('finalized') },
  ]

  const formFields = [
    { name: 'userId', label: t('user'), type: 'select', options: userOptions, required: true },
    { name: 'reflectionDate', label: t('date'), type: 'date', required: true },
    { name: 'energyRating', label: t('energyRating'), type: 'number', required: true },
    { name: 'moodRating', label: t('moodRating'), type: 'number', required: true },
    { name: 'reflectionNotes', label: t('notes'), type: 'textarea' },
    { name: 'isFinalized', label: t('finalized'), type: 'checkbox' },
  ]

  const crud = useAdminCrud({
    queryKey: ['admin', 'reflections'],
    listFn: getReflections, createFn: createReflection, updateFn: updateReflection, deleteFn: deleteReflection,
  })

  return <AdminCrudPage title={t('reflections')} entityName={t('reflection')} columns={columns} fields={formFields} crud={crud} />
}
