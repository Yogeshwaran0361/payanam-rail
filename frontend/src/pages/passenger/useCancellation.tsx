import { useState } from 'react'
import { toApiError } from '../../api/client'
import { bookingApi } from '../../api/endpoints'
import type { Booking, Cancellation } from '../../api/types'
import { ConfirmDialog } from '../../components/ui/Modal'
import { useToast } from '../../context/ToastContext'

type Target = { booking: Booking; passengerNo?: number }

/**
 * Cancel a whole PNR (R3) or one passenger (R4) with a confirmation dialog. The toast reports any
 * waiting-list promotion the cancellation caused, straight from the server's answer.
 */
export function useCancellation(onDone: (c: Cancellation) => void) {
  const toast = useToast()
  const [target, setTarget] = useState<Target | null>(null)
  const [busy, setBusy] = useState(false)

  const run = async () => {
    if (!target) return
    setBusy(true)
    try {
      const c = target.passengerNo
        ? await bookingApi.cancelPassenger(target.booking.pnr, target.passengerNo)
        : await bookingApi.cancel(target.booking.pnr)
      toast.push('success', 'Cancelled', c.message)
      onDone(c)
      setTarget(null)
    } catch (e) {
      toast.push('error', 'Could not cancel', toApiError(e).message)
    } finally {
      setBusy(false)
    }
  }

  const passenger = target?.passengerNo ? target.booking.passengers[target.passengerNo - 1] : undefined
  const dialog = (
    <ConfirmDialog open={!!target} danger loading={busy} onClose={() => setTarget(null)} onConfirm={run}
      title={passenger ? `Cancel passenger ${passenger.passengerNo}?` : `Cancel PNR ${target?.booking.pnr}?`}
      confirmLabel={passenger ? 'Cancel this passenger' : 'Cancel all passengers'}
      message={passenger
        ? <>This cancels <b>{passenger.name}</b> ({passenger.label}). {passenger.status === 'CNF' && 'The seat goes to the first passenger on the waiting list.'} This cannot be undone.</>
        : <>All active passengers on this PNR will be cancelled. Confirmed seats are given to the waiting list in order. This cannot be undone.</>} />
  )
  return { ask: (booking: Booking, passengerNo?: number) => setTarget({ booking, passengerNo }), dialog }
}
