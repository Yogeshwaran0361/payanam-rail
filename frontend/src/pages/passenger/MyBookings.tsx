import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, ClipboardList, Printer, XCircle } from 'lucide-react'
import { bookingApi } from '../../api/endpoints'
import type { Booking } from '../../api/types'
import { TicketBadge, TrainStatusBadge } from '../../components/ui/Badge'
import { EmptyState, ErrorState } from '../../components/ui/EmptyState'
import { PageHeader } from '../../components/ui/PageHeader'
import { TableSkeleton } from '../../components/ui/Skeleton'
import { Tabs } from '../../components/ui/Tabs'
import { useApi } from '../../hooks/useApi'
import { dateTime, hm, rupees } from '../../lib/format'
import { useCancellation } from './useCancellation'

type Tab = 'UPCOMING' | 'COMPLETED' | 'CANCELLED'

function BookingRow({ b, onCancel }: { b: Booking; onCancel: (b: Booking) => void }) {
  return (
    <div className="card card-pad">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-xs muted">PNR <span className="font-mono text-sm font-semibold text-navy-900 dark:text-white">{b.pnr}</span> · booked {dateTime(b.bookedAt)}</p>
          <p className="mt-1 font-semibold"><span className="font-mono">{b.trainNo}</span> {b.trainName}</p>
          <p className="text-sm muted">{b.origin} → {b.destination} · arr {hm(b.arrival)} · dep {hm(b.departure)}{b.platformId && ` · Platform ${b.platformId}`}</p>
        </div>
        <div className="flex items-center gap-2">
          <TrainStatusBadge status={b.trainStatus} />
          <span className="text-sm font-semibold">{rupees(b.totalFare)}</span>
        </div>
      </div>
      <div className="mt-3 flex flex-wrap gap-2">
        {b.passengers.map((t) => (
          <span key={t.passengerNo} className="inline-flex items-center gap-1.5 rounded-lg bg-slate-50 px-2 py-1 text-xs dark:bg-navy-950/50">
            {t.name} <TicketBadge label={t.label} />
          </span>
        ))}
      </div>
      <div className="mt-4 flex flex-wrap gap-2 border-t border-slate-100 pt-3 dark:border-navy-800">
        <Link to={`/passenger/bookings/${b.pnr}`} className="btn-secondary btn-sm">Details <ArrowRight className="h-3.5 w-3.5" /></Link>
        {b.journeyState !== 'CANCELLED' && <Link to={`/passenger/bookings/${b.pnr}/ticket`} className="btn-ghost btn-sm"><Printer className="h-3.5 w-3.5" /> E-ticket</Link>}
        {b.cancellable && <button className="btn-ghost btn-sm ml-auto text-red-600" onClick={() => onCancel(b)}><XCircle className="h-3.5 w-3.5" /> Cancel booking</button>}
      </div>
    </div>
  )
}

/** My Bookings with Upcoming / Completed / Cancelled tabs. */
export function MyBookings() {
  const { data, error, loading, reload } = useApi(() => bookingApi.mine(), [], 15000)
  const [tab, setTab] = useState<Tab>('UPCOMING')
  const cancel = useCancellation(() => reload(true))

  const groups = useMemo(() => {
    const g: Record<Tab, Booking[]> = { UPCOMING: [], COMPLETED: [], CANCELLED: [] }
    ;(data ?? []).forEach((b) => g[b.journeyState].push(b))
    return g
  }, [data])

  return (
    <>
      <PageHeader title="My bookings" crumbs={[{ label: 'Passenger', to: '/passenger' }]}
        actions={<Link to="/passenger/book" className="btn-primary">Book a ticket</Link>} />
      <Tabs label="Booking groups" value={tab} onChange={setTab} tabs={[
        { id: 'UPCOMING', label: 'Upcoming', count: groups.UPCOMING.length },
        { id: 'COMPLETED', label: 'Completed', count: groups.COMPLETED.length },
        { id: 'CANCELLED', label: 'Cancelled', count: groups.CANCELLED.length },
      ]} />
      <div className="mt-5 space-y-4" role="tabpanel">
        {loading && !data ? <TableSkeleton /> : error ? <ErrorState message={error.message} onRetry={() => reload()} />
          : groups[tab].length === 0 ? (
            <div className="card"><EmptyState icon={ClipboardList} title={`No ${tab.toLowerCase()} bookings`}
              message={tab === 'UPCOMING' ? 'Your next booking will appear here.' : undefined} /></div>
          ) : groups[tab].map((b) => <BookingRow key={b.pnr} b={b} onCancel={(x) => cancel.ask(x)} />)}
      </div>
      {cancel.dialog}
    </>
  )
}
