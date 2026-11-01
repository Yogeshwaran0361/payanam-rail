import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ArrowLeft, ArrowRight, Check, CheckCircle2, Plus, Printer, Trash2, TrainFront, UserPlus } from 'lucide-react'
import { toApiError } from '../../api/client'
import { bookingApi, trainApi, type PassengerInput } from '../../api/endpoints'
import type { Booking, Gender, Train } from '../../api/types'
import { JourneyHeader } from '../../components/booking/JourneyHeader'
import { PriorityBadge, TicketBadge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { Card, CardHeader } from '../../components/ui/Card'
import { EmptyState, ErrorState } from '../../components/ui/EmptyState'
import { SelectField, TextField } from '../../components/ui/Field'
import { PageHeader } from '../../components/ui/PageHeader'
import { PageSkeleton } from '../../components/ui/Skeleton'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import { useApi } from '../../hooks/useApi'
import { hm, rupees } from '../../lib/format'

const STEPS = ['Choose train', 'Passengers', 'Review & confirm']
const MAX_PASSENGERS = 4
const EMPTY: PassengerInput = { name: '', age: '', gender: '' }
type Errors = Record<string, string>

function Stepper({ step }: { step: number }) {
  return (
    <ol className="mb-6 flex items-center gap-2" aria-label="Booking steps">
      {STEPS.map((label, i) => (
        <li key={label} className="flex flex-1 items-center gap-2" aria-current={i === step ? 'step' : undefined}>
          <span className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-sm font-semibold ${i < step
            ? 'bg-teal-600 text-white' : i === step ? 'bg-navy-800 text-white dark:bg-teal-500' : 'bg-slate-200 text-slate-500 dark:bg-navy-800'}`}>
            {i < step ? <Check className="h-4 w-4" /> : i + 1}
          </span>
          <span className={`hidden text-sm font-medium sm:block ${i === step ? '' : 'muted'}`}>{label}</span>
          {i < STEPS.length - 1 && <span className={`h-0.5 flex-1 rounded ${i < step ? 'bg-teal-600' : 'bg-slate-200 dark:bg-navy-800'}`} />}
        </li>
      ))}
    </ol>
  )
}

/** What the passenger will most likely get: seats first (R1), then waiting-list numbers in order (R2). */
function predict(train: Train, count: number): string[] {
  return Array.from({ length: count }, (_, i) => (i < train.seatsLeft ? 'CNF' : `WL/${train.waiting + 1 + i - train.seatsLeft}`))
}

function validate(people: PassengerInput[]): Errors {
  const e: Errors = {}
  people.forEach((p, i) => {
    if (!p.name.trim()) e[`passengers[${i}].name`] = 'Passenger name is required'
    else if (p.name.trim().length > 60) e[`passengers[${i}].name`] = 'Name is too long'
    if (p.age === '' || Number(p.age) < 1 || Number(p.age) > 120) e[`passengers[${i}].age`] = 'Age must be 1 to 120'
    if (!p.gender) e[`passengers[${i}].gender`] = 'Gender is required'
  })
  return e
}

/** The 3-step booking wizard: train → passengers → review. Shows the PNR and tickets when done. */
export function BookTicket() {
  const [params] = useSearchParams()
  const { user } = useAuth()
  const toast = useToast()
  const trains = useApi(() => trainApi.timetable())
  const [trainNo, setTrainNo] = useState(params.get('train') ?? '')
  const [step, setStep] = useState(params.get('train') ? 1 : 0)
  const [people, setPeople] = useState<PassengerInput[]>([{ ...EMPTY }])
  const [errors, setErrors] = useState<Errors>({})
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState<string | null>(null)
  const [result, setResult] = useState<Booking | null>(null)

  const bookable = useMemo(() => (trains.data ?? []).filter((t) => t.bookingOpen), [trains.data])
  const train = (trains.data ?? []).find((t) => t.trainNo === trainNo)

  if (trains.loading) return <PageSkeleton />
  if (trains.error) return <ErrorState message={trains.error.message} onRetry={() => trains.reload()} />

  const update = (i: number, patch: Partial<PassengerInput>) => {
    setPeople((list) => list.map((p, k) => (k === i ? { ...p, ...patch } : p)))
    setErrors((old) => {
      const copy = { ...old }
      Object.keys(patch).forEach((f) => delete copy[`passengers[${i}].${f}`])
      return copy
    })
  }
  const addMe = () => {
    if (!user) return
    const age = user.dateOfBirth ? new Date().getFullYear() - new Date(user.dateOfBirth).getFullYear() : ''
    update(0, { name: user.fullName, age, gender: user.gender ?? '' })
  }

  const toReview = () => {
    const errs = validate(people)
    setErrors(errs)
    if (Object.keys(errs).length === 0) setStep(2)
  }

  const confirm = async () => {
    if (!train) return
    setBusy(true)
    setFailure(null)
    try {
      const booking = await bookingApi.book(train.trainNo, people.map((p) => ({ ...p, name: p.name.trim(), age: Number(p.age) })))
      setResult(booking)
      toast.push('success', `PNR ${booking.pnr} booked`, booking.summary)
      trains.reload(true)
    } catch (err) {
      const e = toApiError(err)
      const fe: Errors = {}
      e.fieldErrors.forEach((f) => (fe[f.field] = f.message))
      setErrors(fe)
      setFailure(e.message)
      if (Object.keys(fe).some((k) => k.startsWith('passengers'))) setStep(1)
    } finally {
      setBusy(false)
    }
  }

  const restart = () => {
    setResult(null)
    setPeople([{ ...EMPTY }])
    setStep(0)
    setTrainNo('')
  }

  if (result) {
    return (
      <>
        <PageHeader title="Booking confirmed" crumbs={[{ label: 'Passenger', to: '/passenger' }, { label: 'Book ticket' }]} />
        <Card className="mx-auto max-w-3xl">
          <div className="flex flex-col items-center px-6 pt-8 text-center">
            <CheckCircle2 className="h-12 w-12 text-green-600" />
            <p className="mt-3 text-sm muted">Your PNR number</p>
            <p className="font-mono text-3xl font-bold tracking-widest text-navy-900 dark:text-white">{result.pnr}</p>
            <p className="mt-1 text-sm muted">{result.summary} · {rupees(result.totalFare)}</p>
          </div>
          <div className="p-6"><JourneyHeader booking={result} /></div>
          <ul className="divide-y divide-slate-100 border-t border-slate-100 dark:divide-navy-800 dark:border-navy-800">
            {result.passengers.map((t) => (
              <li key={t.passengerNo} className="flex items-center justify-between px-6 py-3 text-sm">
                <span>{t.passengerNo}. {t.name} <span className="muted">({t.age}, {t.gender.toLowerCase()})</span></span>
                <TicketBadge label={t.label} />
              </li>
            ))}
          </ul>
          {result.passengers.some((t) => t.status === 'WL') && (
            <p className="mx-6 mt-4 rounded-lg bg-amber-50 p-3 text-sm text-amber-800 dark:bg-amber-500/10 dark:text-amber-200">
              Waitlisted passengers move up automatically when someone cancels, and you'll get a notification when a seat is confirmed.
            </p>
          )}
          <div className="flex flex-wrap justify-center gap-3 p-6">
            <Link to={`/passenger/bookings/${result.pnr}/ticket`} className="btn-primary"><Printer className="h-4 w-4" /> View e-ticket</Link>
            <Link to="/passenger/bookings" className="btn-secondary">My bookings</Link>
            <button className="btn-ghost" onClick={restart}>Book another</button>
          </div>
        </Card>
      </>
    )
  }

  return (
    <>
      <PageHeader title="Book ticket" crumbs={[{ label: 'Passenger', to: '/passenger' }]}
        description="Up to 4 passengers per PNR. Seats are confirmed in booking order; when the train is full, passengers join the waiting list." />
      <Stepper step={step} />

      {step === 0 && (
        bookable.length === 0 ? (
          <Card><EmptyState icon={TrainFront} title="No trains are open for booking" message="Booking closes once a train arrives at the platform." /></Card>
        ) : (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {bookable.map((t) => (
              <button key={t.trainNo} onClick={() => { setTrainNo(t.trainNo); setStep(1) }}
                className={`card card-pad text-left transition hover:-translate-y-0.5 hover:shadow-lift ${trainNo === t.trainNo ? 'ring-2 ring-teal-500' : ''}`}>
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <p className="font-mono text-sm font-semibold">{t.trainNo}</p>
                    <p className="font-semibold">{t.name}</p>
                  </div>
                  <PriorityBadge priority={t.priority} />
                </div>
                <p className="mt-2 text-sm muted">{t.origin} → {t.destination}</p>
                <p className="mt-1 text-sm">Arr <span className="tabular-nums font-medium">{hm(t.expectedArrival)}</span> · Dep <span className="tabular-nums font-medium">{hm(t.expectedDeparture)}</span></p>
                <div className="mt-3 flex items-center justify-between border-t border-slate-100 pt-3 dark:border-navy-800">
                  {t.seatsLeft > 0
                    ? <span className="text-sm font-medium text-green-700 dark:text-green-400">{t.seatsLeft} seats left</span>
                    : <span className="text-sm font-medium text-amber-700 dark:text-amber-400">Full · {t.waiting} on waiting list</span>}
                  <span className="font-semibold">{rupees(t.fare)}</span>
                </div>
              </button>
            ))}
          </div>
        )
      )}

      {step === 1 && train && (
        <Card>
          <CardHeader title="Passenger details" subtitle={`${train.trainNo} ${train.name} · ${hm(train.expectedArrival)}`}
            action={<button className="btn-ghost btn-sm" onClick={addMe}><UserPlus className="h-4 w-4" /> Fill in my details</button>} />
          <div className="space-y-4 p-5">
            {people.map((p, i) => (
              <fieldset key={i} className="grid items-start gap-3 rounded-xl border border-slate-200 p-4 sm:grid-cols-[1fr_110px_150px_auto] dark:border-navy-800">
                <legend className="px-1 text-xs font-semibold uppercase tracking-wider muted">Passenger {i + 1}</legend>
                <TextField label="Full name" value={p.name} maxLength={60} onChange={(e) => update(i, { name: e.target.value })} error={errors[`passengers[${i}].name`]} />
                <TextField label="Age" type="number" min={1} max={120} value={p.age}
                  onChange={(e) => update(i, { age: e.target.value === '' ? '' : Number(e.target.value) })} error={errors[`passengers[${i}].age`]} />
                <SelectField label="Gender" value={p.gender} onChange={(e) => update(i, { gender: e.target.value as Gender })} error={errors[`passengers[${i}].gender`]}>
                  <option value="">Select</option><option value="FEMALE">Female</option><option value="MALE">Male</option><option value="OTHER">Other</option>
                </SelectField>
                <button className="btn-ghost mt-6 h-10 text-red-600 disabled:invisible" disabled={people.length === 1}
                  onClick={() => setPeople((l) => l.filter((_, k) => k !== i))} aria-label={`Remove passenger ${i + 1}`}>
                  <Trash2 className="h-4 w-4" />
                </button>
              </fieldset>
            ))}
            <div className="flex flex-wrap items-center justify-between gap-3">
              <button className="btn-secondary" disabled={people.length >= MAX_PASSENGERS} onClick={() => setPeople((l) => [...l, { ...EMPTY }])}>
                <Plus className="h-4 w-4" /> Add passenger
              </button>
              <span className="text-sm muted">{people.length} of {MAX_PASSENGERS} passengers</span>
            </div>
          </div>
          <div className="flex justify-between border-t border-slate-100 px-5 py-4 dark:border-navy-800">
            <button className="btn-ghost" onClick={() => setStep(0)}><ArrowLeft className="h-4 w-4" /> Change train</button>
            <button className="btn-primary" onClick={toReview}>Review <ArrowRight className="h-4 w-4" /></button>
          </div>
        </Card>
      )}

      {step === 1 && !train && <Card><EmptyState icon={TrainFront} title="Train not found" action={<button className="btn-primary" onClick={() => setStep(0)}>Choose a train</button>} /></Card>}

      {step === 2 && train && (
        <Card>
          <CardHeader title="Review and confirm" subtitle="Check the details before booking. Statuses are confirmed by the server when you book." />
          <div className="grid gap-6 p-5 lg:grid-cols-3">
            <div className="lg:col-span-2">
              <p className="font-mono text-sm font-semibold">{train.trainNo} <span className="font-sans font-normal">{train.name}</span></p>
              <p className="text-sm muted">{train.origin} → {train.destination} · arr {hm(train.expectedArrival)} · dep {hm(train.expectedDeparture)}</p>
              <table className="mt-4 min-w-full text-sm">
                <thead className="table-head"><tr><th className="px-3 py-2">#</th><th className="px-3 py-2">Name</th><th className="px-3 py-2">Age</th><th className="px-3 py-2">Gender</th><th className="px-3 py-2">Expected</th></tr></thead>
                <tbody className="divide-y divide-slate-100 dark:divide-navy-800">
                  {people.map((p, i) => (
                    <tr key={i}>
                      <td className="px-3 py-2">{i + 1}</td><td className="px-3 py-2">{p.name}</td><td className="px-3 py-2">{p.age}</td>
                      <td className="px-3 py-2">{String(p.gender).toLowerCase()}</td>
                      <td className="px-3 py-2"><TicketBadge label={predict(train, people.length)[i]} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="rounded-xl bg-slate-50 p-4 dark:bg-navy-950/50">
              <p className="text-sm font-semibold">Fare summary</p>
              <dl className="mt-3 space-y-1.5 text-sm">
                <div className="flex justify-between"><dt className="muted">Fare per passenger</dt><dd>{rupees(train.fare)}</dd></div>
                <div className="flex justify-between"><dt className="muted">Passengers</dt><dd>× {people.length}</dd></div>
                <div className="flex justify-between border-t border-slate-200 pt-2 font-semibold dark:border-navy-800"><dt>Total</dt><dd>{rupees(train.fare * people.length)}</dd></div>
              </dl>
              <p className="mt-3 text-xs muted">Fares are shown for information. There is no payment step in this system.</p>
            </div>
          </div>
          {failure && <p role="alert" className="mx-5 mb-4 rounded-lg border border-red-300 bg-red-50 p-3 text-sm text-red-700 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-200">{failure}</p>}
          <div className="flex justify-between border-t border-slate-100 px-5 py-4 dark:border-navy-800">
            <button className="btn-ghost" onClick={() => setStep(1)}><ArrowLeft className="h-4 w-4" /> Edit passengers</button>
            <Button onClick={confirm} loading={busy}>Confirm booking</Button>
          </div>
        </Card>
      )}
    </>
  )
}
