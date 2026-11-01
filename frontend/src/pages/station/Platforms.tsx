import { useState } from 'react'
import { Pencil, Wrench } from 'lucide-react'
import { toApiError } from '../../api/client'
import { stationApi } from '../../api/endpoints'
import type { PlatformInfo } from '../../api/types'
import { Button } from '../../components/ui/Button'
import { ErrorState } from '../../components/ui/EmptyState'
import { TextField } from '../../components/ui/Field'
import { ConfirmDialog, Modal } from '../../components/ui/Modal'
import { PageHeader } from '../../components/ui/PageHeader'
import { PageSkeleton } from '../../components/ui/Skeleton'
import { useToast } from '../../context/ToastContext'
import { useApi } from '../../hooks/useApi'
import { PlatformCard } from './PlatformBoard'

/** Platform Management: change a platform's length and switch maintenance on or off (P9). */
export function Platforms() {
  const toast = useToast()
  const { data, error, loading, reload } = useApi(() => stationApi.platforms(), [], 5000)
  const [editing, setEditing] = useState<PlatformInfo | null>(null)
  const [length, setLength] = useState('')
  const [toggle, setToggle] = useState<PlatformInfo | null>(null)
  const [busy, setBusy] = useState(false)
  const [problem, setProblem] = useState<string | null>(null)

  const saveLength = async () => {
    if (!editing) return
    const n = Number(length)
    if (!(n >= 100 && n <= 1000)) return setProblem('Length must be 100 to 1000 m')
    setBusy(true)
    try {
      const r = await stationApi.editPlatform(editing.id, n)
      toast.push('success', `${editing.id} updated`, r.message)
      setEditing(null)
      reload(true)
    } catch (e) {
      setProblem(toApiError(e).message)
    } finally {
      setBusy(false)
    }
  }

  const switchMaintenance = async () => {
    if (!toggle) return
    setBusy(true)
    try {
      const r = await stationApi.maintenance(toggle.id, !toggle.maintenance)
      toast.push(toggle.maintenance ? 'success' : 'warning', `${toggle.id} ${toggle.maintenance ? 'back in service' : 'under maintenance'}`, r.message)
      setToggle(null)
      reload(true)
    } catch (e) {
      toast.push('error', 'Could not change maintenance', toApiError(e).message)
    } finally {
      setBusy(false)
    }
  }

  if (loading && !data) return <PageSkeleton />
  if (error || !data) return <ErrorState message={error?.message ?? 'Unavailable'} onRetry={() => reload()} />
  return (
    <>
      <PageHeader title="Platforms" crumbs={[{ label: 'Station', to: '/station' }]}
        description="A platform under maintenance gets no new trains. A train already on it is never displaced (P5, P9)." />
      <div className="grid gap-4 md:grid-cols-3">
        {data.map((p) => (
          <div key={p.id} className="space-y-2">
            <PlatformCard p={p} />
            <div className="flex gap-2">
              <button className="btn-secondary btn-sm flex-1" onClick={() => { setEditing(p); setLength(String(p.lengthMeters)); setProblem(null) }}><Pencil className="h-3.5 w-3.5" /> Edit length</button>
              <button className={`btn-sm flex-1 ${p.maintenance ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setToggle(p)}>
                <Wrench className="h-3.5 w-3.5" /> {p.maintenance ? 'End maintenance' : 'Start maintenance'}
              </button>
            </div>
          </div>
        ))}
      </div>
      <Modal open={!!editing} onClose={() => setEditing(null)} title={`Edit ${editing?.name}`}
        footer={<><Button variant="secondary" onClick={() => setEditing(null)}>Cancel</Button><Button loading={busy} onClick={saveLength}>Save</Button></>}>
        <TextField label="Length (m)" type="number" min={100} max={1000} value={length} onChange={(e) => { setLength(e.target.value); setProblem(null) }} error={problem ?? undefined}
          hint="Shortening is refused if a train already planned on this platform would no longer fit." />
      </Modal>
      <ConfirmDialog open={!!toggle} loading={busy} onClose={() => setToggle(null)} onConfirm={switchMaintenance} danger={!toggle?.maintenance}
        title={toggle?.maintenance ? `End maintenance on ${toggle?.id}?` : `Put ${toggle?.id} under maintenance?`}
        confirmLabel={toggle?.maintenance ? 'End maintenance' : 'Start maintenance'}
        message={toggle?.maintenance ? 'The platform becomes available again and waiting trains are allocated.'
          : 'No new train will be assigned to it. Trains assigned but not yet arrived are re-allocated or queued; a train standing on it stays (P5).'} />
    </>
  )
}
