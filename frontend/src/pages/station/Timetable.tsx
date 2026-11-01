import { useState } from 'react'
import { Pencil, Plus } from 'lucide-react'
import { toApiError } from '../../api/client'
import { trainApi } from '../../api/endpoints'
import type { Priority, Train } from '../../api/types'
import { PriorityBadge, TrainStatusBadge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { DataTable, type Column } from '../../components/ui/DataTable'
import { ErrorState } from '../../components/ui/EmptyState'
import { SelectField, TextField } from '../../components/ui/Field'
import { Modal } from '../../components/ui/Modal'
import { PageHeader } from '../../components/ui/PageHeader'
import { useToast } from '../../context/ToastContext'
import { useApi } from '../../hooks/useApi'
import { hm, rupees } from '../../lib/format'

const MAX_TRAINS = 6
const EMPTY_NEW = { trainNo: '', name: '', origin: '', destination: '', arrival: '', departure: '', lengthMeters: '', priority: 'NORMAL', fare: '' }
type Errors = Record<string, string>

/** Timetable Management: the sorted timetable, edit times / priority, add a service (max 6). */
export function Timetable() {
  const toast = useToast()
  const { data, error, loading, reload } = useApi(() => trainApi.timetable(), [], 10000)
  const [editing, setEditing] = useState<Train | null>(null)
  const [edit, setEdit] = useState({ arrival: '', departure: '', priority: 'NORMAL' as Priority })
  const [adding, setAdding] = useState(false)
  const [form, setForm] = useState(EMPTY_NEW)
  const [errors, setErrors] = useState<Errors>({})
  const [busy, setBusy] = useState(false)

  const startEdit = (t: Train) => {
    setEditing(t)
    setErrors({})
    setEdit({ arrival: hm(t.scheduledArrival), departure: hm(t.scheduledDeparture), priority: t.priority })
  }

  const saveEdit = async () => {
    if (!editing) return
    if (edit.departure <= edit.arrival) return setErrors({ departure: 'Departure must be after arrival' })
    setBusy(true)
    try {
      await trainApi.edit(editing.trainNo, edit)
      toast.push('success', `${editing.trainNo} updated`, `${edit.arrival}–${edit.departure}, ${edit.priority.toLowerCase()}`)
      setEditing(null)
      reload(true)
    } catch (e) {
      const err = toApiError(e)
      setErrors(Object.fromEntries(err.fieldErrors.map((f) => [f.field, f.message])))
      toast.push('error', 'Could not update', err.message)
    } finally {
      setBusy(false)
    }
  }

  const saveNew = async () => {
    const e: Errors = {}
    if (!/^\d{5}$/.test(form.trainNo)) e.trainNo = 'Train number has 5 digits'
    if (!form.name.trim()) e.name = 'Name is required'
    if (!form.origin.trim()) e.origin = 'Origin is required'
    if (!form.destination.trim()) e.destination = 'Destination is required'
    if (!form.arrival) e.arrival = 'Arrival time is required'
    if (!form.departure) e.departure = 'Departure time is required'
    else if (form.departure <= form.arrival) e.departure = 'Departure must be after arrival'
    const len = Number(form.lengthMeters)
    if (!(len >= 50 && len <= 1000)) e.lengthMeters = '50 to 1000 m'
    if (form.fare === '' || Number(form.fare) < 0) e.fare = 'Fare cannot be negative'
    setErrors(e)
    if (Object.keys(e).length) return
    setBusy(true)
    try {
      const t = await trainApi.add({ ...form, lengthMeters: len, fare: Number(form.fare) })
      toast.push('success', `${t.trainNo} ${t.name} added`)
      setAdding(false)
      setForm(EMPTY_NEW)
      reload(true)
    } catch (err) {
      const apiErr = toApiError(err)
      setErrors(Object.fromEntries(apiErr.fieldErrors.map((f) => [f.field, f.message])))
      toast.push('error', 'Could not add the train', apiErr.message)
    } finally {
      setBusy(false)
    }
  }

  const columns: Column<Train>[] = [
    { key: 'arr', header: 'Arrival', sortValue: (t) => t.scheduledArrival, render: (t) => <span className="font-semibold tabular-nums">{hm(t.scheduledArrival)}</span> },
    { key: 'dep', header: 'Departure', sortValue: (t) => t.scheduledDeparture, render: (t) => <span className="tabular-nums">{hm(t.scheduledDeparture)}</span> },
    { key: 'train', header: 'Train', sortValue: (t) => t.trainNo, render: (t) => <div><p className="font-mono font-semibold">{t.trainNo}</p><p className="text-xs muted">{t.name}</p></div> },
    { key: 'route', header: 'Route', render: (t) => <span className="text-sm">{t.origin} → {t.destination}</span> },
    { key: 'len', header: 'Length', sortValue: (t) => t.lengthMeters, render: (t) => `${t.lengthMeters} m` },
    { key: 'pri', header: 'Priority', render: (t) => <PriorityBadge priority={t.priority} /> },
    { key: 'fare', header: 'Fare', render: (t) => rupees(t.fare) },
    { key: 'status', header: 'Status', render: (t) => <TrainStatusBadge status={t.status} /> },
    { key: 'edit', header: '', className: 'text-right', render: (t) => (
      <button className="btn-ghost btn-sm" disabled={t.status !== 'SCHEDULED'} title={t.status !== 'SCHEDULED' ? 'Only trains that have not requested a platform can be edited' : undefined}
        onClick={() => startEdit(t)}><Pencil className="h-3.5 w-3.5" /> Edit</button>
    ) },
  ]

  const count = data?.length ?? 0
  return (
    <>
      <PageHeader title="Timetable" crumbs={[{ label: 'Station', to: '/station' }]}
        description="Kept as a sorted ArrayList by arrival time: a binary search finds the insert position, so the list is never re-sorted."
        actions={<Button onClick={() => { setAdding(true); setErrors({}) }} disabled={count >= MAX_TRAINS} title={count >= MAX_TRAINS ? 'The station runs at most 6 services' : undefined}>
          <Plus className="h-4 w-4" /> Add train ({count}/{MAX_TRAINS})</Button>} />
      {error ? <ErrorState message={error.message} onRetry={() => reload()} />
        : <DataTable rows={data ?? []} columns={columns} rowKey={(t) => t.trainNo} loading={loading && !data} caption="Timetable" />}

      <Modal open={!!editing} onClose={() => setEditing(null)} title={`Edit ${editing?.trainNo} ${editing?.name}`}
        footer={<><Button variant="secondary" onClick={() => setEditing(null)}>Cancel</Button><Button loading={busy} onClick={saveEdit}>Save</Button></>}>
        <div className="grid gap-4 sm:grid-cols-3">
          <TextField label="Arrival" type="time" value={edit.arrival} onChange={(e) => setEdit({ ...edit, arrival: e.target.value })} error={errors.arrival} />
          <TextField label="Departure" type="time" value={edit.departure} onChange={(e) => setEdit({ ...edit, departure: e.target.value })} error={errors.departure} />
          <SelectField label="Priority" value={edit.priority} onChange={(e) => setEdit({ ...edit, priority: e.target.value as Priority })}>
            <option value="NORMAL">Normal</option><option value="CONNECTION">Connection</option><option value="EMERGENCY">Emergency</option>
          </SelectField>
        </div>
        <p className="mt-3 text-xs muted">The train is removed from the sorted timetable and re-inserted at its new position.</p>
      </Modal>

      <Modal open={adding} onClose={() => setAdding(false)} title="Add a train service" size="lg"
        footer={<><Button variant="secondary" onClick={() => setAdding(false)}>Cancel</Button><Button loading={busy} onClick={saveNew}>Add train</Button></>}>
        <div className="grid gap-4 sm:grid-cols-2">
          <TextField label="Train number" inputMode="numeric" maxLength={5} value={form.trainNo} onChange={(e) => setForm({ ...form, trainNo: e.target.value.replace(/\D/g, '') })} error={errors.trainNo} />
          <TextField label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} error={errors.name} />
          <TextField label="Origin" value={form.origin} onChange={(e) => setForm({ ...form, origin: e.target.value })} error={errors.origin} />
          <TextField label="Destination" value={form.destination} onChange={(e) => setForm({ ...form, destination: e.target.value })} error={errors.destination} />
          <TextField label="Arrival" type="time" value={form.arrival} onChange={(e) => setForm({ ...form, arrival: e.target.value })} error={errors.arrival} />
          <TextField label="Departure" type="time" value={form.departure} onChange={(e) => setForm({ ...form, departure: e.target.value })} error={errors.departure} />
          <TextField label="Length (m)" type="number" value={form.lengthMeters} onChange={(e) => setForm({ ...form, lengthMeters: e.target.value })} error={errors.lengthMeters} />
          <SelectField label="Priority" value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value })}>
            <option value="NORMAL">Normal</option><option value="CONNECTION">Connection</option><option value="EMERGENCY">Emergency</option>
          </SelectField>
          <TextField label="Fare (₹)" type="number" min={0} value={form.fare} onChange={(e) => setForm({ ...form, fare: e.target.value })} error={errors.fare} />
        </div>
      </Modal>
    </>
  )
}
