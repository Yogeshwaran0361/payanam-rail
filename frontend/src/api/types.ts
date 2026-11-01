// TypeScript mirrors of the backend DTOs (com.payanam.rail.dto.*).

export type Role = 'PASSENGER' | 'STATION_STAFF'
export type Permission =
  | 'VIEW_DASHBOARD' | 'MANAGE_PLATFORMS' | 'MANAGE_TRAINS' | 'MANAGE_RESERVATIONS'
  | 'CONTROL_SIMULATION' | 'VIEW_AUDIT' | 'RUN_TESTS' | 'MANAGE_SETTINGS'
export type Gender = 'MALE' | 'FEMALE' | 'OTHER'
export type Priority = 'EMERGENCY' | 'CONNECTION' | 'NORMAL'
export type ArrivalType = 'EMERGENCY' | 'CONNECTION' | 'SCHEDULED'

export interface UserProfile {
  id: number; fullName: string; email: string; mobile: string; role: Role; displayTitle: string
  permissions: Permission[]; dateOfBirth?: string; gender?: Gender; city?: string
  employeeId?: string; designation?: string; createdAt: string; lastLoginAt?: string
}
export interface AuthResponse { token: string; expiresAt: string; user: UserProfile }
export interface FieldError { field: string; message: string }
export interface ApiError { status: number; code: string; message: string; path: string; fieldErrors: FieldError[] }
export interface ForgotPasswordResponse { message: string; simulatedOtp: string | null; expiresInMinutes: number }

export interface Train {
  trainNo: string; name: string; origin: string; destination: string
  scheduledArrival: string; scheduledDeparture: string; expectedArrival: string; expectedDeparture: string
  lengthMeters: number; priority: Priority; status: string; liveStatus: string; delayMinutes: number
  delayReason?: string; platformId?: string; requestType?: ArrivalType; fare: number; capacity: number
  confirmed: number; seatsLeft: number; waiting: number; bookingOpen: boolean
  actualArrival?: string; actualDeparture?: string
}
export interface StationStats { stationName: string; platforms: number; trains: number; bookings: number; occupancyPercent: number; seatsLeft: number; now: string }
export interface SeatMap {
  trainNo: string; trainName: string; capacity: number; seats: { seatNo: number; occupied: boolean; mine: boolean }[]
  waitingCount: number; myWaitingPositions: number[]; bookingOpen: boolean
}

export interface TimelineStep { at: string; label: string; note: string }
export interface Ticket {
  passengerNo: number; name: string | null; age: number; gender: Gender; status: 'CNF' | 'WL' | 'CAN'; label: string
  seatNo?: number; waitingPosition: number; promoted: boolean; lastSeatNo?: number; timeline: TimelineStep[]
}
export interface Booking {
  pnr: string; trainNo: string; trainName: string; origin: string; destination: string; arrival: string
  departure: string; platformId?: string; trainStatus: string; delayMinutes: number; bookedAt: string
  bookedBy?: string; farePerPassenger: number; totalFare: number
  journeyState: 'UPCOMING' | 'COMPLETED' | 'CANCELLED'; summary: string; cancellable: boolean; passengers: Ticket[]
}
export interface Cancellation {
  pnr: string; cancelledTickets: string[]; promotions: { ticketId: string; pnr: string; from: string; to: string }[]
  message: string; booking: Booking
}
export interface AppNotification { id: number; type: string; title: string; message: string; createdAt: string; read: boolean; pnr?: string }
export interface PassengerDashboard {
  fullName: string; upcomingJourneys: number; confirmedTickets: number; waitlistedTickets: number
  cancelledTickets: number; nextJourney?: Booking; recentActivity: { at: string; pnr: string; trainNo: string; text: string }[]
  notifications: AppNotification[]; unreadNotifications: number
}

