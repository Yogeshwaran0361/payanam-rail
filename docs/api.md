# REST API

* Base URL: `http://localhost:8080/api/v1`
* Interactive documentation: **Swagger UI** at <http://localhost:8080/swagger-ui.html> (OpenAPI JSON at `/v3/api-docs`),
  grouped by the tags auth, trains, bookings, pnr, platforms, operations, simulation, audit, tests and admin.
  Press **Authorize** in Swagger UI and paste a token from `POST /auth/signin` to try the protected endpoints.
* Format: JSON in and out; times are ISO-8601 local date-times in simulation time (`2026-10-18T07:00:00`).
* Authentication: `Authorization: Bearer <JWT>`. Tokens last 60 minutes (7 days with *remember me*). Logout revokes
  the token immediately because each token's id (`jti`) is checked against its session row.
* Every sample on this page was captured from the running application with the seed data.

## Access levels

| Level | Meaning |
|---|---|
| Public | No token needed |
| Signed in | Any valid token |
| Passenger | `ROLE_PASSENGER` (staff get 403) |
| Staff | `ROLE_STATION_STAFF` plus the named permission. Station Master has every permission; Platform Controller has VIEW_DASHBOARD, MANAGE_PLATFORMS, MANAGE_TRAINS, CONTROL_SIMULATION; Booking Clerk has VIEW_DASHBOARD, MANAGE_RESERVATIONS |

## Error format

Every error, from validation to a 403, has the same shape:

```json
{
  "timestamp": "2026-10-18T06:03:11.033931550Z",
  "status": 404,
  "error": "Not Found",
  "code": "PNR_NOT_FOUND",
  "message": "PNR 4999999999 was not found",
  "path": "/api/v1/pnr/4999999999",
  "fieldErrors": []
}
```

| HTTP | `code` | When |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Bean Validation failed; `fieldErrors` lists `{field, message}` |
| 400 | `BAD_REQUEST` | Unreadable JSON or a wrong parameter type |
| 401 | `AUTH_FAILED` / `UNAUTHENTICATED` | Wrong credentials / missing, expired or revoked token |
| 403 | `FORBIDDEN` / `UNAUTHORIZED_ACTION` | Role or permission missing / someone else's PNR |
| 404 | `TRAIN_NOT_FOUND`, `PNR_NOT_FOUND`, `NOT_FOUND` | Unknown train, PNR or path |
| 409 | `SEAT_UNAVAILABLE`, `INVALID_CANCELLATION`, `PLATFORM_CONFLICT`, `INVALID_OPERATION` | A business rule refused the action |
| 423 | `ACCOUNT_LOCKED` | 5 wrong passwords; locked for 15 minutes |

## Endpoints

### auth

| Method | Path | Access | Purpose |
|---|---|---|---|
| POST | `/auth/signup/passenger` | Public | Register a passenger (18+, unique email and mobile, strong password, terms) |
| POST | `/auth/signup/staff` | Public | Register staff; needs the station access code, `PJ-EMP-XXXX` id and an `@payanam.in` email |
| POST | `/auth/signin` | Public | Sign in with email or mobile; returns the JWT |
| POST | `/auth/forgot-password` | Public | SIMULATED: generates a 6-digit OTP and returns it (no e-mail service) |
| POST | `/auth/reset-password` | Public | Reset the password with the OTP |
| POST | `/auth/logout` | Signed in | Revoke the current session |
| GET | `/auth/me` | Signed in | The signed-in account |
| PUT | `/auth/profile` | Signed in | Edit name, mobile and (passengers) city |
| PUT | `/auth/password` | Signed in | Change the password (signs out other sessions) |

