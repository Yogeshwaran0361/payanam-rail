# Architecture and sequence diagrams

PlantUML versions of every diagram on this page are in [architecture.puml](architecture.puml).
The class diagrams are in [uml-class-diagram.md](uml-class-diagram.md).

## 1. Component diagram

One Spring Boot process serves the React single-page app, the REST API and Swagger UI on port 8080. The React build is
produced by Maven (frontend-maven-plugin) and copied into the jar's `static/` folder, so `./mvnw spring-boot:run` is
the only command needed.

```mermaid
flowchart TB
    subgraph Browser["Browser"]
        SPA["React 18 + TypeScript SPA<br/>(Vite build, Tailwind, Recharts)"]
        AX["Axios client<br/>JWT interceptor, 401 → sign-out"]
        LS[("localStorage<br/>token, theme")]
        SPA --> AX
        AX <--> LS
    end

    subgraph Boot["Spring Boot 3 application (Java 17, port 8080)"]
        STATIC["Static resources<br/>index.html + assets, SPA fallback"]
        SW["springdoc-openapi<br/>/swagger-ui.html, /v3/api-docs"]
        subgraph Sec["Spring Security filter chain"]
            JF["JwtAuthenticationFilter<br/>verify signature + session not revoked"]
            PA["@PreAuthorize<br/>ROLE_* and PERM_* authorities"]
        end
        subgraph Web["Controller layer (/api/v1)"]
            C1["auth · trains · bookings · pnr"]
            C2["platforms · operations · simulation"]
            C3["audit · admin · tests"]
        end
        GEH["GlobalExceptionHandler<br/>one JSON error format"]
        subgraph Svc["Service layer"]
            S1["AuthService · AuditService"]
            S2["BookingService · TimetableService<br/>NotificationService"]
            S3["OperationsService · StaffReservationService<br/>DashboardService · SimulationService"]
            S4["TestScenarioService<br/>(isolated lab stations)"]
            SS["StationService<br/>owns the live station, one lock,<br/>preview() on a deep copy"]
        end
        subgraph Dom["Domain (plain Java, no Spring)"]
            RS["RailwayStation"]
            TR["TrainRegistry<br/>HashMap + sorted ArrayList"]
            RD["ReservationDesk<br/>HashMaps + sorted history"]
            TS["TrainService<br/>seats[10] + ArrayDeque WL"]
            PS["PlatformScheduler<br/>PriorityQueue + release min-heap"]
            PL["Platform<br/>sorted intervals"]
            POL["PlatformAllocationPolicy<br/>BestFit / FirstFit"]
        end
        subgraph Repo["Repository layer (Spring Data JPA)"]
            R1["UserAccount · AuthSession<br/>PasswordResetRequest"]
            R2["AuditLogEntry · StationSnapshot"]
        end
        SEED["seed/*.json<br/>StationFactory, SeedAccountsLoader"]
    end

    H2[("H2 file database<br/>backend/data/payanam-rail.mv.db<br/>accounts, sessions, audit, snapshots ONLY")]

    AX -- "HTTPS JSON, Bearer JWT" --> JF
    SPA -. "GET /, /assets" .-> STATIC
    JF --> PA --> Web
    Web --> Svc
    Web -.-> GEH
    S2 --> SS
    S3 --> SS
    SS --> RS
    RS --> TR & RD & PS
    TR --> TS
    PS --> PL
    PS --> POL
    S1 --> R1
    S1 --> R2
    S3 --> R2
    Repo --> H2
    SEED --> SS
    SEED --> R1
```

### Layers and their rules

| Layer | Package | Responsibility | Rule |
|---|---|---|---|
| Controller | `controller` | HTTP mapping, `@Valid` request DTOs, `@PreAuthorize` | No business logic; returns DTOs only |
| Service | `service`, `service.testlab` | Use cases, audit records, mapping domain → DTO (`StationViews`) | The only code that touches the station or the repositories |
| Domain | `domain.model`, `domain.engine`, `domain.policy`, `domain.user` | Rules R1–R6 and P1–P9 on hand-written collections | Plain Java; no Spring, no database |
| Repository | `repository` | JPA repositories for users, sessions, OTPs, audit, snapshots | Never stores trains, PNRs or platforms |
| Cross-cutting | `security`, `exception`, `config`, `seed` | JWT, error format, properties, seed loading | |

### Concurrency and previews

The live `RailwayStation` is a single object graph in memory. `StationService.read()` and `write()` run a function while
holding the station's monitor, so two requests can never interleave inside an allocation or a promotion.
`StationService.preview()` runs the same function on `station.copy()` (a deep copy of trains, tickets, platforms and
queues, without the notification listener). That is how the delay, arrival and departure previews in Train Operations
show the exact impact without changing anything.

