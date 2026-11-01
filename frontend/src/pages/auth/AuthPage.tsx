import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { AlertCircle, Building2, Check, Info, Lock, UserRound, X } from 'lucide-react'
import { toApiError } from '../../api/client'
import { authApi, type PassengerSignUp, type StaffSignUp } from '../../api/endpoints'
import type { ApiError, AuthResponse, UserProfile } from '../../api/types'
import { Logo } from '../../components/brand/Logo'
import { RailwayIllustration } from '../../components/brand/RailwayIllustration'
import { Button } from '../../components/ui/Button'
import { Checkbox, PasswordField, SelectField, TextField } from '../../components/ui/Field'
import { homeOf, useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import { useDocumentTitle } from '../../hooks/useDocumentTitle'
import { isAdult, isEmail, isEmployeeId, isIndianMobile, isStrongPassword, passwordChecks, strength } from '../../lib/validation'

type Mode = 'signin' | 'signup'
type Kind = 'passenger' | 'staff'
type Errors = Record<string, string>

const DEMO = {
  passenger: { identifier: 'passenger@payanam.in', password: 'Passenger@123', label: 'Arun Kumar, passenger' },
  staff: { identifier: 'stationmaster@payanam.in', password: 'Station@123', label: 'Ramesh Sundaram, Station Master' },
}

/** Server field errors ("fieldErrors") → the same shape as client-side errors. */
function serverErrors(e: ApiError): Errors {
  const out: Errors = {}
  for (const f of e.fieldErrors) out[f.field] = f.message
  return out
}

/** After sign-in, return to the page the user wanted if it belongs to their portal. */
function destination(user: UserProfile, from?: string): string {
  const home = homeOf(user)
  return from && from.startsWith(home) ? from : home
}

function PasswordStrength({ password }: { password: string }) {
  const score = strength(password)
  const colors = ['bg-slate-200', 'bg-red-500', 'bg-orange-500', 'bg-amber-500', 'bg-lime-500', 'bg-green-600']
  const words = ['', 'Very weak', 'Weak', 'Fair', 'Good', 'Strong']
  if (!password) return null
  return (
    <div className="mt-2" aria-live="polite">
      <div className="flex gap-1">
        {[1, 2, 3, 4, 5].map((i) => (
          <span key={i} className={`h-1.5 flex-1 rounded-full ${i <= score ? colors[score] : 'bg-slate-200 dark:bg-navy-700'}`} />
        ))}
      </div>
      <p className="mt-1 text-xs muted">Strength: <span className="font-medium">{words[score]}</span></p>
      <ul className="mt-1.5 grid grid-cols-2 gap-x-3 gap-y-0.5">
        {passwordChecks(password).map((c) => (
          <li key={c.label} className={`flex items-center gap-1 text-xs ${c.ok ? 'text-green-600' : 'muted'}`}>
            {c.ok ? <Check className="h-3 w-3" /> : <X className="h-3 w-3" />} {c.label}
          </li>
        ))}
      </ul>
    </div>
  )
}

function SignInForm({ kind, onDone }: { kind: Kind; onDone: (auth: AuthResponse, remember: boolean) => void }) {
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [remember, setRemember] = useState(false)
  const [errors, setErrors] = useState<Errors>({})
  const [banner, setBanner] = useState<{ tone: 'red' | 'amber'; text: string } | null>(null)
  const [busy, setBusy] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const errs: Errors = {}
    if (!identifier.trim()) errs.identifier = 'Enter your email or mobile number'
    else if (!isEmail(identifier.trim()) && !isIndianMobile(identifier.trim())) errs.identifier = 'Enter a valid email or 10-digit mobile number'
    if (!password) errs.password = 'Enter your password'
    setErrors(errs)
    setBanner(null)
    if (Object.keys(errs).length) return
    setBusy(true)
    try {
      onDone(await authApi.signIn(identifier.trim(), password, remember), remember)
    } catch (err) {
      const apiErr = toApiError(err)
      setErrors(serverErrors(apiErr))
      setBanner({ tone: apiErr.status === 423 ? 'amber' : 'red', text: apiErr.message })
    } finally {
      setBusy(false)
    }
  }

  const demo = DEMO[kind]
  return (
    <form onSubmit={submit} noValidate className="space-y-4">
      {banner && (
        <div role="alert" className={`flex gap-2 rounded-lg border p-3 text-sm ${banner.tone === 'amber'
          ? 'border-amber-300 bg-amber-50 text-amber-800 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-200'
          : 'border-red-300 bg-red-50 text-red-700 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-200'}`}>
          {banner.tone === 'amber' ? <Lock className="mt-0.5 h-4 w-4 shrink-0" /> : <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />}
          {banner.text}
        </div>
      )}
      <TextField label={kind === 'staff' ? 'Official email or mobile' : 'Email or mobile number'} autoComplete="username"
        value={identifier} onChange={(e) => setIdentifier(e.target.value)} error={errors.identifier}
        placeholder={kind === 'staff' ? 'name@payanam.in' : 'you@example.com or 98765 43210'} />
      <PasswordField label="Password" autoComplete="current-password" value={password}
        onChange={(e) => setPassword(e.target.value)} error={errors.password} />
      <div className="flex items-center justify-between">
        <Checkbox label="Remember me for 7 days" checked={remember} onChange={(e) => setRemember(e.target.checked)} />
        <Link to="/forgot-password" className="text-sm font-medium text-teal-600 hover:underline">Forgot password?</Link>
      </div>
      <Button type="submit" loading={busy} className="w-full">Sign in</Button>
      <div className="rounded-lg border border-dashed border-teal-300 bg-teal-50/60 p-3 text-sm dark:border-teal-500/30 dark:bg-teal-500/5">
        <p className="flex items-center gap-1.5 font-medium text-teal-800 dark:text-teal-200"><Info className="h-4 w-4" /> Demo account</p>
        <p className="mt-1 muted">{demo.label}: <span className="font-mono">{demo.identifier}</span> / <span className="font-mono">{demo.password}</span></p>
        <button type="button" className="mt-2 text-xs font-semibold text-teal-700 hover:underline dark:text-teal-300"
          onClick={() => { setIdentifier(demo.identifier); setPassword(demo.password); setErrors({}) }}>
          Fill in demo credentials
        </button>
      </div>
    </form>
  )
}

