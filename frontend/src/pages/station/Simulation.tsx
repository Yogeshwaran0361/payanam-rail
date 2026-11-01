import { useState } from 'react'
import { Camera, Database, Download, FastForward, Pause, Play, RotateCcw, Trash2 } from 'lucide-react'
import { toApiError } from '../../api/client'
import { stationApi } from '../../api/endpoints'
import type { ClockState, Snapshot } from '../../api/types'
import { Button } from '../../components/ui/Button'
import { Card, CardHeader } from '../../components/ui/Card'
import { ErrorState } from '../../components/ui/EmptyState'
import { ConfirmDialog } from '../../components/ui/Modal'
import { PageHeader } from '../../components/ui/PageHeader'
import { PageSkeleton } from '../../components/ui/Skeleton'
import { useToast } from '../../context/ToastContext'
import { useApi } from '../../hooks/useApi'
import { dateTime, hm } from '../../lib/format'

type Danger = 'reset-clock' | 'load-seed' | 'reset-system'
const DANGER: Record<Danger, { title: string; message: string; label: string }> = {
  'reset-clock': { title: 'Reset the clock to the start?', label: 'Reset clock', message: 'Time cannot run backwards on a live plan, so this reloads the seed data with the clock at the start time. Bookings made since will be lost.' },
  'load-seed': { title: 'Load the seed data?', label: 'Load seed', message: 'Replaces the station with the seed dataset: 6 trains, 3 platforms and 32 bookings. Accounts and the audit log are kept.' },
  'reset-system': { title: 'Reset the system?', label: 'Reset system', message: 'Removes every train and booking and keeps only the 3 platforms. Accounts and the audit log are kept.' },
}

function summary(s: Snapshot): string {
  try {
    const k = JSON.parse(s.summaryJson).dashboard.kpis
    return `${k.trainsToday} trains · ${k.onPlatform} on platform · ${k.waitingForPlatform} waiting · ${k.totalBookings} bookings`
  } catch {
    return ''
  }
}

