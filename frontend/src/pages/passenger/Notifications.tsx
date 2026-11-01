import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowUpCircle, Bell, CheckCheck, Ticket, TicketX, TrainFront } from 'lucide-react'
import { bookingApi } from '../../api/endpoints'
import { Card } from '../../components/ui/Card'
import { EmptyState, ErrorState } from '../../components/ui/EmptyState'
import { PageHeader } from '../../components/ui/PageHeader'
import { TableSkeleton } from '../../components/ui/Skeleton'
import { Tabs } from '../../components/ui/Tabs'
import { useApi } from '../../hooks/useApi'
import { dateTime } from '../../lib/format'

const ICON: Record<string, typeof Bell> = { BOOKING: Ticket, CANCELLATION: TicketX, PROMOTION: ArrowUpCircle, TRAIN: TrainFront }

/** All notifications: bookings, cancellations, WL promotions, delays and platform changes. */
export function Notifications() {
  const { data, error, loading, reload, setData } = useApi(() => bookingApi.notifications(), [], 10000)
  const [tab, setTab] = useState<'all' | 'unread'>('all')
  const rows = useMemo(() => (data ?? []).filter((n) => tab === 'all' || !n.read), [data, tab])
  const unread = (data ?? []).filter((n) => !n.read).length

  const markRead = async (id: number) => {
    await bookingApi.markRead(id)
    setData((list) => list?.map((n) => (n.id === id ? { ...n, read: true } : n)) ?? null)
  }
  const markAll = async () => {
    await bookingApi.markAllRead()
    setData((list) => list?.map((n) => ({ ...n, read: true })) ?? null)
  }

  return (
    <>
      <PageHeader title="Notifications" crumbs={[{ label: 'Passenger', to: '/passenger' }]}
        actions={<button className="btn-secondary" disabled={!unread} onClick={markAll}><CheckCheck className="h-4 w-4" /> Mark all read</button>} />
      <Tabs label="Filter" value={tab} onChange={setTab} tabs={[{ id: 'all', label: 'All', count: data?.length ?? 0 }, { id: 'unread', label: 'Unread', count: unread }]} />
      <Card className="mt-5">
        {loading && !data ? <div className="p-5"><TableSkeleton /></div> : error ? <ErrorState message={error.message} onRetry={() => reload()} />
          : rows.length === 0 ? <EmptyState icon={Bell} title={tab === 'unread' ? 'No unread notifications' : 'No notifications yet'} /> : (
            <ul className="divide-y divide-slate-100 dark:divide-navy-800">
              {rows.map((n) => {
                const Icon = ICON[n.type] ?? Bell
                return (
                  <li key={n.id} className={`flex items-start gap-4 px-5 py-4 ${n.read ? '' : 'bg-teal-50/50 dark:bg-teal-500/5'}`}>
                    <span className="rounded-lg bg-slate-100 p-2 text-navy-700 dark:bg-navy-800 dark:text-navy-100"><Icon className="h-4 w-4" /></span>
                    <div className="min-w-0 flex-1">
                      <p className={`text-sm ${n.read ? '' : 'font-semibold'}`}>{n.title}</p>
                      <p className="text-sm muted">{n.message}</p>
                      <p className="mt-1 text-xs muted">{dateTime(n.createdAt)}
                        {n.pnr && <> · <Link className="text-teal-600 hover:underline" to={`/passenger/bookings/${n.pnr}`}>PNR {n.pnr}</Link></>}</p>
                    </div>
                    {!n.read && <button className="btn-ghost btn-sm" onClick={() => markRead(n.id)}>Mark read</button>}
                  </li>
                )
              })}
            </ul>
          )}
      </Card>
    </>
  )
}
