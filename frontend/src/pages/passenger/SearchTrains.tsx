import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Armchair, Ticket } from 'lucide-react'
import { trainApi } from '../../api/endpoints'
import type { Train } from '../../api/types'
import { PriorityBadge, TrainStatusBadge } from '../../components/ui/Badge'
import { DataTable, type Column } from '../../components/ui/DataTable'
import { ErrorState } from '../../components/ui/EmptyState'
import { PageHeader } from '../../components/ui/PageHeader'
import { useApi } from '../../hooks/useApi'
import { hm, rupees } from '../../lib/format'

type Filter = 'all' | 'open' | 'available'

/** Search / timetable: today's trains in arrival order with seats left, filters and booking links. */
export function SearchTrains() {
  const { data, error, loading, reload } = useApi(() => trainApi.timetable(), [], 15000)
  const [filter, setFilter] = useState<Filter>('all')
  const [destination, setDestination] = useState('')
  const [priority, setPriority] = useState('')

  const destinations = useMemo(() => [...new Set((data ?? []).map((t) => t.destination))].sort(), [data])
  const rows = useMemo(() => (data ?? []).filter((t) =>
    (filter === 'all' || (filter === 'open' ? t.bookingOpen : t.bookingOpen && t.seatsLeft > 0))
    && (!destination || t.destination === destination) && (!priority || t.priority === priority)), [data, filter, destination, priority])

  const columns: Column<Train>[] = [
    { key: 'train', header: 'Train', sortValue: (t) => t.trainNo,
      render: (t) => <div><p className="font-mono font-semibold">{t.trainNo}</p><p className="text-xs muted">{t.name}</p></div> },
    { key: 'route', header: 'Route', sortValue: (t) => t.destination, render: (t) => <span className="text-sm">{t.origin} → {t.destination}</span> },
    { key: 'arr', header: 'Arrives', sortValue: (t) => t.expectedArrival,
      render: (t) => <span className="tabular-nums">{hm(t.expectedArrival)}{t.delayMinutes > 0 && <span className="ml-1 text-xs text-amber-600">+{t.delayMinutes}</span>}</span> },
    { key: 'dep', header: 'Departs', sortValue: (t) => t.expectedDeparture, render: (t) => <span className="tabular-nums">{hm(t.expectedDeparture)}</span> },
    { key: 'type', header: 'Type', render: (t) => <PriorityBadge priority={t.priority} /> },
    { key: 'status', header: 'Status', render: (t) => <TrainStatusBadge status={t.status} /> },
    { key: 'pf', header: 'Platform', render: (t) => t.platformId ?? '–' },
    { key: 'fare', header: 'Fare', sortValue: (t) => t.fare, render: (t) => rupees(t.fare) },
    { key: 'seats', header: 'Availability', sortValue: (t) => t.seatsLeft,
      render: (t) => !t.bookingOpen ? <span className="text-sm muted">Closed</span>
        : t.seatsLeft > 0 ? <span className="text-sm font-medium text-green-700 dark:text-green-400">{t.seatsLeft} of {t.capacity} free</span>
        : <span className="text-sm font-medium text-amber-700 dark:text-amber-400">WL {t.waiting + 1} next</span> },
    { key: 'actions', header: '', className: 'text-right',
      render: (t) => (
        <div className="flex justify-end gap-2">
          <Link to={`/passenger/trains/${t.trainNo}/seats`} className="btn-ghost btn-sm" aria-label={`Seat map of ${t.trainNo}`}><Armchair className="h-4 w-4" /></Link>
          {t.bookingOpen
            ? <Link to={`/passenger/book?train=${t.trainNo}`} className="btn-primary btn-sm"><Ticket className="h-3.5 w-3.5" /> Book</Link>
            : <button className="btn-secondary btn-sm" disabled>Book</button>}
        </div>
      ) },
  ]

  return (
    <>
      <PageHeader title="Search trains" crumbs={[{ label: 'Passenger', to: '/passenger' }]}
        description="All 6 trains calling at Payanam Junction today, in arrival order. Each train has 10 seats; when they are full, new passengers join the waiting list." />
      {error ? <ErrorState message={error.message} onRetry={() => reload()} /> : (
        <DataTable rows={rows} columns={columns} rowKey={(t) => t.trainNo} loading={loading} pageSize={10}
          searchText={(t) => `${t.trainNo} ${t.name} ${t.origin} ${t.destination}`} searchPlaceholder="Search by train number, name or station"
          caption="Today's trains"
          filters={<>
            <select className="input w-auto" value={destination} onChange={(e) => setDestination(e.target.value)} aria-label="Destination">
              <option value="">All destinations</option>
              {destinations.map((d) => <option key={d}>{d}</option>)}
            </select>
            <select className="input w-auto" value={priority} onChange={(e) => setPriority(e.target.value)} aria-label="Priority">
              <option value="">All priorities</option>
              <option value="EMERGENCY">Emergency</option>
              <option value="CONNECTION">Connection</option>
              <option value="NORMAL">Normal</option>
            </select>
            <select className="input w-auto" value={filter} onChange={(e) => setFilter(e.target.value as Filter)} aria-label="Availability">
              <option value="all">All trains</option>
              <option value="open">Booking open</option>
              <option value="available">Seats available</option>
            </select>
          </>} />
      )}
    </>
  )
}
