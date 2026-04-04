import { describe, it, expect } from 'vitest'
import { timeToMinutes, minutesToTime, snapTo15, pushBlocks, toGridBlock } from './pushBlocks.js'

describe('timeToMinutes', () => {
  it('converts HH:MM format', () => {
    expect(timeToMinutes('09:00')).toBe(540)
    expect(timeToMinutes('13:30')).toBe(810)
  })

  it('converts HH:MM:SS format (ignores seconds)', () => {
    expect(timeToMinutes('09:00:00')).toBe(540)
    expect(timeToMinutes('13:30:45')).toBe(810)
  })

  it('handles midnight', () => {
    expect(timeToMinutes('00:00')).toBe(0)
    expect(timeToMinutes('00:00:00')).toBe(0)
  })

  it('handles end of day', () => {
    expect(timeToMinutes('23:59')).toBe(1439)
    expect(timeToMinutes('24:00')).toBe(1440)
  })

  it('handles single-digit hours with zero padding', () => {
    expect(timeToMinutes('01:15')).toBe(75)
  })
})

describe('minutesToTime', () => {
  it('converts minutes to HH:MM string', () => {
    expect(minutesToTime(540)).toBe('09:00')
    expect(minutesToTime(810)).toBe('13:30')
  })

  it('zero-pads hours and minutes', () => {
    expect(minutesToTime(0)).toBe('00:00')
    expect(minutesToTime(75)).toBe('01:15')
    expect(minutesToTime(9)).toBe('00:09')
  })

  it('round-trips with timeToMinutes for HH:MM', () => {
    const times = ['00:00', '01:15', '09:00', '13:30', '17:45', '23:59']
    for (const t of times) {
      expect(minutesToTime(timeToMinutes(t))).toBe(t)
    }
  })

  it('handles end of day', () => {
    expect(minutesToTime(1440)).toBe('24:00')
    expect(minutesToTime(1439)).toBe('23:59')
  })
})

describe('snapTo15', () => {
  it('snaps 0 to 0', () => {
    expect(snapTo15(0)).toBe(0)
  })

  it('snaps 7 down to 0 (rounds down)', () => {
    expect(snapTo15(7)).toBe(0)
  })

  it('snaps 8 up to 15 (rounds up)', () => {
    expect(snapTo15(8)).toBe(15)
  })

  it('snaps 22 down to 15', () => {
    expect(snapTo15(22)).toBe(15)
  })

  it('snaps 23 up to 30', () => {
    expect(snapTo15(23)).toBe(30)
  })

  it('snaps 37 down to 30', () => {
    expect(snapTo15(37)).toBe(30)
  })

  it('snaps 38 up to 45', () => {
    expect(snapTo15(38)).toBe(45)
  })

  it('snaps 52 down to 45', () => {
    expect(snapTo15(52)).toBe(45)
  })

  it('snaps 53 up to 60', () => {
    expect(snapTo15(53)).toBe(60)
  })

  it('returns exact 15-minute boundary unchanged', () => {
    expect(snapTo15(15)).toBe(15)
    expect(snapTo15(30)).toBe(30)
    expect(snapTo15(45)).toBe(45)
    expect(snapTo15(60)).toBe(60)
  })
})

