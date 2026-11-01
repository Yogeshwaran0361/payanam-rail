import { Link } from 'react-router-dom'
import { Bell, CalendarCheck, Clock3, FileSearch, ListOrdered, Radio, Search, Ticket, TicketCheck, TicketX } from 'lucide-react'
import { bookingApi } from '../../api/endpoints'
import { JourneyHeader } from '../../components/booking/JourneyHeader'
import { TicketBadge } from '../../components/ui/Badge'
import { Card, CardHeader } from '../../components/ui/Card'
import { EmptyState, ErrorState } from '../../components/ui/EmptyState'
import { KpiCard } from '../../components/ui/KpiCard'
import { PageHeader } from '../../components/ui/PageHeader'
import { PageSkeleton } from '../../components/ui/Skeleton'
import { useApi } from '../../hooks/useApi'
import { dateTime, dayMonth, hm } from '../../lib/format'

const QUICK = [
  { to: '/passenger/book', label: 'Book a ticket', icon: Ticket },
  { to: '/passenger/trains', label: 'Search trains', icon: Search },
  { to: '/passenger/pnr', label: 'PNR status', icon: FileSearch },
  { to: '/passenger/live', label: 'Live status', icon: Radio },
]

/** Passenger home: ticket counts, the next journey, recent status changes and notifications. */
export function PassengerDashboard() {
  const { data, error, loading, reload } = useApi(() => bookingApi.dashboard(), [], 15000)
  if (loading && !data) return <PageSkeleton />
  if (error || !data) return <ErrorState message={error?.message ?? 'Could not load the dashboard'} onRetry={() => reload()} />
  const next = data.nextJourney

  return (
    <>
      <PageHeader title={`Welcome, ${data.fullName.split(' ')[0]}`} description="Your journeys from Payanam Junction at a glance." />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <KpiCard label="Upcoming journeys" value={data.upcomingJourneys} icon={CalendarCheck} accent="navy" />
        <KpiCard label="Confirmed tickets" value={data.confirmedTickets} icon={TicketCheck} accent="green" />
        <KpiCard label="Waitlisted tickets" value={data.waitlistedTickets} icon={ListOrdered} accent="amber" />
        <KpiCard label="Cancelled tickets" value={data.cancelledTickets} icon={TicketX} accent="grey" />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-3">
        <div className="space-y-6 xl:col-span-2">
          <Card>
            <CardHeader title="Next journey" icon={<Clock3 className="h-5 w-5" />}
              action={next && <Link to={`/passenger/bookings/${next.pnr}/ticket`} className="btn-secondary btn-sm">View e-ticket</Link>} />
            {next ? (
              <div className="p-5">
                <JourneyHeader booking={next} />
                <div className="mt-4 flex flex-wrap items-center gap-2 border-t border-slate-100 pt-4 dark:border-navy-800">
                  <span className="text-sm muted">PNR <span className="font-mono font-semibold text-slate-700 dark:text-slate-200">{next.pnr}</span></span>
                  {next.passengers.map((p) => <TicketBadge key={p.passengerNo} label={p.label} />)}
                </div>
              </div>
            ) : (
              <EmptyState icon={Ticket} title="No upcoming journeys" message="Book a ticket to see it here."
                action={<Link to="/passenger/book" className="btn-primary">Book a ticket</Link>} />
            )}
          </Card>

          <Card>
            <CardHeader title="Recent activity" subtitle="Waiting-list moves, promotions and cancellations on your tickets" />
            {data.recentActivity.length === 0 ? (
              <p className="px-5 py-6 text-sm muted">Nothing has changed on your tickets yet.</p>
            ) : (
              <ul className="divide-y divide-slate-100 dark:divide-navy-800">
                {data.recentActivity.map((a, i) => (
                  <li key={i} className="flex items-start gap-3 px-5 py-3 text-sm">
                    <span className="mt-0.5 w-24 shrink-0 tabular-nums muted">{dayMonth(a.at)} {hm(a.at)}</span>
                    <span className="flex-1">{a.text}</span>
                    <Link to={`/passenger/pnr?pnr=${a.pnr}`} className="font-mono text-xs text-teal-600 hover:underline">{a.pnr}</Link>
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </div>

        <div className="space-y-6">
          <Card>
            <CardHeader title="Quick actions" />
            <div className="grid grid-cols-2 gap-3 p-5">
              {QUICK.map((q) => (
                <Link key={q.to} to={q.to}
                  className="flex flex-col items-center gap-2 rounded-xl border border-slate-200 p-4 text-center text-sm font-medium transition hover:border-teal-400 hover:bg-teal-50/50 dark:border-navy-700 dark:hover:bg-teal-500/5">
                  <q.icon className="h-5 w-5 text-teal-600" aria-hidden /> {q.label}
                </Link>
              ))}
            </div>
          </Card>
          <Card>
            <CardHeader title="Notifications" icon={<Bell className="h-5 w-5" />}
              subtitle={data.unreadNotifications ? `${data.unreadNotifications} unread` : 'All read'}
              action={<Link to="/passenger/notifications" className="text-sm font-medium text-teal-600 hover:underline">View all</Link>} />
            <ul className="divide-y divide-slate-100 dark:divide-navy-800">
              {data.notifications.map((n) => (
                <li key={n.id} className="px-5 py-3">
                  <p className={`text-sm ${n.read ? '' : 'font-semibold'}`}>{n.title}</p>
                  <p className="text-xs muted">{n.message}</p>
                  <p className="mt-0.5 text-[11px] muted">{dateTime(n.createdAt)}</p>
                </li>
              ))}
              {data.notifications.length === 0 && <li className="px-5 py-6 text-sm muted">No notifications yet.</li>}
            </ul>
          </Card>
        </div>
      </div>
    </>
  )
}
