import { Link, useParams } from 'react-router-dom'
import { Printer, XCircle } from 'lucide-react'
import { bookingApi } from '../../api/endpoints'
import { PnrStatusView } from '../../components/booking/PnrStatusView'
import { ErrorState } from '../../components/ui/EmptyState'
import { PageHeader } from '../../components/ui/PageHeader'
import { PageSkeleton } from '../../components/ui/Skeleton'
import { useApi } from '../../hooks/useApi'
import { useCancellation } from './useCancellation'

/** One of the passenger's own PNRs (R5), with per-passenger cancellation (R4). */
export function BookingDetail() {
  const { pnr = '' } = useParams()
  const { data, error, loading, reload, setData } = useApi(() => bookingApi.one(pnr), [pnr], 15000)
  const cancel = useCancellation((c) => setData(c.booking))

  if (loading && !data) return <PageSkeleton />
  if (error || !data) return <ErrorState message={error?.message ?? 'Booking not found'} onRetry={() => reload()} />

  return (
    <>
      <PageHeader title={`PNR ${data.pnr}`} crumbs={[{ label: 'Passenger', to: '/passenger' }, { label: 'My bookings', to: '/passenger/bookings' }]}
        actions={<>
          {data.journeyState !== 'CANCELLED' && <Link to={`/passenger/bookings/${data.pnr}/ticket`} className="btn-secondary"><Printer className="h-4 w-4" /> E-ticket</Link>}
          {data.cancellable && <button className="btn-danger" onClick={() => cancel.ask(data)}><XCircle className="h-4 w-4" /> Cancel booking</button>}
        </>} />
      <PnrStatusView booking={data} actions={data.cancellable ? (n) => data.passengers[n - 1].status !== 'CAN' && (
        <button className="btn-ghost btn-sm text-red-600" onClick={() => cancel.ask(data, n)}>Cancel this passenger</button>
      ) : undefined} />
      {cancel.dialog}
    </>
  )
}
