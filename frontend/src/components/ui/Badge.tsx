import type { ReactNode } from 'react'
import { priorityTone, ticketTone, trainStatusTone, TRAIN_STATUS_LABEL, type Tone } from '../../lib/status'
import { titleCase } from '../../lib/format'

const TONES: Record<Tone, string> = {
  green: 'bg-green-100 text-green-800 ring-green-600/20 dark:bg-green-500/15 dark:text-green-300',
  amber: 'bg-amber-100 text-amber-800 ring-amber-600/20 dark:bg-amber-500/15 dark:text-amber-300',
  grey: 'bg-slate-100 text-slate-600 ring-slate-500/20 dark:bg-slate-500/15 dark:text-slate-300',
  red: 'bg-red-100 text-red-700 ring-red-600/20 dark:bg-red-500/15 dark:text-red-300',
  violet: 'bg-violet-100 text-violet-700 ring-violet-600/20 dark:bg-violet-500/15 dark:text-violet-300',
  blue: 'bg-sky-100 text-sky-800 ring-sky-600/20 dark:bg-sky-500/15 dark:text-sky-300',
  teal: 'bg-teal-100 text-teal-800 ring-teal-600/20 dark:bg-teal-500/15 dark:text-teal-300',
  navy: 'bg-navy-100 text-navy-800 ring-navy-600/20 dark:bg-navy-500/20 dark:text-navy-100',
}

export function Badge({ tone, children, className = '' }: { tone: Tone; children: ReactNode; className?: string }) {
  return <span className={`chip ring-1 ring-inset ${TONES[tone]} ${className}`}>{children}</span>
}

/** Ticket status like a real ticket: CNF/S4 (green), WL/2 (amber), CAN (grey). */
export function TicketBadge({ label }: { label: string }) {
  return <Badge tone={ticketTone(label)} className="font-mono">{label}</Badge>
}

export function PriorityBadge({ priority }: { priority: string }) {
  return <Badge tone={priorityTone(priority)}>{titleCase(priority)}</Badge>
}

export function TrainStatusBadge({ status }: { status: string }) {
  return <Badge tone={trainStatusTone(status)}>{TRAIN_STATUS_LABEL[status] ?? titleCase(status)}</Badge>
}
