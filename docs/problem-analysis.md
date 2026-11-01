# Problem analysis

**Mini Project 16: Railway Reservation and Platform Allocation System**
Product: *Payanam Rail*, for the fictional single station *Payanam Junction*.

## 1. The problem

Payanam Junction runs its reservations and its platform plan by hand. The case study names four problems:

| # | Problem today | Consequence | How Payanam Rail solves it |
|---|---|---|---|
| 1 | Cancelled seats are not promptly offered to waiting passengers | Seats travel empty while people stay waitlisted | `TrainService.cancelBooking()` frees the seat and, in the same call, gives that exact seat to the head of the train's FIFO waiting list (rule R3). The promoted passenger gets an in-app notification. |
| 2 | A platform may be assigned to overlapping train intervals | Two trains planned on one platform at the same time | `Platform.occupyPlatform()` refuses any interval that overlaps an existing one (binary search on a sorted list). The check is inside the class, so no caller can skip it (rule P1). |
| 3 | Delays can invalidate a prepared platform plan | The plan silently becomes wrong | `PlatformScheduler.reportDelay()` extends the delayed train's interval, un-assigns every later train that now overlaps, re-queues it, re-allocates it and raises an alert for each (rule P7). Staff see an exact preview before confirming. |
| 4 | Connecting or emergency trains may need priority when several trains wait | The most urgent train can be left waiting | Waiting trains sit in a `PriorityQueue` ordered by priority rank, then arrival time, then train number (rule P4). An emergency train is always served first, but never by moving a train already standing at a platform (P5). |

**Goal:** manage fixed-capacity reservations and cancellations, promote waiting passengers fairly, and allocate
compatible platforms with no time overlap.

## 2. Actors

| Actor | What they do | Portal |
|---|---|---|
| Passenger | Signs up, searches trains, books 1–4 passengers, cancels (whole PNR or one passenger), checks PNR status, prints the e-ticket, follows live status, reads notifications | `/passenger/*` |
| Station Master | Everything below, plus the audit log, Test Lab and settings | `/station/*` |
| Platform Controller | Platforms, trains, arrival requests, delays, arrivals and departures, simulation clock | `/station/*` (limited menu) |
| Booking Clerk | Reservations management: per-train seats and waiting list, counter cancellation | `/station/*` (limited menu) |
| Visitor (not signed in) | Landing page, public timetable numbers, public PNR lookup (names hidden) | `/`, `/pnr` |

## 3. Scope and fixed assumptions

These are taken from the case study and are enforced in code (constants or validation), not only described.

| Assumption | Where it is enforced |
|---|---|
| 1 station, 3 platforms (P1 600 m, P2 450 m, P3 300 m) | `seed/platforms.json`, `StationFactory` |
| At most 6 train services | `TrainRegistry.MAX_TRAINS = 6`; adding a 7th train returns 409 |
| One seat class with exactly 10 seats per train | `TrainService.CAPACITY = 10`; `incrementConfirmed()` / `decrementConfirmed()` guard 0–10 |
| One end-to-end booking segment per train | A booking has a train, not a from/to pair |
| One PNR holds 1–4 passengers | Bean Validation on `BookingRequest` and a check in `ReservationDesk.book()` |
| Platform occupation is the half-open interval `[arrival, departure + clearanceBuffer)` | `PlatformScheduler.occupationStart/End`, `OccupationInterval.overlaps()` |
| Clearance buffer = 10 minutes, configurable | `payanam.station.clearance-buffer-minutes` in `application.yml`; also changeable in Settings |
| A simulation clock is "now" | `SimulationClock`; staff can play, pause, advance (+5/+15/+30) and reset it |
| Cancellations, delays, arrival and departure confirmation are manual | Staff actions in Train Operations and Reservations |
| At least 30 booking requests in the seed | `seed/bookings.json`: 32 PNRs across 15 passenger accounts; Vaigai and Pearl City are full with waiting lists |
| No external APIs, no payment gateway | The fare is shown as information only; OTP is shown on screen and in the log |

**Why a database at all?** The case study does not need a database: all reservation and platform logic runs on
in-memory Java collections, written by hand (see [data-structures.md](data-structures.md)). But a real sign-in system
needs persistent accounts, so an **H2 file database** stores only:

* user accounts (BCrypt password hashes, lockout counters),
* sign-in sessions (the JWT id, so logout can revoke a token) and password-reset OTPs,
* the audit log,
* saved station snapshots.

The database never replaces the in-memory data structures. Restarting the server reloads the seed station; accounts
and the audit log survive the restart.

## 4. Reservation rules

| Rule | Statement | Implementation |
|---|---|---|
| R1 | `bookSeat()`: if confirmed < 10, the ticket is **CNF** with the lowest free seat 1–10; otherwise it joins the back of the train's FIFO queue as **WL/n** | `TrainService.bookSeat()` (`seats[]` array + `ArrayDeque.addLast`) |
| R2 | One PNR holds 1–4 passengers; each passenger has their own seat or WL position, shown per passenger (CNF/S4, WL/2, CAN) | `ReservationDesk.book()` creates one `Reservation` per passenger |
| R3 | `cancelBooking()` frees the seat and **immediately** promotes the **oldest** waiting passenger into **that exact seat**; the promoted passenger's owner is notified ("WL/1 → CNF/S7") | `TrainService.cancelBooking()` (`ArrayDeque.pollFirst`), `StationListener.onPromotion` → `NotificationService` |
| R4 | Partial cancellation: cancel one passenger of a PNR | `ReservationDesk.cancelPassenger()` |
| R5 | Seat counts never exceed 10 or drop below 0. Double cancellation, unknown PNR, booking a departed train and touching someone else's PNR are rejected with clear errors | Guards in `TrainService` and `ReservationDesk`; custom exceptions mapped to JSON errors (409, 404, 403) |
| R6 | Booking closes when the train is ARRIVED or DEPARTED | `TrainStatus.bookingOpen()` checked by `bookSeat()` |

