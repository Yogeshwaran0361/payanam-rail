import { Link } from 'react-router-dom'
import {
  Area, AreaChart, Bar, BarChart, CartesianGrid, Cell, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import {
  AlertTriangle, Ban, CalendarClock, Clock, Gauge, Percent, Ticket, TrainFront, Warehouse, Hourglass,
} from 'lucide-react'
import { stationApi } from '../../api/endpoints'
import { Card, CardHeader } from '../../components/ui/Card'
import { ErrorState } from '../../components/ui/EmptyState'
import { KpiCard } from '../../components/ui/KpiCard'
import { PageHeader } from '../../components/ui/PageHeader'
import { PageSkeleton } from '../../components/ui/Skeleton'
import { useAuth } from '../../context/AuthContext'
import { useTheme } from '../../context/ThemeContext'
import { useApi } from '../../hooks/useApi'
import { hm } from '../../lib/format'

/** Chart colours that read well in both themes. */
function useChartColors() {
  const { theme } = useTheme()
  const dark = theme === 'dark'
  return {
    grid: dark ? '#1f376b' : '#e2e8f0',
    text: dark ? '#94a3b8' : '#64748b',
    tooltip: { background: dark ? '#0f1d3a' : '#ffffff', border: `1px solid ${dark ? '#274585' : '#e2e8f0'}`, borderRadius: 8, fontSize: 12 },
  }
}

/** Station Control Centre: KPIs, three charts and the alerts of the day. Refreshes every 5 seconds. */
export function ControlCentre() {
  const { user, can } = useAuth()
  const c = useChartColors()
  const { data, error, loading, reload } = useApi(() => stationApi.dashboard(), [], 5000)
  if (loading && !data) return <PageSkeleton />
  if (error || !data) return <ErrorState message={error?.message ?? 'Unavailable'} onRetry={() => reload()} />
  const k = data.kpis
  const trainOps = can('MANAGE_TRAINS') || can('MANAGE_PLATFORMS')

  return (
    <>
      <PageHeader title="Control Centre" description={`Good to see you, ${user?.fullName.split(' ')[0]}. Station time ${hm(data.now)}; next platform release ${data.nextRelease}.`}
        actions={trainOps && <Link to="/station/board" className="btn-primary"><Warehouse className="h-4 w-4" /> Platform board</Link>} />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <KpiCard label="Trains today" value={k.trainsToday} icon={TrainFront} accent="navy" hint={`${k.departed} departed`} />
        <KpiCard label="On platform" value={k.onPlatform} icon={Gauge} accent="teal" />
        <KpiCard label="Waiting for platform" value={k.waitingForPlatform} icon={Hourglass} accent="amber" />
        <KpiCard label="Rejected" value={k.rejected} icon={Ban} accent="red" hint="Overlong for every platform" />
        <KpiCard label="Delayed" value={k.delayed} icon={Clock} accent="violet" />
        <KpiCard label="Total bookings" value={k.totalBookings} icon={Ticket} accent="navy" hint={`${k.activeTickets} active tickets`} />
        <KpiCard label="Seat occupancy" value={`${k.occupancyPercent}%`} icon={Percent} accent="green" />
        <KpiCard label="Waitlisted passengers" value={k.waitlisted} icon={CalendarClock} accent="amber" />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-3">
        <Card className="xl:col-span-2">
          <CardHeader title="Seat occupancy per train" subtitle="Confirmed seats (of 10) and waiting-list length" />
          <div className="h-72 p-4" role="img" aria-label="Bar chart of confirmed seats and waiting list per train">
            <ResponsiveContainer>
              <BarChart data={data.seatOccupancy} margin={{ left: -16 }}>
                <CartesianGrid strokeDasharray="3 3" stroke={c.grid} vertical={false} />
                <XAxis dataKey="trainNo" tick={{ fill: c.text, fontSize: 12 }} />
                <YAxis allowDecimals={false} tick={{ fill: c.text, fontSize: 12 }} />
                <Tooltip contentStyle={c.tooltip} labelFormatter={(no) => data.seatOccupancy.find((s) => s.trainNo === no)?.trainName ?? no} />
                <Legend wrapperStyle={{ fontSize: 12 }} />
                <Bar dataKey="confirmed" name="Confirmed" fill="#0d8378" radius={[4, 4, 0, 0]} />
                <Bar dataKey="waiting" name="Waiting list" fill="#f59e0b" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>
        <Card>
          <CardHeader title="Platform utilisation" subtitle="Share of the service window that is planned or occupied" />
          <div className="h-72 p-4" role="img" aria-label="Platform utilisation percentages">
            <ResponsiveContainer>
              <BarChart data={data.platformUtilisation} layout="vertical" margin={{ left: -10 }}>
                <CartesianGrid strokeDasharray="3 3" stroke={c.grid} horizontal={false} />
                <XAxis type="number" domain={[0, 100]} unit="%" tick={{ fill: c.text, fontSize: 12 }} />
                <YAxis type="category" dataKey="platformId" tick={{ fill: c.text, fontSize: 12 }} />
                <Tooltip contentStyle={c.tooltip} formatter={(v: number, _n, p) => [`${v}% (${p.payload.plannedMinutes} min)`, 'Utilisation']} />
                <Bar dataKey="percent" radius={[0, 4, 4, 0]}>
                  {data.platformUtilisation.map((p, i) => <Cell key={p.platformId} fill={['#172a52', '#365aa6', '#13a392'][i % 3]} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>
        <Card className="xl:col-span-2">
          <CardHeader title="Bookings over time" subtitle="PNRs and tickets created per day" />
          <div className="h-64 p-4" role="img" aria-label="Bookings per day">
            <ResponsiveContainer>
              <AreaChart data={data.bookingsOverTime} margin={{ left: -16 }}>
                <defs>
                  <linearGradient id="bk" x1="0" x2="0" y1="0" y2="1"><stop offset="0" stopColor="#13a392" stopOpacity={0.4} /><stop offset="1" stopColor="#13a392" stopOpacity={0} /></linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke={c.grid} vertical={false} />
                <XAxis dataKey="label" tick={{ fill: c.text, fontSize: 12 }} />
                <YAxis allowDecimals={false} tick={{ fill: c.text, fontSize: 12 }} />
                <Tooltip contentStyle={c.tooltip} />
                <Legend wrapperStyle={{ fontSize: 12 }} />
                <Area type="monotone" dataKey="tickets" name="Tickets" stroke="#365aa6" fill="none" strokeWidth={2} />
                <Area type="monotone" dataKey="bookings" name="PNRs" stroke="#0d8378" fill="url(#bk)" strokeWidth={2} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </Card>
        <Card>
          <CardHeader title="Active alerts" icon={<AlertTriangle className="h-5 w-5 text-amber-500" />} subtitle={`${data.alerts.length} today`} />
          <ul className="max-h-64 divide-y divide-slate-100 overflow-y-auto dark:divide-navy-800">
            {data.alerts.map((a) => (
              <li key={a.sequence} className="px-5 py-3 text-sm">
                <p>{a.message}</p>
                <p className="mt-0.5 text-xs muted">{hm(a.time)} · {a.category.toLowerCase()}{a.trainNo && ` · ${a.trainNo}`}</p>
              </li>
            ))}
            {data.alerts.length === 0 && <li className="px-5 py-8 text-center text-sm muted">No alerts. Everything is running to plan.</li>}
          </ul>
        </Card>
      </div>
    </>
  )
}
