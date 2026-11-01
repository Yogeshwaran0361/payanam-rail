# Viva questions (30) with short answers

## Problem and design

**1. What problem does the project solve?**
A single station handles reservations and its platform plan by hand. The system gives a cancelled seat to the oldest
waiting passenger at once, never puts two trains on one platform at the same time, re-plans when a train is delayed,
and serves emergency and connecting trains first when several trains wait.

**2. What are the fixed assumptions?**
One station, at most 6 trains, 3 platforms (600, 450, 300 m), one seat class of exactly 10 seats per train, one
end-to-end segment, 1–4 passengers per PNR, occupation = `[arrival, departure + 10 min)`, and a simulation clock as
"now". They are enforced in code: `TrainService.CAPACITY`, `TrainRegistry.MAX_TRAINS`, validation on `BookingRequest`.

**3. Why do you use a database if the case study does not need one?**
Only for what must survive a restart and belongs to a real sign-in system: accounts (BCrypt hashes, lockout counters),
sessions (so logout can revoke a JWT), password-reset OTPs, the audit log and snapshots. Trains, PNRs, platforms and
queues live only in the in-memory collections; the database never replaces them.

**4. Explain the layered architecture.**
Controller (HTTP, validation, `@PreAuthorize`) → service (use cases, audit, DTO mapping) → domain (plain Java rules
and data structures) → repository (JPA, users and audit only). Controllers never see domain objects; they return DTO
records, so the API can change without touching the rules.

**5. Why is the domain plain Java without Spring?**
So the rules can be unit-tested in milliseconds without a server, read like the case study, and be copied for previews
and the Test Lab. Spring only wires the services around it.

## OOP

**6. Where is encapsulation used?**
`TrainService` keeps `seats`, `confirmedCount` and `waitingList` private; the only way to change them is `bookSeat()`
and `cancelBooking()`, which validate the 0–10 bound. `Platform.occupyPlatform()` is the only way to add an interval
and refuses overlap, maintenance and a too-short platform. `Reservation`'s state changes are package-private, so only
its train can confirm, promote or cancel it.

**7. Where are inheritance and polymorphism?**
Abstract `PlatformRequest` with `EmergencyArrival`, `ConnectionArrival` and `ScheduledArrival`, each overriding
`priorityRank()` (0, 1, 2). The queue comparator calls `priorityRank()` without knowing the subclass. Also abstract
`UserAccount` with `PassengerUser` and `StationStaffUser` overriding `getRole()` and `getPermissions()`.

**8. Where is abstraction?**
The `PlatformAllocationPolicy` interface: the scheduler calls `selectPlatform()` and does not know whether BestFit or
FirstFit is behind it. Switching the policy in Settings swaps the object at runtime (strategy pattern).

**9. Where is composition, and how is it different from aggregation?**
A `TrainService` owns its seats and its waiting `ArrayDeque`: they are created with the train and never shared, so the
parts cannot exist without the whole (composition). `RailwayStation` likewise owns its registry, desk, scheduler,
clock and log. A `Booking` only refers to `Reservation` objects that the train also holds (aggregation).

**10. Name the custom exceptions and how they reach the client.**
`TrainNotFoundException`, `ReservationNotFoundException`, `SeatUnavailableException`, `InvalidCancellationException`,
`PlatformConflictException`, `UnauthorizedActionException` and others, all extending `RailwayException` with a code.
`GlobalExceptionHandler` maps them to one JSON error format with 404, 409, 403, 401, 423 or 400.

## Data structures

**11. Why an ArrayDeque for the waiting list?**
The rule is FIFO: the oldest waiting passenger is promoted. `addLast()` and `pollFirst()` are O(1). An `ArrayList`
would shift every element on `remove(0)` (O(n)); a `LinkedList` allocates a node per passenger.

**12. Why a PriorityQueue for trains waiting for a platform?**
We always need the most urgent waiting train. A binary heap gives `peek()` in O(1) and `add()`/`poll()` in O(log n),
ordered by rank, then arrival time, then train number.

**13. What happens to the PriorityQueue when a queued train is delayed?**
Its ordering key changes, and a heap does not re-sort itself when an element is mutated. So the request is removed
first, the delay is applied, and the request is reinserted at its new position.

**14. What is the min-heap of release times for?**
Each assigned interval pushes `(platform, train, departure + buffer)`. `peek()` gives the next release in O(1); the
clock pops due releases in time order; walking a copy finds the earliest release of a platform long enough for a
particular train, which is the "next compatible release" shown for queued trains.

