import { useMemo, useState, type ReactNode } from 'react'
import { ArrowDown, ArrowUp, ArrowUpDown, ChevronLeft, ChevronRight, Inbox, Search } from 'lucide-react'
import { EmptyState } from './EmptyState'
import { TableSkeleton } from './Skeleton'

export interface Column<T> {
  key: string
  header: string
  render: (row: T) => ReactNode
  /** Value used for sorting; columns without one are not sortable. */
  sortValue?: (row: T) => string | number
  className?: string
}

/**
 * Client-side data table: column sorting, a search box, optional filter controls and pagination.
 * The data sets here are small (≤ 60 rows per train), so sorting in the browser is simplest.
 */
export function DataTable<T>({
  rows, columns, rowKey, searchText, searchPlaceholder = 'Search…', filters, pageSize = 10, loading,
  emptyTitle = 'Nothing to show', emptyMessage, caption, rowClassName,
}: {
  rows: T[]
  columns: Column<T>[]
  rowKey: (row: T) => string | number
  searchText?: (row: T) => string
  searchPlaceholder?: string
  filters?: ReactNode
  pageSize?: number
  loading?: boolean
  emptyTitle?: string
  emptyMessage?: string
  caption?: string
  rowClassName?: (row: T) => string
}) {
  const [query, setQuery] = useState('')
  const [sort, setSort] = useState<{ key: string; dir: 1 | -1 } | null>(null)
  const [page, setPage] = useState(0)

  const visible = useMemo(() => {
    const q = query.trim().toLowerCase()
    let out = q && searchText ? rows.filter((r) => searchText(r).toLowerCase().includes(q)) : rows
    const col = sort && columns.find((c) => c.key === sort.key)
    if (col?.sortValue && sort) {
      const get = col.sortValue
      out = [...out].sort((a, b) => {
        const x = get(a), y = get(b)
        return (x < y ? -1 : x > y ? 1 : 0) * sort.dir
      })
    }
    return out
  }, [rows, query, sort, columns, searchText])

  const pages = Math.max(1, Math.ceil(visible.length / pageSize))
  const current = Math.min(page, pages - 1)
  const pageRows = visible.slice(current * pageSize, current * pageSize + pageSize)

  const toggleSort = (key: string) => {
    setSort((s) => (s?.key === key ? (s.dir === 1 ? { key, dir: -1 } : null) : { key, dir: 1 }))
  }

  return (
    <div className="card overflow-hidden">
      {(searchText || filters) && (
        <div className="flex flex-wrap items-center gap-3 border-b border-slate-200 p-3 dark:border-navy-800">
          {searchText && (
            <div className="relative min-w-[200px] flex-1">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" aria-hidden />
              <input className="input pl-9" placeholder={searchPlaceholder} value={query} aria-label={searchPlaceholder}
                onChange={(e) => { setQuery(e.target.value); setPage(0) }} />
            </div>
          )}
          {filters}
        </div>
      )}
      {loading ? (
        <div className="p-4"><TableSkeleton /></div>
      ) : visible.length === 0 ? (
        <EmptyState icon={Inbox} title={emptyTitle} message={query ? `No rows match “${query}”.` : emptyMessage} />
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 dark:divide-navy-800">
            {caption && <caption className="sr-only">{caption}</caption>}
            <thead className="table-head">
              <tr>
                {columns.map((c) => (
                  <th key={c.key} scope="col" className={`px-4 py-2.5 ${c.className ?? ''}`}
                    aria-sort={sort?.key === c.key ? (sort.dir === 1 ? 'ascending' : 'descending') : undefined}>
                    {c.sortValue ? (
                      <button className="inline-flex items-center gap-1 uppercase hover:text-slate-700 dark:hover:text-slate-200"
                        onClick={() => toggleSort(c.key)}>
                        {c.header}
                        {sort?.key === c.key ? (sort.dir === 1 ? <ArrowUp className="h-3 w-3" /> : <ArrowDown className="h-3 w-3" />)
                          : <ArrowUpDown className="h-3 w-3 opacity-40" />}
                      </button>
                    ) : c.header}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-navy-800">
              {pageRows.map((r) => (
                <tr key={rowKey(r)} className={`hover:bg-slate-50/70 dark:hover:bg-navy-800/40 ${rowClassName?.(r) ?? ''}`}>
                  {columns.map((c) => <td key={c.key} className={`table-cell ${c.className ?? ''}`}>{c.render(r)}</td>)}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {!loading && visible.length > pageSize && (
        <div className="flex items-center justify-between border-t border-slate-200 px-4 py-2.5 text-sm dark:border-navy-800">
          <span className="muted">
            {current * pageSize + 1}–{Math.min(visible.length, (current + 1) * pageSize)} of {visible.length}
          </span>
          <div className="flex items-center gap-1">
            <button className="btn-ghost btn-sm" disabled={current === 0} onClick={() => setPage(current - 1)} aria-label="Previous page">
              <ChevronLeft className="h-4 w-4" />
            </button>
            <span className="px-2 tabular-nums">{current + 1} / {pages}</span>
            <button className="btn-ghost btn-sm" disabled={current >= pages - 1} onClick={() => setPage(current + 1)} aria-label="Next page">
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