### trains

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/trains` | Public | Timetable sorted by expected arrival |
| GET | `/trains/stats` | Public | Station numbers and the simulation time (landing page, top bar clock) |
| GET | `/trains/{trainNo}/live` | Public | Live status: status, platform, delay |
| GET | `/trains/{trainNo}` | Signed in | One train |
| GET | `/trains/{trainNo}/seat-map` | Signed in | 10-seat grid without names; the caller's own seats are marked |
| PUT | `/trains/{trainNo}` | MANAGE_TRAINS | Edit times / priority of a SCHEDULED train |
| POST | `/trains` | MANAGE_TRAINS | Add a train (at most 6) |

### bookings (passenger)

| Method | Path | Access | Purpose |
|---|---|---|---|
| POST | `/bookings` | Passenger | Book 1–4 passengers: CNF with a seat, or WL/n |
| GET | `/bookings` | Passenger | My PNRs, newest first |
| GET | `/bookings/dashboard` | Passenger | Dashboard KPIs, next journey, activity |
| GET | `/bookings/{pnr}` | Passenger | One of my PNRs (403 for someone else's) |
| POST | `/bookings/{pnr}/cancel` | Passenger | Cancel every active passenger; promotes from the waiting list |
| POST | `/bookings/{pnr}/passengers/{passengerNo}/cancel` | Passenger | Partial cancellation |
| GET | `/notifications` | Passenger | My notifications, newest first |
| POST | `/notifications/{id}/read` | Passenger | Mark one as read |
| POST | `/notifications/read-all` | Passenger | Mark all as read |

### pnr

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/pnr/{pnr}` | Public | Status and timeline of every passenger; names are hidden |

### platforms

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/platforms` | MANAGE_PLATFORMS or MANAGE_TRAINS | Platform cards: status, current train, next free time |
| GET | `/platforms/board` | MANAGE_PLATFORMS or MANAGE_TRAINS | Gantt data: intervals per platform, waiting trains, "now" |
| GET | `/platforms/queue` | MANAGE_PLATFORMS or MANAGE_TRAINS | The waiting PriorityQueue in `poll()` order |
| PUT | `/platforms/{platformId}` | MANAGE_PLATFORMS | Change the length (never below a train planned on it) |
| POST | `/platforms/{platformId}/maintenance` | MANAGE_PLATFORMS | Maintenance on/off (P9) |

### operations

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/operations/dashboard` | VIEW_DASHBOARD | Control Centre KPIs, charts, alerts |
| GET | `/operations/events?limit=100` | VIEW_DASHBOARD | Station event feed, newest first |
| GET | `/operations/trains` | VIEW_DASHBOARD | All trains with operational details |
| POST | `/operations/arrival-requests` | MANAGE_TRAINS or MANAGE_PLATFORMS | Request a platform: ASSIGNED, QUEUED or REJECTED |
| POST | `/operations/trains/{trainNo}/arrival` (`/preview`) | MANAGE_TRAINS or MANAGE_PLATFORMS | Confirm arrival (preview changes nothing) |
| POST | `/operations/trains/{trainNo}/departure` (`/preview`) | MANAGE_TRAINS or MANAGE_PLATFORMS | Confirm departure; release at departure + buffer (P8) |
| POST | `/operations/trains/{trainNo}/delay` (`/preview`) | MANAGE_TRAINS or MANAGE_PLATFORMS | Report a delay; re-plan later trains (P7) |
| GET | `/operations/reservations/trains/{trainNo}` | MANAGE_RESERVATIONS | Seats, FIFO waiting list, PNRs, promotion history |
| GET | `/operations/reservations/pnr/{pnr}` | MANAGE_RESERVATIONS | Any PNR with names (counter lookup) |
| POST | `/operations/reservations/pnr/{pnr}/cancel` | MANAGE_RESERVATIONS | Counter cancellation of a whole PNR |
| POST | `/operations/reservations/pnr/{pnr}/passengers/{n}/cancel` | MANAGE_RESERVATIONS | Counter cancellation of one passenger |

### simulation

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/simulation/clock` | VIEW_DASHBOARD | Current simulation time |
| POST | `/simulation/advance` | CONTROL_SIMULATION | Advance 1–240 minutes; releases platforms and serves the queue |
| POST | `/simulation/play`, `/simulation/pause` | CONTROL_SIMULATION | Play adds 1 minute every 2 seconds |
| POST | `/simulation/reset-clock` | CONTROL_SIMULATION | Restart the seed day at 06:30 |
| POST | `/simulation/load-seed` | CONTROL_SIMULATION | Reload the seed dataset |
| POST | `/simulation/reset-system` | CONTROL_SIMULATION | Remove all trains and bookings (platforms stay) |
| GET / POST | `/simulation/snapshots` | CONTROL_SIMULATION | List / save a snapshot of KPIs and the plan (stored in H2) |

### audit, admin, tests

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/audit?action=&role=&q=&page=0&size=25` | VIEW_AUDIT | Filtered audit log, newest first |
| GET | `/audit/actions` | VIEW_AUDIT | Distinct action names |
| GET | `/audit/export?action=&role=&q=` | VIEW_AUDIT | CSV export (`text/csv`) |
| GET | `/admin/settings` | VIEW_DASHBOARD | Allocation policy, buffer, station name |
| PUT | `/admin/settings` | MANAGE_SETTINGS | Switch BestFit / FirstFit, change the buffer |
| GET | `/tests` | RUN_TESTS | List the lab scenarios |
| POST | `/tests/{id}/run` | RUN_TESTS | Run T1–T7 or F1–F4 on a fresh, isolated station |
| POST | `/tests/run-all` | RUN_TESTS | Run all 11 |