**15. How do you check for overlap, and what is its complexity?**
Each platform keeps its intervals in an `ArrayList` sorted by start. A hand-written binary search finds where the new
interval would go; only the neighbours before and after can overlap, because intervals on one platform never overlap
each other. That is O(log n) instead of O(n).

**16. Why half-open intervals `[start, end)`?**
So back-to-back trains do not clash: `[07:00, 07:25)` and `[07:25, 07:40)` share no instant. Two intervals overlap
exactly when `a < d && c < b`.

**17. Why insert into a sorted list at the upper bound?**
Inserting after all equal keys keeps equal items in insertion order (a stable insert), which matters for the event log
and booking history where several items share a timestamp.

**18. Why HashMaps?**
Every action starts with a lookup by id: PNR, train number, platform id, passenger id, account id. A hash map does
that in O(1) average instead of scanning a list.

## Rules

**19. Walk through a cancellation with promotion.**
`cancelBooking()` checks the ticket is not already cancelled, sets `seats[k] = null` and decrements the count, then
`pollFirst()` takes the oldest waiting ticket, puts it in the same seat `k`, increments the count and renumbers the
rest (WL/2 → WL/1). A listener sends the owner a notification "WL/1 → CNF/S7". All of it happens inside one lock.

**20. How does BestFit choose a platform, and why is it the default?**
Among valid platforms (in service, long enough, no overlap) it picks the shortest; on a tie, the lowest id. It keeps
the long platforms free for long trains. FirstFit picks the lowest-id valid platform and can waste P1 on a short train.

**21. How is an emergency train prioritised without breaking P5?**
It goes to the front of the queue (rank 0), so it gets the next suitable platform first. But it never takes a platform
from a train that has arrived; at most a later, not-yet-arrived train can be un-assigned by a delay.

**22. What does a delay do to the plan?**
Times move. A queued train is removed and reinserted. An assigned or arrived train has its occupation extended; every
later, not-yet-arrived train that now overlaps is un-assigned, re-queued and re-allocated, with an alert each. If the
extension would hit an arrived train, the delayed (not-yet-arrived) train gives up its own platform instead.

**23. What happens on departure?**
The interval is trimmed to the real departure plus the 10-minute buffer, the release is pushed on the min-heap, and the
queue is served: requests are polled in priority order, each gets one allocation attempt, and those that still do not
fit go back.

**24. How does the preview show the exact impact without changing anything?**
`StationService.preview()` runs the same method on `station.copy()`, a deep copy of every train, ticket, platform and
queue (an identity map keeps shared tickets shared). The real algorithm runs on the copy and the copy is thrown away.

## Security

**25. How does authentication work?**
Sign-in checks the BCrypt hash, creates an `AuthSession` row and returns a JWT whose `jti` is the session id. Every
request passes `JwtAuthenticationFilter`, which verifies the signature and expiry and checks the session is still
active, then sets `ROLE_*` and `PERM_*` authorities.

**26. How do you stop a passenger from using staff features?**
Three layers: URL rules in `SecurityConfig` (`/api/v1/operations/**` needs the staff role), `@PreAuthorize` with a
permission on each staff endpoint, and route guards plus hidden menu items in React. The API is the real guard; the UI
only hides what would fail. Test F4 makes a real HTTP call and expects 403.

**27. How does the lockout work?**
`UserAccount.registerFailedLogin()` counts failures; the fifth sets `lockedUntil = now + 15 min`. While locked, even
the right password returns 423. A successful sign-in resets the counter.

**28. Why is the forgot-password flow simulated, and is it still safe?**
There is no e-mail or SMS service by design, so the 6-digit OTP is shown on screen and in the log. It is still stored
only as a hash, expires in 10 minutes and allows a limited number of attempts.

## Testing and quality

**29. How did you test it?**
129 JUnit 5 tests: domain tests for every rule R1–R6 and P1–P9, Mockito tests for the auth service, and Spring Boot
API tests over HTTP for roles, validation and the error format. JaCoCo fails the build below 80% line coverage of
domain and service; the actual figure is 95–100%. The Test Lab runs T1–T7 and F1–F4 live in the UI on isolated data.

**30. What are the limitations and what would you add next?**
Single station and in-memory state (a restart reloads the seed day), one seat class, one segment, simulated OTP and no
payment. Next: multi-station routing on a graph (Dijkstra over stations), real payments, SMS/e-mail notifications,
live train feeds instead of manual confirmations, and a mobile app.
