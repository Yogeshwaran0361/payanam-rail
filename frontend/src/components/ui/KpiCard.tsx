import type { LucideIcon } from 'lucide-react'

const ACCENT = {
  teal: 'bg-teal-50 text-teal-700 dark:bg-teal-500/15 dark:text-teal-300',
  navy: 'bg-navy-50 text-navy-700 dark:bg-navy-500/20 dark:text-navy-100',
  green: 'bg-green-50 text-green-700 dark:bg-green-500/15 dark:text-green-300',
  amber: 'bg-amber-50 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300',
  red: 'bg-red-50 text-red-700 dark:bg-red-500/15 dark:text-red-300',
  grey: 'bg-slate-100 text-slate-600 dark:bg-slate-500/15 dark:text-slate-300',
  violet: 'bg-violet-50 text-violet-700 dark:bg-violet-500/15 dark:text-violet-300',
}

/** A KPI stat card: icon, big number, label and an optional hint. */
export function KpiCard({ label, value, icon: Icon, accent = 'teal', hint }: {
  label: string; value: string | number; icon: LucideIcon; accent?: keyof typeof ACCENT; hint?: string
}) {
  return (
    <div className="card card-pad flex items-start gap-4">
      <div className={`rounded-xl p-2.5 ${ACCENT[accent]}`}>
        <Icon className="h-5 w-5" aria-hidden />
      </div>
      <div className="min-w-0">
        <p className="text-sm muted">{label}</p>
        <p className="mt-0.5 text-2xl font-semibold tabular-nums text-navy-900 dark:text-white">{value}</p>
        {hint && <p className="mt-0.5 truncate text-xs muted">{hint}</p>}
      </div>
    </div>
  )
}
