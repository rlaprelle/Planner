import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { getTimeBlocks, createTimeBlock, updateTimeBlock, deleteTimeBlock, getUsers, getTasks } from '@/api/admin'
import { useAdminCrud } from './hooks/useAdminCrud'
import { AdminCrudPage } from './components/AdminCrudPage'

export default function AdminTimeBlocksTable() {
  const { t } = useTranslation('admin')
  const { data: users = [] } = useQuery({ queryKey: ['admin', 'users'], queryFn: getUsers })
  const { data: tasks = [] } = useQuery({ queryKey: ['admin', 'tasks'], queryFn: getTasks })

  const userOptions = users.map(u => ({ value: u.id, label: u.email }))
  const taskOptions = [{ value: '', label: t('none') }, ...tasks.map(tk => ({ value: tk.id, label: `${tk.title} (${tk.userEmail})` }))]

  const columns = [
    { key: 'userEmail', label: t('user') },
    { key: 'blockDate', label: t('date') },
    { key: 'taskTitle', label: t('task') },
    { key: 'startTime', label: t('start') },
    { key: 'endTime', label: t('end') },
    { key: 'wasCompleted', label: t('completed') },
  ]

  const formFields = [
    { name: 'userId', label: t('user'), type: 'select', options: userOptions, required: true },
    { name: 'blockDate', label: t('date'), type: 'date', required: true },
    { name: 'taskId', label: t('task'), type: 'select', options: taskOptions },
    { name: 'startTime', label: t('startTime'), type: 'time', required: true },
    { name: 'endTime', label: t('endTime'), type: 'time', required: true },
    { name: 'sortOrder', label: t('sortOrder'), type: 'number', defaultValue: 0 },
    { name: 'wasCompleted', label: t('completed'), type: 'checkbox' },
  ]

  const crud = useAdminCrud({
    queryKey: ['admin', 'time-blocks'],
    listFn: getTimeBlocks, createFn: createTimeBlock, updateFn: updateTimeBlock, deleteFn: deleteTimeBlock,
  })

  return <AdminCrudPage title={t('timeBlocks')} entityName={t('timeBlock')} columns={columns} fields={formFields} crud={crud} />
}
