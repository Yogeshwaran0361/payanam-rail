import { api } from './client'
import type {
  Allocation, ArrivalType, AuditEntry, AuthResponse, Booking, Cancellation, ClockState, Dashboard, DelayImpact,
  ForgotPasswordResponse, AppNotification, OperationResult, Page, PassengerDashboard, PlatformBoard, PlatformInfo,
  QueueEntry, ScenarioInfo, ScenarioResult, SeatMap, Settings, Snapshot, StaffReservations, StationEvent,
  StationStats, Train, UserProfile, Gender, Priority,
} from './types'

const data = <T,>(p: Promise<{ data: T }>) => p.then((r) => r.data)

export interface PassengerSignUp {
  fullName: string; email: string; mobile: string; dateOfBirth: string; gender: Gender | ''; city: string
  password: string; confirmPassword: string; acceptTerms: boolean
}
export interface StaffSignUp {
  fullName: string; employeeId: string; designation: string; email: string; mobile: string
  password: string; confirmPassword: string; accessCode: string
}
export interface PassengerInput { name: string; age: number | ''; gender: Gender | '' }

export const authApi = {
  signIn: (identifier: string, password: string, rememberMe: boolean) =>
    data<AuthResponse>(api.post('/auth/signin', { identifier, password, rememberMe })),
  signUpPassenger: (form: PassengerSignUp) => data<AuthResponse>(api.post('/auth/signup/passenger', form)),
  signUpStaff: (form: StaffSignUp) => data<AuthResponse>(api.post('/auth/signup/staff', form)),
  logout: () => data(api.post('/auth/logout')),
  me: () => data<UserProfile>(api.get('/auth/me')),
  forgot: (identifier: string) => data<ForgotPasswordResponse>(api.post('/auth/forgot-password', { identifier })),
  reset: (body: { identifier: string; otp: string; password: string; confirmPassword: string }) =>
    data(api.post('/auth/reset-password', body)),
  updateProfile: (body: { fullName: string; mobile: string; city?: string }) => data<UserProfile>(api.put('/auth/profile', body)),
  changePassword: (body: { currentPassword: string; password: string; confirmPassword: string }) =>
    data(api.put('/auth/password', body)),
}

export const trainApi = {
  timetable: () => data<Train[]>(api.get('/trains')),
  stats: () => data<StationStats>(api.get('/trains/stats')),
  train: (no: string) => data<Train>(api.get(`/trains/${no}`)),
  live: (no: string) => data<Train>(api.get(`/trains/${no}/live`)),
  seatMap: (no: string) => data<SeatMap>(api.get(`/trains/${no}/seat-map`)),
  edit: (no: string, body: { arrival: string; departure: string; priority: Priority }) => data<Train>(api.put(`/trains/${no}`, body)),
  add: (body: Record<string, unknown>) => data<Train>(api.post('/trains', body)),
}

export const bookingApi = {
  book: (trainNo: string, passengers: PassengerInput[]) => data<Booking>(api.post('/bookings', { trainNo, passengers })),
  mine: () => data<Booking[]>(api.get('/bookings')),
  one: (pnr: string) => data<Booking>(api.get(`/bookings/${pnr}`)),
  dashboard: () => data<PassengerDashboard>(api.get('/bookings/dashboard')),
  cancel: (pnr: string) => data<Cancellation>(api.post(`/bookings/${pnr}/cancel`)),
  cancelPassenger: (pnr: string, n: number) => data<Cancellation>(api.post(`/bookings/${pnr}/passengers/${n}/cancel`)),
  publicPnr: (pnr: string) => data<Booking>(api.get(`/pnr/${pnr}`)),
  notifications: () => data<AppNotification[]>(api.get('/notifications')),
  markRead: (id: number) => data(api.post(`/notifications/${id}/read`)),
  markAllRead: () => data(api.post('/notifications/read-all')),
}

export const stationApi = {
  dashboard: () => data<Dashboard>(api.get('/operations/dashboard')),
  events: (limit = 100) => data<StationEvent[]>(api.get('/operations/events', { params: { limit } })),
  trains: () => data<Train[]>(api.get('/operations/trains')),
  board: () => data<PlatformBoard>(api.get('/platforms/board')),
  platforms: () => data<PlatformInfo[]>(api.get('/platforms')),
  queue: () => data<QueueEntry[]>(api.get('/platforms/queue')),
  arrivalRequest: (trainNo: string, type?: ArrivalType) => data<Allocation>(api.post('/operations/arrival-requests', { trainNo, type })),
  arrival: (no: string, preview = false) => data<OperationResult>(api.post(`/operations/trains/${no}/arrival${preview ? '/preview' : ''}`)),
  departure: (no: string, preview = false) => data<OperationResult>(api.post(`/operations/trains/${no}/departure${preview ? '/preview' : ''}`)),
  delay: (no: string, minutes: number, reason: string, preview = false) =>
    data<DelayImpact>(api.post(`/operations/trains/${no}/delay${preview ? '/preview' : ''}`, { minutes, reason })),
  editPlatform: (id: string, lengthMeters: number) => data<OperationResult>(api.put(`/platforms/${id}`, { lengthMeters })),
  maintenance: (id: string, maintenance: boolean) => data<OperationResult>(api.post(`/platforms/${id}/maintenance`, { maintenance })),
  reservations: (trainNo: string) => data<StaffReservations>(api.get(`/operations/reservations/trains/${trainNo}`)),
  staffPnr: (pnr: string) => data<Booking>(api.get(`/operations/reservations/pnr/${pnr}`)),
  staffCancel: (pnr: string) => data<Cancellation>(api.post(`/operations/reservations/pnr/${pnr}/cancel`)),
  staffCancelPassenger: (pnr: string, n: number) => data<Cancellation>(api.post(`/operations/reservations/pnr/${pnr}/passengers/${n}/cancel`)),
  clock: () => data<ClockState>(api.get('/simulation/clock')),
  advance: (minutes: number) => data<ClockState>(api.post('/simulation/advance', { minutes })),
  play: () => data<ClockState>(api.post('/simulation/play')),
  pause: () => data<ClockState>(api.post('/simulation/pause')),
  resetClock: () => data<ClockState>(api.post('/simulation/reset-clock')),
  loadSeed: () => data<ClockState>(api.post('/simulation/load-seed')),
  resetSystem: () => data<ClockState>(api.post('/simulation/reset-system')),
  snapshots: () => data<Snapshot[]>(api.get('/simulation/snapshots')),
  saveSnapshot: (label: string) => data<Snapshot>(api.post('/simulation/snapshots', { label })),
  audit: (params: { action?: string; role?: string; q?: string; page: number; size: number }) =>
    data<Page<AuditEntry>>(api.get('/audit', { params })),
  auditActions: () => data<string[]>(api.get('/audit/actions')),
  auditCsv: (params: { action?: string; role?: string; q?: string }) =>
    api.get('/audit/export', { params, responseType: 'blob' }).then((r) => r.data as Blob),
  settings: () => data<Settings>(api.get('/admin/settings')),
  saveSettings: (allocationPolicy: string, clearanceBufferMinutes: number) =>
    data<Settings>(api.put('/admin/settings', { allocationPolicy, clearanceBufferMinutes })),
  scenarios: () => data<ScenarioInfo[]>(api.get('/tests')),
  runScenario: (id: string) => data<ScenarioResult>(api.post(`/tests/${id}/run`)),
  runAll: () => data<ScenarioResult[]>(api.post('/tests/run-all')),
}
