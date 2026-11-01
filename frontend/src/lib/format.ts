// Date/time helpers. The backend sends ISO local date-times ("2026-10-18T07:00:00") in simulation time.

export function hm(iso?: string | null): string {
  if (!iso) return '–'
  const t = iso.split('T')[1]
  return t ? t.slice(0, 5) : '–'
}

export function dayMonth(iso?: string | null): string {
  if (!iso) return '–'
  const d = new Date(iso)
  return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short' })
}

export function dateTime(iso?: string | null): string {
  if (!iso) return '–'
  const d = new Date(iso)
  return `${d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}, ${d.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: false })}`
}

export function rupees(value: number): string {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(value)
}

export function minutesBetween(a: string, b: string): number {
  return Math.round((new Date(b).getTime() - new Date(a).getTime()) / 60000)
}

export function titleCase(s: string): string {
  return s.toLowerCase().replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase())
}
