import { useState } from 'react'
import { ChevronLeft, ChevronRight, Download, ScrollText, Search } from 'lucide-react'
import { toApiError } from '../../api/client'
import { stationApi } from '../../api/endpoints'
import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { EmptyState, ErrorState } from '../../components/ui/EmptyState'
import { PageHeader } from '../../components/ui/PageHeader'
import { TableSkeleton } from '../../components/ui/Skeleton'
import { useToast } from '../../context/ToastContext'
import { useApi } from '../../hooks/useApi'
import { dateTime, hm, titleCase } from '../../lib/format'

const ROLES = ['PASSENGER', 'STATION_MASTER', 'PLATFORM_CONTROLLER', 'BOOKING_CLERK', 'PUBLIC', 'SYSTEM']
const PAGE_SIZE = 20

/** Audit Log: every action with time, user, role, action and details, filtered and paged on the server. */
export function AuditLog() {
  const toast = useToast()
  const [action, setAction] = useState('')
  const [role, setRole] = useState('')
  const [q, setQ] = useState('')
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [exporting, setExporting] = useState(false)
  const actions = useApi(() => stationApi.auditActions())
  const { data, error, loading, reload } = useApi(
    () => stationApi.audit({ action: action || undefined, role: role || undefined, q: query || undefined, page, size: PAGE_SIZE }),
    [action, role, query, page], 10000)

  const exportCsv = async () => {
    setExporting(true)
    try {
      const blob = await stationApi.auditCsv({ action: action || undefined, role: role || undefined, q: query || undefined })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `payanam-audit-${new Date().toISOString().slice(0, 10)}.csv`
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      toast.push('error', 'Export failed', toApiError(e).message)
    } finally {
      setExporting(false)
    }
  }

  return (
    <>
      <PageHeader title="Audit Log" crumbs={[{ label: 'Station', to: '/station' }]}
        description="Stored in the H2 database. Failed and refused actions are recorded too."
        actions={<Button variant="secondary" loading={exporting} onClick={exportCsv}><Download className="h-4 w-4" /> Export CSV</Button>} />
      <Card>
        <form className="flex flex-wrap items-center gap-3 border-b border-slate-200 p-3 dark:border-navy-800"
          onSubmit={(e) => { e.preventDefault(); setPage(0); setQuery(q.trim()) }}>
          <div className="relative min-w-[200px] flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input className="input pl-9" placeholder="Search user or details, then Enter" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search audit log" />
          </div>
          <select className="input w-auto" value={action} onChange={(e) => { setAction(e.target.value); setPage(0) }} aria-label="Action">
            <option value="">All actions</option>
            {(actions.data ?? []).map((a) => <option key={a} value={a}>{titleCase(a)}</option>)}
          </select>
          <select className="input w-auto" value={role} onChange={(e) => { setRole(e.target.value); setPage(0) }} aria-label="Role">
            <option value="">All roles</option>
            {ROLES.map((r) => <option key={r} value={r}>{titleCase(r)}</option>)}
          </select>
        </form>
        {loading && !data ? <div className="p-4"><TableSkeleton rows={8} /></div> : error ? <ErrorState message={error.message} onRetry={() => reload()} />
          : !data?.content.length ? <EmptyState icon={ScrollText} title="No matching entries" /> : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-navy-800">
                <thead className="table-head"><tr>{['Time', 'Station time', 'User', 'Role', 'Action', 'Details', 'Result'].map((h) => <th key={h} className="px-4 py-2.5">{h}</th>)}</tr></thead>
                <tbody className="divide-y divide-slate-100 dark:divide-navy-800">
                  {data.content.map((e) => (
                    <tr key={e.id}>
                      <td className="whitespace-nowrap px-4 py-2.5 tabular-nums">{dateTime(e.timestamp)}</td>
                      <td className="px-4 py-2.5 tabular-nums muted">{hm(e.simulationTime)}</td>
                      <td className="px-4 py-2.5">{e.actor}</td>
                      <td className="px-4 py-2.5 text-xs">{titleCase(e.role)}</td>
                      <td className="px-4 py-2.5 font-mono text-xs">{e.action}</td>
                      <td className="max-w-md px-4 py-2.5">{e.details}</td>
                      <td className="px-4 py-2.5"><Badge tone={e.success ? 'green' : 'red'}>{e.success ? 'OK' : 'Failed'}</Badge></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-slate-200 px-4 py-2.5 text-sm dark:border-navy-800">
            <span className="muted">{data.totalElements} entries</span>
            <div className="flex items-center gap-1">
              <button className="btn-ghost btn-sm" disabled={page === 0} onClick={() => setPage(page - 1)} aria-label="Previous page"><ChevronLeft className="h-4 w-4" /></button>
              <span className="px-2 tabular-nums">{page + 1} / {data.totalPages}</span>
              <button className="btn-ghost btn-sm" disabled={page >= data.totalPages - 1} onClick={() => setPage(page + 1)} aria-label="Next page"><ChevronRight className="h-4 w-4" /></button>
            </div>
          </div>
        )}
      </Card>
    </>
  )
}
