import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, useCallback, useEffect } from 'react'
import {
  getScheduleToday,
  startTimeBlock,
  completeTimeBlock,
  doneForNowTimeBlock,
  extendTimeBlock,
} from '@/api/schedule'
import { getTask } from '@/api/tasks'
import { useActiveSession } from '@/contexts/ActiveSessionContext'
import TimerCircle from './active-session/TimerCircle'
import SubtaskChecklist from './active-session/SubtaskChecklist'
import { playCompletionChime } from './active-session/chime'

const TODAY = new Date().toISOString().slice(0, 10)

export default function ActiveSessionPage() {
  const { blockId } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { startSession, clearSession } = useActiveSession()
  const [showExtendMenu, setShowExtendMenu] = useState(false)
  const [flash, setFlash] = useState(null)
  const [error, setError] = useState(null)

  // Fetch today's schedule and find this block
  const { data: blocks } = useQuery({
    queryKey: ['schedule', TODAY],
    queryFn: getScheduleToday,
  })
  const block = blocks?.find((b) => b.id === blockId)

  // Fetch task details (for child tasks) if block has a task
  const { data: taskDetail } = useQuery({
    queryKey: ['tasks', block?.task?.id],
    queryFn: () => getTask(block.task.id),
    enabled: !!block?.task?.id,
  })

  // Calculate timer values from block times
  const endTime = block
    ? new Date(`${block.blockDate}T${block.endTime}`).getTime()
    : null
  const startTime = block
    ? new Date(`${block.blockDate}T${block.startTime}`).getTime()
    : null
  const totalMinutes = block
    ? Math.round((endTime - startTime) / 60000)
    : 0

  // Start the block on mount
  const startMutation = useMutation({
    mutationFn: () => startTimeBlock(blockId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schedule'] })
    },
    onError: (err) => {
      // 409/422 "already started" is expected on revisit — ignore it
      if (err.status === 409 || err.status === 422) return
      console.error('Failed to start block:', err)
      setError(`Failed to start session: ${err.message}`)
    },
  })

  useEffect(() => {
    if (block && !block.actualStart) {
      startMutation.mutate()
    }
  }, [block?.id]) // eslint-disable-line react-hooks/exhaustive-deps

  // Set session context for header timer
  useEffect(() => {
    if (block?.task && endTime) {
      startSession(blockId, block.task.title, endTime)
    }
  }, [block?.id, endTime]) // eslint-disable-line react-hooks/exhaustive-deps

  // Complete mutation
  const completeMutation = useMutation({
    mutationFn: () => completeTimeBlock(blockId),
    onSuccess: () => {
      clearSession()
      queryClient.invalidateQueries({ queryKey: ['schedule'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      setFlash('Nice work!')
      setTimeout(() => navigate(-1), 1500)
    },
    onError: (err) => {
      console.error('Failed to complete block:', err)
      setError(`Failed to complete: ${err.message}`)
    },
  })

  // Done for now mutation
  const doneForNowMutation = useMutation({
    mutationFn: () => doneForNowTimeBlock(blockId),
    onSuccess: () => {
      clearSession()
      queryClient.invalidateQueries({ queryKey: ['schedule'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      navigate(-1)
    },
    onError: (err) => {
      console.error('Failed (done for now):', err)
      setError(`Something went wrong: ${err.message}`)
    },
  })

  // Extend mutation
  const extendMutation = useMutation({
    mutationFn: (duration) => extendTimeBlock(blockId, duration),
    onSuccess: (newBlock) => {
      clearSession()
      queryClient.invalidateQueries({ queryKey: ['schedule'] })
      setShowExtendMenu(false)
      navigate(`/session/${newBlock.id}`, { replace: true })
    },
    onError: (err) => {
      console.error('Failed to extend:', err)
      setError(`Failed to extend: ${err.message}`)
      setShowExtendMenu(false)
    },
  })

  const handleTimeUp = useCallback(() => {
    playCompletionChime()
  }, [])

  if (!block) {
    return (
      <div className="flex items-center justify-center h-full bg-gray-50">
        <p className="text-gray-400">Loading session...</p>
      </div>
    )
  }

  const children = taskDetail?.children ?? []
  const isPending =
    completeMutation.isPending ||
    doneForNowMutation.isPending ||
    extendMutation.isPending

  return (
    <div className="flex flex-col items-center justify-center h-full bg-gradient-to-b from-gray-50 to-indigo-50/30 px-4">
      {/* Flash message */}
      {flash && (
        <div className="fixed top-8 left-1/2 -translate-x-1/2 bg-white shadow-lg rounded-xl px-6 py-3 text-indigo-600 font-medium z-50">
          {flash}
        </div>
      )}

      {/* Error message */}
      {error && (
        <div className="fixed top-8 left-1/2 -translate-x-1/2 bg-red-50 border border-red-200 shadow-lg rounded-xl px-6 py-3 text-red-700 text-sm z-50 max-w-md text-center">
          <p>{error}</p>
          <button
            onClick={() => setError(null)}
            className="mt-2 text-xs text-red-500 hover:text-red-700 underline"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Project name */}
      {block.task && (
        <p className="text-xs font-semibold text-gray-400 uppercase tracking-widest mb-2">
          {block.task.projectName}
        </p>
      )}

      {/* Task title */}
      <h1 className="text-xl font-semibold text-gray-800 mb-8 text-center">
        {block.task?.title ?? 'Focus time'}
      </h1>

      {/* Timer */}
      {endTime && (
        <TimerCircle
          endTime={endTime}
          totalMinutes={totalMinutes}
          onTimeUp={handleTimeUp}
        />
      )}

      {/* Subtasks — only rendered if children exist */}
      {children.length > 0 && (
        <div className="mt-8">
          <SubtaskChecklist
            subtasks={children}
            projectId={block.task?.projectId}
          />
        </div>
      )}

      {/* Action buttons */}
      <div className="flex gap-3 mt-8 relative">
        <button
          onClick={() => completeMutation.mutate()}
          disabled={isPending}
          className="px-5 py-2.5 bg-indigo-500 text-white rounded-xl text-sm font-medium hover:bg-indigo-600 transition-colors disabled:opacity-50"
        >
          Complete
        </button>

        <div className="relative">
          <button
            onClick={() => setShowExtendMenu(!showExtendMenu)}
            disabled={isPending}
            className="px-5 py-2.5 bg-indigo-50 text-indigo-600 rounded-xl text-sm font-medium hover:bg-indigo-100 transition-colors disabled:opacity-50"
          >
            Extend
          </button>
          {showExtendMenu && (
            <div className="absolute bottom-full mb-2 left-1/2 -translate-x-1/2 bg-white shadow-lg rounded-xl border border-gray-200 py-1 min-w-[120px]">
              {[15, 30, 60].map((mins) => (
                <button
                  key={mins}
                  onClick={() => extendMutation.mutate(mins)}
                  className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-indigo-50 transition-colors"
                >
                  {mins} min
                </button>
              ))}
            </div>
          )}
        </div>

        <button
          onClick={() => doneForNowMutation.mutate()}
          disabled={isPending}
          className="px-5 py-2.5 bg-indigo-50 text-indigo-600 rounded-xl text-sm font-medium hover:bg-indigo-100 transition-colors disabled:opacity-50"
        >
          Done for now
        </button>
      </div>
    </div>
  )
}
