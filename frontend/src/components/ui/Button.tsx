import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { Spinner } from './Spinner'

type Variant = 'primary' | 'secondary' | 'danger' | 'ghost' | 'navy'

/** Button with variants and a loading state (disabled + spinner while a request runs). */
export function Button({ variant = 'primary', loading = false, size, children, className = '', disabled, ...rest }:
  ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant; loading?: boolean; size?: 'sm'; children: ReactNode }) {
  return (
    <button className={`btn-${variant} ${size === 'sm' ? 'btn-sm' : ''} ${className}`} disabled={disabled || loading} {...rest}>
      {loading && <Spinner />}
      {children}
    </button>
  )
}
