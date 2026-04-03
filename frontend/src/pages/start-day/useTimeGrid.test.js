import { describe, it, expect } from 'vitest'
import { renderHook } from '@testing-library/react'
import {
  DAY_START_MINUTES,
  DAY_END_MINUTES,
  DAY_DURATION,
  useTimeGrid,
} from './useTimeGrid'

describe('Constants', () => {
  it('DAY_START_MINUTES is 480 (8 AM)', () => {
    expect(DAY_START_MINUTES).toBe(480)
  })

  it('DAY_END_MINUTES is 1020 (5 PM)', () => {
    expect(DAY_END_MINUTES).toBe(1020)
  })

  it('DAY_DURATION is 540', () => {
    expect(DAY_DURATION).toBe(540)
  })
})

describe('minutesToPercent', () => {
  it('returns 0% at day start (480)', () => {
    const { result } = renderHook(() => useTimeGrid())
    expect(result.current.minutesToPercent(480)).toBe(0)
  })

  it('returns 100% at day end (1020)', () => {
    const { result } = renderHook(() => useTimeGrid())
    expect(result.current.minutesToPercent(1020)).toBe(100)
  })

  it('returns ~50% at midday (750)', () => {
    const { result } = renderHook(() => useTimeGrid())
    expect(result.current.minutesToPercent(750)).toBeCloseTo(50, 5)
  })

  it('returns ~11.1% at 9:00 AM (540)', () => {
    const { result } = renderHook(() => useTimeGrid())
    expect(result.current.minutesToPercent(540)).toBeCloseTo(11.111, 2)
  })
})

describe('durationToPercent', () => {
  it('returns ~11.1% for 60 minutes', () => {
    const { result } = renderHook(() => useTimeGrid())
    expect(result.current.durationToPercent(60)).toBeCloseTo(11.111, 2)
  })

  it('returns 100% for full day (540 minutes)', () => {
    const { result } = renderHook(() => useTimeGrid())
    expect(result.current.durationToPercent(540)).toBe(100)
  })

  it('returns 0% for 0 minutes', () => {
    const { result } = renderHook(() => useTimeGrid())
    expect(result.current.durationToPercent(0)).toBe(0)
  })

  it('returns ~2.78% for 15 minutes', () => {
    const { result } = renderHook(() => useTimeGrid())
    expect(result.current.durationToPercent(15)).toBeCloseTo(2.778, 2)
  })
})

describe('pixelDeltaToMinutes', () => {
  it('returns 0 when gridRef is null', () => {
    const { result } = renderHook(() => useTimeGrid())
    // gridRef.current is null by default (no DOM element attached)
    expect(result.current.pixelDeltaToMinutes(100)).toBe(0)
  })
})

describe('clientXToMinutes', () => {
  it('returns DAY_START_MINUTES when gridRef is null', () => {
    const { result } = renderHook(() => useTimeGrid())
    // gridRef.current is null by default (no DOM element attached)
    expect(result.current.clientXToMinutes(500)).toBe(DAY_START_MINUTES)
  })
})
