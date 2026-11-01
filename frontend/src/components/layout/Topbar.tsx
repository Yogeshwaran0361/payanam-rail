import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Bell, Clock, FileSearch, LogOut, Menu, Moon, Search, Settings, Sun, TrainFront, User } from 'lucide-react'
import { bookingApi, stationApi, trainApi } from '../../api/endpoints'
import type { AppNotification, StationEvent, Train } from '../../api/types'
import { useAuth } from '../../context/AuthContext'
import { useTheme } from '../../context/ThemeContext'
import { useApi } from '../../hooks/useApi'
import { dateTime, hm } from '../../lib/format'

/** Closes a dropdown when the user clicks outside it or presses Escape. */
function useDismiss(open: boolean, close: () => void) {
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (!open) return
    const onClick = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) close()
    }
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && close()
    document.addEventListener('mousedown', onClick)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onClick)
      document.removeEventListener('keydown', onKey)
    }
  }, [open, close])
  return ref
}

/** Global search: a 10-digit number opens that PNR, anything else matches train numbers and names. */
function GlobalSearch({ staff }: { staff: boolean }) {
  const navigate = useNavigate()
  const [q, setQ] = useState('')
  const [open, setOpen] = useState(false)
  const [trains, setTrains] = useState<Train[] | null>(null)
  const input = useRef<HTMLInputElement>(null)
  const box = useDismiss(open, () => setOpen(false))

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault()
        input.current?.focus()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [])

  useEffect(() => {
    if (open && !trains) trainApi.timetable().then(setTrains).catch(() => setTrains([]))
  }, [open, trains])

  const term = q.trim().toLowerCase()
  const isPnr = /^\d{10}$/.test(term)
  const matches = useMemo(
    () => (term && trains ? trains.filter((t) => t.trainNo.includes(term) || t.name.toLowerCase().includes(term)).slice(0, 6) : []),
    [term, trains],
  )

  const openPnr = () => navigate(staff ? `/station/reservations?pnr=${term}` : `/passenger/pnr?pnr=${term}`)
  const openTrain = (t: Train) => navigate(staff ? `/station/operations?train=${t.trainNo}` : `/passenger/live?train=${t.trainNo}`)
  const go = (action: () => void) => {
    action()
    setQ('')
    setOpen(false)
    input.current?.blur()
  }

  return (
    <div ref={box} className="relative w-full max-w-md">
      <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" aria-hidden />
      <input ref={input} className="input h-9 pl-9 pr-14" placeholder="Search train no, name or PNR" value={q}
        aria-label="Search trains or PNR" role="combobox" aria-expanded={open && !!term} aria-controls="global-search-results"
        onFocus={() => setOpen(true)} onChange={(e) => { setQ(e.target.value); setOpen(true) }}
        onKeyDown={(e) => {
          if (e.key === 'Enter') {
            if (isPnr) go(openPnr)
            else if (matches[0]) go(() => openTrain(matches[0]))
          }
        }} />
      <kbd className="pointer-events-none absolute right-2.5 top-1/2 hidden -translate-y-1/2 rounded border border-slate-200 px-1.5 text-[10px] text-slate-400 sm:block dark:border-navy-700">
        Ctrl K
      </kbd>
      {open && term && (
        <div id="global-search-results" role="listbox"
          className="card absolute left-0 right-0 top-11 z-40 max-h-80 overflow-y-auto p-1.5 shadow-lift">
          {isPnr && (
            <button role="option" aria-selected={false} onClick={() => go(openPnr)}
              className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left text-sm hover:bg-slate-100 dark:hover:bg-navy-800">
              <FileSearch className="h-4 w-4 text-teal-600" /> Open PNR <span className="font-mono font-semibold">{term}</span>
            </button>
          )}
          {matches.map((t) => (
            <button key={t.trainNo} role="option" aria-selected={false} onClick={() => go(() => openTrain(t))}
              className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left text-sm hover:bg-slate-100 dark:hover:bg-navy-800">
              <TrainFront className="h-4 w-4 text-navy-500" />
              <span className="font-mono font-semibold">{t.trainNo}</span>
              <span className="flex-1 truncate">{t.name}</span>
              <span className="text-xs muted">{hm(t.expectedArrival)}</span>
            </button>
          ))}
          {!isPnr && matches.length === 0 && (
            <p className="px-3 py-2 text-sm muted">{trains ? 'No train matches. Enter a 10-digit PNR to look it up.' : 'Loading…'}</p>
          )}
        </div>
      )}
    </div>
  )
}

