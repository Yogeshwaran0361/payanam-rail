import { useSearchParams } from 'react-router-dom'
import { AlertTriangle, CheckCircle2, Circle, Radio } from 'lucide-react'
import { trainApi } from '../../api/endpoints'
import type { Train } from '../../api/types'
import { PriorityBadge, TrainStatusBadge } from '../../components/ui/Badge'
import { Card, CardHeader } from '../../components/ui/Card'
import { EmptyState, ErrorState } from '../../components/ui/EmptyState'
import { PageHeader } from '../../components/ui/PageHeader'
import { PageSkeleton } from '../../components/ui/Skeleton'
import { useApi } from '../../hooks/useApi'
import { hm } from '../../lib/format'

const ORDER = ['SCHEDULED', 'WAITING_FOR_PLATFORM', 'PLATFORM_ASSIGNED', 'ARRIVED', 'DEPARTED']

/** Status steps of a train at the junction, with what has already happened ticked. */
function Progress({ t }: { t: Train }) {
  const at = ORDER.indexOf(t.status)
  const steps = [
    { label: 'Scheduled', detail: `Due ${hm(t.scheduledArrival)}` },
    { label: 'Platform requested', detail: t.requestType ? `${t.requestType.toLowerCase()} request` : 'Not yet requested' },
    { label: 'Platform assigned', detail: t.platformId ? `Platform ${t.platformId}` : 'Waiting for a free platform' },
    { label: 'Arrived', detail: t.actualArrival ? `at ${hm(t.actualArrival)}` : `expected ${hm(t.expectedArrival)}` },
    { label: 'Departed', detail: t.actualDeparture ? `at ${hm(t.actualDeparture)}` : `expected ${hm(t.expectedDeparture)}` },
  ]
  if (t.status === 'REJECTED') {
    return <p className="flex items-center gap-2 rounded-lg bg-red-50 p-3 text-sm text-red-700 dark:bg-red-500/10 dark:text-red-200"><AlertTriangle className="h-4 w-4" /> {t.liveStatus}</p>
  }
  return (
    <ol className="grid gap-4 sm:grid-cols-5">
      {steps.map((s, i) => {
        const done = i <= at
        return (
          <li key={s.label} className="flex items-start gap-2 sm:flex-col sm:items-center sm:text-center">
            {done ? <CheckCircle2 className="h-6 w-6 shrink-0 text-teal-600" /> : <Circle className="h-6 w-6 shrink-0 text-slate-300 dark:text-navy-700" />}
            <div>
              <p className={`text-sm font-medium ${done ? '' : 'muted'}`}>{s.label}</p>
              <p className="text-xs muted">{s.detail}</p>
            </div>
          </li>
        )
      })}
    </ol>
  )
}

/** Live train status for every train today; ?train= focuses one (used by the global search). */
export function LiveStatus() {
  const [params, setParams] = useSearchParams()
  const selected = params.get('train') ?? ''
  const { data, error, loading, reload } = useApi(() => trainApi.timetable(), [], 5000)
  if (loading && !data) return <PageSkeleton />
  if (error || !data) return <ErrorState message={error?.message ?? 'Unavailable'} onRetry={() => reload()} />
  const shown = selected ? data.filter((t) => t.trainNo === selected) : data

  return (
    <>
      <PageHeader title="Live train status" crumbs={[{ label: 'Passenger', to: '/passenger' }]}
        description="Updates every few seconds from the station control room."
        actions={
          <select className="input w-auto" value={selected} aria-label="Train" onChange={(e) => setParams(e.target.value ? { train: e.target.value } : {})}>
            <option value="">All trains</option>
            {data.map((t) => <option key={t.trainNo} value={t.trainNo}>{t.trainNo} {t.name}</option>)}
          </select>} />
      {shown.length === 0 ? <div className="card"><EmptyState icon={Radio} title="Train not found" /></div> : (
        <div className="space-y-4">
          {shown.map((t) => (
            <Card key={t.trainNo}>
              <CardHeader title={`${t.trainNo} ${t.name}`} subtitle={`${t.origin} → ${t.destination}`}
                action={<div className="flex gap-2"><PriorityBadge priority={t.priority} /><TrainStatusBadge status={t.status} /></div>} />
              <div className="grid gap-4 p-5 md:grid-cols-[220px_1fr]">
                <div>
                  <p className={`text-lg font-semibold ${t.delayMinutes > 0 ? 'text-amber-600' : 'text-teal-700 dark:text-teal-300'}`}>{t.liveStatus}</p>
                  <p className="mt-1 text-sm muted">Platform <span className="font-semibold text-slate-700 dark:text-slate-200">{t.platformId ?? 'TBA'}</span></p>
                  <p className="text-sm muted">Arr {hm(t.expectedArrival)}{t.delayMinutes > 0 && <s className="ml-1">{hm(t.scheduledArrival)}</s>} · Dep {hm(t.expectedDeparture)}</p>
                  {t.delayReason && <p className="mt-1 text-xs text-amber-700 dark:text-amber-300">Reason: {t.delayReason}</p>}
                </div>
                <Progress t={t} />
              </div>
            </Card>
          ))}
        </div>
      )}
    </>
  )
}
