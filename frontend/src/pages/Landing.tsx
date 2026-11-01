import { Link } from 'react-router-dom'
import {
  ArrowRight, BellRing, CalendarClock, FileSearch, Gauge, Layers, ListOrdered, ShieldCheck, Ticket, TrainFront, UserPlus,
} from 'lucide-react'
import { trainApi } from '../api/endpoints'
import { RailwayIllustration } from '../components/brand/RailwayIllustration'
import { PriorityBadge } from '../components/ui/Badge'
import { Skeleton } from '../components/ui/Skeleton'
import { useApi } from '../hooks/useApi'
import { useDocumentTitle } from '../hooks/useDocumentTitle'
import { hm } from '../lib/format'

const FEATURES = [
  { icon: Ticket, title: 'Instant reservations', text: 'Book up to 4 passengers on one PNR. Seats are confirmed while they last, then you join a fair first-come waiting list.' },
  { icon: ListOrdered, title: 'Automatic WL promotion', text: 'When someone cancels, the first waitlisted passenger moves into the freed seat at once, and you are notified.' },
  { icon: Layers, title: 'Smart platform allocation', text: 'Each arriving train gets the smallest platform that fits it, with a 10-minute clearance buffer between trains.' },
  { icon: Gauge, title: 'Priority-aware control room', text: 'Emergency trains go first, then connections, then scheduled arrivals. Every decision is explained on the board.' },
  { icon: BellRing, title: 'Live status & alerts', text: 'Delays, platform changes and promotions reach passengers and staff as they happen.' },
  { icon: ShieldCheck, title: 'Secure by design', text: 'JWT sessions, BCrypt passwords, account lockout and role-based access for passengers and staff.' },
]

const STEPS = [
  { icon: UserPlus, title: 'Create your account', text: 'Sign up as a passenger in under a minute.' },
  { icon: TrainFront, title: 'Choose a train', text: "Search today's timetable and see seats left." },
  { icon: Ticket, title: 'Book and get your PNR', text: 'Add passengers, confirm, and download the e-ticket.' },
  { icon: FileSearch, title: 'Track your journey', text: 'Follow PNR status, WL movement and your platform.' },
]

