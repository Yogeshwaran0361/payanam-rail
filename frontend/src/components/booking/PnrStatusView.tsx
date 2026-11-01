import type { Booking } from '../../api/types'
import { dateTime, rupees } from '../../lib/format'
import { Badge, TicketBadge } from '../ui/Badge'
import { JourneyHeader } from './JourneyHeader'
import { TicketTimeline } from './TicketTimeline'

const STATE_TONE = { UPCOMING: 'teal', COMPLETED: 'grey', CANCELLED: 'red' } as const

/** Full PNR status: journey, each passenger's current status and how it changed over time. */
export function PnrStatusView({ booking, actions }: { booking: Booking; actions?: (passengerNo: number) => React.ReactNode }) {
  return (
    <div className="space-y-5">
      <div className="card card-pad">
        <div className="mb-4 flex flex-wrap items-center gap-3">
          <p className="text-sm muted">PNR</p>
          <p className="font-mono text-xl font-bold tracking-wider text-navy-900 dark:text-white">{booking.pnr}</p>
          <Badge tone={STATE_TONE[booking.journeyState]}>{booking.journeyState.toLowerCase()}</Badge>
          <span className="text-sm muted">{booking.summary}</span>
          <span className="ml-auto text-xs muted">Booked {dateTime(booking.bookedAt)}{booking.bookedBy ? ` by ${booking.bookedBy}` : ''}</span>
        </div>
        <JourneyHeader booking={booking} />
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        {booking.passengers.map((t) => (
          <div key={t.passengerNo} className={`card card-pad ${t.status === 'CAN' ? 'opacity-75' : ''}`}>
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="text-xs font-semibold uppercase tracking-wider muted">Passenger {t.passengerNo}</p>
                <p className="mt-0.5 font-semibold">{t.name ?? 'Hidden for privacy'}</p>
                <p className="text-sm muted">{t.age} yrs · {t.gender.toLowerCase()}</p>
              </div>
              <div className="text-right">
                <TicketBadge label={t.label} />
                {t.promoted && <p className="mt-1 text-xs font-medium text-green-600">Promoted from WL</p>}
              </div>
            </div>
            <div className="mt-4"><TicketTimeline steps={t.timeline} /></div>
            {actions && <div className="mt-4 border-t border-slate-100 pt-3 dark:border-navy-800">{actions(t.passengerNo)}</div>}
          </div>
        ))}
      </div>
      <p className="text-right text-sm muted">
        Fare {rupees(booking.farePerPassenger)} per passenger · total for active tickets <span className="font-semibold text-slate-700 dark:text-slate-200">{rupees(booking.totalFare)}</span>
      </p>
    </div>
  )
}
