import { Link, useNavigate, useParams } from 'react-router-dom'
import { Armchair, Ticket } from 'lucide-react'
import { trainApi } from '../../api/endpoints'
import { ErrorState } from '../../components/ui/EmptyState'
import { Card, CardHeader } from '../../components/ui/Card'
import { PageHeader } from '../../components/ui/PageHeader'
import { PageSkeleton } from '../../components/ui/Skeleton'
import { useApi } from '../../hooks/useApi'

/** The train's 10-seat array as a coach layout. Only "your" seats are identified; nobody else's names are sent. */
export function SeatMap() {
  const { trainNo = '' } = useParams()
  const navigate = useNavigate()
  const trains = useApi(() => trainApi.timetable())
  const { data, error, loading, reload } = useApi(() => trainApi.seatMap(trainNo), [trainNo], 10000)
  if (loading && !data) return <PageSkeleton />
  if (error || !data) return <ErrorState message={error?.message ?? 'Seat map unavailable'} onRetry={() => reload()} />

  const free = data.seats.filter((s) => !s.occupied).length
  const rows = [data.seats.slice(0, 5), data.seats.slice(5, 10)]

  return (
    <>
      <PageHeader title={`Seat map · ${data.trainNo}`} crumbs={[{ label: 'Passenger', to: '/passenger' }, { label: 'Search trains', to: '/passenger/trains' }]}
        description={`${data.trainName}. Each train has ${data.capacity} seats, stored as a fixed array: seat n is index n-1.`}
        actions={<>
          <select className="input w-auto" value={trainNo} aria-label="Choose train" onChange={(e) => navigate(`/passenger/trains/${e.target.value}/seats`)}>
            {(trains.data ?? [{ trainNo, name: data.trainName }]).map((t) => <option key={t.trainNo} value={t.trainNo}>{t.trainNo} {t.name}</option>)}
          </select>
          {data.bookingOpen && <Link to={`/passenger/book?train=${trainNo}`} className="btn-primary"><Ticket className="h-4 w-4" /> Book</Link>}
        </>} />
      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader title="Coach S1" subtitle={`${free} of ${data.capacity} seats free${data.bookingOpen ? '' : ' · booking closed'}`} />
          <div className="overflow-x-auto p-6">
            <div className="mx-auto w-fit rounded-[28px] border-2 border-slate-300 p-5 dark:border-navy-700">
              <p className="mb-3 text-center text-[11px] font-semibold uppercase tracking-wider muted">Engine side</p>
              {rows.map((row, r) => (
                <div key={r}>
                  <div className="flex gap-3">
                    {row.map((s) => (
                      <div key={s.seatNo} title={`Seat ${s.seatNo}: ${s.mine ? 'your seat' : s.occupied ? 'booked' : 'free'}`}
                        className={`flex h-16 w-14 flex-col items-center justify-center rounded-xl border-2 text-xs font-semibold ${s.mine
                          ? 'border-teal-600 bg-teal-600 text-white'
                          : s.occupied ? 'border-slate-300 bg-slate-200 text-slate-500 dark:border-navy-700 dark:bg-navy-800 dark:text-slate-400'
                          : 'border-green-500 bg-green-50 text-green-800 dark:bg-green-500/10 dark:text-green-300'}`}>
                        <Armchair className="h-5 w-5" aria-hidden />S{s.seatNo}
                      </div>
                    ))}
                  </div>
                  {r === 0 && <div className="my-3 h-6 rounded bg-slate-100 text-center text-[10px] leading-6 tracking-wider muted dark:bg-navy-950">AISLE</div>}
                </div>
              ))}
            </div>
          </div>
          <div className="flex flex-wrap justify-center gap-5 border-t border-slate-100 px-5 py-3 text-xs dark:border-navy-800">
            <span className="flex items-center gap-1.5"><span className="h-3 w-3 rounded border-2 border-green-500 bg-green-50" /> Free</span>
            <span className="flex items-center gap-1.5"><span className="h-3 w-3 rounded bg-slate-300" /> Booked</span>
            <span className="flex items-center gap-1.5"><span className="h-3 w-3 rounded bg-teal-600" /> Your seat</span>
          </div>
        </Card>
        <Card>
          <CardHeader title="Waiting list" subtitle="First come, first served (a FIFO queue)" />
          <div className="space-y-3 p-5 text-sm">
            <p><span className="text-3xl font-bold tabular-nums text-navy-900 dark:text-white">{data.waitingCount}</span> <span className="muted">passengers waiting</span></p>
            {data.myWaitingPositions.length > 0
              ? <p className="rounded-lg bg-amber-50 p-3 text-amber-800 dark:bg-amber-500/10 dark:text-amber-200">You are at position{data.myWaitingPositions.length > 1 ? 's' : ''} <b>{data.myWaitingPositions.map((p) => `WL/${p}`).join(', ')}</b>.</p>
              : <p className="muted">You have no waitlisted tickets on this train.</p>}
            <p className="muted">When a confirmed passenger cancels, the head of the queue takes that exact seat and everyone behind moves up by one.</p>
          </div>
        </Card>
      </div>
    </>
  )
}
