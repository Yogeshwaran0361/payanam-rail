import { useMemo, useState, type FormEvent } from 'react'
import { CheckCircle2, Clock, Send, XCircle } from 'lucide-react'
import { toApiError } from '../../api/client'
import { stationApi } from '../../api/endpoints'
import type { Allocation, ArrivalType } from '../../api/types'
import { Badge, PriorityBadge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { Card, CardHeader } from '../../components/ui/Card'
import { SelectField } from '../../components/ui/Field'
import { PageHeader } from '../../components/ui/PageHeader'
import { useToast } from '../../context/ToastContext'
import { useApi } from '../../hooks/useApi'
import { hm } from '../../lib/format'
import { outcomeTone } from '../../lib/status'

const DEFAULT_TYPE: Record<string, ArrivalType> = { EMERGENCY: 'EMERGENCY', CONNECTION: 'CONNECTION', NORMAL: 'SCHEDULED' }
const ICON = { ASSIGNED: CheckCircle2, QUEUED: Clock, REJECTED: XCircle }

/** Create an arrival request: the allocator answers Assigned P#, Queued (with position) or Rejected (with reason). */
export function ArrivalRequests() {
  const toast = useToast()
  const trains = useApi(() => stationApi.trains(), [], 5000)
  const events = useApi(() => stationApi.events(200).then((e) => e.filter((x) => x.category === 'PLATFORM' && x.trainNo).reverse().slice(0, 12)), [], 5000)
  const [trainNo, setTrainNo] = useState('')
  const [type, setType] = useState<ArrivalType | ''>('')
  const [busy, setBusy] = useState(false)
  const [results, setResults] = useState<Allocation[]>([])

  const open = useMemo(() => (trains.data ?? []).filter((t) => t.status === 'SCHEDULED'), [trains.data])
  const selected = (trains.data ?? []).find((t) => t.trainNo === trainNo)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    if (!trainNo) return
    setBusy(true)
    try {
      const r = await stationApi.arrivalRequest(trainNo, type || undefined)
      setResults((list) => [r, ...list].slice(0, 6))
      toast.push(r.outcome === 'REJECTED' ? 'error' : r.outcome === 'QUEUED' ? 'warning' : 'success', `${r.trainNo}: ${r.outcome}`, r.message)
      setTrainNo('')
      setType('')
      trains.reload(true)
      events.reload(true)
    } catch (err) {
      toast.push('error', 'Request refused', toApiError(err).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <PageHeader title="Arrival Requests" crumbs={[{ label: 'Station', to: '/station' }]}
        description="Ask the allocator for a platform. It picks a free platform that is long enough and has no overlapping interval (P1); otherwise the train is queued (P3) or rejected when no platform is long enough (P2)." />
      <div className="grid gap-6 lg:grid-cols-5">
        <Card className="lg:col-span-2">
          <CardHeader title="New request" icon={<Send className="h-5 w-5" />} />
          <form onSubmit={submit} className="space-y-4 p-5">
            <SelectField label="Train (not yet requested)" value={trainNo} onChange={(e) => { setTrainNo(e.target.value); setType('') }}>
              <option value="">Select a train</option>
              {open.map((t) => <option key={t.trainNo} value={t.trainNo}>{t.trainNo} {t.name} · {hm(t.expectedArrival)} · {t.lengthMeters} m</option>)}
            </SelectField>
            {selected && (
              <div className="rounded-lg bg-slate-50 p-3 text-sm dark:bg-navy-950/50">
                <p>Declared priority <PriorityBadge priority={selected.priority} /></p>
                <p className="mt-1 muted">Arr {hm(selected.expectedArrival)} · dep {hm(selected.expectedDeparture)} · {selected.lengthMeters} m</p>
              </div>
            )}
            <SelectField label="Request type" value={type || (selected ? DEFAULT_TYPE[selected.priority] : '')} onChange={(e) => setType(e.target.value as ArrivalType)}>
              <option value="SCHEDULED">Scheduled arrival</option>
              <option value="CONNECTION">Connection (rank 1)</option>
              <option value="EMERGENCY">Emergency (rank 0)</option>
            </SelectField>
            <Button type="submit" loading={busy} disabled={!trainNo} className="w-full">Request platform</Button>
            {open.length === 0 && <p className="text-sm muted">Every train has already sent its arrival request.</p>}
          </form>
        </Card>
        <div className="space-y-6 lg:col-span-3">
          <Card>
            <CardHeader title="Results" subtitle="Answers to the requests sent from this page" />
            <ul className="divide-y divide-slate-100 dark:divide-navy-800">
              {results.map((r, i) => {
                const Icon = ICON[r.outcome]
                return (
                  <li key={i} className="flex items-start gap-3 px-5 py-4">
                    <Icon className={`mt-0.5 h-5 w-5 ${r.outcome === 'ASSIGNED' ? 'text-green-600' : r.outcome === 'QUEUED' ? 'text-amber-500' : 'text-red-600'}`} />
                    <div className="flex-1">
                      <p className="font-semibold"><span className="font-mono">{r.trainNo}</span> <Badge tone={outcomeTone(r.outcome)}>{r.outcome}{r.platformId && ` ${r.platformId}`}{r.outcome === 'QUEUED' && ` #${r.queuePosition}`}</Badge></p>
                      <p className="mt-0.5 text-sm muted">{r.message}</p>
                    </div>
                  </li>
                )
              })}
              {results.length === 0 && <li className="px-5 py-8 text-center text-sm muted">Send a request to see the allocator's answer here.</li>}
            </ul>
          </Card>
          <Card>
            <CardHeader title="Recent platform decisions" subtitle="From the station event log" />
            <ul className="divide-y divide-slate-100 dark:divide-navy-800">
              {(events.data ?? []).map((e) => (
                <li key={e.sequence} className={`px-5 py-2.5 text-sm ${e.alert ? 'text-amber-700 dark:text-amber-300' : ''}`}>
                  <span className="mr-2 tabular-nums muted">{hm(e.time)}</span>{e.message}
                </li>
              ))}
            </ul>
          </Card>
        </div>
      </div>
    </>
  )
}
