import { useState, type FormEvent } from 'react'
import { KeyRound, UserRound } from 'lucide-react'
import { toApiError } from '../../api/client'
import { authApi } from '../../api/endpoints'
import { Button } from '../../components/ui/Button'
import { Card, CardHeader } from '../../components/ui/Card'
import { PasswordField, TextField } from '../../components/ui/Field'
import { PageHeader } from '../../components/ui/PageHeader'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import { dateTime } from '../../lib/format'
import { isIndianMobile, isStrongPassword } from '../../lib/validation'

type Errors = Record<string, string>
const fieldErrors = (e: ReturnType<typeof toApiError>): Errors =>
  Object.fromEntries(e.fieldErrors.map((f) => [f.field, f.message]))

/** Profile details, contact update and password change. Used by both portals (staff have no city). */
export function Profile({ staff = false }: { staff?: boolean }) {
  const { user, refresh } = useAuth()
  const toast = useToast()
  const [form, setForm] = useState({ fullName: user?.fullName ?? '', mobile: user?.mobile ?? '', city: user?.city ?? '' })
  const [pw, setPw] = useState({ currentPassword: '', password: '', confirmPassword: '' })
  const [errors, setErrors] = useState<Errors>({})
  const [pwErrors, setPwErrors] = useState<Errors>({})
  const [saving, setSaving] = useState(false)
  const [changing, setChanging] = useState(false)
  if (!user) return null

  const save = async (e: FormEvent) => {
    e.preventDefault()
    const errs: Errors = {}
    if (form.fullName.trim().length < 2) errs.fullName = 'Name must be 2-80 characters'
    if (!isIndianMobile(form.mobile)) errs.mobile = 'Enter a 10-digit Indian mobile number starting with 6-9'
    if (!staff && !form.city.trim()) errs.city = 'City is required'
    setErrors(errs)
    if (Object.keys(errs).length) return
    setSaving(true)
    try {
      await authApi.updateProfile({ fullName: form.fullName.trim(), mobile: form.mobile, city: staff ? undefined : form.city.trim() })
      await refresh()
      toast.push('success', 'Profile updated')
    } catch (err) {
      const apiErr = toApiError(err)
      setErrors(fieldErrors(apiErr))
      toast.push('error', 'Could not save', apiErr.message)
    } finally {
      setSaving(false)
    }
  }

  const changePassword = async (e: FormEvent) => {
    e.preventDefault()
    const errs: Errors = {}
    if (!pw.currentPassword) errs.currentPassword = 'Enter your current password'
    if (!isStrongPassword(pw.password)) errs.password = '8+ characters with upper, lower, digit and special character'
    if (pw.password !== pw.confirmPassword) errs.confirmPassword = 'Passwords do not match'
    setPwErrors(errs)
    if (Object.keys(errs).length) return
    setChanging(true)
    try {
      await authApi.changePassword(pw)
      setPw({ currentPassword: '', password: '', confirmPassword: '' })
      toast.push('success', 'Password changed', 'Use the new password next time you sign in.')
    } catch (err) {
      setPwErrors(fieldErrors(toApiError(err)))
    } finally {
      setChanging(false)
    }
  }

  const facts: [string, string | undefined][] = staff
    ? [['Employee ID', user.employeeId], ['Designation', user.displayTitle], ['Official email', user.email]]
    : [['Email', user.email], ['Date of birth', user.dateOfBirth], ['Gender', user.gender?.toLowerCase()]]

  return (
    <>
      <PageHeader title={staff ? 'My account' : 'Profile'} crumbs={staff ? [] : [{ label: 'Passenger', to: '/passenger' }]} />
      <div className="grid gap-6 lg:grid-cols-3">
        <Card>
          <div className="flex flex-col items-center p-6 text-center">
            <span className="flex h-16 w-16 items-center justify-center rounded-full bg-navy-800 text-xl font-bold text-white dark:bg-teal-600">
              {user.fullName.split(' ').map((p) => p[0]).slice(0, 2).join('').toUpperCase()}
            </span>
            <p className="mt-3 text-lg font-semibold">{user.fullName}</p>
            <p className="text-sm muted">{user.displayTitle}</p>
          </div>
          <dl className="space-y-2 border-t border-slate-100 p-5 text-sm dark:border-navy-800">
            {facts.map(([k, v]) => <div key={k} className="flex justify-between gap-3"><dt className="muted">{k}</dt><dd className="text-right font-medium">{v ?? '–'}</dd></div>)}
            <div className="flex justify-between gap-3"><dt className="muted">Member since</dt><dd className="font-medium">{dateTime(user.createdAt)}</dd></div>
            <div className="flex justify-between gap-3"><dt className="muted">Last sign-in</dt><dd className="font-medium">{dateTime(user.lastLoginAt)}</dd></div>
          </dl>
        </Card>
        <div className="space-y-6 lg:col-span-2">
          <Card>
            <CardHeader title="Contact details" icon={<UserRound className="h-5 w-5" />} subtitle="Email cannot be changed because it is your sign-in ID." />
            <form onSubmit={save} noValidate className="grid gap-4 p-5 sm:grid-cols-2">
              <TextField label="Full name" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} error={errors.fullName} />
              <TextField label="Mobile number" inputMode="numeric" maxLength={10} value={form.mobile}
                onChange={(e) => setForm({ ...form, mobile: e.target.value.replace(/\D/g, '') })} error={errors.mobile} />
              {!staff && <TextField label="City" value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} error={errors.city} />}
              <div className="flex items-end justify-end sm:col-span-2"><Button type="submit" loading={saving}>Save changes</Button></div>
            </form>
          </Card>
          <Card>
            <CardHeader title="Change password" icon={<KeyRound className="h-5 w-5" />} />
            <form onSubmit={changePassword} noValidate className="grid gap-4 p-5 sm:grid-cols-3">
              <PasswordField label="Current password" autoComplete="current-password" value={pw.currentPassword}
                onChange={(e) => setPw({ ...pw, currentPassword: e.target.value })} error={pwErrors.currentPassword} />
              <PasswordField label="New password" autoComplete="new-password" value={pw.password}
                onChange={(e) => setPw({ ...pw, password: e.target.value })} error={pwErrors.password} />
              <PasswordField label="Confirm new password" autoComplete="new-password" value={pw.confirmPassword}
                onChange={(e) => setPw({ ...pw, confirmPassword: e.target.value })} error={pwErrors.confirmPassword} />
              <div className="flex justify-end sm:col-span-3"><Button type="submit" variant="navy" loading={changing}>Change password</Button></div>
            </form>
          </Card>
        </div>
      </div>
    </>
  )
}
