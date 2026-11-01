import { useState } from 'react'
import { CheckCircle2, FlaskConical, PlayCircle, XCircle } from 'lucide-react'
import { toApiError } from '../../api/client'
import { stationApi } from '../../api/endpoints'
import type { ScenarioInfo, ScenarioResult } from '../../api/types'
import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { Card, CardHeader } from '../../components/ui/Card'
import { ErrorState } from '../../components/ui/EmptyState'
import { PageHeader } from '../../components/ui/PageHeader'
import { PageSkeleton } from '../../components/ui/Skeleton'
import { useToast } from '../../context/ToastContext'
import { useApi } from '../../hooks/useApi'

function ResultPanel({ r }: { r: ScenarioResult }) {
  return (
    <div className="mt-4 space-y-3 border-t border-slate-100 pt-4 dark:border-navy-800">
      <div className="grid gap-3 text-sm sm:grid-cols-2">
        <div className="rounded-lg bg-slate-50 p-3 dark:bg-navy-950/50"><p className="text-xs font-semibold uppercase tracking-wider muted">Expected</p><p className="mt-1">{r.expected}</p></div>
        <div className={`rounded-lg p-3 ${r.passed ? 'bg-green-50 dark:bg-green-500/10' : 'bg-red-50 dark:bg-red-500/10'}`}><p className="text-xs font-semibold uppercase tracking-wider muted">Actual</p><p className="mt-1">{r.actual}</p></div>
      </div>
      <ol className="space-y-1.5">
        {r.steps.map((s, i) => (
          <li key={i} className="flex gap-2 text-sm">
            {s.ok ? <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-green-600" /> : <XCircle className="mt-0.5 h-4 w-4 shrink-0 text-red-600" />}
            <span>{s.description}</span>
          </li>
        ))}
      </ol>
      <p className="text-xs muted">Ran in {r.durationMs} ms on an isolated dataset</p>
    </div>
  )
}

/** Test Scenarios Lab: T1–T7 show the rules working, F1–F4 show bad requests refused. Each run uses fresh data. */
export function TestLab() {
  const toast = useToast()
  const { data, error, loading, reload } = useApi(() => stationApi.scenarios())
  const [results, setResults] = useState<Record<string, ScenarioResult>>({})
  const [running, setRunning] = useState<string | null>(null)

  const run = async (id: string) => {
    setRunning(id)
    try {
      const r = await stationApi.runScenario(id)
      setResults((m) => ({ ...m, [id]: r }))
    } catch (e) {
      toast.push('error', `${id} could not run`, toApiError(e).message)
    } finally {
      setRunning(null)
    }
  }
  const runAll = async () => {
    setRunning('ALL')
    try {
      const list = await stationApi.runAll()
      setResults(Object.fromEntries(list.map((r) => [r.id, r])))
      const passed = list.filter((r) => r.passed).length
      toast.push(passed === list.length ? 'success' : 'error', `${passed} of ${list.length} scenarios passed`)
    } catch (e) {
      toast.push('error', 'Run all failed', toApiError(e).message)
    } finally {
      setRunning(null)
    }
  }

  if (loading) return <PageSkeleton />
  if (error || !data) return <ErrorState message={error?.message ?? 'Unavailable'} onRetry={() => reload()} />
  const done = Object.values(results)
  const passed = done.filter((r) => r.passed).length

  const section = (kind: ScenarioInfo['kind'], title: string, subtitle: string) => (
    <Card>
      <CardHeader title={title} subtitle={subtitle} />
      <div className="divide-y divide-slate-100 dark:divide-navy-800">
        {data.filter((s) => s.kind === kind).map((s) => {
          const r = results[s.id]
          return (
            <div key={s.id} className="p-5">
              <div className="flex flex-wrap items-start gap-3">
                <span className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl font-mono text-sm font-bold ${kind === 'POSITIVE' ? 'bg-teal-50 text-teal-700 dark:bg-teal-500/15 dark:text-teal-300' : 'bg-red-50 text-red-700 dark:bg-red-500/15 dark:text-red-300'}`}>{s.id}</span>
                <div className="min-w-0 flex-1">
                  <p className="font-semibold">{s.title} <span className="ml-1 text-xs font-normal muted">{s.rule}</span></p>
                  <p className="text-sm muted">{s.description}</p>
                </div>
                {r && <Badge tone={r.passed ? 'green' : 'red'} className="text-sm">{r.passed ? 'PASS' : 'FAIL'}</Badge>}
                <Button size="sm" variant="secondary" loading={running === s.id} disabled={!!running} onClick={() => run(s.id)}><PlayCircle className="h-3.5 w-3.5" /> Run</Button>
              </div>
              {r && <ResultPanel r={r} />}
            </div>
          )
        })}
      </div>
    </Card>
  )

  return (
    <>
      <PageHeader title="Test Scenarios Lab" crumbs={[{ label: 'Station', to: '/station' }]}
        description="One-click demonstrations for the viva. Every scenario builds its own fresh station, so the live station is never touched."
        actions={<Button loading={running === 'ALL'} disabled={!!running} onClick={runAll}><FlaskConical className="h-4 w-4" /> Run all {data.length}</Button>} />
      {done.length > 0 && (
        <div className={`mb-6 flex items-center gap-3 rounded-xl border p-4 ${passed === done.length ? 'border-green-300 bg-green-50 dark:border-green-500/30 dark:bg-green-500/10' : 'border-red-300 bg-red-50 dark:border-red-500/30 dark:bg-red-500/10'}`}>
          {passed === done.length ? <CheckCircle2 className="h-6 w-6 text-green-600" /> : <XCircle className="h-6 w-6 text-red-600" />}
          <p className="font-semibold">{passed} of {done.length} run scenarios passed</p>
        </div>
      )}
      <div className="space-y-6">
        {section('POSITIVE', 'Rule scenarios (T1–T7)', 'The system does the right thing')}
        {section('FAILURE', 'Failure scenarios (F1–F4)', 'Bad requests are refused with a clear error')}
      </div>
    </>
  )
}
