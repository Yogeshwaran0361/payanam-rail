// Pure maths for the Platform Board's Gantt timeline (kept separate so it is unit-tested).

export interface Window { start: number; end: number }

export function toWindow(start: string, end: string): Window {
  return { start: new Date(start).getTime(), end: new Date(end).getTime() }
}

/** Horizontal position of a time as a percentage of the window, clamped to [0, 100]. */
export function percentAt(w: Window, iso: string): number {
  const t = new Date(iso).getTime()
  const p = ((t - w.start) / (w.end - w.start)) * 100
  return Math.min(100, Math.max(0, p))
}

/** Left offset and width (in %) of a block [start, end). */
export function blockGeometry(w: Window, start: string, end: string): { left: number; width: number } {
  const left = percentAt(w, start)
  const right = percentAt(w, end)
  return { left, width: Math.max(0.6, right - left) }
}

/** Half-open overlap test, the same rule as the backend: a < d && c < b. */
export function overlaps(aStart: string, aEnd: string, bStart: string, bEnd: string): boolean {
  return new Date(aStart) < new Date(bEnd) && new Date(bStart) < new Date(aEnd)
}

/** Hourly (or half-hourly) tick marks between the window start and end. */
export function ticks(w: Window, stepMinutes = 30): number[] {
  const out: number[] = []
  for (let t = w.start; t <= w.end; t += stepMinutes * 60000) out.push(t)
  return out
}
