import {
  Activity, Bell, CalendarClock, ClipboardList, FlaskConical, Gauge, LayoutDashboard, ListOrdered, PlayCircle,
  Radio, ScrollText, Search, Settings, SignpostBig, Ticket, TicketCheck, TrainFront, User, Warehouse, FileSearch,
  type LucideIcon,
} from 'lucide-react'
import type { Permission } from '../../api/types'

export interface NavItem {
  to: string
  label: string
  icon: LucideIcon
  /** The user needs at least one of these permissions (station portal only). */
  anyOf?: Permission[]
  end?: boolean
}
export interface NavSection { title: string; items: NavItem[] }

const TRAIN_OPS: Permission[] = ['MANAGE_TRAINS', 'MANAGE_PLATFORMS']

export const PASSENGER_NAV: NavSection[] = [
  {
    title: 'Travel',
    items: [
      { to: '/passenger', label: 'Dashboard', icon: LayoutDashboard, end: true },
      { to: '/passenger/trains', label: 'Search Trains', icon: Search },
      { to: '/passenger/book', label: 'Book Ticket', icon: Ticket },
      { to: '/passenger/bookings', label: 'My Bookings', icon: ClipboardList },
    ],
  },
  {
    title: 'Track',
    items: [
      { to: '/passenger/pnr', label: 'PNR Status', icon: FileSearch },
      { to: '/passenger/live', label: 'Live Train Status', icon: Radio },
      { to: '/passenger/notifications', label: 'Notifications', icon: Bell },
      { to: '/passenger/profile', label: 'Profile', icon: User },
    ],
  },
]

export const STATION_NAV: NavSection[] = [
  {
    title: 'Overview',
    items: [
      { to: '/station', label: 'Control Centre', icon: Gauge, anyOf: ['VIEW_DASHBOARD'], end: true },
      { to: '/station/board', label: 'Platform Board', icon: SignpostBig, anyOf: TRAIN_OPS },
      { to: '/station/queue', label: 'Platform Queue', icon: ListOrdered, anyOf: TRAIN_OPS },
    ],
  },
  {
    title: 'Operations',
    items: [
      { to: '/station/arrivals', label: 'Arrival Requests', icon: Activity, anyOf: TRAIN_OPS },
      { to: '/station/operations', label: 'Train Operations', icon: TrainFront, anyOf: TRAIN_OPS },
      { to: '/station/reservations', label: 'Reservations', icon: TicketCheck, anyOf: ['MANAGE_RESERVATIONS'] },
      { to: '/station/timetable', label: 'Timetable', icon: CalendarClock, anyOf: ['MANAGE_TRAINS'] },
      { to: '/station/platforms', label: 'Platforms', icon: Warehouse, anyOf: ['MANAGE_PLATFORMS'] },
    ],
  },
  {
    title: 'Administration',
    items: [
      { to: '/station/simulation', label: 'Simulation', icon: PlayCircle, anyOf: ['CONTROL_SIMULATION'] },
      { to: '/station/audit', label: 'Audit Log', icon: ScrollText, anyOf: ['VIEW_AUDIT'] },
      { to: '/station/tests', label: 'Test Scenarios Lab', icon: FlaskConical, anyOf: ['RUN_TESTS'] },
      { to: '/station/settings', label: 'Settings', icon: Settings, anyOf: ['VIEW_DASHBOARD'] },
    ],
  },
]

/** Sections filtered to what the user may open, so a Booking Clerk never sees platform tools. */
export function visibleNav(sections: NavSection[], can: (p: Permission) => boolean): NavSection[] {
  return sections
    .map((s) => ({ ...s, items: s.items.filter((i) => !i.anyOf || i.anyOf.some(can)) }))
    .filter((s) => s.items.length > 0)
}
