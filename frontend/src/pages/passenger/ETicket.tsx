import { Link, useParams } from 'react-router-dom'
import { QRCodeSVG } from 'qrcode.react'
import { ArrowLeft, Printer } from 'lucide-react'
import { bookingApi } from '../../api/endpoints'
import { Logo } from '../../components/brand/Logo'
import { TicketBadge } from '../../components/ui/Badge'
import { ErrorState } from '../../components/ui/EmptyState'
import { PageSkeleton } from '../../components/ui/Skeleton'
import { useApi } from '../../hooks/useApi'
import { useDocumentTitle } from '../../hooks/useDocumentTitle'
import { dateTime, dayMonth, hm, rupees } from '../../lib/format'

/** Printable e-ticket. "Download PDF" uses the browser's print dialog (Save as PDF); print CSS hides the app frame. */
export function ETicket() {
  const { pnr = '' } = useParams()
  const { data: b, error, loading, reload } = useApi(() => bookingApi.one(pnr), [pnr])
  useDocumentTitle(`E-ticket ${pnr}`)
  if (loading) return <PageSkeleton />
  if (error || !b) return <ErrorState message={error?.message ?? 'Ticket not found'} onRetry={() => reload()} />

  const qr = JSON.stringify({ pnr: b.pnr, train: b.trainNo, date: b.arrival.slice(0, 10),
    passengers: b.passengers.map((p) => `${p.passengerNo}:${p.label}`) })

  return (
    <>
      <div className="no-print mb-5 flex flex-wrap items-center justify-between gap-3">
        <Link to={`/passenger/bookings/${b.pnr}`} className="btn-ghost"><ArrowLeft className="h-4 w-4" /> Back to booking</Link>
        <button className="btn-primary" onClick={() => window.print()}><Printer className="h-4 w-4" /> Print / save as PDF</button>
      </div>
      <article className="card print-area mx-auto max-w-3xl overflow-hidden">
        <header className="flex items-center justify-between bg-navy-900 px-6 py-4 text-white print:bg-white print:text-black">
          <Logo light />
          <div className="text-right">
            <p className="text-xs uppercase tracking-wider text-navy-200">Electronic reservation slip</p>
            <p className="font-mono text-lg font-bold tracking-widest">PNR {b.pnr}</p>
          </div>
        </header>
        <div className="grid gap-6 p-6 sm:grid-cols-[1fr_auto]">
          <div className="space-y-4">
            <div>
              <p className="text-xs uppercase tracking-wider muted">Train</p>
              <p className="text-lg font-semibold"><span className="font-mono">{b.trainNo}</span> {b.trainName}</p>
              <p className="text-sm muted">{b.origin} → {b.destination}</p>
            </div>
            <dl className="grid grid-cols-2 gap-4 text-sm sm:grid-cols-4">
              <div><dt className="text-xs muted">Date</dt><dd className="font-semibold">{dayMonth(b.arrival)}</dd></div>
              <div><dt className="text-xs muted">Arrives Payanam Jn</dt><dd className="font-semibold tabular-nums">{hm(b.arrival)}</dd></div>
              <div><dt className="text-xs muted">Departs</dt><dd className="font-semibold tabular-nums">{hm(b.departure)}</dd></div>
              <div><dt className="text-xs muted">Platform</dt><dd className="font-semibold">{b.platformId ?? 'To be announced'}</dd></div>
            </dl>
          </div>
          <div className="flex flex-col items-center gap-1">
            <div className="rounded-lg bg-white p-2"><QRCodeSVG value={qr} size={124} level="M" /></div>
            <p className="text-[10px] muted">Scan to verify</p>
          </div>
        </div>
        <table className="min-w-full border-t border-slate-200 text-sm dark:border-navy-800">
          <thead className="table-head"><tr><th className="px-6 py-2">#</th><th className="px-6 py-2">Passenger</th><th className="px-6 py-2">Age</th><th className="px-6 py-2">Gender</th><th className="px-6 py-2">Status</th></tr></thead>
          <tbody className="divide-y divide-slate-100 dark:divide-navy-800">
            {b.passengers.map((p) => (
              <tr key={p.passengerNo}>
                <td className="px-6 py-2.5">{p.passengerNo}</td><td className="px-6 py-2.5 font-medium">{p.name}</td>
                <td className="px-6 py-2.5">{p.age}</td><td className="px-6 py-2.5">{p.gender.toLowerCase()}</td>
                <td className="px-6 py-2.5"><TicketBadge label={p.label} /></td>
              </tr>
            ))}
          </tbody>
        </table>
        <footer className="flex flex-wrap justify-between gap-3 border-t border-dashed border-slate-300 px-6 py-4 text-xs muted dark:border-navy-700">
          <span>Booked by {b.bookedBy} on {dateTime(b.bookedAt)}</span>
          <span>Fare {rupees(b.farePerPassenger)} × {b.passengers.filter((p) => p.status !== 'CAN').length} active = <b>{rupees(b.totalFare)}</b></span>
          <span className="w-full">Carry a photo ID. WL passengers are promoted automatically when a seat is freed. Platform may change; check live status.</span>
        </footer>
      </article>
    </>
  )
}