export function Landing() {
  useDocumentTitle('Railway reservations and platform allocation')
  const stats = useApi(() => trainApi.stats())
  const trains = useApi(() => trainApi.timetable())

  return (
    <>
      {/* hero */}
      <section className="relative overflow-hidden bg-gradient-to-b from-navy-900 to-navy-800 text-white">
        <div className="mx-auto grid max-w-7xl items-center gap-10 px-4 py-16 sm:px-6 lg:grid-cols-2 lg:py-24">
          <div>
            <p className="inline-flex items-center gap-2 rounded-full bg-white/10 px-3 py-1 text-xs font-semibold text-teal-200">
              <span className="h-1.5 w-1.5 rounded-full bg-teal-300" /> Payanam Junction is live
            </p>
            <h1 className="mt-5 text-4xl font-bold leading-tight tracking-tight text-white sm:text-5xl">
              Every seat booked fairly.<br />Every train on the right platform.
            </h1>
            <p className="mt-5 max-w-xl text-lg text-navy-100">
              Payanam Rail runs reservations and platform allocation for Payanam Junction, from your booking to the
              moment your train pulls in.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to="/signin" className="btn-primary px-5 py-2.5">Book a ticket <ArrowRight className="h-4 w-4" /></Link>
              <Link to="/pnr" className="btn bg-white/10 px-5 py-2.5 text-white hover:bg-white/20">Check PNR status</Link>
              <Link to="/signin?as=staff" className="btn px-5 py-2.5 text-navy-100 hover:text-white">Staff sign in</Link>
            </div>
          </div>
          <RailwayIllustration className="w-full drop-shadow-2xl" />
        </div>
      </section>

      {/* live stats */}
      <section className="relative z-10 mx-auto -mt-10 max-w-7xl px-4 sm:px-6" aria-label="Station today">
        <div className="card grid grid-cols-2 divide-slate-200 md:grid-cols-4 md:divide-x dark:divide-navy-800">
          {[
            { label: 'Trains today', value: stats.data?.trains },
            { label: 'Platforms', value: stats.data?.platforms },
            { label: 'Bookings made', value: stats.data?.bookings },
            { label: 'Seat occupancy', value: stats.data ? `${stats.data.occupancyPercent}%` : undefined },
          ].map((s) => (
            <div key={s.label} className="p-6 text-center">
              {s.value === undefined ? <Skeleton className="mx-auto h-8 w-16" /> : (
                <p className="text-3xl font-bold tabular-nums text-navy-900 dark:text-white">{s.value}</p>
              )}
              <p className="mt-1 text-sm muted">{s.label}</p>
            </div>
          ))}
        </div>
      </section>

      {/* features */}
      <section id="features" className="mx-auto max-w-7xl scroll-mt-20 px-4 py-20 sm:px-6">
        <p className="text-sm font-semibold uppercase tracking-wider text-teal-600">Features</p>
        <h2 className="mt-2 text-3xl font-bold tracking-tight">Built for passengers and the control room</h2>
        <div className="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {FEATURES.map((f) => (
            <div key={f.title} className="card card-pad transition hover:-translate-y-0.5 hover:shadow-lift">
              <div className="inline-flex rounded-xl bg-teal-50 p-2.5 text-teal-700 dark:bg-teal-500/15 dark:text-teal-300">
                <f.icon className="h-5 w-5" aria-hidden />
              </div>
              <h3 className="mt-4 font-semibold">{f.title}</h3>
              <p className="mt-1.5 text-sm muted">{f.text}</p>
            </div>
          ))}
        </div>
      </section>

      {/* how it works */}
      <section id="how" className="scroll-mt-20 bg-white py-20 dark:bg-navy-900">
        <div className="mx-auto max-w-7xl px-4 sm:px-6">
          <p className="text-sm font-semibold uppercase tracking-wider text-teal-600">How it works</p>
          <h2 className="mt-2 text-3xl font-bold tracking-tight">From sign-up to platform in four steps</h2>
          <ol className="mt-10 grid gap-6 md:grid-cols-4">
            {STEPS.map((s, i) => (
              <li key={s.title} className="relative">
                <div className="flex items-center gap-3">
                  <span className="flex h-10 w-10 items-center justify-center rounded-full bg-navy-800 text-sm font-bold text-white dark:bg-teal-600">
                    {i + 1}
                  </span>
                  <s.icon className="h-5 w-5 text-teal-600" aria-hidden />
                </div>
                <h3 className="mt-4 font-semibold">{s.title}</h3>
                <p className="mt-1 text-sm muted">{s.text}</p>
              </li>
            ))}
          </ol>
        </div>
      </section>

      {/* today's trains */}
      <section className="mx-auto max-w-7xl px-4 py-20 sm:px-6">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <p className="text-sm font-semibold uppercase tracking-wider text-teal-600">Today at Payanam Junction</p>
            <h2 className="mt-2 text-3xl font-bold tracking-tight">Arrivals</h2>
          </div>
          <Link to="/signin" className="btn-secondary"><CalendarClock className="h-4 w-4" /> Sign in to book</Link>
        </div>
        <div className="card mt-8 overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 dark:divide-navy-800">
            <thead className="table-head">
              <tr>
                <th className="px-4 py-2.5">Train</th><th className="px-4 py-2.5">Route</th><th className="px-4 py-2.5">Arrives</th>
                <th className="px-4 py-2.5">Departs</th><th className="px-4 py-2.5">Type</th><th className="px-4 py-2.5">Status</th>
                <th className="px-4 py-2.5 text-right">Seats left</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-navy-800">
              {(trains.data ?? []).map((t) => (
                <tr key={t.trainNo}>
                  <td className="table-cell"><span className="font-mono font-semibold">{t.trainNo}</span> {t.name}</td>
                  <td className="table-cell muted">{t.origin} → {t.destination}</td>
                  <td className="table-cell tabular-nums">{hm(t.expectedArrival)}</td>
                  <td className="table-cell tabular-nums">{hm(t.expectedDeparture)}</td>
                  <td className="table-cell"><PriorityBadge priority={t.priority} /></td>
                  <td className="table-cell text-sm">{t.liveStatus}</td>
                  <td className="table-cell text-right tabular-nums">{t.bookingOpen ? t.seatsLeft : '–'}</td>
                </tr>
              ))}
              {trains.loading && <tr><td colSpan={7} className="p-4"><Skeleton className="h-24 w-full" /></td></tr>}
            </tbody>
          </table>
        </div>
      </section>

      {/* call to action */}
      <section className="mx-auto max-w-7xl px-4 pb-20 sm:px-6">
        <div className="flex flex-col items-start justify-between gap-6 rounded-2xl bg-gradient-to-r from-teal-700 to-navy-800 p-8 text-white md:flex-row md:items-center">
          <div>
            <h2 className="text-2xl font-bold text-white">Ready to travel?</h2>
            <p className="mt-1 text-teal-100">Create a free passenger account and book your first ticket.</p>
          </div>
          <Link to="/signup" className="btn bg-white px-5 py-2.5 text-navy-900 hover:bg-teal-50">Create account <ArrowRight className="h-4 w-4" /></Link>
        </div>
      </section>
    </>
  )
}
