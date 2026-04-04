export function ProgressBar({ value, max }) {
  const pct = max > 0 ? Math.round((value / max) * 100) : 0
  return (
    <div className="mt-2 h-2 bg-primary-100 rounded-full overflow-hidden">
      <div
        data-testid="progress-fill"
        className="h-full bg-primary-500 rounded-full transition-all"
        style={{ width: `${pct}%` }}
      />
    </div>
  )
}
