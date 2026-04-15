import { useEffect, useRef, useState } from 'react'
import { FlyAwayCard } from './FlyAwayCard'

/**
 * Isolated test of the FlyAwayCard component.
 *
 * This story deliberately uses abstract, labeled scaffolding — NOT a mockup
 * of the real Echel Planner UI — so there's no ambiguity about what's being
 * tested. The FlyAwayCard itself is the real production component; everything
 * else in the frame is test scaffolding only.
 *
 * For a story that shows the FlyAwayCard in the actual Quick Capture flow
 * (with the real QuickCapture component + mocked backend), see the separate
 * QuickCapture story.
 *
 * A "Replay" button re-mounts FlyAwayCard with a new key to restart the
 * keyframe animation, since CSS animations don't auto-repeat on state change.
 */
function AnimationDemo() {
  const startRef = useRef(null)
  const endRef = useRef(null)
  const [rects, setRects] = useState(null)
  const [playId, setPlayId] = useState(0)

  // Measure on mount + on replay. rAF ensures layout is settled before reading rects.
  useEffect(() => {
    const id = requestAnimationFrame(() => {
      if (startRef.current && endRef.current) {
        setRects({
          start: startRef.current.getBoundingClientRect(),
          end: endRef.current.getBoundingClientRect(),
        })
      }
    })
    return () => cancelAnimationFrame(id)
  }, [playId])

  function replay() {
    setRects(null)
    setPlayId(n => n + 1)
  }

  return (
    <div style={{ position: 'relative', minHeight: 500, background: '#FAF8F6' }}>
      <button
        onClick={replay}
        style={{ position: 'absolute', top: 16, left: 16, zIndex: 80 }}
        className="px-3 py-1.5 text-sm font-medium text-primary-500 bg-surface-raised border border-primary-300 rounded-md hover:bg-primary-50"
      >
        ▶ Replay
      </button>

      {/* Labeled endpoint — the animation's target. */}
      <div
        ref={endRef}
        style={{
          position: 'absolute',
          top: 16,
          right: 16,
          width: 120,
          height: 40,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          border: '2px dashed #B8A6CF',
          borderRadius: 6,
          color: '#574876',
          fontSize: 12,
          fontWeight: 500,
        }}
      >
        end rect
      </div>

      {/* Labeled origin — the animation's source. */}
      <div
        style={{
          position: 'absolute',
          left: '50%',
          bottom: 32,
          transform: 'translateX(-50%)',
        }}
      >
        <div
          ref={startRef}
          style={{
            width: 360,
            height: 90,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            border: '2px dashed #B8A6CF',
            borderRadius: 6,
            color: '#574876',
            fontSize: 12,
            fontWeight: 500,
          }}
        >
          start rect
        </div>
      </div>

      {rects && (
        <FlyAwayCard
          key={playId}
          startRect={rects.start}
          endRect={rects.end}
          onComplete={() => {}}
        />
      )}
    </div>
  )
}

export default {
  title: 'Components/FlyAwayCard',
  component: FlyAwayCard,
  parameters: {
    layout: 'fullscreen',
  },
}

export const Isolated = {
  name: 'Isolated',
  render: () => <AnimationDemo />,
}