const EMPTY_PASSENGER: PassengerSignUp = {
  fullName: '', email: '', mobile: '', dateOfBirth: '', gender: '', city: '', password: '', confirmPassword: '', acceptTerms: false,
}
const EMPTY_STAFF: StaffSignUp = {
  fullName: '', employeeId: '', designation: '', email: '', mobile: '', password: '', confirmPassword: '', accessCode: '',
}
const NAME_RE = /^[A-Za-z][A-Za-z .'-]*$/

function validateCommon(f: { fullName: string; email: string; mobile: string; password: string; confirmPassword: string }): Errors {
  const e: Errors = {}
  const name = f.fullName.trim()
  if (!name) e.fullName = 'Full name is required'
  else if (name.length < 2 || name.length > 80) e.fullName = 'Name must be 2-80 characters'
  else if (!NAME_RE.test(name)) e.fullName = "Name can contain letters, spaces, . ' and - only"
  if (!f.email.trim()) e.email = 'Email is required'
  else if (!isEmail(f.email.trim())) e.email = 'Enter a valid email address'
  if (!f.mobile) e.mobile = 'Mobile number is required'
  else if (!isIndianMobile(f.mobile)) e.mobile = 'Enter a 10-digit Indian mobile number starting with 6-9'
  if (!f.password) e.password = 'Password is required'
  else if (!isStrongPassword(f.password)) e.password = 'Password does not meet all the rules below'
  if (!f.confirmPassword) e.confirmPassword = 'Please confirm the password'
  else if (f.password !== f.confirmPassword) e.confirmPassword = 'Passwords do not match'
  return e
}

function PassengerSignUpForm({ onDone }: { onDone: (auth: AuthResponse) => void }) {
  const [f, setF] = useState(EMPTY_PASSENGER)
  const [errors, setErrors] = useState<Errors>({})
  const [banner, setBanner] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const set = <K extends keyof PassengerSignUp>(k: K, v: PassengerSignUp[K]) => {
    setF((old) => ({ ...old, [k]: v }))
    setErrors((old) => ({ ...old, [k]: '' }))
  }

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const errs = validateCommon(f)
    if (!f.dateOfBirth) errs.dateOfBirth = 'Date of birth is required'
    else if (!isAdult(f.dateOfBirth)) errs.dateOfBirth = 'You must be at least 18 years old'
    if (!f.gender) errs.gender = 'Gender is required'
    if (!f.city.trim()) errs.city = 'City is required'
    if (!f.acceptTerms) errs.acceptTerms = 'You must accept the terms of use'
    setErrors(errs)
    setBanner(null)
    if (Object.values(errs).some(Boolean)) return
    setBusy(true)
    try {
      onDone(await authApi.signUpPassenger({ ...f, fullName: f.fullName.trim(), email: f.email.trim(), city: f.city.trim() }))
    } catch (err) {
      const apiErr = toApiError(err)
      setErrors(serverErrors(apiErr))
      setBanner(apiErr.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} noValidate className="space-y-4">
      {banner && <div role="alert" className="rounded-lg border border-red-300 bg-red-50 p-3 text-sm text-red-700 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-200">{banner}</div>}
      <TextField label="Full name" autoComplete="name" value={f.fullName} onChange={(e) => set('fullName', e.target.value)} error={errors.fullName} />
      <div className="grid gap-4 sm:grid-cols-2">
        <TextField label="Email" type="email" autoComplete="email" value={f.email} onChange={(e) => set('email', e.target.value)} error={errors.email} />
        <TextField label="Mobile number" inputMode="numeric" maxLength={10} autoComplete="tel-national" value={f.mobile}
          onChange={(e) => set('mobile', e.target.value.replace(/\D/g, ''))} error={errors.mobile} hint="10 digits, starts with 6-9" />
      </div>
      <div className="grid gap-4 sm:grid-cols-3">
        <TextField label="Date of birth" type="date" value={f.dateOfBirth} max={new Date().toISOString().slice(0, 10)}
          onChange={(e) => set('dateOfBirth', e.target.value)} error={errors.dateOfBirth} />
        <SelectField label="Gender" value={f.gender} onChange={(e) => set('gender', e.target.value as PassengerSignUp['gender'])} error={errors.gender}>
          <option value="">Select</option><option value="FEMALE">Female</option><option value="MALE">Male</option><option value="OTHER">Other</option>
        </SelectField>
        <TextField label="City" autoComplete="address-level2" value={f.city} onChange={(e) => set('city', e.target.value)} error={errors.city} />
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <PasswordField label="Password" autoComplete="new-password" value={f.password} onChange={(e) => set('password', e.target.value)} error={errors.password} />
        </div>
        <PasswordField label="Confirm password" autoComplete="new-password" value={f.confirmPassword}
          onChange={(e) => set('confirmPassword', e.target.value)} error={errors.confirmPassword} />
      </div>
      <PasswordStrength password={f.password} />
      <Checkbox checked={f.acceptTerms} onChange={(e) => set('acceptTerms', e.target.checked)} error={errors.acceptTerms}
        label="I accept the terms of use and the privacy policy of Payanam Rail." />
      <Button type="submit" loading={busy} className="w-full">Create passenger account</Button>
    </form>
  )
}

function StaffSignUpForm({ onDone }: { onDone: (auth: AuthResponse) => void }) {
  const [f, setF] = useState(EMPTY_STAFF)
  const [errors, setErrors] = useState<Errors>({})
  const [banner, setBanner] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const set = <K extends keyof StaffSignUp>(k: K, v: StaffSignUp[K]) => {
    setF((old) => ({ ...old, [k]: v }))
    setErrors((old) => ({ ...old, [k]: '' }))
  }

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const errs = validateCommon(f)
    if (!errs.email && !f.email.trim().toLowerCase().endsWith('@payanam.in')) errs.email = 'Use your official @payanam.in email address'
    if (!f.employeeId) errs.employeeId = 'Employee ID is required'
    else if (!isEmployeeId(f.employeeId)) errs.employeeId = 'Employee ID must look like PJ-EMP-1234'
    if (!f.designation) errs.designation = 'Designation is required'
    if (!f.accessCode.trim()) errs.accessCode = 'Station access code is required'
    setErrors(errs)
    setBanner(null)
    if (Object.values(errs).some(Boolean)) return
    setBusy(true)
    try {
      onDone(await authApi.signUpStaff({ ...f, fullName: f.fullName.trim(), email: f.email.trim() }))
    } catch (err) {
      const apiErr = toApiError(err)
      setErrors(serverErrors(apiErr))
      setBanner(apiErr.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} noValidate className="space-y-4">
      {banner && <div role="alert" className="rounded-lg border border-red-300 bg-red-50 p-3 text-sm text-red-700 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-200">{banner}</div>}
      <div className="grid gap-4 sm:grid-cols-2">
        <TextField label="Full name" autoComplete="name" value={f.fullName} onChange={(e) => set('fullName', e.target.value)} error={errors.fullName} />
        <TextField label="Employee ID" placeholder="PJ-EMP-1234" value={f.employeeId}
          onChange={(e) => set('employeeId', e.target.value.toUpperCase())} error={errors.employeeId} />
      </div>
      <SelectField label="Designation" value={f.designation} onChange={(e) => set('designation', e.target.value)} error={errors.designation}>
        <option value="">Select your role</option>
        <option value="STATION_MASTER">Station Master (all station tools)</option>
        <option value="PLATFORM_CONTROLLER">Platform Controller (platforms, trains, simulation)</option>
        <option value="BOOKING_CLERK">Booking Clerk (reservations)</option>
      </SelectField>
      <div className="grid gap-4 sm:grid-cols-2">
        <TextField label="Official email" type="email" placeholder="name@payanam.in" value={f.email}
          onChange={(e) => set('email', e.target.value)} error={errors.email} />
        <TextField label="Mobile number" inputMode="numeric" maxLength={10} value={f.mobile}
          onChange={(e) => set('mobile', e.target.value.replace(/\D/g, ''))} error={errors.mobile} />
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <PasswordField label="Password" autoComplete="new-password" value={f.password} onChange={(e) => set('password', e.target.value)} error={errors.password} />
        <PasswordField label="Confirm password" autoComplete="new-password" value={f.confirmPassword}
          onChange={(e) => set('confirmPassword', e.target.value)} error={errors.confirmPassword} />
      </div>
      <PasswordStrength password={f.password} />
      <PasswordField label="Station access code" value={f.accessCode} onChange={(e) => set('accessCode', e.target.value)}
        error={errors.accessCode} hint="Issued by the Station Master. Demo code: PAYANAM-2026" />
      <Button type="submit" variant="navy" loading={busy} className="w-full">Create staff account</Button>
    </form>
  )
}

/** Sign In and Sign Up on one page, with a Passenger | Station Staff switch. */
export function AuthPage({ mode }: { mode: Mode }) {
  const [params, setParams] = useSearchParams()
  const kind: Kind = params.get('as') === 'staff' ? 'staff' : 'passenger'
  const { accept, sessionMessage, clearSessionMessage } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const toast = useToast()
  useDocumentTitle(mode === 'signin' ? 'Sign in' : 'Create account')

  const from = (location.state as { from?: string } | null)?.from
  const setKind = (k: Kind) => setParams(k === 'staff' ? { as: 'staff' } : {}, { replace: true })
  const switchMode = (m: Mode) => navigate({ pathname: m === 'signin' ? '/signin' : '/signup', search: params.toString() }, { replace: true })

  const done = (auth: AuthResponse, remember: boolean, welcome: string) => {
    accept(auth, remember)
    toast.push('success', welcome, `Signed in as ${auth.user.displayTitle}.`)
    navigate(destination(auth.user, from), { replace: true })
  }

  return (
    <div className="grid min-h-screen lg:grid-cols-[1fr_minmax(0,560px)] xl:grid-cols-[1fr_640px]">
      <aside className="relative hidden flex-col justify-between overflow-hidden bg-gradient-to-br from-navy-900 to-navy-800 p-10 text-white lg:flex">
        <Link to="/" aria-label="Payanam Rail home"><Logo light /></Link>
        <div>
          <RailwayIllustration className="w-full max-w-xl" />
          <h2 className="mt-8 text-3xl font-bold text-white">
            {kind === 'staff' ? 'Payanam Junction control room' : 'Your journey starts at Payanam Junction'}
          </h2>
          <p className="mt-3 max-w-lg text-navy-100">
            {kind === 'staff'
              ? 'Allocate platforms, manage the arrival queue, handle delays and keep every reservation in order.'
              : 'Book seats in seconds, follow your waiting list in real time and know your platform before you arrive.'}
          </p>
        </div>
        <p className="text-xs text-navy-300">Secured with JWT sessions and BCrypt-hashed passwords.</p>
      </aside>

      <section className="flex flex-col px-4 py-8 sm:px-10">
        <div className="mb-8 lg:hidden"><Link to="/"><Logo /></Link></div>
        <div className="mx-auto w-full max-w-lg flex-1">
          {sessionMessage && (
            <div role="status" className="mb-5 flex items-start gap-2 rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-800 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-200">
              <Info className="mt-0.5 h-4 w-4 shrink-0" />
              <span className="flex-1">{sessionMessage}</span>
              <button onClick={clearSessionMessage} aria-label="Dismiss"><X className="h-4 w-4" /></button>
            </div>
          )}
          <h1 className="text-2xl font-bold">{mode === 'signin' ? 'Welcome back' : 'Create your account'}</h1>
          <p className="mt-1 text-sm muted">
            {mode === 'signin' ? 'Sign in to continue to your portal.' : 'It takes less than a minute.'}
          </p>

          <div role="tablist" aria-label="Sign in or sign up" className="mt-6 grid grid-cols-2 border-b border-slate-200 dark:border-navy-800">
            {(['signin', 'signup'] as Mode[]).map((m) => (
              <button key={m} role="tab" aria-selected={mode === m} onClick={() => switchMode(m)}
                className={`-mb-px border-b-2 py-2.5 text-sm font-semibold transition ${mode === m
                  ? 'border-teal-600 text-teal-700 dark:text-teal-300' : 'border-transparent muted hover:text-slate-700'}`}>
                {m === 'signin' ? 'Sign In' : 'Sign Up'}
              </button>
            ))}
          </div>

          <div className="mt-5 grid grid-cols-2 gap-2 rounded-xl bg-slate-100 p-1 dark:bg-navy-800" role="radiogroup" aria-label="Account type">
            {([['passenger', 'Passenger', UserRound], ['staff', 'Station Staff', Building2]] as const).map(([k, label, Icon]) => (
              <button key={k} role="radio" aria-checked={kind === k} onClick={() => setKind(k)}
                className={`flex items-center justify-center gap-2 rounded-lg py-2 text-sm font-medium transition ${kind === k
                  ? 'bg-white text-navy-900 shadow-sm dark:bg-navy-950 dark:text-white' : 'muted hover:text-slate-700 dark:hover:text-slate-200'}`}>
                <Icon className="h-4 w-4" /> {label}
              </button>
            ))}
          </div>

          <div className="mt-6">
            {mode === 'signin' ? (
              <SignInForm key={kind} kind={kind} onDone={(a, r) => done(a, r, `Welcome back, ${a.user.fullName.split(' ')[0]}`)} />
            ) : kind === 'passenger' ? (
              <PassengerSignUpForm onDone={(a) => done(a, false, 'Account created')} />
            ) : (
              <StaffSignUpForm onDone={(a) => done(a, false, 'Staff account created')} />
            )}
          </div>

          <p className="mt-6 text-center text-sm muted">
            {mode === 'signin' ? "Don't have an account? " : 'Already registered? '}
            <button className="font-semibold text-teal-600 hover:underline" onClick={() => switchMode(mode === 'signin' ? 'signup' : 'signin')}>
              {mode === 'signin' ? 'Sign up' : 'Sign in'}
            </button>
          </p>
        </div>
      </section>
    </div>
  )
}
