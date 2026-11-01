import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { ChevronRight } from 'lucide-react'
import { useDocumentTitle } from '../../hooks/useDocumentTitle'

export interface Crumb { label: string; to?: string }

/** Page title with breadcrumbs, a description and action buttons. Also sets the browser tab title. */
export function PageHeader({ title, description, crumbs = [], actions }: {
  title: string; description?: string; crumbs?: Crumb[]; actions?: ReactNode
}) {
  useDocumentTitle(title)
  return (
    <div className="mb-6">
      {crumbs.length > 0 && (
        <nav aria-label="Breadcrumb" className="mb-2 flex flex-wrap items-center gap-1 text-xs muted">
          {crumbs.map((c, i) => (
            <span key={i} className="flex items-center gap-1">
              {c.to ? <Link to={c.to} className="hover:text-teal-600">{c.label}</Link> : <span>{c.label}</span>}
              {i < crumbs.length - 1 && <ChevronRight className="h-3 w-3" aria-hidden />}
            </span>
          ))}
          <ChevronRight className="h-3 w-3" aria-hidden />
          <span className="font-medium text-slate-700 dark:text-slate-200">{title}</span>
        </nav>
      )}
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
          {description && <p className="mt-1 max-w-3xl text-sm muted">{description}</p>}
        </div>
        {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
      </div>
    </div>
  )
}
