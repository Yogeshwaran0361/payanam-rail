import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { PageSkeleton } from './components/ui/Skeleton'
import { PassengerLayout } from './layouts/PassengerLayout'
import { PublicLayout } from './layouts/PublicLayout'
import { StationLayout } from './layouts/StationLayout'
import { AuthPage } from './pages/auth/AuthPage'
import { ForgotPassword } from './pages/auth/ForgotPassword'
import { Landing } from './pages/Landing'
import { NotFound } from './pages/NotFound'
import { PnrStatus } from './pages/PnrStatus'
import { BookingDetail } from './pages/passenger/BookingDetail'
import { BookTicket } from './pages/passenger/BookTicket'
import { PassengerDashboard } from './pages/passenger/Dashboard'
import { ETicket } from './pages/passenger/ETicket'
import { LiveStatus } from './pages/passenger/LiveStatus'
import { MyBookings } from './pages/passenger/MyBookings'
import { Notifications } from './pages/passenger/Notifications'
import { Profile } from './pages/passenger/Profile'
import { SearchTrains } from './pages/passenger/SearchTrains'
import { SeatMap } from './pages/passenger/SeatMap'
import { ArrivalRequests } from './pages/station/ArrivalRequests'
import { AuditLog } from './pages/station/AuditLog'
import { PlatformBoard } from './pages/station/PlatformBoard'
import { PlatformQueue } from './pages/station/PlatformQueue'
import { Platforms } from './pages/station/Platforms'
import { Reservations } from './pages/station/Reservations'
import { Settings } from './pages/station/Settings'
import { Simulation } from './pages/station/Simulation'
import { TestLab } from './pages/station/TestLab'
import { Timetable } from './pages/station/Timetable'
import { TrainOperations } from './pages/station/TrainOperations'
import { GuestOnly, RequirePermission, RequireRole } from './routes/guards'
import type { Permission } from './api/types'

/** The Control Centre carries the charting library, so it loads as a separate chunk on first visit. */
const ControlCentre = lazy(() => import('./pages/station/ControlCentre').then((m) => ({ default: m.ControlCentre })))

const TRAIN_OPS: Permission[] = ['MANAGE_TRAINS', 'MANAGE_PLATFORMS']

/** All routes. Portal routes sit behind role guards; station pages also check the designation's permission. */
export default function App() {
  const staff = (anyOf: Permission[], page: JSX.Element) => <RequirePermission anyOf={anyOf}>{page}</RequirePermission>
  return (
    <Routes>
      <Route element={<PublicLayout />}>
        <Route path="/" element={<Landing />} />
        <Route path="/pnr" element={<PnrStatus publicPage />} />
      </Route>
      <Route path="/signin" element={<GuestOnly><AuthPage mode="signin" /></GuestOnly>} />
      <Route path="/signup" element={<GuestOnly><AuthPage mode="signup" /></GuestOnly>} />
      <Route path="/forgot-password" element={<GuestOnly><ForgotPassword /></GuestOnly>} />

      <Route path="/passenger" element={<RequireRole role="PASSENGER" />}>
        <Route element={<PassengerLayout />}>
          <Route index element={<PassengerDashboard />} />
          <Route path="trains" element={<SearchTrains />} />
          <Route path="trains/:trainNo/seats" element={<SeatMap />} />
          <Route path="book" element={<BookTicket />} />
          <Route path="bookings" element={<MyBookings />} />
          <Route path="bookings/:pnr" element={<BookingDetail />} />
          <Route path="bookings/:pnr/ticket" element={<ETicket />} />
          <Route path="pnr" element={<PnrStatus />} />
          <Route path="live" element={<LiveStatus />} />
          <Route path="notifications" element={<Notifications />} />
          <Route path="profile" element={<Profile />} />
          <Route path="*" element={<NotFound />} />
        </Route>
      </Route>

      <Route path="/station" element={<RequireRole role="STATION_STAFF" />}>
        <Route element={<StationLayout />}>
          <Route index element={staff(['VIEW_DASHBOARD'], <Suspense fallback={<PageSkeleton />}><ControlCentre /></Suspense>)} />
          <Route path="board" element={staff(TRAIN_OPS, <PlatformBoard />)} />
          <Route path="queue" element={staff(TRAIN_OPS, <PlatformQueue />)} />
          <Route path="arrivals" element={staff(TRAIN_OPS, <ArrivalRequests />)} />
          <Route path="operations" element={staff(TRAIN_OPS, <TrainOperations />)} />
          <Route path="reservations" element={staff(['MANAGE_RESERVATIONS'], <Reservations />)} />
          <Route path="timetable" element={staff(['MANAGE_TRAINS'], <Timetable />)} />
          <Route path="platforms" element={staff(['MANAGE_PLATFORMS'], <Platforms />)} />
          <Route path="simulation" element={staff(['CONTROL_SIMULATION'], <Simulation />)} />
          <Route path="audit" element={staff(['VIEW_AUDIT'], <AuditLog />)} />
          <Route path="tests" element={staff(['RUN_TESTS'], <TestLab />)} />
          <Route path="settings" element={staff(['VIEW_DASHBOARD'], <Settings />)} />
          <Route path="*" element={<NotFound />} />
        </Route>
      </Route>

      <Route path="/login" element={<Navigate to="/signin" replace />} />
      <Route path="*" element={<NotFound />} />
    </Routes>
  )
}
