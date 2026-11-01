import { describe, expect, it } from 'vitest'
import { blockGeometry, overlaps, percentAt, ticks, toWindow } from './gantt'

const w = toWindow('2026-10-18T06:00:00', '2026-10-18T09:00:00')

describe('Gantt maths', () => {
  it('places times as a percentage of the window', () => {
    expect(percentAt(w, '2026-10-18T06:00:00')).toBe(0)
    expect(percentAt(w, '2026-10-18T07:30:00')).toBe(50)
    expect(percentAt(w, '2026-10-18T09:00:00')).toBe(100)
  })
  it('clamps times outside the window', () => {
    expect(percentAt(w, '2026-10-18T05:00:00')).toBe(0)
    expect(percentAt(w, '2026-10-18T10:00:00')).toBe(100)
  })
  it('computes block left and width', () => {
    const g = blockGeometry(w, '2026-10-18T07:00:00', '2026-10-18T07:30:00')
    expect(g.left).toBeCloseTo(33.333, 2)
    expect(g.width).toBeCloseTo(16.667, 2)
  })
  it('uses half-open intervals like the backend (touching blocks do not overlap)', () => {
    expect(overlaps('2026-10-18T07:00:00', '2026-10-18T07:25:00', '2026-10-18T07:25:00', '2026-10-18T07:40:00')).toBe(false)
    expect(overlaps('2026-10-18T07:00:00', '2026-10-18T07:26:00', '2026-10-18T07:25:00', '2026-10-18T07:40:00')).toBe(true)
  })
  it('creates half-hour ticks including both ends', () => {
    expect(ticks(w)).toHaveLength(7)
  })
})
