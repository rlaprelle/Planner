import { createPortal } from 'react-dom'

/**
 * Animated blank card that flies from a start position to an end position
 * along a smooth upward arc, shrinking and fading as it goes. Used to
 * visually reinforce that a captured thought is being "filed away" to the inbox.
 *
 * The arc is achieved by splitting horizontal and vertical motion into two
 * nested elements with different timing functions — the outer moves X + fades
 * linearly while the inner moves Y + scales with an ease-out curve, producing
 * a natural arc without any keyframe midpoints that could cause stuttering.
 */
export function FlyAwayCard({ startRect, endRect, onComplete }) {
  const dx = endRect.left + endRect.width / 2 - (startRect.left + startRect.width / 2)
  const dy = endRect.top + endRect.height / 2 - (startRect.top + startRect.height / 2)
  const duration = 1600

  // 3×5 index card proportions (landscape)
  const width = 150
  const height = 90

  return createPortal(
    <div
      className="fixed z-[70] pointer-events-none"
      style={{
        left: startRect.left,
        top: startRect.top,
        animation: `flyX ${duration}ms linear forwards`,
      }}
    >
      <div
        className="rounded-lg bg-surface-soft shadow-card"
        style={{
          width,
          height,
          animation: `flyY ${duration}ms cubic-bezier(0.12, 0.8, 0.4, 1) forwards`,
        }}
        onAnimationEnd={onComplete}
      >
      </div>
      <style>{`
        @keyframes flyX {
          0% { opacity: 0.85; transform: translateX(0); }
          100% { opacity: 0; transform: translateX(${dx}px); }
        }
        @keyframes flyY {
          0% { transform: translateY(0) scale(1); }
          100% { transform: translateY(${dy}px) scale(0.3); }
        }
      `}</style>
    </div>,
    document.body
  )
}
