/**
 * Echel Planner logo — a warm cocoa notebook with scribble lines
 * and an angled pencil drawing the bottom scribble.
 *
 * Detail simplifies automatically below 32px.
 */
export function EchelLogo({ size = 24, className = '' }) {
  const showFullDetail = size >= 32

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 64 64"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      aria-hidden="true"
    >
      {/* Notebook */}
      <rect
        x="8" y="8" width="34" height="46" rx="4"
        fill="#FAF3EB"
        stroke="#7A5C3A"
        strokeWidth={showFullDetail ? 2.5 : 5}
      />

      {showFullDetail ? (
        <>
          {/* Scribble 1 */}
          <path
            d="M16 21 Q19 18, 22 21 Q25 24, 28 21 Q31 18, 34 21"
            stroke="#D5C3AA"
            strokeWidth={size >= 48 ? 2 : 3}
            strokeLinecap="round"
            fill="none"
          />
          {/* Scribble 2 */}
          <path
            d="M16 29 Q18.5 26.5, 21 29 Q23.5 31.5, 26 29 Q28.5 26.5, 31 29"
            stroke="#D5C3AA"
            strokeWidth={size >= 48 ? 2 : 3}
            strokeLinecap="round"
            fill="none"
          />
          {/* Scribble 3 (active — being drawn) */}
          <path
            d="M16 37 Q19 34, 22 37 Q25 40, 28 37 Q31 34, 34 37 Q37 40, 40 37"
            stroke="#9B7B4F"
            strokeWidth={size >= 48 ? 2 : 3}
            strokeLinecap="round"
            fill="none"
          />
          {/* Pencil at ~30 degrees */}
          <g transform="translate(40, 37)">
            <g transform="rotate(30)">
              <rect x="-2.5" y="-27" width="5" height="24" rx="1"
                fill="#9B7B4F" stroke="#7A5C3A" strokeWidth={size >= 48 ? 1.2 : 1.5} />
              <rect x="-2.5" y="-27" width="5" height="4" rx="1" fill="#7A5C3A" />
              <polygon points="-2,0 3,0 0.5,3.5" fill="#D4AA3E" />
            </g>
          </g>
        </>
      ) : (
        <>
          {/* Simplified: two scribbles, heavier strokes */}
          <path
            d="M16 23 Q20 19, 24 23 Q28 27, 32 23"
            stroke="#D5C3AA"
            strokeWidth="4"
            strokeLinecap="round"
            fill="none"
          />
          <path
            d="M16 35 Q19 31, 22 35 Q25 39, 28 35 Q31 31, 34 35 Q37 39, 40 35"
            stroke="#9B7B4F"
            strokeWidth="3.5"
            strokeLinecap="round"
            fill="none"
          />
          {/* Simplified pencil */}
          <g transform="translate(40, 35)">
            <g transform="rotate(30)">
              <rect x="-3" y="-26" width="6" height="23" rx="2"
                fill="#9B7B4F" stroke="#7A5C3A" strokeWidth="2" />
              <polygon points="-2.5,0 3.5,0 0.5,4.5" fill="#D4AA3E" />
            </g>
          </g>
        </>
      )}
    </svg>
  )
}
