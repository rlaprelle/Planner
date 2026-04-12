import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { getDeferredItems, createDeferredItem, updateDeferredItem, deleteDeferredItem, getUsers } from '@/api/admin'
import { useAdminCrud } from './hooks/useAdminCrud'
import { AdminCrudPage } from './components/AdminCrudPage'

export default function AdminDeferredTable() {
  const { t } = useTranslation('admin')
  const { data: users = [] } = useQuery({ queryKey: ['admin', 'users'], queryFn: getUsers })

  const userOptions = users.map(u => ({ value: u.id, label: u.email }))

  const columns = [
    { key: 'userEmail', label: t('user') },
    { key: 'rawText', label: t('text') },
    { key: 'isProcessed', label: t('processed') },
    { key: 'deferralCount', label: t('deferrals') },
    { key: 'deferredUntilDate', label: t('deferredUntil') },
    { key: 'capturedAt', label: t('captured') },
  ]

  const formFields = [
    { name: 'userId', label: t('user'), type: 'select', options: userOptions, required: true },
    { name: 'rawText', label: t('text'), type: 'textarea', required: true },
    { name: 'isProcessed', label: t('processed'), type: 'checkbox' },
    { name: 'deferredUntilDate', label: t('deferredUntil'), type: 'date' },
    { name: 'deferralCount', label: t('deferralCount'), type: 'number', defaultValue: 0 },
  ]

  const crud = useAdminCrud({
    queryKey: ['admin', 'deferred-items'],
    listFn: getDeferredItems, createFn: createDeferredItem, updateFn: updateDeferredItem, deleteFn: deleteDeferredItem,
  })

  return <AdminCrudPage title={t('deferredItems')} entityName={t('deferredItem')} columns={columns} fields={formFields} crud={crud} />
}
