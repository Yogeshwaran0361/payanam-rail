// Colour coding of the design system: CNF green, WL amber, CAN grey, REJECTED red,
// EMERGENCY red, CONNECTION violet, NORMAL slate.

export type Tone = 'green' | 'amber' | 'grey' | 'red' | 'violet' | 'blue' | 'teal' | 'navy'

export function ticketTone(label: string): Tone {
  if (label.startsWith('CNF')) return 'green'
  if (label.startsWith('WL')) return 'amber'
  if (label.startsWith('CAN')) return 'grey'
  return 'blue'
}

export function priorityTone(priority: string): Tone {
  switch (priority) {
    case 'EMERGENCY': return 'red'
    case 'CONNECTION': return 'violet'
    default: return 'navy'
  }
}

export function trainStatusTone(status: string): Tone {
  switch (status) {
    case 'ARRIVED': return 'teal'
    case 'PLATFORM_ASSIGNED': return 'blue'
    case 'WAITING_FOR_PLATFORM': return 'amber'
    case 'DEPARTED': return 'grey'
    case 'REJECTED': return 'red'
    default: return 'navy'
  }
}

export function outcomeTone(outcome: string): Tone {
  switch (outcome) {
    case 'ASSIGNED': return 'green'
    case 'QUEUED': return 'amber'
    case 'REJECTED': return 'red'
    default: return 'grey'
  }
}

export function platformTone(status: string): Tone {
  switch (status) {
    case 'FREE': return 'green'
    case 'OCCUPIED': return 'blue'
    case 'MAINTENANCE': return 'amber'
    default: return 'grey'
  }
}

export const TRAIN_STATUS_LABEL: Record<string, string> = {
  SCHEDULED: 'Scheduled',
  WAITING_FOR_PLATFORM: 'Waiting',
  PLATFORM_ASSIGNED: 'Assigned',
  ARRIVED: 'On platform',
  DEPARTED: 'Departed',
  REJECTED: 'Rejected',
}

/** Bar colours for the Gantt chart by declared priority. */
export const PRIORITY_COLOR: Record<string, string> = {
  EMERGENCY: '#dc2626',
  CONNECTION: '#7c3aed',
  NORMAL: '#0d8378',
}
