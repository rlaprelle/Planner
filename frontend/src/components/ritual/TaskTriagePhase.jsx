import { useState, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getActiveTasks, deferTask, cancelTask, updateTask } from '@/api/tasks'

function TaskTriageInner({ initialTasks, onPhaseComplete }) {
  const queryClient = useQueryClient()
  const [currentIndex, setCurrentIndex] = useState(0)
  const [tasks, setTasks] = useState(initialTasks)
  const [editingDeadline, setEditingDeadline] = useState(false)
  const [newDeadline, setNewDeadline] = useState('')

  const deferMutation = useMutation({
    mutationFn: ({ taskId, target }) => deferTask(taskId, target),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      advance()
    },
  })

  const cancelMutation = useMutation({
    mutationFn: (taskId) => cancelTask(taskId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      advance()
    },
  })

  const deadlineMutation = useMutation({
    mutationFn: ({ taskId, dueDate }) => {
      const task = tasks[currentIndex]
      return updateTask(taskId, { ...task, dueDate: dueDate || null })
    },
    onSuccess: (updated) => {
      setTasks(prev => prev.map(t => t.id === updated.id ? updated : t))
      setEditingDeadline(false)
      setNewDeadline('')
    },
  })

  function advance() {
    const nextIndex = currentIndex + 1
    if (nextIndex >= tasks.length) {
      onPhaseComplete()
    } else {
      setCurrentIndex(nextIndex)
      setEditingDeadline(false)
      setNewDeadline('')
    }
  }

  function handleSkip() {
    advance()
  }

  function handleDeferAll() {
    const remaining = tasks.slice(currentIndex)
    Promise.all(remaining.map(t => deferTask(t.id, 'TOMORROW'))).then(() => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      onPhaseComplete()
    })
  }

  if (tasks.length === 0) {
    return null
  }

  const currentTask = tasks[currentIndex]
  if (!currentTask) return null

  const dueDate = currentTask.dueDate
  const isPending = deferMutation.isPending || cancelMutation.isPending

  function canDefer(target) {
    if (!dueDate) return true
    const today = new Date()
    let targetDate
    switch (target) {
      case 'TOMORROW':
        targetDate = new Date(today)
        targetDate.setDate(targetDate.getDate() + 1)
        break
      case 'NEXT_WEEK': {
        const day = today.getDay()
        const daysUntilMonday = day === 0 ? 1 : 8 - day
        targetDate = new Date(today)
        targetDate.setDate(targetDate.getDate() + daysUntilMonday)
        break
      }
      case 'NEXT_MONTH':
        targetDate = new Date(today.getFullYear(), today.getMonth() + 1, 1)
        break
      default:
        return true
    }
    return targetDate <= new Date(dueDate + 'T23:59:59')
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-semibold text-ink-heading">Review your tasks</h2>
        <span className="text-sm text-ink-muted">
          {currentIndex + 1} of {tasks.length}
        </span>
      </div>

      <div className="w-full bg-surface-soft rounded-full h-1.5 mb-6">
        <div
          className="bg-primary-400 h-1.5 rounded-full transition-all duration-300"
          style={{ width: `${((currentIndex) / tasks.length) * 100}%` }}
        />
      </div>

      <div className="bg-surface-raised border border-edge rounded-xl p-6 shadow-card mb-6">
        <div className="flex items-start gap-3 mb-3">
          <div
            className="w-3 h-3 rounded-full mt-1 flex-shrink-0"
            style={{ backgroundColor: currentTask.projectColor || '#6366f1' }}
          />
          <div className="flex-1 min-w-0">
            <h3 className="text-lg font-medium text-ink-heading">{currentTask.title}</h3>
            <p className="text-sm text-ink-muted mt-0.5">{currentTask.projectName}</p>
          </div>
        </div>

        {dueDate && (
          <div className="flex items-center gap-2 mb-3">
            <span className="text-xs font-medium text-ink-muted">Due:</span>
            <span className="text-xs font-medium text-orange-600 bg-orange-50 px-2 py-0.5 rounded">
              {dueDate}
            </span>
          </div>
        )}

        {currentTask.deferralCount > 0 && (
          <p className="text-xs text-ink-muted mb-3">
            {currentTask.deferralCount >= 3
              ? `Deferred ${currentTask.deferralCount} times — is this still on your radar?`
              : `Deferred ${currentTask.deferralCount} time${currentTask.deferralCount > 1 ? 's' : ''}`}
          </p>
        )}

        {currentTask.pointsEstimate && (
          <span className="inline-block text-xs bg-primary-50 text-primary-600 px-2 py-0.5 rounded font-medium">
            {currentTask.pointsEstimate} pt{currentTask.pointsEstimate > 1 ? 's' : ''}
          </span>
        )}
      </div>

      {editingDeadline && (
        <div className="bg-surface-raised border border-edge rounded-lg p-4 mb-4">
          <p className="text-sm font-medium text-ink-body mb-2">Change deadline</p>
          <div className="flex gap-2">
            <input
              type="date"
              value={newDeadline}
              onChange={(e) => setNewDeadline(e.target.value)}
              className="flex-1 px-2 py-1.5 text-sm border border-edge-subtle rounded-md focus:outline-none focus:ring-2 focus:ring-edge-focus"
            />
            <button
              onClick={() => deadlineMutation.mutate({ taskId: currentTask.id, dueDate: newDeadline })}
              disabled={deadlineMutation.isPending}
              className="px-3 py-1.5 text-sm rounded-md bg-primary-500 text-white hover:bg-primary-600 disabled:opacity-50 transition-colors"
            >
              Save
            </button>
            <button
              onClick={() => { setEditingDeadline(false); setNewDeadline('') }}
              className="px-3 py-1.5 text-sm rounded-md text-ink-muted hover:text-ink-body transition-colors"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      <div className="space-y-3">
        <div className="grid grid-cols-3 gap-3">
          <button
            onClick={() => deferMutation.mutate({ taskId: currentTask.id, target: 'TOMORROW' })}
            disabled={isPending || !canDefer('TOMORROW')}
            className="py-3 text-sm rounded-lg border border-edge hover:bg-surface-soft disabled:opacity-40 disabled:cursor-not-allowed transition-colors font-medium text-ink-body"
          >
            Tomorrow
          </button>
          <button
            onClick={() => deferMutation.mutate({ taskId: currentTask.id, target: 'NEXT_WEEK' })}
            disabled={isPending || !canDefer('NEXT_WEEK')}
            className="py-3 text-sm rounded-lg border border-edge hover:bg-surface-soft disabled:opacity-40 disabled:cursor-not-allowed transition-colors font-medium text-ink-body"
          >
            Next week
          </button>
          <button
            onClick={() => deferMutation.mutate({ taskId: currentTask.id, target: 'NEXT_MONTH' })}
            disabled={isPending || !canDefer('NEXT_MONTH')}
            className="py-3 text-sm rounded-lg border border-edge hover:bg-surface-soft disabled:opacity-40 disabled:cursor-not-allowed transition-colors font-medium text-ink-body"
          >
            Next month
          </button>
        </div>

        <div className="flex gap-3">
          <button
            onClick={handleSkip}
            disabled={isPending}
            className="flex-1 py-2.5 text-sm rounded-lg bg-primary-50 text-primary-600 hover:bg-primary-100 disabled:opacity-50 transition-colors font-medium"
          >
            Keep for tomorrow
          </button>
          <button
            onClick={() => cancelMutation.mutate(currentTask.id)}
            disabled={isPending}
            className="py-2.5 px-4 text-sm rounded-lg text-red-500 hover:bg-red-50 disabled:opacity-50 transition-colors"
          >
            Cancel task
          </button>
        </div>

        {dueDate && (
          <button
            onClick={() => { setEditingDeadline(true); setNewDeadline(dueDate) }}
            className="text-xs text-ink-muted hover:text-ink-secondary transition-colors underline"
          >
            Change deadline
          </button>
        )}
      </div>

      {tasks.length - currentIndex > 2 && (
        <div className="mt-8 pt-4 border-t border-edge-subtle">
          <button
            onClick={handleDeferAll}
            className="text-xs text-ink-muted hover:text-ink-secondary transition-colors"
          >
            Defer all {tasks.length - currentIndex} remaining to tomorrow
          </button>
        </div>
      )}

      {(deferMutation.isError || cancelMutation.isError) && (
        <p className="mt-4 text-sm text-error">Something went wrong. Try again.</p>
      )}
    </div>
  )
}

export function TaskTriagePhase({ onPhaseComplete }) {
  const { data, isLoading } = useQuery({
    queryKey: ['tasks', 'active'],
    queryFn: getActiveTasks,
  })

  useEffect(() => {
    if (data && data.length === 0) {
      onPhaseComplete()
    }
  }, [data, onPhaseComplete])

  if (isLoading || !data) {
    return <div className="p-8 text-ink-muted text-sm">Loading tasks…</div>
  }

  if (data.length === 0) return null

  return <TaskTriageInner initialTasks={data} onPhaseComplete={onPhaseComplete} />
}
