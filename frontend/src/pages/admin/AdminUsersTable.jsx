import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getUsers, createUser, updateUser, deleteUser, getUserDependents } from '@/api/admin'
import { useAdminCrud } from './hooks/useAdminCrud'
import { AdminCrudPage } from './components/AdminCrudPage'

function RoleBadge({ value }) {
  const { t } = useTranslation('admin')
  if (value === 'ADMIN') {
    return (
      <span className="inline-flex items-center rounded-full bg-primary-100 px-2 py-0.5 text-xs font-medium text-primary-700">
        {t('roleAdmin')}
      </span>
    )
  }
  return <span className="text-gray-700">{t('roleUser')}</span>
}

export default function AdminUsersTable() {
  const { t } = useTranslation('admin')
  const [dependents, setDependents] = useState(null)

  const columns = [
    { key: 'email', label: t('email') },
    { key: 'displayName', label: t('displayName') },
    { key: 'role', label: t('role'), render: (value) => <RoleBadge value={value} /> },
    { key: 'timezone', label: t('timezone') },
    { key: 'createdAt', label: t('created') },
  ]

  const formFields = [
    { name: 'email', label: t('email'), type: 'email', required: true },
    { name: 'password', label: t('password'), type: 'password', required: false, placeholder: t('passwordPlaceholder') },
    { name: 'displayName', label: t('displayName'), required: true },
    { name: 'timezone', label: t('timezone'), defaultValue: 'UTC' },
    {
      name: 'role',
      label: t('role'),
      type: 'select',
      required: true,
      defaultValue: 'USER',
      options: [
        { value: 'USER', label: t('roleUser') },
        { value: 'ADMIN', label: t('roleAdmin') },
      ],
      dynamicHint: (current, initial) => (initial && current && current !== initial ? t('roleChangeLagNote') : null),
    },
  ]

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
  const activeFormFields = crud.editItem
    ? formFields
    : formFields.map(f => f.name === 'password' ? { ...f, required: true } : f)

  return (
    <AdminCrudPage
      title={t('users')}
      entityName={t('user')}
      columns={columns}
      fields={activeFormFields}
      crud={{ ...crud, openDelete: handleDelete }}
      dependentCounts={dependents}
    />
  )
}
