import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { AlertTriangle, ArrowRight, Clock, LogIn, LogOut, Eye } from 'lucide-react'
import { toApiError } from '../../api/client'
import { stationApi } from '../../api/endpoints'
import type { DelayImpact, OperationResult, Train } from '../../api/types'
import { Badge, PriorityBadge, TrainStatusBadge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { DataTable, type Column } from '../../components/ui/DataTable'
import { ErrorState } from '../../components/ui/EmptyState'
import { TextField } from '../../components/ui/Field'
import { Modal } from '../../components/ui/Modal'
import { PageHeader } from '../../components/ui/PageHeader'
import { useToast } from '../../context/ToastContext'
import { useApi } from '../../hooks/useApi'
import { hm } from '../../lib/format'
import { outcomeTone } from '../../lib/status'

type Action = 'arrival' | 'departure' | 'delay'
const TITLES: Record<Action, string> = { arrival: 'Confirm arrival', departure: 'Confirm departure', delay: 'Report delay' }

function OperationPreview({ r }: { r: OperationResult }) {
  return (
    <div className="space-y-3 text-sm">
      <p>{r.message}</p>
      {r.allocations.length > 0 && (
        <div>
          <p className="font-medium">Allocations this causes</p>
          <ul className="mt-1 space-y-1">{r.allocations.map((a) => <li key={a.trainNo}><Badge tone={outcomeTone(a.outcome)}>{a.outcome}</Badge> {a.message}</li>)}</ul>
        </div>
      )}
      {r.events.length > 0 && (
        <div>
          <p className="font-medium">Station events</p>
          <ul className="mt-1 space-y-0.5 text-xs muted">{r.events.map((e) => <li key={e.sequence}>{hm(e.time)} · {e.message}</li>)}</ul>
        </div>
      )}
    </div>
  )
}

function DelayPreview({ d }: { d: DelayImpact }) {
  return (
    <div className="space-y-3 text-sm">
      <div className="grid grid-cols-2 gap-3 rounded-lg bg-slate-50 p-3 dark:bg-navy-950/50">
        <div><p className="text-xs muted">Arrival</p><p className="font-medium tabular-nums">{hm(d.oldArrival)} <ArrowRight className="inline h-3 w-3" /> {hm(d.newArrival)}</p></div>
        <div><p className="text-xs muted">Departure</p><p className="font-medium tabular-nums">{hm(d.oldDeparture)} <ArrowRight className="inline h-3 w-3" /> {hm(d.newDeparture)}</p></div>
        {d.occupationExtended && <div className="col-span-2"><p className="text-xs muted">{d.platformId} occupation</p><p className="font-medium tabular-nums">ends {hm(d.oldOccupationEnd)} <ArrowRight className="inline h-3 w-3" /> {hm(d.newOccupationEnd)}</p></div>}
        {d.requeued && <p className="col-span-2 text-amber-700 dark:text-amber-300">The train is in the queue: it is removed and re-inserted with its new arrival time.</p>}
        {d.lostPlatform && <p className="col-span-2 text-amber-700 dark:text-amber-300">The train loses its platform and is re-allocated.</p>}
      </div>
      {d.bumped.length > 0 && (
        <div>
          <p className="font-medium text-red-700 dark:text-red-300">Later trains un-assigned (now overlap)</p>
          <ul className="mt-1 space-y-1">{d.bumped.map((a) => <li key={a.trainNo}>{a.trainNo}: {a.message}</li>)}</ul>
        </div>
      )}
      {d.reallocated.length > 0 && (
        <div>
          <p className="font-medium">Re-allocation</p>
          <ul className="mt-1 space-y-1">{d.reallocated.map((a) => <li key={a.trainNo}><Badge tone={outcomeTone(a.outcome)}>{a.outcome}</Badge> {a.message}</li>)}</ul>
        </div>
      )}
      {d.alerts.length > 0 && (
        <ul className="space-y-1">{d.alerts.map((a, i) => <li key={i} className="flex gap-2 text-amber-700 dark:text-amber-300"><AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />{a}</li>)}</ul>
      )}
      {!d.bumped.length && !d.alerts.length && <p className="muted">No other train is affected.</p>}
    </div>
  )
}

/** Confirm arrival / departure and report delays, always with an exact impact preview first (computed on a copy). */
export function TrainOperations() {
  const toast = useToast()
  const [params] = useSearchParams()
  const highlight = params.get('train')
  const { data, error, loading, reload } = useApi(() => stationApi.trains(), [], 5000)
  const [target, setTarget] = useState<{ train: Train; action: Action } | null>(null)
  const [preview, setPreview] = useState<OperationResult | DelayImpact | null>(null)
  const [minutes, setMinutes] = useState('15')
  const [reason, setReason] = useState('')
  const [busy, setBusy] = useState(false)
  const [problem, setProblem] = useState<string | null>(null)

  const open = (train: Train, action: Action) => {
    setTarget({ train, action })
    setPreview(null)
    setProblem(null)
    setMinutes('15')
    setReason('')
    if (action !== 'delay') load(train, action)
  }
  const close = () => setTarget(null)

  const call = (train: Train, action: Action, dry: boolean) =>
    action === 'arrival' ? stationApi.arrival(train.trainNo, dry)
      : action === 'departure' ? stationApi.departure(train.trainNo, dry)
      : stationApi.delay(train.trainNo, Number(minutes), reason.trim(), dry)

  const load = async (train: Train, action: Action) => {
    setBusy(true)
    setProblem(null)
    try {
      setPreview(await call(train, action, true))
    } catch (e) {
      setProblem(toApiError(e).message)
    } finally {
      setBusy(false)
    }
  }

  const confirm = async () => {
    if (!target) return
    setBusy(true)
    try {
      const r = await call(target.train, target.action, false)
      toast.push('success', TITLES[target.action], r.message)
      close()
      reload(true)
    } catch (e) {
      setProblem(toApiError(e).message)
    } finally {
      setBusy(false)
    }
  }

  const validDelay = Number(minutes) >= 1 && Number(minutes) <= 720
  const columns: Column<Train>[] = [
    { key: 'train', header: 'Train', sortValue: (t) => t.trainNo, render: (t) => <div><p className="font-mono font-semibold">{t.trainNo}</p><p className="text-xs muted">{t.name}</p></div> },
    { key: 'pri', header: 'Priority', render: (t) => <PriorityBadge priority={t.priority} /> },
    { key: 'arr', header: 'Arr / Dep', sortValue: (t) => t.expectedArrival,
      render: (t) => <span className="tabular-nums">{hm(t.expectedArrival)} – {hm(t.expectedDeparture)}{t.delayMinutes > 0 && <span className="ml-1 text-xs text-amber-600">+{t.delayMinutes}</span>}</span> },
    { key: 'len', header: 'Length', sortValue: (t) => t.lengthMeters, render: (t) => `${t.lengthMeters} m` },
    { key: 'pf', header: 'Platform', render: (t) => t.platformId ?? '–' },
    { key: 'status', header: 'Status', render: (t) => <div><TrainStatusBadge status={t.status} /><p className="mt-1 text-xs muted">{t.liveStatus}</p></div> },
    { key: 'act', header: 'Actions', className: 'text-right', render: (t) => (
      <div className="flex flex-wrap justify-end gap-1.5">
        <button className="btn-secondary btn-sm" disabled={t.status !== 'PLATFORM_ASSIGNED'} onClick={() => open(t, 'arrival')}><LogIn className="h-3.5 w-3.5" /> Arrival</button>
        <button className="btn-secondary btn-sm" disabled={t.status !== 'ARRIVED'} onClick={() => open(t, 'departure')}><LogOut className="h-3.5 w-3.5" /> Departure</button>
        <button className="btn-secondary btn-sm" disabled={t.status === 'DEPARTED' || t.status === 'REJECTED'} onClick={() => open(t, 'delay')}><Clock className="h-3.5 w-3.5" /> Delay</button>
      </div>
    ) },
  ]

  return (
    <>
      <PageHeader title="Train Operations" crumbs={[{ label: 'Station', to: '/station' }]}
        description="Every action shows its impact first. The preview runs on a copy of the station, so nothing changes until you confirm." />
      {error ? <ErrorState message={error.message} onRetry={() => reload()} /> : (
        <DataTable rows={data ?? []} columns={columns} rowKey={(t) => t.trainNo} loading={loading && !data} caption="Trains"
          searchText={(t) => `${t.trainNo} ${t.name}`} searchPlaceholder="Filter trains"
          rowClassName={(t) => (t.trainNo === highlight ? 'bg-teal-50/70 dark:bg-teal-500/10' : '')} />
      )}
      <Modal open={!!target} onClose={close} size="lg" title={target ? `${TITLES[target.action]} · ${target.train.trainNo} ${target.train.name}` : ''}
        footer={<>
          <Button variant="secondary" onClick={close}>Cancel</Button>
          {target?.action === 'delay' && <Button variant="secondary" onClick={() => target && load(target.train, 'delay')} disabled={!validDelay} loading={busy && !preview}><Eye className="h-4 w-4" /> Preview impact</Button>}
          <Button onClick={confirm} loading={busy && !!preview} disabled={!preview || (target?.action === 'delay' && !validDelay)}>Confirm</Button>
        </>}>
        {target?.action === 'delay' && (
          <div className="mb-4 grid gap-3 sm:grid-cols-[140px_1fr]">
            <TextField label="Delay (minutes)" type="number" min={1} max={720} value={minutes}
              onChange={(e) => { setMinutes(e.target.value); setPreview(null) }} error={validDelay ? undefined : '1 to 720 minutes'} />
            <TextField label="Reason" maxLength={120} placeholder="e.g. Signal failure near Tambaram" value={reason}
              onChange={(e) => { setReason(e.target.value); setPreview(null) }} />
          </div>
        )}
        {problem && <p role="alert" className="mb-3 rounded-lg border border-red-300 bg-red-50 p-3 text-sm text-red-700 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-200">{problem}</p>}
        {busy && !preview && <p className="text-sm muted">Computing the impact…</p>}
        {preview && (
          <div>
            <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-teal-700 dark:text-teal-300">Impact preview</p>
            {'oldArrival' in preview ? <DelayPreview d={preview} /> : <OperationPreview r={preview} />}
          </div>
        )}
        {target?.action === 'delay' && !preview && !busy && !problem && <p className="text-sm muted">Enter the delay and press Preview impact.</p>}
      </Modal>
    </>
  )
}
