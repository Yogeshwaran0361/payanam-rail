import { useEffect, useState } from 'react'
import { Lock, SlidersHorizontal } from 'lucide-react'
import { toApiError } from '../../api/client'
import { stationApi } from '../../api/endpoints'
import { Button } from '../../components/ui/Button'
import { Card, CardHeader } from '../../components/ui/Card'
import { ErrorState } from '../../components/ui/EmptyState'
import { TextField } from '../../components/ui/Field'
import { PageSkeleton } from '../../components/ui/Skeleton'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import { useApi } from '../../hooks/useApi'
import { Profile } from '../passenger/Profile'

const POLICIES: Record<string, { name: string; text: string }> = {
  BEST_FIT: { name: 'Best Fit (default)', text: 'The SHORTEST free platform that is long enough; ties go to the lowest id. Keeps long platforms for long trains.' },
  FIRST_FIT: { name: 'First Fit', text: 'The first free platform in id order (P1, P2, P3) that is long enough. Simpler, but can waste long platforms.' },
}

/** Station Settings: allocation policy (P6) and clearance buffer, then the staff member's own profile. */
export function Settings() {
  const { can } = useAuth()
  const toast = useToast()
  const editable = can('MANAGE_SETTINGS')
  const { data, error, loading, reload, setData } = useApi(() => stationApi.settings())
  const [policy, setPolicy] = useState('')
  const [buffer, setBuffer] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (data) {
      setPolicy(data.allocationPolicy)
      setBuffer(String(data.clearanceBufferMinutes))
    }
  }, [data])

  const save = async () => {
    const b = Number(buffer)
    if (!(b >= 0 && b <= 60)) return toast.push('error', 'Buffer must be 0 to 60 minutes')
    setBusy(true)
    try {
      const s = await stationApi.saveSettings(policy, b)
      setData(s)
      toast.push('success', 'Settings saved', `${POLICIES[s.allocationPolicy].name}, ${s.clearanceBufferMinutes} min buffer`)
    } catch (e) {
      toast.push('error', 'Could not save', toApiError(e).message)
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <PageSkeleton />
  if (error || !data) return <ErrorState message={error?.message ?? 'Unavailable'} onRetry={() => reload()} />
  return (
    <>
      <Card className="mb-8">
        <CardHeader title={`Station settings · ${data.stationName}`} icon={<SlidersHorizontal className="h-5 w-5" />}
          subtitle={editable ? 'Changes apply to new allocations; existing plans are kept.' : 'Only the Station Master can change these settings.'}
          action={!editable && <span className="flex items-center gap-1 text-xs muted"><Lock className="h-3.5 w-3.5" /> Read only</span>} />
        <div className="grid gap-6 p-5 lg:grid-cols-3">
          <fieldset className="lg:col-span-2" disabled={!editable}>
            <legend className="label">Allocation policy (PlatformAllocationPolicy)</legend>
            <div className="grid gap-3 sm:grid-cols-2">
              {data.availablePolicies.map((p) => (
                <label key={p} className={`cursor-pointer rounded-xl border-2 p-4 transition ${policy === p ? 'border-teal-600 bg-teal-50/50 dark:bg-teal-500/10' : 'border-slate-200 dark:border-navy-700'} ${editable ? '' : 'cursor-not-allowed opacity-80'}`}>
                  <input type="radio" name="policy" value={p} checked={policy === p} onChange={() => setPolicy(p)} className="sr-only" />
                  <p className="font-semibold">{POLICIES[p]?.name ?? p}</p>
                  <p className="mt-1 text-sm muted">{POLICIES[p]?.text}</p>
                </label>
              ))}
            </div>
          </fieldset>
          <div className="space-y-4">
            <TextField label="Clearance buffer (minutes)" type="number" min={0} max={60} value={buffer} disabled={!editable}
              onChange={(e) => setBuffer(e.target.value)} hint="Occupation = [arrival, departure + buffer)" />
            {editable && <Button onClick={save} loading={busy} className="w-full">Save settings</Button>}
          </div>
        </div>
      </Card>
      <Profile staff />
    </>
  )
}
