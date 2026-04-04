import { useQuery } from '@tanstack/react-query'
import { getDeferredItems, createDeferredItem, updateDeferredItem, deleteDeferredItem, getUsers } from '@/api/admin'
import { useAdminCrud } from './hooks/useAdminCrud'
import { AdminCrudPage } from './components/AdminCrudPage'

const COLUMNS = [
  { key: 'userEmail', label: 'User' },
  { key: 'rawText', label: 'Text' },
  { key: 'isProcessed', label: 'Processed' },
  { key: 'deferralCount', label: 'Deferrals' },
  { key: 'deferredUntilDate', label: 'Deferred Until' },
  { key: 'capturedAt', label: 'Captured' },
]

export default function AdminDeferredTable() {
  const { data: users = [] } = useQuery({ queryKey: ['admin', 'users'], queryFn: getUsers })

  const userOptions = users.map(u => ({ value: u.id, label: u.email }))
  const formFields = [
    { name: 'userId', label: 'User', type: 'select', options: userOptions, required: true },
    { name: 'rawText', label: 'Text', type: 'textarea', required: true },
    { name: 'isProcessed', label: 'Processed', type: 'checkbox' },
    { name: 'deferredUntilDate', label: 'Deferred Until', type: 'date' },
    { name: 'deferralCount', label: 'Deferral Count', type: 'number', defaultValue: 0 },
  ]

  const crud = useAdminCrud({
    queryKey: ['admin', 'deferred-items'],
    listFn: getDeferredItems, createFn: createDeferredItem, updateFn: updateDeferredItem, deleteFn: deleteDeferredItem,
  })

  return <AdminCrudPage title="Deferred Items" entityName="Deferred Item" columns={COLUMNS} fields={formFields} crud={crud} />
}