## 2. Sequence: Book a ticket (R1, R2, R6)

```mermaid
sequenceDiagram
    autonumber
    actor P as Passenger
    participant UI as BookTicket page
    participant BC as BookingController
    participant BS as BookingService
    participant SS as StationService
    participant RD as ReservationDesk
    participant T as TrainService
    participant A as AuditService

    P->>UI: choose train, add 1–4 passengers, Confirm
    UI->>BC: POST /api/v1/bookings {trainNo, passengers[]}
    BC->>BC: @Valid BookingRequest (1–4 passengers, name, age, gender)
    BC->>BS: book(user, request)
    BS->>SS: write(station → station.book(...))
    SS->>RD: book(ownerId, ownerName, trainNo, people, now)
    RD->>RD: registry.get(trainNo) (HashMap, O(1)), else TrainNotFoundException
    RD->>RD: new PNR "4xxxxxxxxx", one Reservation per passenger
    loop each passenger
        RD->>T: bookSeat(ticket, now)
        alt train ARRIVED / DEPARTED (R6)
            T-->>RD: SeatUnavailableException → 409
        else confirmed < 10 (R1)
            T->>T: seats[lowestFreeSeat] = ticket, confirmedCount++
            T-->>RD: CNF/S n
        else train full
            T->>T: waitingList.addLast(ticket)
            T-->>RD: WL/n
        end
    end
    RD->>RD: bookingsByPnr.put, pnrsByOwner.add, history.insertSorted
    RD-->>SS: Booking
    SS-->>BS: BookingView (DTO)
    BS->>A: record("BOOKING_CREATED", ...)
    BS-->>BC: BookingView
    BC-->>UI: 201 Created {pnr, passengers[label]}
    UI-->>P: success page: PNR + CNF/S5, WL/2 ...
```

## 3. Sequence: Cancel → Promote (R3, R4, R5)

```mermaid
sequenceDiagram
    autonumber
    actor P as Passenger (or Booking Clerk)
    participant BC as BookingController / StaffReservationController
    participant BS as BookingService / StaffReservationService
    participant SS as StationService
    participant RD as ReservationDesk
    participant T as TrainService
    participant N as NotificationService
    participant A as AuditService

    P->>BC: POST /bookings/{pnr}/cancel (or /passengers/{n}/cancel)
    BC->>BS: cancel(user, pnr)
    BS->>SS: write(station → cancelBooking(pnr, userId, staffOverride))
    SS->>RD: cancelBooking(pnr, actor, staffOverride, now)
    RD->>RD: bookingsByPnr.get(pnr), else ReservationNotFoundException (404)
    RD->>RD: owner check, else UnauthorizedActionException (403)
    RD->>RD: no active tickets left, else InvalidCancellationException (409)
    loop each active ticket of the PNR
        RD->>T: cancelBooking(ticket, now)
        alt ticket was WL
            T->>T: waitingList.remove(ticket), renumber WL/1..n
        else ticket was CNF/S k
            T->>T: seats[k] = null, confirmedCount--
            T->>T: promoted = waitingList.pollFirst()  (oldest, O(1))
            opt somebody was waiting
                T->>T: seats[k] = promoted, confirmedCount++
                T->>T: promoted.promote(k): WL/1 → CNF/S k
                T->>T: renumber the rest: WL/2 → WL/1 ...
            end
        end
        T-->>RD: CancellationOutcome(cancelled, freedSeat, promoted)
        opt promoted != null
            RD->>N: onPromotion(PromotionNotice) via StationListener
            N->>N: in-app notification for the promoted PNR's owner "WL/1 → CNF/S7"
        end
    end
    RD-->>BS: outcomes
    BS->>A: record("BOOKING_CANCELLED", ...)
    BS-->>P: 200 {cancelledTickets, promotions, message}
```

## 4. Sequence: Arrival request → Allocate (P1–P4, P6)