## Request bodies

| Endpoint | Body |
|---|---|
| `POST /auth/signup/passenger` | `{fullName, email, mobile, dateOfBirth, gender, city, password, confirmPassword, acceptTerms}` |
| `POST /auth/signup/staff` | `{fullName, employeeId, designation, email, mobile, password, confirmPassword, accessCode}` |
| `POST /auth/signin` | `{identifier, password, rememberMe}` (identifier = email or 10-digit mobile) |
| `POST /auth/forgot-password` | `{identifier}` |
| `POST /auth/reset-password` | `{identifier, otp, password, confirmPassword}` |
| `PUT /auth/profile` | `{fullName, mobile, city}` |
| `PUT /auth/password` | `{currentPassword, password, confirmPassword}` |
| `POST /bookings` | `{trainNo, passengers: [{name, age, gender}]}` (1–4 passengers) |
| `POST /trains` | `{trainNo, name, origin, destination, arrival "HH:mm", departure "HH:mm", lengthMeters, priority, fare}` |
| `PUT /trains/{no}` | `{arrival, departure, priority}` |
| `POST /operations/arrival-requests` | `{trainNo, type}`; type EMERGENCY / CONNECTION / SCHEDULED, or omitted for the declared priority |
| `POST /operations/trains/{no}/delay` | `{minutes (1–720), reason}` |
| `PUT /platforms/{id}` | `{lengthMeters}` |
| `POST /platforms/{id}/maintenance` | `{maintenance: true}` |
| `PUT /admin/settings` | `{allocationPolicy, clearanceBufferMinutes}` |
| `POST /simulation/advance` | `{minutes}` |
| `POST /simulation/snapshots` | `{label}` |

## Samples

### Sign in

```http
POST /api/v1/auth/signin
{"identifier": "passenger@payanam.in", "password": "Passenger@123", "rememberMe": false}
```

```json
200 OK
{
  "token": "eyJhbGciOiJIUzM4NCJ9.eyJ…",
  "expiresAt": "2026-10-18T07:03:10.593983274Z",
  "user": {
    "id": 1,
    "fullName": "Arun Kumar",
    "email": "passenger@payanam.in",
    "role": "PASSENGER",
    "displayTitle": "Passenger",
    "permissions": []
  }
}
```

Wrong password:

```json
401 Unauthorized
{
  "status": 401,
  "code": "AUTH_FAILED",
  "message": "Invalid email/mobile or password. 4 attempts left before the account is locked.",
  "path": "/api/v1/auth/signin",
  "fieldErrors": []
}
```

### Sign-up validation

```http
POST /api/v1/auth/signup/passenger
{"fullName": "A", "email": "x", "mobile": "123", "dateOfBirth": "2015-01-01", "gender": "MALE", "city": "",
 "password": "weak", "confirmPassword": "other", "acceptTerms": false}
```

```json
400 Bad Request
{
  "code": "VALIDATION_FAILED",
  "message": "Please correct the highlighted fields",
  "fieldErrors": [
    {"field": "mobile", "message": "Enter a 10-digit Indian mobile number starting with 6-9"},
    {"field": "city", "message": "City is required"},
    {"field": "fullName", "message": "Name must be 2-80 characters"},
    {"field": "confirmPassword", "message": "Passwords do not match"},
    {"field": "password", "message": "Password needs 8+ characters with upper and lower case letters, a digit and a special character"},
    {"field": "acceptTerms", "message": "You must accept the terms of use"},
    {"field": "dateOfBirth", "message": "You must be at least 18 years old to register"},
    {"field": "email", "message": "Enter a valid email address"}
  ]
}
```

