import { useState } from 'react'
import * as Dialog from '@radix-ui/react-dialog'

function buildDefaults(fields, initialValues) {
  const defaults = {}
  fields.forEach(f => {
    defaults[f.name] = initialValues?.[f.name] ?? f.defaultValue ?? ''
  })
  return defaults
}

function FormContent({ fields, initialValues, onSubmit, isPending }) {
  const [values, setValues] = useState(() => buildDefaults(fields, initialValues))

  const handleChange = (name, value) => {
    setValues(prev => ({ ...prev, [name]: value }))
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    const cleaned = {}
    fields.forEach(f => {
      const v = values[f.name]
      if (v === '' && !f.required) {
        cleaned[f.name] = null
      } else if (f.type === 'number' && v !== '' && v != null) {
        cleaned[f.name] = Number(v)
      } else if (f.type === 'checkbox') {
        cleaned[f.name] = Boolean(v)
      } else {
        cleaned[f.name] = v
      }
    })
    onSubmit(cleaned)
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
              <option value="">&mdash; Select &mdash;</option>
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
      <div className="flex justify-end gap-2 pt-2">
        <Dialog.Close asChild>
          <button type="button" className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">
            Cancel
          </button>
        </Dialog.Close>
        <button
          type="submit"
          disabled={isPending}
          className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
        >
          {isPending ? 'Saving...' : 'Save'}
        </button>
      </div>
    </form>
  )
}

export function AdminFormModal({ open, onOpenChange, title, fields, initialValues, onSubmit, isPending }) {
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
            />
          )}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
