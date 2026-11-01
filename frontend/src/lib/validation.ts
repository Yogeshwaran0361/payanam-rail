// Client-side validation that mirrors the backend rules, so errors show inline before submitting.
// The backend still validates everything (never trust the browser).

export interface PasswordCheck { label: string; ok: boolean }

export function passwordChecks(pw: string): PasswordCheck[] {
  return [
    { label: 'At least 8 characters', ok: pw.length >= 8 },
    { label: 'An upper-case letter', ok: /[A-Z]/.test(pw) },
    { label: 'A lower-case letter', ok: /[a-z]/.test(pw) },
    { label: 'A digit', ok: /\d/.test(pw) },
    { label: 'A special character', ok: /[^A-Za-z0-9\s]/.test(pw) },
  ]
}

export function isStrongPassword(pw: string): boolean {
  return passwordChecks(pw).every((c) => c.ok)
}

/** 10-digit Indian mobile number starting with 6, 7, 8 or 9. */
export function isIndianMobile(m: string): boolean {
  return /^[6-9]\d{9}$/.test(m)
}

export function isEmail(e: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(e)
}

export function isEmployeeId(id: string): boolean {
  return /^PJ-EMP-\d{4}$/.test(id)
}

/** Whole years between the date of birth and today. */
export function ageOn(dob: string, today = new Date()): number {
  const d = new Date(dob)
  let age = today.getFullYear() - d.getFullYear()
  const m = today.getMonth() - d.getMonth()
  if (m < 0 || (m === 0 && today.getDate() < d.getDate())) age--
  return age
}

export function isAdult(dob: string, today = new Date()): boolean {
  if (!dob) return false
  const age = ageOn(dob, today)
  return age >= 18 && age <= 120
}

/** Password strength 0–5 for the strength meter. */
export function strength(pw: string): number {
  return passwordChecks(pw).filter((c) => c.ok).length
}
