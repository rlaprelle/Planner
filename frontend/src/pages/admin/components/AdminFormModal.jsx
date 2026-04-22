import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import * as Dialog from '@radix-ui/react-dialog'

function buildDefaults(fields, initialValues) {
  const defaults = {}
  fields.forEach(f => {
    defaults[f.name] = initialValues?.[f.name] ?? f.defaultValue ?? ''
  })
  return defaults
}

function cleanFieldValue(field, rawValue) {
  if (rawValue === '' && !field.required) return null
  if (field.type === 'number' && rawValue !== '' && rawValue != null) return Number(rawValue)
  if (field.type === 'checkbox') return Boolean(rawValue)
  return rawValue
}

function cleanFormValues(fields, values) {
  const cleaned = {}
  fields.forEach(f => {
    cleaned[f.name] = cleanFieldValue(f, values[f.name])
  })
  return cleaned
}

function FormContent({ fields, initialValues, onSubmit, isPending, saveError }) {
  const { t } = useTranslation('admin')
  const [values, setValues] = useState(() => buildDefaults(fields, initialValues))

  const handleChange = (name, value) => {
    setValues(prev => ({ ...prev, [name]: value }))
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    onSubmit(cleanFormValues(fields, values))
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      {fields.map(f => (
        <div key={f.name}>
          <label className="block text-xs font-medium text-gray-600 mb-1">
            {f.label}{f.required && <span className="text-red-500 ml-0.5">*</span>}
          </label>
          {f.type === 'select' ? (
            <select
              value={values[f.name] ?? ''}
              onChange={e => handleChange(f.name, e.target.value)}
              required={f.required}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">{t('selectDefault')}</option>
              {f.options?.map(opt => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
          ) : f.type === 'textarea' ? (
            <textarea
              value={values[f.name] ?? ''}
              onChange={e => handleChange(f.name, e.target.value)}
              required={f.required}
              rows={3}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          ) : f.type === 'checkbox' ? (
            <input
              type="checkbox"
              checked={!!values[f.name]}
              onChange={e => handleChange(f.name, e.target.checked)}
              className="rounded border-gray-300"
            />
          ) : (
            <input
              type={f.type || 'text'}
              value={values[f.name] ?? ''}
              onChange={e => handleChange(f.name, e.target.value)}
              required={f.required}
              placeholder={f.placeholder}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          )}
        </div>
      ))}
      {saveError && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          {saveError.message}
        </div>
      )}
      <div className="flex justify-end gap-2 pt-2">
        <Dialog.Close asChild>
          <button type="button" className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">
            {t('common:cancel')}
          </button>
        </Dialog.Close>
        <button
          type="submit"
          disabled={isPending}
          className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
        >
          {isPending ? t('common:saving') : t('common:save')}
        </button>
      </div>
    </form>
  )
}

export function AdminFormModal({ open, onOpenChange, title, fields, initialValues, onSubmit, isPending, saveError }) {
  // Use a key to remount FormContent when the modal opens or initialValues change,
  // so useState initializer re-runs with fresh defaults
  const formKey = open ? `${initialValues?.id ?? 'new'}-${open}` : 'closed'

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/40 z-40" />
        <Dialog.Content className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-50 bg-white rounded-xl shadow-xl p-6 w-full max-w-lg max-h-[85vh] overflow-y-auto">
          <Dialog.Title className="text-lg font-semibold text-gray-900 mb-4">{title}</Dialog.Title>
          {open && (
            <FormContent
              key={formKey}
              fields={fields}
              initialValues={initialValues}
              onSubmit={onSubmit}
              isPending={isPending}
              saveError={saveError}
            />
          )}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