### Forgot password (simulated OTP)

```http
POST /api/v1/auth/forgot-password
{"identifier": "passenger@payanam.in"}
```

```json
200 OK
{
  "message": "If the account exists, a one-time password has been sent (simulated: shown below).",
  "simulatedOtp": "441885",
  "expiresInMinutes": 10
}
```

### Timetable (one train shown)

```http
GET /api/v1/trains
```

```json
200 OK
[
  {
    "trainNo": "12635",
    "name": "Vaigai Express",
    "origin": "Chennai Egmore",
    "destination": "Madurai Jn",
    "scheduledArrival": "2026-10-18T07:00:00",
    "scheduledDeparture": "2026-10-18T07:15:00",
    "expectedArrival": "2026-10-18T07:00:00",
    "expectedDeparture": "2026-10-18T07:15:00",
    "lengthMeters": 420,
    "priority": "NORMAL",
    "status": "PLATFORM_ASSIGNED",
    "liveStatus": "Platform P2 assigned",
    "delayMinutes": 0,
    "platformId": "P2",
    "requestType": "SCHEDULED",
    "fare": 485,
    "capacity": 10,
    "confirmed": 10,
    "seatsLeft": 0,
    "waiting": 4,
    "bookingOpen": true
  }
]
```

### Book (R1, R2)

```http
POST /api/v1/bookings
Authorization: Bearer …
{"trainNo": "12637", "passengers": [
  {"name": "Arun Kumar", "age": 31, "gender": "MALE"},
  {"name": "Meena Kumar", "age": 34, "gender": "FEMALE"}]}
```

```json
201 Created
{
  "pnr": "4027212889",
  "trainNo": "12637",
  "trainName": "Pandian Express",
  "arrival": "2026-10-18T07:45:00",
  "departure": "2026-10-18T08:00:00",
  "trainStatus": "SCHEDULED",
  "bookedAt": "2026-10-18T06:30:00",
  "bookedBy": "Arun Kumar",
  "farePerPassenger": 510,
  "totalFare": 1020,
  "journeyState": "UPCOMING",
  "summary": "2 CNF",
  "cancellable": true,
  "passengers": [
    {"passengerNo": 1, "name": "Arun Kumar", "age": 31, "gender": "MALE", "status": "CNF", "label": "CNF/S5", "seatNo": 5, "waitingPosition": 0},
    {"passengerNo": 2, "name": "Meena Kumar", "age": 34, "gender": "FEMALE", "status": "CNF", "label": "CNF/S6", "seatNo": 6, "waitingPosition": 0}
  ]
}
```

On a full train (Vaigai Express) the same call returns `"summary": "1 WL"` and `"label": "WL/5"`.

### Cancel one passenger → promotion (R3, R4)

```http
POST /api/v1/bookings/4000824633/passengers/1/cancel
```

```json
200 OK
{
  "pnr": "4000824633",
  "cancelledTickets": ["4000824633/1 (S1)"],
  "promotions": [
    {"ticketId": "4011544862/1", "pnr": "4011544862", "from": "WL/1", "to": "CNF/S1"}
  ],
  "message": "Cancelled 4000824633/1 (S1). Promoted 4011544862/1: WL/1 → CNF/S1.",
  "booking": { "pnr": "4000824633", "summary": "1 CNF, 1 CAN", "totalFare": 485, "passengers": [ … ] }
}
```

Cancelling the same passenger again:

```json
409 Conflict
{
  "code": "INVALID_CANCELLATION",
  "message": "Ticket 4027212889/2 is already cancelled",
  "path": "/api/v1/bookings/4027212889/passengers/2/cancel"
}
```

### Public PNR status

```http
GET /api/v1/pnr/4027212889
```

```json
200 OK
{
  "pnr": "4027212889",
  "trainNo": "12637",
  "bookedBy": null,
  "summary": "2 CNF",
  "passengers": [
    {
      "passengerNo": 1,
      "name": null,
      "label": "CNF/S5",
      "timeline": [
        {"at": "2026-10-18T06:30:00", "label": "Booked", "note": "Booking received"},
        {"at": "2026-10-18T06:30:00", "label": "CNF/S5", "note": "Seat 5 confirmed"}
      ]
    }
  ]
}
```