```mermaid
sequenceDiagram
    autonumber
    actor C as Platform Controller
    participant OC as OperationsController
    participant OS as OperationsService
    participant SS as StationService
    participant PS as PlatformScheduler
    participant POL as PlatformAllocationPolicy (BestFit)
    participant PL as Platform
    participant Q as waitingQueue (PriorityQueue)
    participant H as releaseHeap (min-heap)

    C->>OC: POST /operations/arrival-requests {trainNo, type}
    OC->>OS: requestPlatform(trainNo, type)
    OS->>SS: write(station → requestPlatform)
    SS->>PS: requestPlatform(trainNo, type)
    PS->>PS: longest platform < train length?
    alt overlong (P2)
        PS-->>OS: REJECTED "Overlong train – exceeds all platform lengths"
    else fits somewhere
        PS->>PS: request = PlatformRequest.create(type, train)  (Emergency/Connection/Scheduled)
        PS->>POL: selectPlatform(train, [arr, dep + buffer), platforms)
        loop each platform
            POL->>PL: isAvailableForAllocation(), canFit(length), isFree(start, end)
            PL->>PL: binary search neighbours in the sorted interval list
        end
        POL-->>PS: shortest valid platform (tie → lowest id) or empty
        alt platform found (P1, P6)
            PS->>PL: occupyPlatform(trainNo, start, end, length)  re-checks P1
            PS->>H: add(PlatformRelease(platform, train, end))
            PS-->>OS: ASSIGNED P#
        else all suitable platforms busy (P3)
            PS->>Q: add(request)  ordered by rank → arrival → trainNo (P4)
            PS->>H: walk a copy for the next release of a long-enough platform
            PS-->>OS: QUEUED at position n, "next compatible release: P2 at 07:25"
        end
    end
    OS->>OS: audit ARRIVAL_REQUEST
    OS-->>C: AllocationView
```

## 5. Sequence: Delay → Reallocate (P5, P7)

```mermaid
sequenceDiagram
    autonumber
    actor C as Platform Controller
    participant UI as Train Operations modal
    participant OS as OperationsService
    participant SS as StationService
    participant PS as PlatformScheduler
    participant PL as Platform
    participant Q as waitingQueue
    participant N as NotificationService

    C->>UI: Report delay 20 min "Signal failure", Preview impact
    UI->>OS: POST /trains/12635/delay/preview
    OS->>SS: preview(station → reportDelay)  runs on station.copy()
    SS-->>UI: DelayImpact (old/new times, extended, bumped, re-allocated, alerts)
    C->>UI: Confirm
    UI->>OS: POST /trains/12635/delay
    OS->>SS: write(station → reportDelay)
    SS->>PS: reportDelay(trainNo, 20, reason)
    alt train is WAITING_FOR_PLATFORM
        PS->>Q: remove(request)  before its key changes
        PS->>PS: registry.delay(train) (timetable re-sorted)
        PS->>Q: add(request)  reinserted at its new rank
    else train is PLATFORM_ASSIGNED or ARRIVED
        PS->>PL: removeOccupation(train), remove its release
        PS->>PS: registry.delay(train): new [arr, dep + buffer)
        PS->>PL: findOverlapping(newStart, newEnd)
        alt a clash may not be moved (arrived or earlier train, P5)
            PS->>Q: the delayed (not arrived) train gives up its platform and waits
        else every clash is a later, not-yet-arrived train
            loop each later overlapping train
                PS->>PL: removeOccupation(later)
                PS->>Q: add(later request)  un-assigned and re-queued
                PS->>PS: alert "22671 un-assigned from P3 … re-queued"
            end
            PS->>PL: reoccupy(train, newStart, newEnd)  occupation extended
        end
    end
    PS->>PS: processQueue(): poll in P4 order, tryAllocate each, re-add the rest
    PS-->>SS: DelayImpact
    SS->>N: onTrainUpdate for every PNR of the affected trains ("Delay", "Platform update")
    OS->>OS: audit DELAY_REPORTED
    OS-->>C: 200 DelayImpact
```

## 6. Sequence: Sign in and an authorised request

```mermaid
sequenceDiagram
    autonumber
    actor U as User
    participant UI as AuthPage
    participant AC as AuthController
    participant AS as AuthService
    participant DB as H2 (UserAccount, AuthSession)
    participant F as JwtAuthenticationFilter
    participant API as Any /api/v1 controller

    U->>UI: email or mobile + password (+ remember me)
    UI->>AC: POST /auth/signin
    AC->>AS: signIn(request)
    AS->>DB: findByEmail / findByMobile
    alt locked until > now
        AS-->>UI: 423 ACCOUNT_LOCKED
    else BCrypt mismatch
        AS->>DB: failedLoginAttempts++ (5th → lockedUntil = now + 15 min)
        AS-->>UI: 401 AUTH_FAILED "n attempts left"
    else correct
        AS->>DB: save AuthSession(id = jti, expiresAt)
        AS-->>UI: 200 {token, expiresAt, user}
    end
    UI->>UI: store token, schedule auto-logout at expiresAt
    UI->>API: GET ... Authorization: Bearer token
    API->>F: (filter runs first)
    F->>F: verify signature and expiry
    F->>DB: AuthSession(jti) active and not revoked?
    F->>API: SecurityContext = ROLE_* + PERM_* authorities
    API->>API: @PreAuthorize("hasAuthority('PERM_...')") else 403 FORBIDDEN
```
