import { PRESET_COLORS, DEFAULT_COLOR } from './constants'

export function ColorPicker({ value, onChange }) {
  const selected = value || DEFAULT_COLOR
  return (
    <div className="flex flex-wrap gap-2">
      {PRESET_COLORS.map(({ hex, label }) => (
        <button
          key={hex}
          type="button"
          title={label}
          onClick={() => onChange(hex)}
          className={[
            'w-7 h-7 rounded-full transition-transform focus:outline-none focus:ring-2 focus:ring-offset-1 focus:ring-edge-focus',
            selected === hex ? 'ring-2 ring-offset-1 ring-gray-600 scale-110' : 'hover:scale-110',
          ].join(' ')}
          style={{ backgroundColor: hex }}
          aria-label={label}
          aria-pressed={selected === hex}
        />
      ))}
    </div>
  )
}
