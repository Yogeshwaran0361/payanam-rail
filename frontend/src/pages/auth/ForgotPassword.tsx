import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ArrowLeft, KeyRound, MessageSquareText } from 'lucide-react'
import { toApiError } from '../../api/client'
import { authApi } from '../../api/endpoints'
import type { ForgotPasswordResponse } from '../../api/types'
import { Logo } from '../../components/brand/Logo'
import { Button } from '../../components/ui/Button'
import { PasswordField, TextField } from '../../components/ui/Field'
import { useToast } from '../../context/ToastContext'
import { useDocumentTitle } from '../../hooks/useDocumentTitle'
import { isEmail, isIndianMobile, isStrongPassword, passwordChecks } from '../../lib/validation'

/**
 * Forgot password, simulated: step 1 asks for the email or mobile and the server creates a 6-digit OTP.
 * There is no SMS gateway in this project, so the OTP is shown on screen. Step 2 sets the new password.
 */
export function ForgotPassword() {
  useDocumentTitle('Reset password')
  const navigate = useNavigate()
  const toast = useToast()
  const [identifier, setIdentifier] = useState('')
  const [sent, setSent] = useState<ForgotPasswordResponse | null>(null)
  const [otp, setOtp] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [banner, setBanner] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const requestOtp = async (e: FormEvent) => {
    e.preventDefault()
    const id = identifier.trim()
    if (!isEmail(id) && !isIndianMobile(id)) {
      setErrors({ identifier: 'Enter a valid email or 10-digit mobile number' })
      return
    }
    setErrors({})
    setBanner(null)
    setBusy(true)
    try {
      setSent(await authApi.forgot(id))
    } catch (err) {
      setBanner(toApiError(err).message)
    } finally {
      setBusy(false)
    }
  }

  const reset = async (e: FormEvent) => {
    e.preventDefault()
    const errs: Record<string, string> = {}
    if (!/^\d{6}$/.test(otp)) errs.otp = 'Enter the 6-digit OTP'
    if (!isStrongPassword(password)) errs.password = 'Password does not meet all the rules'
    if (password !== confirm) errs.confirmPassword = 'Passwords do not match'
    setErrors(errs)
    setBanner(null)
    if (Object.keys(errs).length) return
    setBusy(true)
    try {
      await authApi.reset({ identifier: identifier.trim(), otp, password, confirmPassword: confirm })
      toast.push('success', 'Password changed', 'Sign in with your new password.')
      navigate('/signin', { replace: true })
    } catch (err) {
      const apiErr = toApiError(err)
      const fe: Record<string, string> = {}
      apiErr.fieldErrors.forEach((f) => (fe[f.field] = f.message))
      setErrors(fe)
      setBanner(apiErr.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-b from-slate-50 to-slate-100 px-4 py-10 dark:from-navy-950 dark:to-navy-900">
      <div className="w-full max-w-md">
        <Link to="/" className="mb-6 inline-block"><Logo /></Link>
        <div className="card p-6 sm:p-8">
          <div className="inline-flex rounded-xl bg-teal-50 p-2.5 text-teal-700 dark:bg-teal-500/15 dark:text-teal-300"><KeyRound className="h-5 w-5" /></div>
          <h1 className="mt-4 text-xl font-bold">Reset your password</h1>
          <p className="mt-1 text-sm muted">
            {sent ? 'Enter the OTP and choose a new password.' : "Enter your account's email or mobile number and we'll send a one-time password."}
          </p>
          {banner && <div role="alert" className="mt-4 rounded-lg border border-red-300 bg-red-50 p-3 text-sm text-red-700 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-200">{banner}</div>}

          {!sent ? (
            <form onSubmit={requestOtp} noValidate className="mt-6 space-y-4">
              <TextField label="Email or mobile number" value={identifier} onChange={(e) => setIdentifier(e.target.value)} error={errors.identifier} autoFocus />
              <Button type="submit" loading={busy} className="w-full">Send OTP</Button>
            </form>
          ) : (
            <form onSubmit={reset} noValidate className="mt-6 space-y-4">
              <div className="rounded-lg border border-teal-300 bg-teal-50 p-3 text-sm dark:border-teal-500/30 dark:bg-teal-500/10">
                <p className="flex items-center gap-1.5 font-medium text-teal-800 dark:text-teal-200"><MessageSquareText className="h-4 w-4" /> Simulated SMS</p>
                <p className="mt-1 muted">{sent.message}</p>
                {sent.simulatedOtp && (
                  <p className="mt-2">Your OTP is <span className="font-mono text-lg font-bold tracking-widest text-navy-900 dark:text-white">{sent.simulatedOtp}</span>
                    <span className="muted"> (valid {sent.expiresInMinutes} min)</span></p>
                )}
              </div>
              <TextField label="OTP" inputMode="numeric" maxLength={6} value={otp} onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))} error={errors.otp} />
              <PasswordField label="New password" autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} error={errors.password}
                hint={passwordChecks(password).filter((c) => !c.ok).map((c) => c.label).join(' · ') || 'Looks good'} />
              <PasswordField label="Confirm new password" autoComplete="new-password" value={confirm} onChange={(e) => setConfirm(e.target.value)} error={errors.confirmPassword} />
              <Button type="submit" loading={busy} className="w-full">Change password</Button>
              <button type="button" className="w-full text-sm muted hover:text-teal-600" onClick={() => { setSent(null); setOtp('') }}>
                Use a different account
              </button>
            </form>
          )}
        </div>
        <Link to="/signin" className="mt-5 inline-flex items-center gap-1.5 text-sm font-medium text-teal-600 hover:underline">
          <ArrowLeft className="h-4 w-4" /> Back to sign in
        </Link>
      </div>
    </div>
  )
}
