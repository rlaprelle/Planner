import { useState } from 'react'
import * as Dialog from '@radix-ui/react-dialog'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createTask } from '@/api/tasks'
import { PlusIcon } from './icons'

export function AddTaskModal({ open, onOpenChange, projectId, parentTaskId = null, parentTitle = null }) {
  const [title, setTitle] = useState('')
  const [dueDate, setDueDate] = useState('')
  const [priority, setPriority] = useState('')
  const [titleError, setTitleError] = useState('')

  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (data) => createTask(projectId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks', projectId] })
      onOpenChange(false)
      resetForm()
    },
  })

  function resetForm() {
    setTitle('')
    setDueDate('')
    setPriority('')
    setTitleError('')
    mutation.reset()
  }

  function handleOpenChange(val) {
    if (!val) resetForm()
    onOpenChange(val)
  }

  function handleSubmit(e) {
    e.preventDefault()
    if (mutation.isPending) return
    if (!title.trim()) {
      setTitleError('Title is required.')
      return
    }
    setTitleError('')
    const data = {
      title: title.trim(),
      ...(dueDate ? { dueDate } : {}),
      ...(priority ? { priority: Number(priority) } : {}),
      ...(parentTaskId ? { parentTaskId } : {}),
    }
    mutation.mutate(data)
  }

  const modalTitle = parentTitle ? `Add subtask to "${parentTitle}"` : 'Add task'

  return (
    <Dialog.Root open={open} onOpenChange={handleOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/40 z-40 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0" />
        <Dialog.Content className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-50 w-full max-w-md bg-white rounded-xl shadow-xl p-6 focus:outline-none data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95">
          <Dialog.Title className="text-lg font-semibold text-gray-900 mb-4">
            {modalTitle}
          </Dialog.Title>

          <form onSubmit={handleSubmit} noValidate className="space-y-4">
            {/* Title */}
            <div>
              <label htmlFor="task-title" className="block text-sm font-medium text-gray-700 mb-1">
                Title <span className="text-red-500">*</span>
              </label>
              <input
                id="task-title"
                type="text"
                value={title}
                onChange={(e) => { setTitle(e.target.value); setTitleError('') }}
                placeholder="What needs to be done?"
                className={[
                  'w-full px-3 py-2 text-sm border rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-colors',
                  titleError ? 'border-red-400' : 'border-gray-300',
                ].join(' ')}
                autoFocus
              />
              {titleError && <p className="mt-1 text-xs text-red-500">{titleError}</p>}
            </div>

            {/* Due date */}
            <div>
              <label htmlFor="task-due-date" className="block text-sm font-medium text-gray-700 mb-1">
                Due date <span className="text-gray-400 font-normal">(optional)</span>
              </label>
              <input
                id="task-due-date"
                type="date"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                className="w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-colors"
              />
            </div>

            {/* Priority */}
            <div>
              <label htmlFor="task-priority" className="block text-sm font-medium text-gray-700 mb-1">
                Priority <span className="text-gray-400 font-normal">(optional)</span>
              </label>
              <select
                id="task-priority"
                value={priority}
                onChange={(e) => setPriority(e.target.value)}
                className="w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-colors bg-white"
              >
                <option value="">— none —</option>
                <option value="1">1 — Lowest</option>
                <option value="2">2 — Low</option>
                <option value="3">3 — Medium</option>
                <option value="4">4 — High</option>
                <option value="5">5 — Highest</option>
              </select>
            </div>

            {mutation.isError && (
              <p className="text-sm text-red-500">
                {mutation.error?.message || 'Something went wrong. Please try again.'}
              </p>
            )}

            <div className="flex justify-end gap-3 pt-2">
              <Dialog.Close asChild>
                <button
                  type="button"
                  className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-1 transition-colors"
                >
                  Cancel
                </button>
              </Dialog.Close>
              <button
                type="submit"
                disabled={mutation.isPending}
                className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 rounded-md hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-1 disabled:opacity-60 disabled:cursor-not-allowed transition-colors"
              >
                {mutation.isPending ? 'Adding…' : 'Add task'}
              </button>
            </div>
          </form>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
