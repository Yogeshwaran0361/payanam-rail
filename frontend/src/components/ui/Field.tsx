import { useId, useState, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes } from 'react'
import { Eye, EyeOff } from 'lucide-react'

interface FieldProps { label: string; error?: string; hint?: ReactNode }

/** Text input with a label, inline error and hint, wired for screen readers (aria-invalid / describedby). */
export function TextField({ label, error, hint, className = '', ...rest }: FieldProps & InputHTMLAttributes<HTMLInputElement>) {
  const id = useId()
  return (
    <div className={className}>
      <label htmlFor={id} className="label">{label}</label>
      <input id={id} className={`input ${error ? 'input-error' : ''}`} aria-invalid={!!error}
        aria-describedby={error ? `${id}-err` : hint ? `${id}-hint` : undefined} {...rest} />
      {error ? <p id={`${id}-err`} className="field-error">{error}</p>
        : hint ? <div id={`${id}-hint`} className="mt-1 text-xs muted">{hint}</div> : null}
    </div>
  )
}

/** Password input with a show/hide toggle. */
export function PasswordField({ label, error, hint, className = '', ...rest }: FieldProps & InputHTMLAttributes<HTMLInputElement>) {
  const id = useId()
  const [show, setShow] = useState(false)
  return (
    <div className={className}>
      <label htmlFor={id} className="label">{label}</label>
      <div className="relative">
        <input id={id} type={show ? 'text' : 'password'} className={`input pr-10 ${error ? 'input-error' : ''}`}
          aria-invalid={!!error} aria-describedby={error ? `${id}-err` : undefined} {...rest} />
        <button type="button" onClick={() => setShow((s) => !s)} aria-label={show ? 'Hide password' : 'Show password'}
          className="absolute inset-y-0 right-0 flex items-center px-3 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200">
          {show ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
        </button>
      </div>
      {error ? <p id={`${id}-err`} className="field-error">{error}</p> : hint ? <div className="mt-1 text-xs muted">{hint}</div> : null}
    </div>
  )
}

export function SelectField({ label, error, children, className = '', ...rest }: FieldProps & SelectHTMLAttributes<HTMLSelectElement> & { children: ReactNode }) {
  const id = useId()
  return (
    <div className={className}>
      <label htmlFor={id} className="label">{label}</label>
      <select id={id} className={`input ${error ? 'input-error' : ''}`} aria-invalid={!!error} {...rest}>{children}</select>
      {error && <p className="field-error">{error}</p>}
    </div>
  )
}

export function Checkbox({ label, error, ...rest }: { label: ReactNode; error?: string } & InputHTMLAttributes<HTMLInputElement>) {
  const id = useId()
  return (
    <div>
      <div className="flex items-start gap-2">
        <input id={id} type="checkbox" className="mt-0.5 h-4 w-4 rounded border-slate-300 text-teal-600 focus:ring-teal-500" {...rest} />
        <label htmlFor={id} className="text-sm text-slate-600 dark:text-slate-300">{label}</label>
      </div>
      {error && <p className="field-error">{error}</p>}
    </div>
  )
}