export interface Allocation { trainNo: string; outcome: 'ASSIGNED' | 'QUEUED' | 'REJECTED'; platformId?: string; start?: string; end?: string; queuePosition: number; message: string }
export interface StationEvent { sequence: number; time: string; category: string; alert: boolean; trainNo?: string; message: string }
export interface OperationResult { preview: boolean; message: string; allocations: Allocation[]; alerts: string[]; events: StationEvent[] }
export interface DelayImpact {
  preview: boolean; trainNo: string; minutes: number; reason: string; platformId?: string
  oldArrival: string; oldDeparture: string; newArrival: string; newDeparture: string
  oldOccupationEnd?: string; newOccupationEnd?: string; occupationExtended: boolean; requeued: boolean
  lostPlatform: boolean; bumped: Allocation[]; reallocated: Allocation[]; alerts: string[]; message: string
}
export interface Block { trainNo: string; trainName: string; start: string; end: string; trainStatus: string; priority: Priority; lengthMeters: number }
export interface PlatformInfo {
  id: string; name: string; lengthMeters: number; status: 'FREE' | 'OCCUPIED' | 'MAINTENANCE'; maintenance: boolean
  currentTrainNo?: string; currentTrainName?: string; currentStart?: string; currentEnd?: string; nextFreeTime: string
  nextTrainNo?: string; nextTrainArrival?: string; blocks: Block[]
}
export interface QueueEntry {
  rank: number; trainNo: string; trainName: string; type: ArrivalType; priorityRank: number; arrival: string
  desiredEnd: string; lengthMeters: number; requestedAt: string; delayMinutes: number; waitReason: string
  nextReleasePlatform?: string; nextReleaseTime?: string
}
export interface PlatformBoard {
  now: string; windowStart: string; windowEnd: string; clearanceBufferMinutes: number; policy: string
  platforms: PlatformInfo[]; waiting: QueueEntry[]; nextRelease: string
}
export interface Dashboard {
  now: string
  kpis: { trainsToday: number; onPlatform: number; waitingForPlatform: number; rejected: number; delayed: number
    totalBookings: number; activeTickets: number; occupancyPercent: number; departed: number; waitlisted: number }
  seatOccupancy: { trainNo: string; trainName: string; confirmed: number; waiting: number; capacity: number }[]
  platformUtilisation: { platformId: string; percent: number; plannedMinutes: number }[]
  bookingsOverTime: { label: string; bookings: number; tickets: number }[]
  alerts: StationEvent[]; nextRelease: string
}
export interface ClockState { now: string; start: string; elapsedMinutes: number; running: boolean; tickSeconds: number; playStepMinutes: number; events: string[] }
export interface StaffReservations {
  trainNo: string; trainName: string; trainStatus: string; capacity: number
  confirmed: { seatNo: number; ticketId: string; pnr: string; passengerName: string; age: number; gender: Gender; promoted: boolean; bookedBy: string }[]
  waiting: { position: number; ticketId: string; pnr: string; passengerName: string; age: number; gender: Gender; bookedAt: string; bookedBy: string }[]
  promotions: StationEvent[]
  pnrs: { pnr: string; bookedBy: string; bookedAt: string; summary: string; cancellable: boolean }[]
}
export interface Settings { allocationPolicy: string; availablePolicies: string[]; clearanceBufferMinutes: number; stationName: string }
export interface AuditEntry { id: number; timestamp: string; simulationTime?: string; actor: string; role: string; action: string; details: string; success: boolean }
export interface Page<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number }
export interface Snapshot { id: number; createdAt: string; simulationTime: string; label: string; createdBy: string; summaryJson: string }
export interface ScenarioStep { description: string; ok: boolean }
export interface ScenarioResult {
  id: string; title: string; rule: string; kind: 'POSITIVE' | 'FAILURE'; passed: boolean
  expected: string; actual: string; steps: ScenarioStep[]; durationMs: number
}
export interface ScenarioInfo { id: string; title: string; rule: string; kind: 'POSITIVE' | 'FAILURE'; description: string }
