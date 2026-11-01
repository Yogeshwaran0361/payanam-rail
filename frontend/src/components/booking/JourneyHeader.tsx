import { ArrowRight, MapPin } from 'lucide-react'
import type { Booking } from '../../api/types'
import { dayMonth, hm } from '../../lib/format'
import { TrainStatusBadge } from '../ui/Badge'

/** Train, route, times and platform at the top of a booking or ticket. */
export function JourneyHeader({ booking }: { booking: Booking }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-4">
      <div>
        <p className="text-sm muted"><span className="font-mono font-semibold text-navy-900 dark:text-white">{booking.trainNo}</span> · {booking.trainName}</p>
        <div className="mt-1 flex items-center gap-3 text-lg font-semibold">
          <span>{booking.origin}</span><ArrowRight className="h-4 w-4 text-slate-400" aria-hidden /><span>{booking.destination}</span>
        </div>
        <p className="mt-1 text-sm muted">
          At Payanam Junction {dayMonth(booking.arrival)}: arrives <span className="font-medium tabular-nums text-slate-700 dark:text-slate-200">{hm(booking.arrival)}</span>,
          departs <span className="font-medium tabular-nums text-slate-700 dark:text-slate-200">{hm(booking.departure)}</span>
          {booking.delayMinutes > 0 && <span className="ml-1 font-medium text-amber-600">(delayed {booking.delayMinutes} min)</span>}
        </p>
      </div>
      <div className="flex items-center gap-3">
        <div className="rounded-xl bg-navy-50 px-4 py-2 text-center dark:bg-navy-800">
          <p className="flex items-center justify-center gap-1 text-[11px] font-semibold uppercase tracking-wider muted"><MapPin className="h-3 w-3" /> Platform</p>
          <p className="text-xl font-bold text-navy-900 dark:text-white">{booking.platformId ?? 'TBA'}</p>
        </div>
        <TrainStatusBadge status={booking.trainStatus} />
      </div>
    </div>
  )
}
