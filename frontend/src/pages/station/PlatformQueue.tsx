import { ListOrdered } from 'lucide-react'
import { stationApi } from '../../api/endpoints'
import type { QueueEntry } from '../../api/types'
import { PriorityBadge } from '../../components/ui/Badge'
import { Card, CardHeader } from '../../components/ui/Card'
import { EmptyState, ErrorState } from '../../components/ui/EmptyState'
import { PageHeader } from '../../components/ui/PageHeader'
import { TableSkeleton } from '../../components/ui/Skeleton'
import { useApi } from '../../hooks/useApi'
import { hm } from '../../lib/format'

const TYPE_PRIORITY: Record<string, string> = { EMERGENCY: 'EMERGENCY', CONNECTION: 'CONNECTION', SCHEDULED: 'NORMAL' }

/**
 * The platform waiting PriorityQueue in poll() order: priority rank, then arrival time, then train number.
 * The "next release" column comes from the min-heap of platform release times.
 */
export function PlatformQueue() {
  const { data, error, loading, reload } = useApi(() => stationApi.queue(), [], 4000)
  return (
    <>
      <PageHeader title="Platform Queue" crumbs={[{ label: 'Station', to: '/station' }]}
        description="Trains waiting for a platform. Order: EMERGENCY (0) → CONNECTION (1) → SCHEDULED (2), then earlier arrival, then lower train number." />
      <Card>
        <CardHeader title="PriorityQueue" icon={<ListOrdered className="h-5 w-5" />} subtitle={data ? `${data.length} train(s) waiting` : undefined} />
        {loading && !data ? <div className="p-5"><TableSkeleton /></div> : error ? <ErrorState message={error.message} onRetry={() => reload()} />
          : !data?.length ? <EmptyState icon={ListOrdered} title="The queue is empty" message="Every train that asked for a platform has one." /> : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 dark:divide-navy-800">
                <thead className="table-head">
                  <tr>{['Rank', 'Train', 'Request type', 'Priority rank', 'Arrival', 'Needs until', 'Length', 'Requested', 'Why waiting', 'Next compatible release'].map((h) => <th key={h} className="px-4 py-2.5">{h}</th>)}</tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-navy-800">
                  {data.map((q: QueueEntry) => (
                    <tr key={q.trainNo}>
                      <td className="table-cell"><span className="flex h-7 w-7 items-center justify-center rounded-full bg-navy-800 text-xs font-bold text-white">{q.rank}</span></td>
                      <td className="table-cell"><p className="font-mono font-semibold">{q.trainNo}</p><p className="text-xs muted">{q.trainName}</p></td>
                      <td className="table-cell"><PriorityBadge priority={TYPE_PRIORITY[q.type]} /> <span className="ml-1 text-xs muted">{q.type.toLowerCase()}</span></td>
                      <td className="table-cell tabular-nums">{q.priorityRank}</td>
                      <td className="table-cell tabular-nums">{hm(q.arrival)}{q.delayMinutes > 0 && <span className="ml-1 text-xs text-amber-600">+{q.delayMinutes}</span>}</td>
                      <td className="table-cell tabular-nums">{hm(q.desiredEnd)}</td>
                      <td className="table-cell tabular-nums">{q.lengthMeters} m</td>
                      <td className="table-cell tabular-nums">{hm(q.requestedAt)}</td>
                      <td className="table-cell max-w-xs text-sm">{q.waitReason}</td>
                      <td className="table-cell">{q.nextReleasePlatform ? `${q.nextReleasePlatform} at ${hm(q.nextReleaseTime)}` : <span className="muted">none yet</span>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
      </Card>
    </>
  )
}
