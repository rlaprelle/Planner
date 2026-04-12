import { useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { DndContext, DragOverlay, PointerSensor, useSensor, useSensors } from '@dnd-kit/core'
import { format } from 'date-fns'
import { useTranslation } from 'react-i18next'

import { getSuggestedTasks, getScheduleToday, savePlan } from '@/api/schedule'
import { getEventsForDate } from '@/api/events'
import { useTimeGrid } from './start-day/useTimeGrid'
import { pushBlocks, toGridBlock, snapTo15, minutesToTime, timeToMinutes } from './start-day/pushBlocks'
import { TimeBlockGrid } from './start-day/TimeBlockGrid'
import { TaskBrowserRow } from './start-day/TaskBrowserRow'

const TODAY = format(new Date(), 'yyyy-MM-dd')

export function StartDayPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { t, i18n } = useTranslation('timeBlocking')

  const [dayStartHour, setDayStartHour] = useState(8)
  const [dayEndHour, setDayEndHour] = useState(17)

  const dayStartMinutes = dayStartHour * 60
  const dayEndMinutes = dayEndHour * 60

  // --- Grid helpers ---
  const { gridRef, minutesToPercent, durationToPercent, pixelDeltaToMinutes, clientXToMinutes, startResize } =
    useTimeGrid(dayStartMinutes, dayEndMinutes)

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

  // Fetch events for today so they appear on the grid before savePlan materializes them
  const { data: todayEvents = [] } = useQuery({
    queryKey: ['events', 'for-date', TODAY],
    queryFn: () => getEventsForDate(TODAY),
  })

  // --- Local state ---
  const [blocks, setBlocks] = useState(null) // null = not yet initialised from server
  const [selectedTaskIds, setSelectedTaskIds] = useState(new Set())
  const [addWarning, setAddWarning] = useState(null)
  const [activeTaskCard, setActiveTaskCard] = useState(null) // for DragOverlay
  const [dropPreview, setDropPreview] = useState(null) // { startMinutes, endMinutes } while dragging over grid

  // Build event grid blocks from the events query (for days with no saved plan yet).
  // If existingBlocks already contains materialized event TimeBlocks, skip duplicates.
  const eventGridBlocks = useMemo(() => {
    const materializedEventIds = new Set(
      existingBlocks.filter((b) => b.isEvent).map((b) => b.event?.id)
    )
    return todayEvents
      .filter((evt) => !materializedEventIds.has(evt.id))
      .map((evt) => ({
        id: `event-${evt.id}`,
        blockDate: evt.blockDate,
        startTime: evt.startTime,
        endTime: evt.endTime,
        startMinutes: timeToMinutes(evt.startTime),
        endMinutes: timeToMinutes(evt.endTime),
        sortOrder: 0,
        actualStart: null,
        actualEnd: null,
        wasCompleted: false,
        task: null,
        event: {
          id: evt.id,
          title: evt.title,
          projectId: evt.projectId,
          projectName: evt.projectName,
          projectColor: evt.projectColor,
          energyLevel: evt.energyLevel,
        },
        isEvent: true,
      }))
  }, [todayEvents, existingBlocks])

  // Merge server blocks with event blocks, sorted by start time
  const serverBlocks = useMemo(() => {
    const combined = [...existingBlocks, ...eventGridBlocks]
    combined.sort((a, b) => a.startMinutes - b.startMinutes)
    return combined
  }, [existingBlocks, eventGridBlocks])

  // Initialise blocks from server once (mid-day replanning support)
  const gridBlocks = blocks ?? serverBlocks

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
    // Build a list of occupied ranges from event blocks so we can skip over them
    const eventRanges = gridBlocks
      .filter((b) => b.isEvent)
      .map((b) => ({ start: b.startMinutes, end: b.endMinutes }))
      .sort((a, b) => a.start - b.start)

    const lastEnd =
      gridBlocks.length > 0 ? Math.max(...gridBlocks.map((b) => b.endMinutes)) : dayStartMinutes

    let currentStart = lastEnd
    const newBlocks = []
    for (const task of toAdd) {
      // Skip over any event ranges that overlap with where we'd place the block
      for (const range of eventRanges) {
        if (currentStart < range.end && currentStart + 60 > range.start) {
          currentStart = range.end
        }
      }
      if (currentStart + 60 > dayEndMinutes) break
      newBlocks.push(makeBlock(task, currentStart, currentStart + 60, gridBlocks.length + newBlocks.length))
      currentStart += 60
    }

    const skipped = toAdd.length - newBlocks.length
    setBlocks([...gridBlocks, ...newBlocks])
    setSelectedTaskIds(new Set())
    if (skipped > 0) {
      setAddWarning(t('tasksDontFit', { count: skipped }))
    }
  }

  function handleRemoveBlock(blockId) {
    const target = gridBlocks.find((b) => b.id === blockId)
    if (target?.isEvent) return // events cannot be removed
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

  // --- Range-change validation ---
  function handleStartHourChange(newHour) {
    const earliestBlock = gridBlocks.length > 0
      ? Math.min(...gridBlocks.map((b) => b.startMinutes))
      : Infinity
    if (newHour * 60 > earliestBlock) {
      setAddWarning(t('blocksBeforeHour', { hour: formatDropdownHour(newHour) }))
      return
    }
    setAddWarning(null)
    setDayStartHour(newHour)
  }

  function handleEndHourChange(newHour) {
    const latestBlock = gridBlocks.length > 0
      ? Math.max(...gridBlocks.map((b) => b.endMinutes))
      : -Infinity
    if (newHour * 60 < latestBlock) {
      setAddWarning(t('blocksAfterHour', { hour: formatDropdownHour(newHour) }))
      return
    }
    setAddWarning(null)
    setDayEndHour(newHour)
  }

  const formatDropdownHour = (h) =>
    new Intl.DateTimeFormat(i18n.language, { hour: 'numeric' }).format(new Date(2000, 0, 1, h))

  // --- Save plan ---
  const saveMutation = useMutation({
    mutationFn: () =>
      savePlan(
        TODAY,
        gridBlocks
          .filter((b) => !b.isEvent)
          .map((b) => ({
            taskId: b.task.id,
            startTime: minutesToTime(b.startMinutes),
            endTime: minutesToTime(b.endMinutes),
          })),
        dayStartHour,
        dayEndHour
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schedule'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      navigate('/', { state: { successMessage: t('planSaved') } })
    },
  })

  // --- dnd-kit drag events ---
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }))

  /** Snap a cursor clientX to a centered 1-hour block start time. */
  function cursorToSnappedStart(clientX) {
    const rawCenter = clientXToMinutes(clientX)
    const rawStart = rawCenter - 30 // center the 1-hour block on cursor
    return Math.max(dayStartMinutes, Math.min(dayEndMinutes - 60, snapTo15(rawStart)))
  }

  function handleDragStart(event) {
    if (event.active.data.current?.type === 'task-card') {
      setActiveTaskCard(event.active.data.current.task)
    }
  }

  function handleDragMove(event) {
    const { active, over, delta } = event
    if (active.data.current?.type !== 'task-card') return

    if (!over || over.id !== 'time-block-grid') {
      setDropPreview(null)
      return
    }

    const currentX = event.activatorEvent.clientX + delta.x
    const snapped = cursorToSnappedStart(currentX)
    setDropPreview({ startMinutes: snapped, endMinutes: snapped + 60 })
  }

  function handleDragEnd(event) {
    setActiveTaskCard(null)
    setDropPreview(null)
    const { active, over, delta } = event
    if (!over || over.id !== 'time-block-grid') return

    const activeData = active.data.current

    if (activeData.type === 'calendar-block') {
      // Event blocks cannot be dragged
      if (activeData.block.isEvent) return
      // Moving an existing block within the calendar
      const { block, blockIndex } = activeData
      const deltaMins = pixelDeltaToMinutes(delta.x)
      const duration = block.endMinutes - block.startMinutes
      const rawStart = block.startMinutes + deltaMins
      const snapped = Math.max(dayStartMinutes, Math.min(dayEndMinutes - duration, snapTo15(rawStart)))

      const updated = gridBlocks.map((b, i) =>
        i === blockIndex ? { ...b, startMinutes: snapped, endMinutes: snapped + duration } : { ...b }
      )
      const sorted = [...updated].sort((a, b) => a.startMinutes - b.startMinutes)
      const movedIndex = sorted.findIndex((b) => b.id === block.id)
      const pushed = pushBlocks(sorted, movedIndex, dayEndMinutes)
      if (pushed) setBlocks(pushed)
    } else if (activeData.type === 'task-card') {
      // Dropping a task card from the browser onto the calendar
      const task = activeData.task
      if (scheduledTaskIds.has(task.id)) return

      const dropClientX = event.activatorEvent.clientX + delta.x
      const snapped = cursorToSnappedStart(dropClientX)

      const newBlock = makeBlock(task, snapped, snapped + 60, gridBlocks.length)
      const combined = [...gridBlocks, newBlock].sort((a, b) => a.startMinutes - b.startMinutes)
      const insertedIdx = combined.findIndex((b) => b.id === newBlock.id)
      const pushed = pushBlocks(combined, insertedIdx, dayEndMinutes)
      if (pushed) setBlocks(pushed)
    }
  }

  const selectedCount = [...selectedTaskIds].filter((id) => !scheduledTaskIds.has(id)).length

  if (loadingTasks) {
    return <div className="p-8 text-ink-muted text-sm">{t('loadingTasks')}</div>
  }

  return (
    <DndContext sensors={sensors} onDragStart={handleDragStart} onDragMove={handleDragMove} onDragEnd={handleDragEnd}>
      <div className="p-6 max-w-7xl mx-auto space-y-4">

        <h1 className="text-2xl font-semibold text-ink-heading">
          Start Day — {new Date().toLocaleDateString(i18n.language, { weekday: 'long', month: 'long', day: 'numeric' })}
        </h1>

        {/* Row 1: All tasks by project */}
        <section className="bg-surface-raised border border-edge rounded-lg p-4 shadow-card">
          <div className="text-xs font-semibold text-primary-700 uppercase tracking-wider mb-3">
            {t('allTasks')}
            <span className="ml-2 font-normal text-ink-muted normal-case tracking-normal">
              {t('dragToSchedule')}
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
            <span className="text-xs text-ink-muted">
              {selectedCount > 0 ? t('selectedCount', { count: selectedCount }) : t('noneSelected')}
            </span>
            <button
              onClick={handleAddToCalendar}
              disabled={selectedCount === 0}
              className="px-3 py-1.5 text-xs rounded-md bg-primary-500 text-white hover:bg-primary-600 disabled:opacity-40 transition-colors font-medium"
            >
              {t('addToCalendar')}
            </button>
            {addWarning && (
              <span className="text-xs text-deadline-week-text">{addWarning}</span>
            )}
          </div>
        </section>

        {/* Row 2: Due today (left) + Due this week (right) */}
        <div className="flex gap-4">
          <section className="flex-1 min-w-0 bg-surface-raised border border-deadline-today-bg rounded-lg p-4 shadow-card">
            <div className="text-xs font-semibold text-deadline-today-text uppercase tracking-wider mb-3">
              {t('dueToday')}
            </div>
            <TaskBrowserRow
              tasks={dueTodayTasks}
              selectedTaskIds={selectedTaskIds}
              scheduledTaskIds={scheduledTaskIds}
              onToggle={toggleTask}
              emptyMessage={t('nothingDueToday')}
              section="due-today"
            />
          </section>

          <section className="flex-1 min-w-0 bg-surface-raised border border-deadline-week-bg rounded-lg p-4 shadow-card">
            <div className="text-xs font-semibold text-deadline-week-text uppercase tracking-wider mb-3">
              {t('dueThisWeek')}
            </div>
            <TaskBrowserRow
              tasks={dueThisWeekTasks}
              selectedTaskIds={selectedTaskIds}
              scheduledTaskIds={scheduledTaskIds}
              onToggle={toggleTask}
              emptyMessage={t('nothingDueThisWeek')}
              section="due-week"
            />
          </section>
        </div>

        {/* Row 3: Horizontal calendar */}
        <section className="bg-surface-raised border border-edge rounded-lg p-4 shadow-card">
          <div className="flex items-center justify-between mb-3">
            <div className="text-xs font-semibold text-primary-700 uppercase tracking-wider">
              {t('todaysPlan')}
              <span className="ml-2 font-normal text-ink-muted normal-case tracking-normal">
                {t('dragInstructions')}
              </span>
            </div>
            <div className="flex items-center gap-1.5 text-xs text-ink-muted">
              <span>{t('hours')}</span>
              <select
                value={dayStartHour}
                onChange={(e) => handleStartHourChange(Number(e.target.value))}
                className="border border-edge rounded-md px-1.5 py-0.5 text-xs bg-surface-raised text-ink-base focus:outline-none focus:ring-1 focus:ring-primary-400"
              >
                {Array.from({ length: 24 }, (_, i) => i).filter((h) => h < dayEndHour).map((h) => (
                  <option key={h} value={h}>{formatDropdownHour(h)}</option>
                ))}
              </select>
              <span>{t('hourSeparator')}</span>
              <select
                value={dayEndHour}
                onChange={(e) => handleEndHourChange(Number(e.target.value))}
                className="border border-edge rounded-md px-1.5 py-0.5 text-xs bg-surface-raised text-ink-base focus:outline-none focus:ring-1 focus:ring-primary-400"
              >
                {Array.from({ length: 24 }, (_, i) => i + 1).filter((h) => h > dayStartHour).map((h) => (
                  <option key={h} value={h}>{formatDropdownHour(h)}</option>
                ))}
              </select>
            </div>
          </div>

          <TimeBlockGrid
            blocks={gridBlocks}
            onBlocksChange={setBlocks}
            onRemoveBlock={handleRemoveBlock}
            dropPreview={dropPreview}
            gridRef={gridRef}
            minutesToPercent={minutesToPercent}
            durationToPercent={durationToPercent}
            startResize={startResize}
            dayStartMinutes={dayStartMinutes}
            dayEndMinutes={dayEndMinutes}
          />

          {saveMutation.isError && (
            <p className="mt-2 text-xs text-error">
              {saveMutation.error?.message ?? t('common:genericError')}
            </p>
          )}

          <div className="mt-3 flex justify-end">
            <button
              onClick={() => saveMutation.mutate()}
              disabled={gridBlocks.length === 0 || saveMutation.isPending}
              className="px-4 py-2 text-sm rounded-md bg-primary-500 text-white hover:bg-primary-600 disabled:opacity-40 transition-colors font-medium"
            >
              {saveMutation.isPending ? t('common:saving') : t('confirmPlan')}
            </button>
          </div>
        </section>
      </div>

      {/* DragOverlay — shown while dragging a task card from the browser */}
      <DragOverlay>
        {activeTaskCard ? (
          <div className="bg-primary-500 text-white text-xs font-medium rounded px-2 py-1.5 shadow-lg opacity-90 max-w-[160px] truncate">
            {activeTaskCard.title}
          </div>
        ) : null}
      </DragOverlay>
    </DndContext>
  )
}