/** Simulation Control: the station clock (play / pause / advance), reloading data and saved snapshots. */
export function Simulation() {
  const toast = useToast()
  const clock = useApi(() => stationApi.clock(), [], 2000)
  const snaps = useApi(() => stationApi.snapshots())
  const events = useApi(() => stationApi.events(30).then((e) => [...e].reverse()), [], 3000)
  const [busy, setBusy] = useState<string | null>(null)
  const [danger, setDanger] = useState<Danger | null>(null)
  const [label, setLabel] = useState('')

  const act = async (key: string, fn: () => Promise<ClockState>, done: string) => {
    setBusy(key)
    try {
      const c = await fn()
      clock.setData(c)
      events.reload(true)
      toast.push('success', done, c.events.length ? c.events.slice(-3).join(' · ') : `Station time ${hm(c.now)}`)
    } catch (e) {
      toast.push('error', 'Simulation action failed', toApiError(e).message)
    } finally {
      setBusy(null)
    }
  }

  const runDanger = async () => {
    if (!danger) return
    const fn = danger === 'reset-clock' ? stationApi.resetClock : danger === 'load-seed' ? stationApi.loadSeed : stationApi.resetSystem
    await act(danger, fn, DANGER[danger].label + ' done')
    setDanger(null)
  }

  const save = async () => {
    if (!label.trim()) return
    setBusy('snap')
    try {
      await stationApi.saveSnapshot(label.trim())
      toast.push('success', 'Snapshot saved', label.trim())
      setLabel('')
      snaps.reload(true)
    } catch (e) {
      toast.push('error', 'Could not save', toApiError(e).message)
    } finally {
      setBusy(null)
    }
  }

  const download = (s: Snapshot) => {
    const url = URL.createObjectURL(new Blob([JSON.stringify(JSON.parse(s.summaryJson), null, 2)], { type: 'application/json' }))
    const a = document.createElement('a')
    a.href = url
    a.download = `payanam-snapshot-${s.id}.json`
    a.click()
    URL.revokeObjectURL(url)
  }

  if (clock.loading && !clock.data) return <PageSkeleton />
  if (clock.error || !clock.data) return <ErrorState message={clock.error?.message ?? 'Unavailable'} onRetry={() => clock.reload()} />
  const c = clock.data

  return (
    <>
      <PageHeader title="Simulation" crumbs={[{ label: 'Station', to: '/station' }]}
        description="The simulation clock is the station's 'now'. Advancing it releases platforms whose time has come and allocates waiting trains." />
      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <div className="flex flex-col items-center gap-6 p-8 sm:flex-row sm:items-center sm:justify-between">
            <div className="text-center sm:text-left">
              <p className="text-sm muted">Station time</p>
              <p className="text-6xl font-bold tabular-nums tracking-tight text-navy-900 dark:text-white">{hm(c.now)}</p>
              <p className="mt-1 text-sm muted">Started {hm(c.start)} · {c.elapsedMinutes} min elapsed ·{' '}
                <span className={c.running ? 'font-medium text-green-600' : 'font-medium text-amber-600'}>{c.running ? `playing (+${c.playStepMinutes} min every ${c.tickSeconds}s)` : 'paused'}</span>
              </p>
            </div>
            <div className="flex flex-wrap justify-center gap-2">
              {c.running
                ? <Button variant="navy" loading={busy === 'pause'} onClick={() => act('pause', stationApi.pause, 'Clock paused')}><Pause className="h-4 w-4" /> Pause</Button>
                : <Button loading={busy === 'play'} onClick={() => act('play', stationApi.play, 'Clock playing')}><Play className="h-4 w-4" /> Play</Button>}
              {[5, 15, 30].map((m) => (
                <Button key={m} variant="secondary" loading={busy === `+${m}`} onClick={() => act(`+${m}`, () => stationApi.advance(m), `Clock advanced ${m} min`)}>
                  <FastForward className="h-4 w-4" /> +{m} min
                </Button>
              ))}
            </div>
          </div>
          <div className="grid gap-3 border-t border-slate-100 p-5 sm:grid-cols-3 dark:border-navy-800">
            <button className="btn-secondary" onClick={() => setDanger('reset-clock')}><RotateCcw className="h-4 w-4" /> Reset clock</button>
            <button className="btn-secondary" onClick={() => setDanger('load-seed')}><Database className="h-4 w-4" /> Load seed data</button>
            <button className="btn-secondary text-red-600" onClick={() => setDanger('reset-system')}><Trash2 className="h-4 w-4" /> Reset system</button>
          </div>
        </Card>
        <Card>
          <CardHeader title="Event log" subtitle="Latest station events" />
          <ul className="max-h-80 divide-y divide-slate-100 overflow-y-auto text-sm dark:divide-navy-800">
            {(events.data ?? []).map((e) => (
              <li key={e.sequence} className={`px-5 py-2 ${e.alert ? 'text-amber-700 dark:text-amber-300' : ''}`}><span className="mr-2 tabular-nums muted">{hm(e.time)}</span>{e.message}</li>
            ))}
          </ul>
        </Card>
        <Card className="lg:col-span-3">
          <CardHeader title="Snapshots" icon={<Camera className="h-5 w-5" />} subtitle="Save the KPIs, timetable and platform board to the database (H2) for the report" />
          <div className="flex flex-wrap gap-3 border-b border-slate-100 p-5 dark:border-navy-800">
            <input className="input max-w-sm" maxLength={80} placeholder="Label, e.g. After the 07:30 delay" value={label} onChange={(e) => setLabel(e.target.value)} aria-label="Snapshot label" />
            <Button onClick={save} loading={busy === 'snap'} disabled={!label.trim()}>Save snapshot</Button>
          </div>
          <ul className="divide-y divide-slate-100 dark:divide-navy-800">
            {(snaps.data ?? []).map((s) => (
              <li key={s.id} className="flex flex-wrap items-center gap-3 px-5 py-3 text-sm">
                <div className="flex-1">
                  <p className="font-medium">{s.label}</p>
                  <p className="text-xs muted">Station time {hm(s.simulationTime)} · saved {dateTime(s.createdAt)} by {s.createdBy} · {summary(s)}</p>
                </div>
                <button className="btn-ghost btn-sm" onClick={() => download(s)}><Download className="h-3.5 w-3.5" /> JSON</button>
              </li>
            ))}
            {snaps.data?.length === 0 && <li className="px-5 py-6 text-center text-sm muted">No snapshots yet.</li>}
          </ul>
        </Card>
      </div>
      <ConfirmDialog open={!!danger} danger loading={!!busy} onClose={() => setDanger(null)} onConfirm={runDanger}
        title={danger ? DANGER[danger].title : ''} confirmLabel={danger ? DANGER[danger].label : ''} message={danger ? DANGER[danger].message : ''} />
    </>
  )
}
