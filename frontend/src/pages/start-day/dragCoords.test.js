import { describe, it, expect, vi, afterEach } from 'vitest'
import { activatorClientX } from './dragCoords.js'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('activatorClientX', () => {
  it('reads clientX from a mouse/pointer activator', () => {
    expect(activatorClientX({ activatorEvent: { clientX: 120 } })).toBe(120)
  })

  it('handles a legitimate clientX of 0', () => {
    expect(activatorClientX({ activatorEvent: { clientX: 0 } })).toBe(0)
  })

  it('reads clientX from a touch activator via touches[0]', () => {
    const event = { activatorEvent: { type: 'touchstart', touches: [{ clientX: 210 }] } }
    expect(activatorClientX(event)).toBe(210)
  })

  it('falls back to changedTouches[0] when touches is empty (touchend)', () => {
    const event = {
      activatorEvent: { type: 'touchend', touches: [], changedTouches: [{ clientX: 305 }] },
    }
    expect(activatorClientX(event)).toBe(305)
  })

  it('warns and returns 0 when no coordinate is available', () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {})
    expect(activatorClientX({ activatorEvent: { type: 'unknown' } })).toBe(0)
    expect(warn).toHaveBeenCalledOnce()
  })
})