describe('pushBlocks', () => {
  const DAY_END = 17 * 60 // 1020 = 5 PM

  it('returns unchanged array when there is no overlap', () => {
    const blocks = [
      { id: 'a', startMinutes: 540, endMinutes: 600 }, // 9:00–10:00
      { id: 'b', startMinutes: 610, endMinutes: 670 }, // 10:10–11:10
    ]
    const result = pushBlocks(blocks, 0, DAY_END)
    expect(result).toEqual(blocks)
  })

  it('pushes a single overlapping block forward', () => {
    const blocks = [
      { id: 'a', startMinutes: 540, endMinutes: 630 }, // 9:00–10:30
      { id: 'b', startMinutes: 600, endMinutes: 660 }, // 10:00–11:00 (overlaps by 30)
    ]
    const result = pushBlocks(blocks, 0, DAY_END)
    expect(result[1].startMinutes).toBe(630)
    expect(result[1].endMinutes).toBe(690)
  })

  it('cascades a push through multiple overlapping blocks', () => {
    const blocks = [
      { id: 'a', startMinutes: 540, endMinutes: 630 }, // 9:00–10:30 (extended 30 min)
      { id: 'b', startMinutes: 600, endMinutes: 660 }, // originally 10:00–11:00
      { id: 'c', startMinutes: 660, endMinutes: 720 }, // originally 11:00–12:00
    ]
    const result = pushBlocks(blocks, 0, DAY_END)
    // b shifts to 10:30–11:30
    expect(result[1].startMinutes).toBe(630)
    expect(result[1].endMinutes).toBe(690)
    // c is now overlapping b's new end (690), so shifts to 11:30–12:30
    expect(result[2].startMinutes).toBe(690)
    expect(result[2].endMinutes).toBe(750)
  })

  it('returns null when push causes last block to exceed dayEndMinutes', () => {
    const blocks = [
      { id: 'a', startMinutes: 900, endMinutes: 1000 }, // big block ending at 16:40
      { id: 'b', startMinutes: 960, endMinutes: 1020 }, // overlaps by 40; would push to 17:40
    ]
    const result = pushBlocks(blocks, 0, DAY_END)
    expect(result).toBeNull()
  })

  it('stops pushing at the first gap', () => {
    const blocks = [
      { id: 'a', startMinutes: 540, endMinutes: 630 }, // 9:00–10:30
      { id: 'b', startMinutes: 600, endMinutes: 660 }, // overlaps, shifts to 10:30–11:30
      { id: 'c', startMinutes: 720, endMinutes: 780 }, // 12:00–13:00, no overlap with shifted b
    ]
    const result = pushBlocks(blocks, 0, DAY_END)
    // b shifts
    expect(result[1].startMinutes).toBe(630)
    expect(result[1].endMinutes).toBe(690)
    // c should be untouched (gap between 690 and 720)
    expect(result[2].startMinutes).toBe(720)
    expect(result[2].endMinutes).toBe(780)
  })

  it('does not mutate the input array', () => {
    const blocks = [
      { id: 'a', startMinutes: 540, endMinutes: 630 },
      { id: 'b', startMinutes: 600, endMinutes: 660 },
    ]
    const original = blocks.map(b => ({ ...b }))
    pushBlocks(blocks, 0, DAY_END)
    expect(blocks).toEqual(original)
  })

  it('returns an empty array for an empty input', () => {
    expect(pushBlocks([], 0, DAY_END)).toEqual([])
  })

  it('returns a single-block array unchanged when the block fits in the day', () => {
    const blocks = [{ id: 'a', startMinutes: 540, endMinutes: 600 }]
    const result = pushBlocks(blocks, 0, DAY_END)
    expect(result).toEqual(blocks)
  })

  it('returns null for a single block that already exceeds dayEndMinutes', () => {
    const blocks = [{ id: 'a', startMinutes: 960, endMinutes: 1021 }]
    const result = pushBlocks(blocks, 0, DAY_END)
    expect(result).toBeNull()
  })

  it('only pushes blocks after changedIndex, not before', () => {
    const blocks = [
      { id: 'a', startMinutes: 540, endMinutes: 600 }, // index 0
      { id: 'b', startMinutes: 580, endMinutes: 640 }, // index 1 — overlaps a, but changedIndex=1 so we check from index 2
      { id: 'c', startMinutes: 700, endMinutes: 760 }, // index 2
    ]
    // changedIndex=1 means only check from index 2 onward
    const result = pushBlocks(blocks, 1, DAY_END)
    // block a should be unchanged
    expect(result[0]).toEqual(blocks[0])
    // block b should be unchanged (it's the changed block)
    expect(result[1]).toEqual(blocks[1])
    // block c has no overlap with b so unchanged
    expect(result[2]).toEqual(blocks[2])
  })

  it('preserves all other block properties when pushing', () => {
    const blocks = [
      { id: 'a', startMinutes: 540, endMinutes: 630, label: 'Meeting', color: 'purple' },
      { id: 'b', startMinutes: 600, endMinutes: 660, label: 'Focus', color: 'blue' },
    ]
    const result = pushBlocks(blocks, 0, DAY_END)
    expect(result[1].id).toBe('b')
    expect(result[1].label).toBe('Focus')
    expect(result[1].color).toBe('blue')
  })
})

describe('toGridBlock', () => {
  it('adds startMinutes and endMinutes from startTime and endTime strings', () => {
    const serverBlock = {
      id: '1',
      startTime: '09:00:00',
      endTime: '10:30:00',
      label: 'Deep Work',
    }
    const result = toGridBlock(serverBlock)
    expect(result.startMinutes).toBe(540)
    expect(result.endMinutes).toBe(630)
  })

  it('preserves all original server block properties', () => {
    const serverBlock = {
      id: '42',
      startTime: '13:00:00',
      endTime: '14:00:00',
      label: 'Lunch',
      color: 'green',
      userId: 'user-1',
    }
    const result = toGridBlock(serverBlock)
    expect(result.id).toBe('42')
    expect(result.label).toBe('Lunch')
    expect(result.color).toBe('green')
    expect(result.userId).toBe('user-1')
    expect(result.startTime).toBe('13:00:00')
    expect(result.endTime).toBe('14:00:00')
  })

  it('does not mutate the original server block', () => {
    const serverBlock = { id: '1', startTime: '09:00:00', endTime: '10:00:00' }
    const original = { ...serverBlock }
    toGridBlock(serverBlock)
    expect(serverBlock).toEqual(original)
  })

  it('handles HH:MM format without seconds', () => {
    const serverBlock = { id: '1', startTime: '08:30', endTime: '09:00' }
    const result = toGridBlock(serverBlock)
    expect(result.startMinutes).toBe(510)
    expect(result.endMinutes).toBe(540)
  })
})
