import { useEffect, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { FileSearch, Search } from 'lucide-react'
import { toApiError } from '../api/client'
import { bookingApi } from '../api/endpoints'
import type { Booking } from '../api/types'
import { PnrStatusView } from '../components/booking/PnrStatusView'
import { Button } from '../components/ui/Button'
import { EmptyState } from '../components/ui/EmptyState'
import { PageHeader } from '../components/ui/PageHeader'
import { useAuth } from '../context/AuthContext'

/**
 * PNR status. Anyone can check a PNR (names are hidden); a signed-in passenger checking their own PNR
 * sees the names too, because the server only returns them to the owner.
 */
export function PnrStatus({ publicPage = false }: { publicPage?: boolean }) {
  const [params, setParams] = useSearchParams()
  const { user } = useAuth()
  const [pnr, setPnr] = useState(params.get('pnr') ?? '')
  const [booking, setBooking] = useState<Booking | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const lookup = async (value: string) => {
    setBusy(true)
    setError(null)
    try {
      let b: Booking
      if (user?.role === 'PASSENGER') {
        b = await bookingApi.one(value).catch(() => bookingApi.publicPnr(value))
      } else {
        b = await bookingApi.publicPnr(value)
      }
      setBooking(b)
    } catch (e) {
      setBooking(null)
      setError(toApiError(e).message)
    } finally {
      setBusy(false)
    }
  }

  const current = params.get('pnr')
  useEffect(() => {
    if (current && /^\d{10}$/.test(current)) {
      setPnr(current)
      lookup(current)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [current])

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (!/^\d{10}$/.test(pnr)) {
      setError('A PNR has 10 digits')
      return
    }
    setParams({ pnr })
    if (pnr === current) lookup(pnr)
  }

  return (
    <div className={publicPage ? 'mx-auto max-w-5xl px-4 py-10 sm:px-6' : ''}>
      <PageHeader title="PNR status" crumbs={publicPage ? [] : [{ label: 'Passenger', to: '/passenger' }]}
        description="Current status of every passenger on a PNR, and how it changed: confirmations, waiting-list moves, promotions and cancellations." />
      <form onSubmit={submit} className="card card-pad flex flex-wrap items-end gap-3" noValidate>
        <div className="min-w-[220px] flex-1">
          <label htmlFor="pnr" className="label">PNR number</label>
          <input id="pnr" className={`input font-mono tracking-widest ${error ? 'input-error' : ''}`} inputMode="numeric" maxLength={10}
            placeholder="10-digit PNR" value={pnr} onChange={(e) => setPnr(e.target.value.replace(/\D/g, ''))}
            aria-invalid={!!error} aria-describedby={error ? 'pnr-err' : undefined} />
        </div>
        <Button type="submit" loading={busy}><Search className="h-4 w-4" /> Check status</Button>
        {error && <p id="pnr-err" className="field-error w-full">{error}</p>}
      </form>
      <div className="mt-6">
        {booking ? <PnrStatusView booking={booking} /> : !error && (
          <div className="card"><EmptyState icon={FileSearch} title="Enter a PNR to see its status" message="You'll find the PNR on your e-ticket and in My Bookings." /></div>
        )}
      </div>
    </div>
  )
}
