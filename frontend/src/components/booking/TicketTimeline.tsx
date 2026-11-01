import { CheckCircle2, Circle, XCircle, ArrowUpCircle } from 'lucide-react'
import type { TimelineStep } from '../../api/types'
import { hm } from '../../lib/format'

/** A ticket's history (Booked, CNF or WL, WL moves, promotion, cancellation), oldest first. */
export function TicketTimeline({ steps }: { steps: TimelineStep[] }) {
  return (
    <ol className="relative ml-2 border-l border-slate-200 dark:border-navy-700">
      {steps.map((s, i) => {
        const Icon = s.label === 'CAN' ? XCircle
          : s.note.startsWith('Promoted') ? ArrowUpCircle
          : i === steps.length - 1 ? CheckCircle2 : Circle
        const tone = Icon === XCircle ? 'text-slate-400' : Icon === ArrowUpCircle ? 'text-green-600' : 'text-teal-600'
        return (
          <li key={i} className="mb-3 ml-5 last:mb-0">
            <span className={`absolute -left-[9px] flex h-[18px] w-[18px] items-center justify-center rounded-full bg-white dark:bg-navy-900 ${tone}`}>
              <Icon className="h-[18px] w-[18px]" aria-hidden />
            </span>
            <p className="text-sm font-medium">{s.label} <span className="ml-1 text-xs font-normal muted tabular-nums">{hm(s.at)}</span></p>
            {s.note && <p className="text-xs muted">{s.note}</p>}
          </li>
        )
      })}
    </ol>
  )
}
