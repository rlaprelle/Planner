import { useTranslation } from 'react-i18next'

/**
 * A non-draggable, non-resizable event block on the calendar grid.
 *
 * Events are immovable hard boundaries — they cannot be moved, resized,
 * or removed by the user. Task blocks pushed into an event slot are rejected.
 */
export function EventBlock({ block, minutesToPercent, durationToPercent }) {
  const { t } = useTranslation('timeBlocking')
  const left = minutesToPercent(block.startMinutes)
  const width = durationToPercent(block.endMinutes - block.startMinutes)

  const startLabel = `${Math.floor(block.startMinutes / 60)}:${String(block.startMinutes % 60).padStart(2, '0')}`
  const endLabel = `${Math.floor(block.endMinutes / 60)}:${String(block.endMinutes % 60).padStart(2, '0')}`

  return (
    <div
      style={{
        position: 'absolute',
        left: `${left}%`,
        width: `${Math.max(width, 1)}%`,
        top: '3px',
        bottom: '3px',
        zIndex: 2,
        userSelect: 'none',
        cursor: 'default',
      }}
      className="rounded flex items-center select-none border border-amber-300 bg-amber-100 text-amber-800"
    >
      <div className="flex-1 flex items-center gap-1.5 px-2 overflow-hidden min-w-0">
        {/* Calendar icon */}
        <svg
          className="w-3.5 h-3.5 shrink-0 text-amber-500"
          viewBox="0 0 20 20"
          fill="currentColor"
          aria-hidden="true"
        >
          <path
            fillRule="evenodd"
            d="M5.75 2a.75.75 0 01.75.75V4h7V2.75a.75.75 0 011.5 0V4h.25A2.75 2.75 0 0118 6.75v8.5A2.75 2.75 0 0115.25 18H4.75A2.75 2.75 0 012 15.25v-8.5A2.75 2.75 0 014.75 4H5V2.75A.75.75 0 015.75 2zm-1 5.5a.75.75 0 00-.75.75v6.5c0 .69.56 1.25 1.25 1.25h9.5c.69 0 1.25-.56 1.25-1.25v-6.5a.75.75 0 00-.75-.75h-10.5z"
            clipRule="evenodd"
          />
        </svg>
        <span className="text-xs font-medium truncate">
          {block.event?.title ?? t('eventFallback')}
        </span>
        <span className="text-xs shrink-0 text-amber-500">
          {startLabel}–{endLabel}
        </span>
      </div>
    </div>
  )
}