Everyone behind a promoted or cancelled waiting passenger moves up one place (WL/3 → WL/2), and each move is added to
that passenger's timeline, which is what the PNR page shows (Booked → WL/2 → WL/1 → CNF/S7).

## 5. Platform allocation rules

| Rule | Statement | Implementation |
|---|---|---|
| P1 | Assign only a platform that is not in maintenance, is at least as long as the train, and has **no overlap** with its existing occupations | `PlatformAllocationPolicy.isValid()` and, as the last line of defence, `Platform.occupyPlatform()` |
| P2 | No platform is long enough → **REJECTED** "Overlong train – exceeds all platform lengths" | `PlatformScheduler.requestPlatform()` compares with the longest platform |
| P3 | Suitable platforms are busy → the train joins the waiting `PriorityQueue` | `PlatformScheduler.waitingQueue` |
| P4 | Queue order: `priorityRank()` (EMERGENCY 0 < CONNECTION 1 < NORMAL 2) → arrival time → trainNo | `PlatformRequest.QUEUE_ORDER`; each subclass overrides `priorityRank()` |
| P5 | Never displace a train that is on a platform | Only `PLATFORM_ASSIGNED` trains that start later can be bumped (`isBumpable`); an ARRIVED train is never touched |
| P6 | Default policy BestFit (shortest platform that fits, tie → lowest id); FirstFit also implemented and switchable | `BestFitAllocationPolicy`, `FirstFitAllocationPolicy`; Settings page / `PUT /api/v1/admin/settings` |
| P7 | Delay: update the times. Queued → remove and reinsert. Assigned or on platform → extend the occupation; every later, not-yet-arrived train that now overlaps is un-assigned, re-queued and re-allocated, with an alert for each | `PlatformScheduler.applyDelay()` |
| P8 | Departure → the platform is released at departure + buffer, the release min-heap is updated, waiting trains are allocated automatically | `PlatformScheduler.confirmDeparture()`, `advanceClock()` pops due releases |
| P9 | A platform in maintenance gets no new allocations; a train already on it stays | `Platform.isAvailableForAllocation()`; `setMaintenance()` re-queues only planned trains |

### Processing a platform request

```
requestPlatform(train, type)
 ├─ longest platform < train length ?  → REJECTED (P2)
 ├─ create the request object (EmergencyArrival / ConnectionArrival / ScheduledArrival)
 ├─ policy.selectPlatform(train, [arr, dep+buffer), platforms)   (P1, P6)
 │    ├─ found → platform.occupyPlatform(...)  re-checks P1
 │    │          push (platform, releaseTime) on the release min-heap
 │    │          → ASSIGNED P#
 │    └─ none  → waitingQueue.add(request) → QUEUED at position n, with the reason and
 │                the next compatible release (min-heap walk)   (P3)
```

### What changes the queue

* **Departure (P8):** the platform's release is moved to departure + buffer and pushed on the min-heap; then the queue
  is served: requests are polled in P4 order and each gets one allocation attempt; those that still do not fit go
  back. A train that cannot fit never blocks a lower-ranked train that fits elsewhere.
* **Clock advance:** due releases are popped from the min-heap; a waiting train past its arrival time is "held at the
  signal" (its times move to now, which is a delay, so it is removed and reinserted); an arrived train past its
  departure keeps its platform (overstay, extended in place); then the queue is served.
* **Delay (P7), maintenance (P9), platform length change:** may un-assign planned trains, which go back into the queue,
  and then the queue is served.

### Seed scenario at 06:30

| Train | Length | Priority | Interval (incl. 10-min buffer) | Result at start |
|---|---|---|---|---|
| 12635 Vaigai Express | 420 m | NORMAL | 07:00–07:25 | P2 (best fit: 450 m) |
| 16127 Guruvayur Express | 440 m | CONNECTION | 07:05–07:30 | P1 (P2 busy) |
| 06055 Medical Relief Special | 560 m | EMERGENCY | 07:12–07:35 | Queued: only P1 is long enough and it is busy until 07:30 |
| 22671 Tejas Express | 280 m | NORMAL | 07:30–07:50 | P3 |
| 12693 Pearl City Express | 430 m | NORMAL | 07:20–07:45 | No request yet (demo: request it → queued behind the emergency) |
| 12637 Pandian Express | 650 m | NORMAL | 07:45–08:10 | No request yet (demo: request it → rejected, overlong) |

## 6. Non-functional requirements

| Area | Decision |
|---|---|
| Security | Spring Security, stateless JWT (HS384), BCrypt hashes, role + permission authorities, 5 failed sign-ins lock the account for 15 minutes, logout revokes the session id inside the token |
| Consistency | The live `RailwayStation` is accessed under one lock (`StationService.read/write`); previews run on a deep copy, so "what-if" never changes the live plan |
| Errors | One JSON error format `{timestamp, status, error, code, message, path, fieldErrors}` from a global exception handler |
| Traceability | Every state-changing action is written to the audit log with the user, role and details |
| Usability | Responsive layout, light/dark theme, keyboard focus states, accessible labels, skeleton loaders and empty states |
| Testability | 129 JUnit tests, ≥ 80% JaCoCo line coverage gate on domain and service (actual 95–100%), and an in-app Test Lab |