Names are `null` on the public lookup. An unknown PNR returns 404 `PNR_NOT_FOUND`.

### Seat map (no names)

```http
GET /api/v1/trains/12635/seat-map
```

```json
200 OK
{
  "trainNo": "12635",
  "capacity": 10,
  "seats": [
    {"seatNo": 1, "occupied": true, "mine": false},
    {"seatNo": 2, "occupied": true, "mine": true},
    "… seats 3–10 …"
  ],
  "waitingCount": 4,
  "myWaitingPositions": [],
  "bookingOpen": true
}
```

### Arrival requests (P2, P3)

```http
POST /api/v1/operations/arrival-requests
{"trainNo": "12693", "type": "SCHEDULED"}
```

```json
200 OK
{
  "trainNo": "12693",
  "outcome": "QUEUED",
  "platformId": null,
  "start": "2026-10-18T07:20:00",
  "end": "2026-10-18T07:45:00",
  "queuePosition": 2,
  "message": "Queued 12693 at position 2. Platforms long enough (P1, P2) are busy during 07:20–07:45; next compatible release: P2 at 07:25"
}
```

An overlong train:

```json
200 OK
{
  "trainNo": "12637",
  "outcome": "REJECTED",
  "queuePosition": 0,
  "message": "Overlong train – exceeds all platform lengths: 12637 is 650 m, the longest platform is 600 m"
}
```

### Waiting queue (P4)

```http
GET /api/v1/platforms/queue
```

```json
200 OK
[
  {
    "rank": 1, "trainNo": "06055", "trainName": "Medical Relief Special", "type": "EMERGENCY", "priorityRank": 0,
    "arrival": "2026-10-18T07:12:00", "desiredEnd": "2026-10-18T07:35:00", "lengthMeters": 560,
    "waitReason": "Platforms long enough (P1) are busy during 07:12–07:35; next compatible release: P1 at 07:30",
    "nextReleasePlatform": "P1", "nextReleaseTime": "2026-10-18T07:30:00"
  },
  {
    "rank": 2, "trainNo": "12693", "trainName": "Pearl City Express", "type": "SCHEDULED", "priorityRank": 2,
    "arrival": "2026-10-18T07:20:00", "desiredEnd": "2026-10-18T07:45:00", "lengthMeters": 430,
    "waitReason": "Platforms long enough (P1, P2) are busy during 07:20–07:45; next compatible release: P2 at 07:25",
    "nextReleasePlatform": "P2", "nextReleaseTime": "2026-10-18T07:25:00"
  }
]
```

### Delay preview (P7)

```http
POST /api/v1/operations/trains/12635/delay/preview
{"minutes": 20, "reason": "Signal failure"}
```

```json
200 OK
{
  "preview": true,
  "trainNo": "12635",
  "minutes": 20,
  "reason": "Signal failure",
  "platformId": "P2",
  "oldArrival": "2026-10-18T07:00:00",
  "oldDeparture": "2026-10-18T07:15:00",
  "newArrival": "2026-10-18T07:20:00",
  "newDeparture": "2026-10-18T07:35:00",
  "oldOccupationEnd": "2026-10-18T07:25:00",
  "newOccupationEnd": "2026-10-18T07:45:00",
  "occupationExtended": true,
  "requeued": false,
  "lostPlatform": false,
  "bumped": [],
  "reallocated": [],
  "alerts": [],
  "message": "Train 12635 delayed by 20 min (Signal failure): arrival 07:00 → 07:20, departure 07:15 → 07:35. P2 occupation extended to 07:45."
}
```

The same body without `/preview` applies the delay; `"preview": false` is returned and the change is audited.

### Control Centre (trimmed)

```http
GET /api/v1/operations/dashboard
```

```json
200 OK
{
  "now": "2026-10-18T07:00:00",
  "nextRelease": "P2 at 07:25 (after 12635)",
  "kpis": {
    "trainsToday": 6, "onPlatform": 0, "waitingForPlatform": 2, "rejected": 1, "delayed": 0,
    "totalBookings": 34, "activeTickets": 44, "occupancyPercent": 63, "departed": 0, "waitlisted": 6
  },
  "seatOccupancy": [
    {"trainNo": "12635", "trainName": "Vaigai Express", "confirmed": 10, "waiting": 4, "capacity": 10}
  ],
  "alerts": [
    {"sequence": 53, "time": "2026-10-18T06:30:00", "category": "PLATFORM", "alert": true, "trainNo": "12637",
     "message": "REJECTED – Overlong train – exceeds all platform lengths: 12637 is 650 m, the longest platform is 600 m"}
  ]
}
```

