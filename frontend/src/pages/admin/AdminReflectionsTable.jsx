import { useQuery } from '@tanstack/react-query'
import { getReflections, createReflection, updateReflection, deleteReflection, getUsers } from '@/api/admin'
import { useAdminCrud } from './hooks/useAdminCrud'
import { AdminCrudPage } from './components/AdminCrudPage'

const COLUMNS = [
  { key: 'userEmail', label: 'User' },
  { key: 'reflectionDate', label: 'Date' },
  { key: 'energyRating', label: 'Energy' },
  { key: 'moodRating', label: 'Mood' },
  { key: 'reflectionNotes', label: 'Notes' },
  { key: 'isFinalized', label: 'Finalized' },
]

export default function AdminReflectionsTable() {
  const { data: users = [] } = useQuery({ queryKey: ['admin', 'users'], queryFn: getUsers })

  const userOptions = users.map(u => ({ value: u.id, label: u.email }))
  const formFields = [
    { name: 'userId', label: 'User', type: 'select', options: userOptions, required: true },
    { name: 'reflectionDate', label: 'Date', type: 'date', required: true },
    { name: 'energyRating', label: 'Energy Rating (1-5)', type: 'number', required: true },
    { name: 'moodRating', label: 'Mood Rating (1-5)', type: 'number', required: true },
    { name: 'reflectionNotes', label: 'Notes', type: 'textarea' },
    { name: 'isFinalized', label: 'Finalized', type: 'checkbox' },
  ]

  const crud = useAdminCrud({
    queryKey: ['admin', 'reflections'],
    listFn: getReflections, createFn: createReflection, updateFn: updateReflection, deleteFn: deleteReflection,
  })

  return <AdminCrudPage title="Reflections" entityName="Reflection" columns={COLUMNS} fields={formFields} crud={crud} />
}
