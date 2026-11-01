import { useEffect, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { ArrowUpCircle, Armchair, ListOrdered, Search, X } from 'lucide-react'
import { toApiError } from '../../api/client'
import { stationApi, trainApi } from '../../api/endpoints'
import type { Booking } from '../../api/types'
import { PnrStatusView } from '../../components/booking/PnrStatusView'
import { TrainStatusBadge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { Card, CardHeader } from '../../components/ui/Card'
import { ErrorState } from '../../components/ui/EmptyState'
import { ConfirmDialog } from '../../components/ui/Modal'
import { PageHeader } from '../../components/ui/PageHeader'
import { TableSkeleton } from '../../components/ui/Skeleton'
import { useToast } from '../../context/ToastContext'
import { useApi } from '../../hooks/useApi'
import { dateTime, hm } from '../../lib/format'

type CancelTarget = { pnr: string; passengerNo?: number; label: string }

/**
 * Reservations Management for the booking counter: per-train confirmed seats and the FIFO waiting list,
 * PNR lookup, manual cancellation (with automatic WL promotion) and the promotion history.
 */
export function Reservations() {
  const toast = useToast()
  const [params, setParams] = useSearchParams()
  const trains = useApi(() => trainApi.timetable())
  const [trainNo, setTrainNo] = useState('')
  const view = useApi(() => (trainNo ? stationApi.reservations(trainNo) : Promise.resolve(null)), [trainNo], 5000)
  const [pnrInput, setPnrInput] = useState(params.get('pnr') ?? '')
  const [found, setFound] = useState<Booking | null>(null)
  const [lookupError, setLookupError] = useState<string | null>(null)
  const [target, setTarget] = useState<CancelTarget | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!trainNo && trains.data?.length) setTrainNo(trains.data[0].trainNo)
  }, [trains.data, trainNo])

  const lookup = async (pnr: string) => {
    setLookupError(null)
    try {
      const b = await stationApi.staffPnr(pnr)
      setFound(b)
      setTrainNo(b.trainNo)
    } catch (e) {
      setFound(null)
      setLookupError(toApiError(e).message)
    }
  }
  const urlPnr = params.get('pnr')
  useEffect(() => {
    if (urlPnr) lookup(urlPnr)
  }, [urlPnr])

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (!/^\d{10}$/.test(pnrInput)) return setLookupError('A PNR has 10 digits')
    setParams({ pnr: pnrInput })
    if (pnrInput === urlPnr) lookup(pnrInput)
  }

  const cancel = async () => {
    if (!target) return
    setBusy(true)
    try {
      const c = target.passengerNo ? await stationApi.staffCancelPassenger(target.pnr, target.passengerNo) : await stationApi.staffCancel(target.pnr)
      toast.push('success', 'Cancelled at the counter', c.message)
      if (found?.pnr === c.pnr) setFound(c.booking)
      view.reload(true)
      trains.reload(true)
      setTarget(null)
    } catch (e) {
      toast.push('error', 'Could not cancel', toApiError(e).message)
    } finally {
      setBusy(false)
    }
  }

  const v = view.data
  return (
    <>
      <PageHeader title="Reservations" crumbs={[{ label: 'Station', to: '/station' }]}
        description="Counter view of every train's 10 seats and its waiting list. Cancelling a confirmed ticket gives the seat to the oldest waiting passenger at once." />

      <form onSubmit={submit} className="card card-pad mb-6 flex flex-wrap items-end gap-3">
        <div className="min-w-[220px] flex-1">
          <label htmlFor="staff-pnr" className="label">Find a PNR</label>
          <input id="staff-pnr" className="input font-mono tracking-widest" inputMode="numeric" maxLength={10} placeholder="10-digit PNR"
            value={pnrInput} onChange={(e) => setPnrInput(e.target.value.replace(/\D/g, ''))} />
        </div>
        <Button type="submit"><Search className="h-4 w-4" /> Look up</Button>
        {found && <button type="button" className="btn-ghost" onClick={() => { setFound(null); setParams({}) }}><X className="h-4 w-4" /> Clear</button>}
        {lookupError && <p className="field-error w-full">{lookupError}</p>}
      </form>

      {found && (
        <div className="mb-8">
          <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
            <h2 className="text-lg font-semibold">PNR {found.pnr}</h2>
            {found.cancellable && <button className="btn-danger btn-sm" onClick={() => setTarget({ pnr: found.pnr, label: `all active passengers of PNR ${found.pnr}` })}>Cancel whole PNR</button>}
          </div>
          <PnrStatusView booking={found} actions={found.cancellable ? (n) => found.passengers[n - 1].status !== 'CAN' && (
            <button className="btn-ghost btn-sm text-red-600" onClick={() => setTarget({ pnr: found.pnr, passengerNo: n, label: `${found.passengers[n - 1].name} (${found.passengers[n - 1].label})` })}>Cancel this passenger</button>
          ) : undefined} />
        </div>
      )}

      <div className="mb-4 flex flex-wrap gap-2" role="tablist" aria-label="Train">
        {(trains.data ?? []).map((t) => (
          <button key={t.trainNo} role="tab" aria-selected={trainNo === t.trainNo} onClick={() => setTrainNo(t.trainNo)}
            className={`rounded-lg border px-3 py-1.5 text-sm font-medium ${trainNo === t.trainNo ? 'border-teal-600 bg-teal-600 text-white' : 'border-slate-200 bg-white hover:border-teal-400 dark:border-navy-700 dark:bg-navy-900'}`}>
            {t.trainNo} <span className="opacity-80">{t.confirmed}/10{t.waiting > 0 && ` +${t.waiting} WL`}</span>
          </button>
        ))}
      </div>

      {view.error ? <ErrorState message={view.error.message} onRetry={() => view.reload()} /> : !v ? <TableSkeleton /> : (
        <div className="grid gap-6 xl:grid-cols-2">
          <Card>
            <CardHeader title={`Confirmed seats · ${v.trainNo} ${v.trainName}`} icon={<Armchair className="h-5 w-5" />}
              subtitle={`${v.confirmed.length} of ${v.capacity} seats`} action={<TrainStatusBadge status={v.trainStatus} />} />
            <div className="overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead className="table-head"><tr>{['Seat', 'Passenger', 'Age', 'PNR', 'Booked by', ''].map((h) => <th key={h} className="px-4 py-2">{h}</th>)}</tr></thead>
                <tbody className="divide-y divide-slate-100 dark:divide-navy-800">
                  {v.confirmed.map((c) => (
                    <tr key={c.ticketId}>
                      <td className="px-4 py-2 font-mono font-semibold">S{c.seatNo}</td>
                      <td className="px-4 py-2">{c.passengerName}{c.promoted && <ArrowUpCircle className="ml-1 inline h-3.5 w-3.5 text-green-600" aria-label="promoted from WL" />}</td>
                      <td className="px-4 py-2">{c.age}</td>
                      <td className="px-4 py-2"><button className="font-mono text-teal-700 hover:underline dark:text-teal-300" onClick={() => setParams({ pnr: c.pnr })}>{c.pnr}</button></td>
                      <td className="px-4 py-2 muted">{c.bookedBy}</td>
                      <td className="px-4 py-2 text-right">
                        {v.trainStatus !== 'ARRIVED' && v.trainStatus !== 'DEPARTED' && (
                          <button className="btn-ghost btn-sm text-red-600" onClick={() => setTarget({ pnr: c.pnr, passengerNo: Number(c.ticketId.split('/')[1]), label: `${c.passengerName} (S${c.seatNo})` })}>Cancel</button>
                        )}
                      </td>
                    </tr>
                  ))}
                  {Array.from({ length: v.capacity - v.confirmed.length }, (_, i) => (
                    <tr key={`free-${i}`}><td colSpan={6} className="px-4 py-2 text-sm text-green-700 dark:text-green-400">Free seat</td></tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Card>
          <div className="space-y-6">
            <Card>
              <CardHeader title="Waiting list (FIFO)" icon={<ListOrdered className="h-5 w-5" />} subtitle="The head of the queue gets the next freed seat" />
              <ol className="divide-y divide-slate-100 dark:divide-navy-800">
                {v.waiting.map((w) => (
                  <li key={w.ticketId} className="flex items-center gap-3 px-5 py-2.5 text-sm">
                    <span className="w-14 font-mono font-semibold text-amber-700 dark:text-amber-300">WL/{w.position}</span>
                    <span className="flex-1">{w.passengerName} <span className="muted">· PNR {w.pnr} · {dateTime(w.bookedAt)}</span></span>
                    <button className="btn-ghost btn-sm text-red-600" onClick={() => setTarget({ pnr: w.pnr, passengerNo: Number(w.ticketId.split('/')[1]), label: `${w.passengerName} (WL/${w.position})` })}>Cancel</button>
                  </li>
                ))}
                {v.waiting.length === 0 && <li className="px-5 py-6 text-center text-sm muted">Nobody is waiting on this train.</li>}
              </ol>
            </Card>
            <Card>
              <CardHeader title="Promotion history" icon={<ArrowUpCircle className="h-5 w-5" />} />
              <ul className="divide-y divide-slate-100 dark:divide-navy-800">
                {v.promotions.map((p) => <li key={p.sequence} className="px-5 py-2.5 text-sm"><span className="mr-2 tabular-nums muted">{hm(p.time)}</span>{p.message}</li>)}
                {v.promotions.length === 0 && <li className="px-5 py-6 text-center text-sm muted">No promotions yet on this train.</li>}
              </ul>
            </Card>
          </div>
        </div>
      )}
      <ConfirmDialog open={!!target} danger loading={busy} onClose={() => setTarget(null)} onConfirm={cancel}
        title="Cancel at the counter?" confirmLabel="Cancel ticket"
        message={<>This cancels {target?.label}. A freed confirmed seat goes to the first passenger on the waiting list, and the passenger is notified. This cannot be undone.</>} />
    </>
  )
}