### Counter view of a train (trimmed)

```http
GET /api/v1/operations/reservations/trains/12635
```

```json
200 OK
{
  "trainNo": "12635",
  "trainStatus": "PLATFORM_ASSIGNED",
  "capacity": 10,
  "confirmed": [
    {"seatNo": 1, "ticketId": "4011544862/1", "pnr": "4011544862", "passengerName": "Arun Kumar", "promoted": true}
  ],
  "waiting": [
    {"position": 1, "ticketId": "4014843394/1", "pnr": "4014843394", "passengerName": "Rahul Iyer", "bookedAt": "2026-10-15T12:15:00"},
    {"position": 2, "ticketId": "4020615825/1", "pnr": "4020615825", "passengerName": "Anitha Selvam", "bookedAt": "2026-10-16T16:30:00"}
  ],
  "promotions": [
    {"time": "2026-10-18T07:00:00", "category": "PROMOTION", "message": "Promoted 4011544862/1: WL/1 -> CNF/S1"}
  ]
}
```

### Advance the clock

```http
POST /api/v1/simulation/advance
{"minutes": 30}
```

```json
200 OK
{
  "now": "2026-10-18T07:00:00",
  "start": "2026-10-18T06:30:00",
  "elapsedMinutes": 30,
  "running": false,
  "tickSeconds": 2,
  "playStepMinutes": 1,
  "events": []
}
```

### Audit log (trimmed)

```http
GET /api/v1/audit?size=2
```

```json
200 OK
{
  "content": [
    {"id": 19, "timestamp": "2026-10-18T06:15:52.927857", "simulationTime": "2026-10-18T07:00:00",
     "actor": "priya.raman@example.in", "role": "PASSENGER", "action": "PASSENGER_CANCELLED",
     "details": "Cancelled 4000824633/1 (S1). Promoted 4011544862/1: WL/1 → CNF/S1.", "success": true}
  ],
  "page": 0, "size": 2, "totalElements": 20, "totalPages": 10
}
```

### Test Lab

```http
POST /api/v1/tests/T1/run
```

```json
200 OK
{
  "id": "T1",
  "title": "Cancelled seat goes to the oldest waiting passenger",
  "rule": "R3",
  "kind": "POSITIVE",
  "passed": true,
  "expected": "WL/1 becomes CNF/S4 and WL/2 moves up to WL/1",
  "actual": "Anu CNF/S4, Babu WL/1",
  "steps": [
    {"description": "Booked 10 passengers: confirmed = 10 / 10", "ok": true},
    {"description": "Anu is WL/1, Babu is WL/2", "ok": true},
    {"description": "Cancel PNR 4003298532 (CNF/S4)", "ok": true},
    {"description": "Anu promoted into the freed seat: CNF/S4", "ok": true},
    {"description": "Babu moved up: WL/1", "ok": true},
    {"description": "Seat count still within 0–10: 10", "ok": true}
  ],
  "durationMs": 2
}
```

### Role checks (F4)

A passenger calling a staff API, or a Booking Clerk calling a platform API:

```http
GET /api/v1/operations/dashboard     (passenger token)
GET /api/v1/platforms                (clerk token)
```

```json
403 Forbidden
{
  "status": 403,
  "error": "Forbidden",
  "code": "FORBIDDEN",
  "message": "Your account is not allowed to use this feature",
  "path": "/api/v1/platforms",
  "fieldErrors": []
}
```

## Trying it with curl

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/signin \
  -H 'Content-Type: application/json' \
  -d '{"identifier":"stationmaster@payanam.in","password":"Station@123"}' | jq -r .token)

curl -s http://localhost:8080/api/v1/platforms/queue -H "Authorization: Bearer $TOKEN" | jq
curl -s -X POST http://localhost:8080/api/v1/tests/run-all -H "Authorization: Bearer $TOKEN" | jq '.[] | {id, passed}'
```
