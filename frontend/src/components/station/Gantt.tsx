import { useState } from 'react'
import { AlertTriangle } from 'lucide-react'
import type { Block, PlatformBoard } from '../../api/types'
import { hm } from '../../lib/format'
import { blockGeometry, overlaps, percentAt, ticks, toWindow } from '../../lib/gantt'
import { PRIORITY_COLOR, TRAIN_STATUS_LABEL } from '../../lib/status'

interface Hover { block: Block; platform: string; x: number; y: number }

/** Blocks on one platform that overlap another block there (should never happen: P1 forbids it). */
function conflicts(blocks: Block[]): Set<string> {
  const out = new Set<string>()
  for (let i = 0; i < blocks.length; i++) {
    for (let j = i + 1; j < blocks.length; j++) {
      if (overlaps(blocks[i].start, blocks[i].end, blocks[j].start, blocks[j].end)) {
        out.add(blocks[i].trainNo)
        out.add(blocks[j].trainNo)
      }
    }
  }
  return out
}

/**
 * Gantt timeline: x = time, one row per platform, a block per occupation interval
 * [arrival, departure + buffer), coloured by declared priority, plus a row of trains waiting in the queue
 * (dashed, at the time they would like). The red line is the simulation clock.
 */
export function Gantt({ board }: { board: PlatformBoard }) {
  const w = toWindow(board.windowStart, board.windowEnd)
  const [hover, setHover] = useState<Hover | null>(null)
  const nowPct = percentAt(w, board.now)
  const marks = ticks(w, 30)

  return (
    <div className="relative" onMouseLeave={() => setHover(null)}>
      <div className="overflow-x-auto">
        <div className="min-w-[760px]">
          {/* time axis */}
          <div className="relative ml-24 h-6 border-b border-slate-200 dark:border-navy-800">
            {marks.map((t) => (
              <span key={t} className="absolute -translate-x-1/2 text-[11px] tabular-nums muted" style={{ left: `${percentAt(w, new Date(t).toISOString())}%` }}>
                {new Date(t).toTimeString().slice(0, 5)}
              </span>
            ))}
          </div>
          {board.platforms.map((p) => {
            const bad = conflicts(p.blocks)
            return (
              <div key={p.id} className="flex items-stretch border-b border-slate-100 dark:border-navy-800">
                <div className="w-24 shrink-0 py-3 pr-2">
                  <p className="text-sm font-semibold">{p.id}</p>
                  <p className="text-[11px] muted">{p.lengthMeters} m{p.maintenance && ' · maint.'}</p>
                </div>
                <div className={`relative h-14 flex-1 ${p.maintenance ? 'bg-[repeating-linear-gradient(45deg,transparent,transparent_6px,rgba(245,158,11,.12)_6px,rgba(245,158,11,.12)_12px)]' : ''}`}>
                  {marks.map((t) => (
                    <span key={t} className="absolute inset-y-0 border-l border-dashed border-slate-100 dark:border-navy-800"
                      style={{ left: `${percentAt(w, new Date(t).toISOString())}%` }} />
                  ))}
                  {p.blocks.map((b) => {
                    const g = blockGeometry(w, b.start, b.end)
                    const departed = b.trainStatus === 'DEPARTED'
                    return (
                      <button key={b.trainNo} type="button"
                        className={`absolute top-2 flex h-10 items-center overflow-hidden rounded-md px-2 text-left text-[11px] font-semibold text-white shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-teal-400
                          ${departed ? 'opacity-40' : ''} ${b.trainStatus === 'ARRIVED' ? 'ring-2 ring-white/70 ring-inset' : ''} ${bad.has(b.trainNo) ? '!ring-2 !ring-red-500 ring-offset-1' : ''}`}
                        style={{ left: `${g.left}%`, width: `${g.width}%`, background: PRIORITY_COLOR[b.priority] }}
                        aria-label={`${b.trainNo} ${b.trainName} on ${p.id}, ${hm(b.start)} to ${hm(b.end)}, ${TRAIN_STATUS_LABEL[b.trainStatus] ?? b.trainStatus}`}
                        onMouseEnter={(e) => setHover({ block: b, platform: p.id, x: e.clientX, y: e.clientY })}
                        onFocus={(e) => { const r = e.currentTarget.getBoundingClientRect(); setHover({ block: b, platform: p.id, x: r.left, y: r.bottom }) }}
                        onBlur={() => setHover(null)}>
                        {bad.has(b.trainNo) && <AlertTriangle className="mr-1 h-3 w-3 shrink-0" />}
                        <span className="truncate">{b.trainNo}</span>
                      </button>
                    )
                  })}
                  <span className="pointer-events-none absolute inset-y-0 z-10 w-0.5 bg-red-500" style={{ left: `${nowPct}%` }} />
                </div>
              </div>
            )
          })}
          {board.waiting.length > 0 && (
            <div className="flex items-stretch">
              <div className="w-24 shrink-0 py-3 pr-2">
                <p className="text-sm font-semibold text-amber-600">Queue</p>
                <p className="text-[11px] muted">{board.waiting.length} waiting</p>
              </div>
              <div className="relative flex-1" style={{ height: `${Math.max(1, board.waiting.length) * 30 + 12}px` }}>
                {board.waiting.map((q, i) => {
                  const g = blockGeometry(w, q.arrival, q.desiredEnd)
                  return (
                    <span key={q.trainNo} className="absolute flex h-6 items-center truncate rounded-md border-2 border-dashed px-2 text-[11px] font-semibold"
                      style={{ left: `${g.left}%`, width: `${g.width}%`, top: 6 + i * 30, borderColor: PRIORITY_COLOR[q.type === 'SCHEDULED' ? 'NORMAL' : q.type], color: PRIORITY_COLOR[q.type === 'SCHEDULED' ? 'NORMAL' : q.type] }}
                      title={`${q.trainNo} ${q.trainName}: ${q.waitReason}`}>
                      #{q.rank} {q.trainNo}
                    </span>
                  )
                })}
                <span className="pointer-events-none absolute inset-y-0 w-0.5 bg-red-500" style={{ left: `${nowPct}%` }} />
              </div>
            </div>
          )}
        </div>
      </div>
      <div className="mt-3 flex flex-wrap gap-4 text-xs muted">
        {Object.entries(PRIORITY_COLOR).map(([k, c]) => (
          <span key={k} className="flex items-center gap-1.5"><span className="h-3 w-3 rounded" style={{ background: c }} />{k.toLowerCase()}</span>
        ))}
        <span className="flex items-center gap-1.5"><span className="h-3 w-0.5 bg-red-500" />now {hm(board.now)}</span>
        <span>Blocks include the {board.clearanceBufferMinutes}-min clearance buffer · faded = departed · outlined = on platform</span>
      </div>
      {hover && (
        <div role="tooltip" className="card pointer-events-none fixed z-50 w-60 p-3 text-xs shadow-lift"
          style={{ left: Math.min(hover.x + 12, window.innerWidth - 260), top: hover.y + 12 }}>
          <p className="text-sm font-semibold">{hover.block.trainNo} {hover.block.trainName}</p>
          <p className="mt-1 muted">{hover.platform} · {hm(hover.block.start)}–{hm(hover.block.end)} (incl. buffer)</p>
          <p className="muted">{hover.block.lengthMeters} m · {hover.block.priority.toLowerCase()} · {TRAIN_STATUS_LABEL[hover.block.trainStatus] ?? hover.block.trainStatus}</p>
        </div>
      )}
    </div>
  )
}
