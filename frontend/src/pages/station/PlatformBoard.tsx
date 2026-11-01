import { Link } from 'react-router-dom'
import { Clock, MapPin, Ruler, Wrench } from 'lucide-react'
import { stationApi } from '../../api/endpoints'
import type { PlatformInfo } from '../../api/types'
import { Gantt } from '../../components/station/Gantt'
import { Badge } from '../../components/ui/Badge'
import { Card, CardHeader } from '../../components/ui/Card'
import { ErrorState } from '../../components/ui/EmptyState'
import { PageHeader } from '../../components/ui/PageHeader'
import { PageSkeleton } from '../../components/ui/Skeleton'
import { useApi } from '../../hooks/useApi'
import { hm, titleCase } from '../../lib/format'
import { platformTone } from '../../lib/status'

export function PlatformCard({ p }: { p: PlatformInfo }) {
  return (
    <div className="card card-pad">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider muted">{p.name}</p>
          <p className="text-2xl font-bold text-navy-900 dark:text-white">{p.id}</p>
        </div>
        <Badge tone={platformTone(p.status)}>{p.maintenance && <Wrench className="h-3 w-3" />}{titleCase(p.status)}</Badge>
      </div>
      <p className="mt-2 flex items-center gap-1.5 text-sm muted"><Ruler className="h-4 w-4" /> {p.lengthMeters} m long</p>
      <div className="mt-4 rounded-lg bg-slate-50 p-3 text-sm dark:bg-navy-950/50">
        {p.currentTrainNo ? (
          <>
            <p className="text-xs muted">Current train</p>
            <p className="font-semibold"><span className="font-mono">{p.currentTrainNo}</span> {p.currentTrainName}</p>
            <p className="text-xs muted">Interval {hm(p.currentStart)}–{hm(p.currentEnd)}</p>
          </>
        ) : <p className="muted">No train on the platform</p>}
      </div>
      <dl className="mt-3 space-y-1 text-sm">
        <div className="flex justify-between"><dt className="flex items-center gap-1.5 muted"><Clock className="h-3.5 w-3.5" /> Next free</dt><dd className="font-medium tabular-nums">{hm(p.nextFreeTime)}</dd></div>
        <div className="flex justify-between"><dt className="flex items-center gap-1.5 muted"><MapPin className="h-3.5 w-3.5" /> Next assigned</dt>
          <dd className="font-medium">{p.nextTrainNo ? `${p.nextTrainNo} at ${hm(p.nextTrainArrival)}` : '–'}</dd></div>
      </dl>
    </div>
  )
}

/** Platform Board: the three platforms and the Gantt timeline, refreshed every 4 seconds. */
export function PlatformBoard() {
  const { data, error, loading, reload } = useApi(() => stationApi.board(), [], 4000)
  if (loading && !data) return <PageSkeleton />
  if (error || !data) return <ErrorState message={error?.message ?? 'Unavailable'} onRetry={() => reload()} />
  return (
    <>
      <PageHeader title="Platform Board" crumbs={[{ label: 'Station', to: '/station' }]}
        description={`Policy ${data.policy === 'BEST_FIT' ? 'Best Fit' : 'First Fit'} · clearance buffer ${data.clearanceBufferMinutes} min · next release ${data.nextRelease}`}
        actions={<Link to="/station/queue" className="btn-secondary">Queue ({data.waiting.length})</Link>} />
      <div className="grid gap-4 md:grid-cols-3">{data.platforms.map((p) => <PlatformCard key={p.id} p={p} />)}</div>
      <Card className="mt-6">
        <CardHeader title="Timeline" subtitle="Occupation intervals [arrival, departure + buffer). Hover a block for details." />
        <div className="p-5"><Gantt board={data} /></div>
      </Card>
    </>
  )
}