/** Simulation time of the station, the same for everyone. */
function ClockChip() {
  const { data } = useApi(() => trainApi.stats(), [], 5000)
  return (
    <div className="hidden items-center gap-2 rounded-lg bg-teal-50 px-3 py-1.5 text-sm font-medium text-teal-800 md:flex dark:bg-teal-500/10 dark:text-teal-200"
      title="Simulation time at Payanam Junction">
      <Clock className="h-4 w-4" aria-hidden />
      <span className="tabular-nums">{data ? hm(data.now) : '--:--'}</span>
      <span className="text-xs font-normal opacity-70">station time</span>
    </div>
  )
}

const SEEN_KEY = 'payanam-alerts-seen'

/** Passengers see their notifications; staff see recent station alerts. */
function NotificationBell({ staff }: { staff: boolean }) {
  const [open, setOpen] = useState(false)
  const box = useDismiss(open, () => setOpen(false))
  const passenger = useApi<AppNotification[]>(() => (staff ? Promise.resolve([]) : bookingApi.notifications()), [staff], 10000)
  const alerts = useApi<StationEvent[]>(
    () => (staff ? stationApi.events(40).then((e) => e.filter((x) => x.alert).slice(0, 8)) : Promise.resolve([])), [staff], 10000)
  const [seen, setSeen] = useState(() => {
    try {
      return Number(localStorage.getItem(SEEN_KEY) ?? 0)
    } catch {
      return 0
    }
  })

  const unread = staff
    ? (alerts.data ?? []).filter((a) => a.sequence > seen).length
    : (passenger.data ?? []).filter((n) => !n.read).length

  const toggle = () => {
    setOpen((o) => !o)
    if (staff && alerts.data?.length) {
      const latest = Math.max(...alerts.data.map((a) => a.sequence))
      setSeen(latest)
      try {
        localStorage.setItem(SEEN_KEY, String(latest))
      } catch {
        /* ignore */
      }
    }
  }

  const markAll = async () => {
    await bookingApi.markAllRead()
    passenger.reload(true)
  }

  return (
    <div ref={box} className="relative">
      <button className="btn-ghost relative h-9 w-9 !p-0" onClick={toggle} aria-label={`Notifications${unread ? `, ${unread} unread` : ''}`}
        aria-expanded={open}>
        <Bell className="h-[18px] w-[18px]" />
        {unread > 0 && (
          <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-bold text-white">
            {unread}
          </span>
        )}
      </button>
      {open && (
        <div className="card absolute right-0 top-11 z-40 w-[min(360px,calc(100vw-2rem))] shadow-lift">
          <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3 dark:border-navy-800">
            <p className="text-sm font-semibold">{staff ? 'Station alerts' : 'Notifications'}</p>
            {!staff && unread > 0 && <button className="text-xs font-medium text-teal-600 hover:underline" onClick={markAll}>Mark all read</button>}
          </div>
          <ul className="max-h-80 divide-y divide-slate-100 overflow-y-auto dark:divide-navy-800">
            {staff
              ? (alerts.data ?? []).map((a) => (
                  <li key={a.sequence} className="px-4 py-3 text-sm">
                    <p>{a.message}</p>
                    <p className="mt-0.5 text-xs muted">{hm(a.time)} · {a.category}</p>
                  </li>
                ))
              : (passenger.data ?? []).slice(0, 6).map((n) => (
                  <li key={n.id} className={`px-4 py-3 text-sm ${n.read ? '' : 'bg-teal-50/60 dark:bg-teal-500/5'}`}>
                    <p className="font-medium">{n.title}</p>
                    <p className="mt-0.5 text-xs muted">{n.message}</p>
                    <p className="mt-0.5 text-[11px] muted">{dateTime(n.createdAt)}</p>
                  </li>
                ))}
            {(staff ? alerts.data : passenger.data)?.length === 0 && <li className="px-4 py-6 text-center text-sm muted">You're all caught up.</li>}
          </ul>
          <Link to={staff ? '/station' : '/passenger/notifications'} onClick={() => setOpen(false)}
            className="block border-t border-slate-200 px-4 py-2.5 text-center text-sm font-medium text-teal-600 hover:bg-slate-50 dark:border-navy-800 dark:hover:bg-navy-800">
            {staff ? 'Open Control Centre' : 'View all notifications'}
          </Link>
        </div>
      )}
    </div>
  )
}

function ProfileMenu() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const box = useDismiss(open, () => setOpen(false))
  if (!user) return null
  const staff = user.role === 'STATION_STAFF'
  const initials = user.fullName.split(' ').map((p) => p[0]).slice(0, 2).join('').toUpperCase()

  const signOut = async () => {
    await logout()
    navigate('/signin', { replace: true })
  }

  return (
    <div ref={box} className="relative">
      <button onClick={() => setOpen((o) => !o)} aria-expanded={open} aria-label="Account menu"
        className="flex items-center gap-2.5 rounded-lg p-1 pr-2 hover:bg-slate-100 dark:hover:bg-navy-800">
        <span className="flex h-8 w-8 items-center justify-center rounded-full bg-navy-800 text-xs font-bold text-white dark:bg-teal-600">
          {initials}
        </span>
        <span className="hidden text-left leading-tight lg:block">
          <span className="block text-sm font-medium text-navy-900 dark:text-white">{user.fullName}</span>
          <span className="block text-[11px] muted">{user.displayTitle}</span>
        </span>
      </button>
      {open && (
        <div className="card absolute right-0 top-12 z-40 w-60 p-1.5 shadow-lift" role="menu">
          <div className="px-3 py-2">
            <p className="truncate text-sm font-semibold">{user.fullName}</p>
            <p className="truncate text-xs muted">{user.email}</p>
          </div>
          <div className="my-1 border-t border-slate-200 dark:border-navy-800" />
          <Link role="menuitem" to={staff ? '/station/settings' : '/passenger/profile'} onClick={() => setOpen(false)}
            className="flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm hover:bg-slate-100 dark:hover:bg-navy-800">
            {staff ? <Settings className="h-4 w-4" /> : <User className="h-4 w-4" />} {staff ? 'Settings' : 'My profile'}
          </Link>
          <button role="menuitem" onClick={signOut}
            className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-left text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-500/10">
            <LogOut className="h-4 w-4" /> Sign out
          </button>
        </div>
      )}
    </div>
  )
}

/** The bar across the top of every portal page. */
export function Topbar({ onMenu }: { onMenu: () => void }) {
  const { user } = useAuth()
  const { theme, toggle } = useTheme()
  const staff = user?.role === 'STATION_STAFF'
  return (
    <header className="no-print sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-slate-200 bg-white/90 px-4 backdrop-blur sm:px-6 dark:border-navy-800 dark:bg-navy-900/90">
      <button className="btn-ghost h-9 w-9 !p-0 lg:hidden" onClick={onMenu} aria-label="Open menu">
        <Menu className="h-5 w-5" />
      </button>
      <GlobalSearch staff={staff} />
      <div className="ml-auto flex items-center gap-1.5 sm:gap-2">
        <ClockChip />
        <button className="btn-ghost h-9 w-9 !p-0" onClick={toggle} aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}>
          {theme === 'dark' ? <Sun className="h-[18px] w-[18px]" /> : <Moon className="h-[18px] w-[18px]" />}
        </button>
        <NotificationBell staff={staff} />
        <ProfileMenu />
      </div>
    </header>
  )
}
