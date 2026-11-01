import type { LucideIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import { AlertOctagon } from 'lucide-react'

export function EmptyState({ icon: Icon, title, message, action }: { icon: LucideIcon; title: string; message?: string; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center justify-center px-6 py-12 text-center">
      <div className="rounded-2xl bg-slate-100 p-4 text-slate-400 dark:bg-navy-800 dark:text-slate-500">
        <Icon className="h-8 w-8" aria-hidden />
      </div>
      <h3 className="mt-4 text-base font-semibold">{title}</h3>
      {message && <p className="mt-1 max-w-sm text-sm muted">{message}</p>}
      {action && <div className="mt-5">{action}</div>}
    </div>
  )
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="card">
      <EmptyState icon={AlertOctagon} title="Could not load this page" message={message}
        action={onRetry && <button className="btn-secondary" onClick={onRetry}>Try again</button>} />
    </div>
  )
}
