export function Card({ children, className = '', hoverable = false, onClick, ...rest }) {
  const base = 'bg-surface-raised border border-edge rounded-xl p-5 shadow-card'
  const hover = hoverable || onClick ? 'hover:shadow-card-hover transition-shadow' : ''
  const clickable = onClick ? 'cursor-pointer' : ''
  return (
    <div
      className={`${base} ${hover} ${clickable} ${className}`}
      onClick={onClick}
      {...rest}
    >
      {children}
    </div>
  )
}
