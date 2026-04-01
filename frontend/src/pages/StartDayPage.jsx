import { useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { DndContext, DragOverlay, PointerSensor, useSensor, useSensors } from '@dnd-kit/core'
import { format } from 'date-fns'

import { getSuggestedTasks, getScheduleToday, savePlan } from '@/api/schedule'
import { useTimeGrid, DAY_START_MINUTES, DAY_END_MINUTES } from './start-day/useTimeGrid'
import { pushBlocks, toGridBlock, snapTo15, minutesToTime } from './start-day/pushBlocks'
import { TimeBlockGrid } from './start-day/TimeBlockGrid'
import { TaskBrowserRow } from './start-day/TaskBrowserRow'

const TODAY = format(new Date(), 'yyyy-MM-dd')

export function StartDayPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  // --- Grid helpers ---
  const { gridRef, minutesToPercent, durationToPercent, pixelDeltaToMinutes, clientXToMinutes, startResize } =
    useTimeGrid()

  // --- Server data ---
  const { data: suggestedTasks = [], isLoading: loadingTasks } = useQuery({
    queryKey: ['tasks', 'suggested', TODAY],
    queryFn: () => getSuggestedTasks(TODAY),
  })

  const { data: existingBlocks = [] } = useQuery({
    queryKey: ['schedule', TODAY],
    queryFn: getScheduleToday,
    select: (data) => data.map(toGridBlock),
  })

  // --- Local state ---
  const [blocks, setBlocks] = useState(null) // null = not yet initialised from server
  const [selectedTaskIds, setSelectedTaskIds] = useState(new Set())
  const [addWarning, setAddWarning] = useState(null)
  const [activeTaskCard, setActiveTaskCard] = useState(null) // for DragOverlay

  // Initialise blocks from server once (mid-day replanning support)
  const gridBlocks = blocks ?? existingBlocks

  const scheduledTaskIds = useMemo(
    () => new Set(gridBlocks.map((b) => b.task?.id).filter(Boolean)),
    [gridBlocks]
  )

  // --- Filtered task lists for deadline rows ---
  const dueTodayTasks = useMemo(
    () => suggestedTasks.filter((t) => t.deadlineGroup === 'TODAY'),
    [suggestedTasks]
  )
  const dueThisWeekTasks = useMemo(
    () => suggestedTasks.filter((t) => t.deadlineGroup === 'THIS_WEEK'),
    [suggestedTasks]
  )

  // --- Selection ---
  function toggleTask(taskId) {
    setSelectedTaskIds((prev) => {
      const next = new Set(prev)
      if (next.has(taskId)) next.delete(taskId)
      else next.add(taskId)
      return next
    })
  }

  // --- Add selected tasks to calendar ---
  function handleAddToCalendar() {
    setAddWarning(null)
    const toAdd = suggestedTasks.filter(
      (t) => selectedTaskIds.has(t.id) && !scheduledTaskIds.has(t.id)
    )
    const lastEnd =
      gridBlocks.length > 0 ? Math.max(...gridBlocks.map((b) => b.endMinutes)) : DAY_START_MINUTES

    let currentStart = lastEnd
    const newBlocks = []
    for (const task of toAdd) {
      if (currentStart + 60 > DAY_END_MINUTES) break
      newBlocks.push(makeBlock(task, currentStart, currentStart + 60, gridBlocks.length + newBlocks.length))
      currentStart += 60
    }

    const skipped = toAdd.length - newBlocks.length
    setBlocks([...gridBlocks, ...newBlocks])
    setSelectedTaskIds(new Set())
    if (skipped > 0) {
      setAddWarning(`${skipped} task${skipped > 1 ? 's' : ''} didn't fit — you can resize blocks to make room`)
    }
  }

  function handleRemoveBlock(blockId) {
    setBlocks(gridBlocks.filter((b) => b.id !== blockId))
  }

  function makeBlock(task, startMinutes, endMinutes, sortOrder) {
    return {
      id: `temp-${task.id}-${Date.now()}`,
      blockDate: TODAY,
      startMinutes,
      endMinutes,
      startTime: minutesToTime(startMinutes),
      endTime: minutesToTime(endMinutes),
      sortOrder,
      task,
    }
  }

  // --- Save plan ---
  const saveMutation = useMutation({
    mutationFn: () =>
      savePlan(
        TODAY,
        gridBlocks.map((b) => ({
          taskId: b.task.id,
          startTime: minutesToTime(b.startMinutes),
          endTime: minutesToTime(b.endMinutes),
        }))
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schedule'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      navigate('/', { state: { successMessage: 'Plan saved. Good luck today!' } })
    },
  })

  // --- dnd-kit drag events ---
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }))

  function handleDragStart(event) {
    if (event.active.data.current?.type === 'task-card') {
      setActiveTaskCard(event.active.data.current.task)
    }
  }

  function handleDragEnd(event) {
    setActiveTaskCard(null)
    const { active, over, delta } = event
    if (!over || over.id !== 'time-block-grid') return

    const activeData = active.data.current

    if (activeData.type === 'calendar-block') {
      // Moving an existing block within the calendar
      const { block, blockIndex } = activeData
      const deltaMins = pixelDeltaToMinutes(delta.x)
      const duration = block.endMinutes - block.startMinutes
      const rawStart = block.startMinutes + deltaMins
      const snapped = Math.max(DAY_START_MINUTES, Math.min(DAY_END_MINUTES - duration, snapTo15(rawStart)))

      const updated = gridBlocks.map((b, i) =>
        i === blockIndex ? { ...b, startMinutes: snapped, endMinutes: snapped + duration } : { ...b }
      )
      const sorted = [...updated].sort((a, b) => a.startMinutes - b.startMinutes)
      const movedIndex = sorted.findIndex((b) => b.id === block.id)
      const pushed = pushBlocks(sorted, movedIndex, DAY_END_MINUTES)
      if (pushed) setBlocks(pushed)
    } else if (activeData.type === 'task-card') {
      // Dropping a task card from the browser onto the calendar
      const task = activeData.task
      if (scheduledTaskIds.has(task.id)) return

      // Use the final pointer position over the grid
      const dropClientX = event.activatorEvent.clientX + delta.x
      const rawStart = clientXToMinutes(dropClientX)
      const snapped = Math.max(
        DAY_START_MINUTES,
        Math.min(DAY_END_MINUTES - 60, snapTo15(rawStart))
      )

      const newBlock = makeBlock(task, snapped, snapped + 60, gridBlocks.length)
      const combined = [...gridBlocks, newBlock].sort((a, b) => a.startMinutes - b.startMinutes)
      const insertedIdx = combined.findIndex((b) => b.id === newBlock.id)
      const pushed = pushBlocks(combined, insertedIdx, DAY_END_MINUTES)
      if (pushed) setBlocks(pushed)
    }
  }

  const selectedCount = [...selectedTaskIds].filter((id) => !scheduledTaskIds.has(id)).length

  if (loadingTasks) {
    return <div className="p-8 text-gray-400 text-sm">Loading your tasks…</div>
  }

  return (
    <DndContext sensors={sensors} onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
      <div className="p-6 max-w-7xl mx-auto space-y-4">

        <h1 className="text-2xl font-semibold text-gray-900">
          Start Day — {new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })}
        </h1>

        {/* Row 1: All tasks by project */}
        <section className="bg-white border border-gray-200 rounded-lg p-4">
          <div className="text-xs font-semibold text-indigo-700 uppercase tracking-wider mb-3">
            All Tasks
            <span className="ml-2 font-normal text-gray-400 normal-case tracking-normal">
              — drag or check to schedule
            </span>
          </div>
          <TaskBrowserRow
            tasks={suggestedTasks}
            selectedTaskIds={selectedTaskIds}
            scheduledTaskIds={scheduledTaskIds}
            onToggle={toggleTask}
            emptyMessage="No tasks to schedule — you're all caught up!"
            section="all"
          />
          <div className="mt-3 flex items-center gap-3">
            <span className="text-xs text-gray-500">
              {selectedCount > 0 ? `${selectedCount} selected` : 'None selected'}
            </span>
            <button
              onClick={handleAddToCalendar}
              disabled={selectedCount === 0}
              className="px-3 py-1.5 text-xs rounded-md bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-40 transition-colors font-medium"
            >
              + Add to calendar
            </button>
            {addWarning && (
              <span className="text-xs text-amber-600">{addWarning}</span>
            )}
          </div>
        </section>

        {/* Row 2: Due today (left) + Due this week (right) */}
        <div className="flex gap-4">
          <section className="flex-1 min-w-0 bg-white border border-red-200 rounded-lg p-4">
            <div className="text-xs font-semibold text-red-700 uppercase tracking-wider mb-3">
              Due Today
            </div>
            <TaskBrowserRow
              tasks={dueTodayTasks}
              selectedTaskIds={selectedTaskIds}
              scheduledTaskIds={scheduledTaskIds}
              onToggle={toggleTask}
              emptyMessage="Nothing due today"
              section="due-today"
            />
          </section>

          <section className="flex-1 min-w-0 bg-white border border-amber-200 rounded-lg p-4">
            <div className="text-xs font-semibold text-amber-700 uppercase tracking-wider mb-3">
              Due This Week
            </div>
            <TaskBrowserRow
              tasks={dueThisWeekTasks}
              selectedTaskIds={selectedTaskIds}
              scheduledTaskIds={scheduledTaskIds}
              onToggle={toggleTask}
              emptyMessage="Nothing due this week"
              section="due-week"
            />
          </section>
        </div>

        {/* Row 3: Horizontal calendar */}
        <section className="bg-white border border-gray-200 rounded-lg p-4">
          <div className="text-xs font-semibold text-indigo-700 uppercase tracking-wider mb-3">
            Today's Plan
            <span className="ml-2 font-normal text-gray-400 normal-case tracking-normal">
              — drag blocks to move · drag right edge to resize
            </span>
          </div>

          <TimeBlockGrid
            blocks={gridBlocks}
            onBlocksChange={setBlocks}
            onRemoveBlock={handleRemoveBlock}
            gridRef={gridRef}
            minutesToPercent={minutesToPercent}
            durationToPercent={durationToPercent}
            startResize={startResize}
          />

          {saveMutation.isError && (
            <p className="mt-2 text-xs text-red-500">
              {saveMutation.error?.message ?? 'Something went wrong. Please try again.'}
            </p>
          )}

          <div className="mt-3 flex justify-end">
            <button
              onClick={() => saveMutation.mutate()}
              disabled={gridBlocks.length === 0 || saveMutation.isPending}
              className="px-4 py-2 text-sm rounded-md bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-40 transition-colors font-medium"
            >
              {saveMutation.isPending ? 'Saving…' : 'Confirm plan'}
            </button>
          </div>
        </section>
      </div>

      {/* DragOverlay — shown while dragging a task card from the browser */}
      <DragOverlay>
        {activeTaskCard ? (
          <div className="bg-indigo-500 text-white text-xs font-medium rounded px-2 py-1.5 shadow-lg opacity-90 max-w-[160px] truncate">
            {activeTaskCard.title}
          </div>
        ) : null}
      </DragOverlay>
    </DndContext>
  )
}
