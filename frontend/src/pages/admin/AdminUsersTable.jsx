import { useState } from 'react'
import { getUsers, createUser, updateUser, deleteUser, getUserDependents } from '@/api/admin'
import { useAdminCrud } from './hooks/useAdminCrud'
import { AdminCrudPage } from './components/AdminCrudPage'

const COLUMNS = [
  { key: 'email', label: 'Email' },
  { key: 'displayName', label: 'Display Name' },
  { key: 'timezone', label: 'Timezone' },
  { key: 'createdAt', label: 'Created' },
]

const FORM_FIELDS = [
  { name: 'email', label: 'Email', type: 'email', required: true },
  { name: 'password', label: 'Password', type: 'password', required: false, placeholder: 'Leave blank to keep current' },
  { name: 'displayName', label: 'Display Name', required: true },
  { name: 'timezone', label: 'Timezone', defaultValue: 'UTC' },
]

export default function AdminUsersTable() {
  const [dependents, setDependents] = useState(null)

  const crud = useAdminCrud({
    queryKey: ['admin', 'users'],
    listFn: getUsers, createFn: createUser, updateFn: updateUser, deleteFn: deleteUser,
  })

  const handleDelete = async (row) => {
    const counts = await getUserDependents(row.id)
    setDependents(counts)
    crud.setDeleteItem(row)
  }

  // Password is required only when creating a new user
  const formFields = crud.editItem
    ? FORM_FIELDS
    : FORM_FIELDS.map(f => f.name === 'password' ? { ...f, required: true } : f)

  return (
    <AdminCrudPage
      title="Users"
      entityName="User"
      columns={COLUMNS}
      fields={formFields}
      crud={{ ...crud, openDelete: handleDelete }}
      dependentCounts={dependents}
    />
  )
}
