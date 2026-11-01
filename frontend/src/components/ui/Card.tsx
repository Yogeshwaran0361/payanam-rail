import type { ReactNode } from 'react'

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <section className={`card ${className}`}>{children}</section>
}

export function CardHeader({ title, subtitle, action, icon }: { title: string; subtitle?: string; action?: ReactNode; icon?: ReactNode }) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-3 border-b border-slate-100 px-5 py-4 dark:border-navy-800">
      <div className="flex min-w-0 items-start gap-3">
        {icon && <div className="mt-0.5 text-teal-600 dark:text-teal-400">{icon}</div>}
        <div className="min-w-0">
          <h2 className="text-base font-semibold">{title}</h2>
          {subtitle && <p className="mt-0.5 text-sm muted">{subtitle}</p>}
        </div>
      </div>
      {action}
    </div>
  )
}
