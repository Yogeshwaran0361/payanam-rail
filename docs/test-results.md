# Test results

Generated from the JUnit 5 run of `./mvnw test` on 2026-10-18 (surefire reports in `backend/target/surefire-reports`, JaCoCo report in `backend/target/site/jacoco/index.html`). The Test Lab rows (T1–T7, F1–F4) quote the `expected` and `actual` text that the running application returned from `POST /api/v1/tests/run-all`.

## Summary

| Item | Result |
|---|---|
| Backend tests (JUnit 5 + Mockito + Spring Boot Test) | **129 passed**, 0 failed, 0 skipped, in 14 test classes |
| Frontend tests (Vitest) | 12 passed (validation rules and Gantt geometry) |
| JaCoCo line coverage, domain packages | engine 98.0%, model 97.1%, policy 100%, user 95.4% |
| JaCoCo line coverage, service packages | service 95.1%, service.testlab 92.5% |
| JaCoCo gate | the build fails below 80% line coverage of `domain.*` and `service` |
| Test Lab in the UI | 11 / 11 PASS (`Run all`) |

## Rule-level results

Each row names the JUnit tests that check it; **Status** is computed from those tests in the surefire report.

| Test ID | Description | Input | Expected | Actual | Status | JUnit tests |
|---|---|---|---|---|---|---|
| R1 | Book on a train with free seats / a full train | 12 single-passenger bookings on one 10-seat train | Tickets 1-10 CNF/S1-S10; 11th WL/1, 12th WL/2 | S1-S10 confirmed, then WL/1 and WL/2 in FIFO order | ✅ PASS | `TrainServiceReservationTest.firstTenConfirmed`<br>`TrainServiceReservationTest.overflowWaitlisted`<br>`BookingApiTest.book` |
| R2 | One PNR holds 1-4 passengers, status per passenger | Two 4-passenger PNRs, then a third 4-passenger PNR (2 seats left); PNRs with 0 and 5 passengers | CNF/S9, CNF/S10, WL/1, WL/2 on one PNR; 0 or 5 passengers refused | CNF/S9, CNF/S10, WL/1, WL/2; 5 passengers refused (400 VALIDATION_FAILED in the API) | ✅ PASS | `ReservationDeskTest.multiPassenger`<br>`ReservationDeskTest.mixedStatus`<br>`ReservationDeskTest.passengerLimits`<br>`BookingApiTest.bookingValidation` |
| R3 | Cancel promotes the oldest waiting passenger into the same seat | Full train with WL/1 Anu, WL/2 Babu; cancel CNF/S4 | Anu CNF/S4 (promoted into the same seat), Babu WL/1, in-app notification for Anu's account | Anu CNF/S4, Babu WL/1, unread PROMOTION notification for the owner | ✅ PASS | `TrainServiceReservationTest.cancelPromotesOldest`<br>`ReservationDeskTest.promotionNotifies`<br>`BookingApiTest.cancelPromotes`<br>`NotificationServiceTest.promotionNotification` |
| R4 | Partial cancellation | Cancel passenger 2 of a 3-passenger PNR, then the rest | Only passenger 2 is CAN; the others keep their seats; the PNR is not fully cancelled | CNF, CAN, CNF; 2 active tickets; later full cancel cancels the other 2 | ✅ PASS | `ReservationDeskTest.partialCancel` |
| R5 | Seat bounds and invalid actions | Double cancel, unknown PNR, someone else's PNR, unknown train, 11th seat | Each refused with a clear error; seat count stays 0-10 | 409 INVALID_CANCELLATION, 404 PNR_NOT_FOUND, 403 FORBIDDEN, 404 TRAIN_NOT_FOUND; count bounded | ✅ PASS | `TrainServiceReservationTest.doubleCancel`<br>`TrainServiceReservationTest.countBounds`<br>`ReservationDeskTest.ownership`<br>`ReservationDeskTest.unknownPnr`<br>`BookingApiTest.someoneElsesPnr` |
| R6 | Booking closes on ARRIVED / DEPARTED | Book after the arrival (or departure) is confirmed | Refused with SEAT_UNAVAILABLE (409) | 409 SEAT_UNAVAILABLE "Booking is closed for train …" | ✅ PASS | `TrainServiceReservationTest.bookingClosed`<br>`ReservationDeskTest.departedTrain`<br>`BookingApiTest.bookingClosedAfterArrival` |
| P1 | Free, long-enough platform with no overlap | 420 m train 07:00-07:15 with P1 600, P2 450, P3 300 free | P2 for [07:00, 07:25) (10-min buffer); overlapping interval on the same platform refused | P2 [07:00, 07:25); overlap raises PlatformConflictException | ✅ PASS | `PlatformSchedulerTest.assignsBestFit`<br>`PlatformTest.refusesOverlap`<br>`PlatformTest.refusesTooLong`<br>`OccupationIntervalTest.backToBackDoesNotOverlap` |
| P2 | Overlong train rejected | 650 m train (Pandian Express 12637) | REJECTED "Overlong train – exceeds all platform lengths" | REJECTED: Overlong train – exceeds all platform lengths: 12637 is 650 m, the longest platform is 600 m | ✅ PASS | `PlatformSchedulerTest.overlongRejected`<br>`AllocationPolicyTest.none` |
| P3 | Busy platforms put the train in the queue | Pearl City 12693 requested while P1 and P2 are busy | QUEUED with a wait reason and next compatible release | QUEUED at position 2, "next compatible release: P2 at 07:25" | ✅ PASS | `PlatformSchedulerTest.overlappingArrivals`<br>`OperationsApiTest.arrivalRequests` |
| P4 | Queue order rank → arrival → trainNo | Scheduled 10001 at 07:00, connection 10002 at 07:30, emergency 10003 at 08:00, scheduled 10004 at 06:50 | Emergency, connection, then scheduled by arrival time | poll() order [10003, 10002, 10004, 10001] | ✅ PASS | `PlatformRequestTest.queueOrder`<br>`PlatformSchedulerTest.emergencyFirst`<br>`PlatformSchedulerTest.connectionRank` |
| P5 | Never displace an occupying train | Emergency request while the only long platform holds an ARRIVED train | Emergency train queued; arrived train stays | 06055 QUEUED at position 1; 50001 stays on P1 | ✅ PASS | `PlatformSchedulerTest.neverDisplace`<br>`PlatformSchedulerTest.delayedTrainLosesPlatform` |
| P6 | BestFit default, FirstFit switchable | 280 m and 420 m trains with P1 600, P2 450, P3 300 free; then FirstFit | BestFit: 280 m → P3, 420 m → P2 (shortest that fits); FirstFit: 280 m → P1 (lowest id) | BestFit P3 and P2, FirstFit P1; tie on length → lowest id | ✅ PASS | `AllocationPolicyTest.bestFitShortest`<br>`AllocationPolicyTest.bestFitTie`<br>`AllocationPolicyTest.firstFit`<br>`PlatformSchedulerTest.switchPolicy` |
| P7 | Delay re-plans the platforms | Delay an assigned train by 20 min; delay a waiting train | Occupation extended; later overlapping train un-assigned, re-queued, re-allocated, alert raised; waiting train removed and reinserted | 20002 extended to 07:45; 20003 WAITING_FOR_PLATFORM; queued train reordered | ✅ PASS | `PlatformSchedulerTest.delayBumpsLaterTrain`<br>`PlatformSchedulerTest.delayReordersQueue`<br>`PlatformSchedulerTest.bumpedStaysQueued`<br>`OperationsApiTest.delayPreviewAndApply` |
| P8 | Departure releases the platform and serves the queue | 590 m train on P1 departs at 07:05; emergency 10002 and normal 10003 queued for P1 | P1 released at 07:15 (departure + 10 min) via the min-heap; emergency allocated first | Release 07:15; at 07:15 "Assigned 10002 to P1"; 10003 still queued | ✅ PASS | `PlatformSchedulerTest.departureAllocatesQueue`<br>`PlatformSchedulerTest.lateDeparture`<br>`OperationsApiTest.arrivalAndDeparture` |
| P9 | Maintenance excludes a platform | P3 into maintenance with 10001 arrived on it and 10002 planned on it later | Arrived train stays; planned train re-allocated; no new allocation to P3 | 10001 stays on P3, 10002 moved to P2, a new 280 m train gets P2 | ✅ PASS | `PlatformSchedulerTest.maintenance`<br>`PlatformSchedulerTest.allInMaintenance`<br>`AllocationPolicyTest.skipsMaintenance`<br>`OperationsApiTest.maintenanceAndLength` |
| T1 | Cancelled seat → oldest WL promoted | Fresh lab station, 12 bookings, cancel CNF/S4 | WL/1 becomes CNF/S4 and WL/2 moves up to WL/1 | Anu CNF/S4, Babu WL/1 | ✅ PASS | `TestLabApiTest.scenarioPasses`<br>`TestLabApiTest.runAllIsIsolated` |
| T2 | Overlapping arrivals | Three overlapping 400 m trains | 1st → P2 (best fit), 2nd overlaps P2 → P1, 3rd has no free compatible platform → queued | 10001 → P2, 10002 → P1, 10003 → QUEUED | ✅ PASS | `TestLabApiTest.scenarioPasses`<br>`TestLabApiTest.runAllIsIsolated` |
| T3 | Overlong train rejected | 650 m train | REJECTED: overlong train, exceeds all platform lengths | REJECTED: Overlong train – exceeds all platform lengths: 12637 is 650 m, the longest platform is 600 m | ✅ PASS | `TestLabApiTest.scenarioPasses`<br>`TestLabApiTest.runAllIsIsolated` |
| T4 | Delay extends occupation, later train re-queued | Delay 20002 by 20 min with 20003 planned after it | P2 occupation extended; the later train is un-assigned and re-queued with an alert | 20002 extended to 07:45; 20003 WAITING_FOR_PLATFORM | ✅ PASS | `TestLabApiTest.scenarioPasses`<br>`TestLabApiTest.runAllIsIsolated` |
| T5 | Emergency train jumps ahead of a normal train | Normal 30002 and emergency 06055 waiting for P1 | Queue order EMERGENCY first; the emergency train gets P1 although it arrived later | Queue [06055, 30002]; P1 → 06055 | ✅ PASS | `TestLabApiTest.scenarioPasses`<br>`TestLabApiTest.runAllIsIsolated` |
| T6 | Priority tie → deterministic order | Same rank and arrival time, trains 22001 and 11002 | Order by arrival time, then by train number: 11002 before 22001 | Queue [11002, 22001] both times | ✅ PASS | `TestLabApiTest.scenarioPasses`<br>`TestLabApiTest.runAllIsIsolated` |
| T7 | Train never displaced from an occupied platform | Emergency request while 50001 stands on P1 | The emergency train is queued; the arrived train stays on P1 | 06055 QUEUED at position 1; 50001 stays on P1 | ✅ PASS | `TestLabApiTest.scenarioPasses`<br>`TestLabApiTest.runAllIsIsolated` |
| F1 | Invalid PNR | Look up PNR 4999999999 | Refused with PNR_NOT_FOUND (HTTP 404) | ReservationNotFoundException: PNR 4999999999 was not found | ✅ PASS | `TestLabApiTest.scenarioPasses`<br>`TestLabApiTest.runAllIsIsolated` |
| F2 | Double cancellation | Cancel the same PNR twice | Second cancellation refused with InvalidCancellationException; seat counts unchanged | InvalidCancellationException: PNR 4000824633 is already fully cancelled | ✅ PASS | `TestLabApiTest.scenarioPasses`<br>`TestLabApiTest.runAllIsIsolated` |
| F3 | Unknown train | Book train 99999 | Refused with TrainNotFoundException (HTTP 404) | TrainNotFoundException: Train 99999 is not in the Payanam Junction timetable | ✅ PASS | `TestLabApiTest.scenarioPasses`<br>`TestLabApiTest.runAllIsIsolated` |
| F4 | Passenger calling a staff API | Real HTTP GET /api/v1/operations/dashboard with a passenger JWT | HTTP 403 Forbidden with the JSON error body | HTTP 403 FORBIDDEN | ✅ PASS | `TestLabApiTest.scenarioPasses`<br>`TestLabApiTest.runAllIsIsolated` |
| AUTH-1 | Sign-up validation | Weak password, age 11, 3-digit mobile, mismatched confirm, terms unticked | 400 VALIDATION_FAILED with one message per field | 400 with 8 fieldErrors (see api.md) | ✅ PASS | `AuthApiTest.signUpValidation`<br>`AuthApiTest.duplicate` |
| AUTH-2 | Staff registration | Staff sign-up without / with the access code PAYANAM-2026 | Refused without the code; PJ-EMP-XXXX and @payanam.in enforced | Refused, then created with the right code | ✅ PASS | `AuthApiTest.staffSignUp` |
| AUTH-3 | Wrong password | passenger@payanam.in / wrong password | 401 AUTH_FAILED with the attempts left | 401 "… 4 attempts left before the account is locked." | ✅ PASS | `AuthApiTest.wrongPassword`<br>`AuthServiceMockitoTest.unknownAccount` |
| AUTH-4 | Lockout | 5 wrong passwords, then the right one | 423 locked for 15 minutes, even with the right password | 423 ACCOUNT_LOCKED; audited | ✅ PASS | `AuthApiTest.lockout`<br>`AuthServiceMockitoTest.fifthFailureLocks` |
| AUTH-5 | Role access | Passenger → staff API; staff → passenger API; clerk → platforms | 403 FORBIDDEN in every case | 403 FORBIDDEN | ✅ PASS | `AuthApiTest.roleSeparation`<br>`AuthApiTest.designationPermissions`<br>`TestLabApiTest.labIsRestricted` |
| AUTH-6 | Session handling | Logout, remember me, simulated OTP reset, change password | Revoked token refused; 7-day token; OTP reset works, wrong OTP refused | As expected | ✅ PASS | `AuthApiTest.logout`<br>`AuthApiTest.rememberMe`<br>`AuthApiTest.forgotPassword`<br>`AuthApiTest.profileAndPassword` |

## Appendix: every JUnit test


### AllocationPolicyTest (6)

| Test | Display name | Time (s) | Status |
|---|---|---|---|
| `skipsMaintenance` | P9: platforms in maintenance are excluded | 0.001 | PASS |
| `firstFit` | P6: first fit chooses the lowest-id valid platform (polymorphism) | 0.001 | PASS |
| `none` | P2: no valid platform gives an empty result | 0.002 | PASS |
| `bestFitTie` | P6: best fit tie on length goes to the lowest platform id | 0.003 | PASS |
| `bestFitShortest` | P6: best fit chooses the SHORTEST platform that is long enough | 0.004 | PASS |
| `skipsBusy` | P1: busy platforms are skipped by both policies | 0.002 | PASS |

### AuthApiTest (13)

| Test | Display name | Time (s) | Status |
|---|---|---|---|
| `signUpValidation` | AUTH: sign-up validation – weak password, under 18, bad mobile, mismatch, terms | 1.008 | PASS |
| `logout` | AUTH: logout revokes the token immediately | 0.433 | PASS |
| `wrongPassword` | AUTH: a wrong password is refused with the attempts left | 0.311 | PASS |
| `staffSignUp` | AUTH: staff registration needs the access code, PJ-EMP-XXXX id and an official email | 0.214 | PASS |
| `profileAndPassword` | AUTH: profile edit and change password | 0.705 | PASS |
| `signUpPassenger` | AUTH: a valid passenger sign-up returns a JWT and the profile | 0.180 | PASS |
| `designationPermissions` | ROLE: designations limit staff – a clerk cannot manage platforms, a controller cannot read the audit log | 0.424 | PASS |
| `signInEmailOrMobile` | AUTH: sign in with email or with mobile | 0.331 | PASS |
| `roleSeparation` | F4 / ROLE: a passenger gets 403 on staff APIs and staff get 403 on passenger APIs | 0.253 | PASS |
| `lockout` | AUTH: 5 wrong passwords lock the account for 15 minutes (even the right password is refused) | 0.588 | PASS |
| `rememberMe` | AUTH: remember me gives a longer-lived token | 0.229 | PASS |
| `forgotPassword` | AUTH: simulated forgot password – OTP shown, wrong OTP refused, reset works | 0.569 | PASS |
| `duplicate` | AUTH: a duplicate email or mobile is refused | 0.075 | PASS |

### AuthServiceMockitoTest (3)

| Test | Display name | Time (s) | Status |
|---|---|---|---|
| `fifthFailureLocks` | AUTH (Mockito): the fifth wrong password locks the account and is audited | 1.263 | PASS |
| `signInByMobile` | AUTH (Mockito): sign-in by mobile uses findByMobile and issues a token | 0.013 | PASS |
| `unknownAccount` | AUTH (Mockito): an unknown account gives the same generic error | 0.005 | PASS |

### BookingApiTest (10)

| Test | Display name | Time (s) | Status |
|---|---|---|---|
| `bookingValidation` | R2 API: 5 passengers or an invalid passenger are rejected | 0.177 | PASS |
| `bookingClosedAfterArrival` | R6 API: booking after arrival is refused with 409 SEAT_UNAVAILABLE | 0.324 | PASS |
| `pnrLookup` | F1 API: an unknown PNR returns 404; the public lookup hides passenger names | 0.178 | PASS |
| `unknownTrain` | F3 API: booking an unknown train returns 404 TRAIN_NOT_FOUND | 0.124 | PASS |
| `book` | R1/R2 API: booking 2 passengers on a train with seats gives CNF seats; on a full train WL | 0.153 | PASS |
| `seed` | SEED: 6 trains, 32 PNRs, Vaigai and Pearl City full with waiting lists | 0.061 | PASS |
| `someoneElsesPnr` | R5 API: someone else's PNR cannot be opened or cancelled (403) | 0.224 | PASS |
| `doubleCancel` | R5 / F2 API: double cancellation returns 409 INVALID_CANCELLATION | 0.171 | PASS |
| `cancelPromotes` | T1 / R3 API: a cancellation promotes the oldest WL passenger and notifies them | 0.236 | PASS |
| `seatMap` | Seat map: no names, my own seats are marked | 0.126 | PASS |

### NotificationServiceTest (3)

| Test | Display name | Time (s) | Status |
|---|---|---|---|
| `capped` | Notifications keep the newest 50 per user | 0.002 | PASS |
| `trainUpdateNotification` | Notifications: a delay or platform change on a train becomes a TRAIN notification for the PNR owner | 0.004 | PASS |
| `promotionNotification` | R3: a promotion becomes an unread in-app notification 'WL/1 → CNF/S7' for the PNR owner | 0.010 | PASS |

### OccupationIntervalTest (3)

| Test | Display name | Time (s) | Status |
|---|---|---|---|
| `contains` | P1: contains() includes the start and excludes the end | 0.001 | PASS |
| `backToBackDoesNotOverlap` | P1: half-open intervals [a,b) and [b,c) do not overlap | 0.000 | PASS |
| `invalid` | P1: an interval must end after it starts | 0.001 | PASS |

### OperationsApiTest (10)

| Test | Display name | Time (s) | Status |
|---|---|---|---|
| `arrivalRequests` | P3/P4 API: Pearl City queues behind the emergency; Pandian is rejected as overlong | 0.170 | PASS |
| `maintenanceAndLength` | P9 API: maintenance on P1 re-allocates Guruvayur; platform length is validated | 0.166 | PASS |
| `openApi` | Swagger: the OpenAPI document lists the versioned API | 1.071 | PASS |
| `timetableManagement` | Timetable management: edit a SCHEDULED train; a 7th train is refused | 0.159 | PASS |
| `clerkReservations` | Reservations management: clerk sees the FIFO list and cancels at the counter | 0.152 | PASS |
| `board` | Board: seed allocations – Vaigai P2, Guruvayur P1, Tejas P3, Relief Special queued | 0.125 | PASS |
| `arrivalAndDeparture` | P8 API: arrival, departure preview and departure release the platform | 0.145 | PASS |
| `stationMasterTools` | Dashboard, events, settings, simulation and snapshots for the Station Master | 0.241 | PASS |
| `delayPreviewAndApply` | P7 API: the delay preview changes nothing; the real delay bumps the later train and notifies passengers | 0.228 | PASS |
| `auditLog` | Audit log: actions are recorded, filterable and exportable as CSV | 0.208 | PASS |

### PlatformRequestTest (4)

| Test | Display name | Time (s) | Status |
|---|---|---|---|
| `polymorphicRanks` | OOP: each PlatformRequest subclass overrides priorityRank() | 0.004 | PASS |
| `liveKey` | P7: a request's arrival time is read live, so a delay changes its key | 0.004 | PASS |
| `queueOrder` | P4: queue order is priority rank, then arrival time, then trainNo | 0.002 | PASS |
| `tieBreak` | T6: a full tie (same rank and arrival) is broken by trainNo, deterministically | 0.002 | PASS |

### PlatformSchedulerTest (26)

| Test | Display name | Time (s) | Status |
|---|---|---|---|
| `lateDeparture` | P8: a late departure is treated as a delay first | 0.001 | PASS |
| `delayReordersQueue` | P7: a delayed WAITING train is removed and reinserted with its new rank | 0.002 | PASS |
| `neverDisplace` | P5 / T7: a train standing on a platform is never displaced, even by an emergency | 0.002 | PASS |
| `overlappingArrivals` | T2: an overlapping second train gets another platform, a third is queued | 0.002 | PASS |
| `emergencyFirst` | P4 / T5: an emergency train jumps ahead of a normal train in the queue | 0.002 | PASS |
| `bumpedStaysQueued` | P7: a bumped train stays queued when no other platform is free | 0.001 | PASS |
| `heldAtSignal` | Clock: a waiting train past its arrival time is held at the signal (times move to now) | 0.001 | PASS |
| `arrivalValidation` | Arrival: requires an assigned platform and a clock at or after the arrival time | 0.002 | PASS |
| `waitingCannotArrive` | Arrival: a waiting train cannot arrive | 0.001 | PASS |
| `overlongRejected` | P2 / T3: a train longer than every platform is REJECTED | 0.001 | PASS |
| `delayScheduled` | P7: delaying a SCHEDULED train only changes its times | 0.001 | PASS |
| `departureAllocatesQueue` | P8: departure releases the platform at departure + buffer and allocates the queue | 0.003 | PASS |
| `copyIsIsolated` | Preview: an operation on a copy never changes the live station | 0.002 | PASS |
| `changeLength` | Platform management: length changes are validated and can unblock the queue | 0.002 | PASS |
| `timetable` | Timetable: edits keep the timetable sorted and at most 6 trains are allowed | 0.002 | PASS |
| `maintenance` | P9: maintenance re-queues planned trains, keeps an arrived train, and excludes the platform | 0.002 | PASS |
| `overstayAndRelease` | Clock: an arrived train past its departure keeps its platform (overstay) and releases are popped | 0.001 | PASS |
| `assignsBestFit` | P1 / P6: a train gets the best-fit free platform for [arrival, departure + 10 min) | 0.001 | PASS |
| `requestMany` | Seed helper: requesting platforms for a list of trains | 0.001 | PASS |
| `switchPolicy` | P6: switching to first fit changes the chosen platform | 0.001 | PASS |
| `delayedTrainLosesPlatform` | P7 / P5: a delayed train that would clash with an arrived train gives up its own platform | 0.001 | PASS |
| `requestValidation` | P3: a request is refused twice and unknown trains raise TrainNotFound (F3) | 0.002 | PASS |
| `connectionRank` | P4: connection trains rank between emergency and normal trains | 0.001 | PASS |
| `delayArrived` | P7: delaying an arrived train extends its occupation; departed trains cannot be delayed | 0.002 | PASS |
| `delayBumpsLaterTrain` | P7 / T4: a delay extends the occupation and a later overlapping train is re-queued | 0.002 | PASS |
| `allInMaintenance` | P9: when every suitable platform is in maintenance the train waits | 0.001 | PASS |

### PlatformTest (8)

| Test | Display name | Time (s) | Status |
|---|---|---|---|
| `replaceAndRemove` | Platform: replace/reoccupy/remove keep the timeline consistent | 0.010 | PASS |
| `refusesTooLong` | P1: occupyPlatform refuses a train longer than the platform | 0.006 | PASS |
| `refusesOverlap` | P1: occupyPlatform keeps intervals sorted and refuses an overlap | 0.002 | PASS |
| `changeLength` | Platform management: length cannot drop below a planned train | 0.002 | PASS |
| `idsAndCopy` | Platform: ids sort naturally (P2 before P10) and copy is independent | 0.002 | PASS |
| `refusesDuplicateTrain` | P1: the same train cannot hold two intervals on one platform | 0.002 | PASS |
| `nextFreeTime` | Platform: next free time skips back-to-back intervals | 0.002 | PASS |
| `maintenanceRefuses` | P9: a platform in maintenance accepts no new occupation | 0.002 | PASS |

### ReservationDeskTest (12)

| Test | Display name | Time (s) | Status |
|---|---|---|---|
| `departedTrain` | R6: booking a departed train is rejected | 0.003 | PASS |
| `unknownTrain` | R5 / F3: booking an unknown train is rejected | 0.002 | PASS |
| `doubleCancelPnr` | R5 / F2: cancelling a fully cancelled PNR again is rejected | 0.002 | PASS |
| `promotionNotifies` | R3: cancelling a PNR promotes the oldest waiting passenger and notifies its owner | 0.002 | PASS |
| `passengerLimits` | R2: 0 or 5 passengers are rejected | 0.001 | PASS |
| `ownership` | R5: someone else's PNR cannot be cancelled or opened, but staff may cancel it | 0.002 | PASS |
| `uniquePnrs` | PNR numbers are unique 10-digit numbers | 0.005 | PASS |
| `partialCancel` | R4: partial cancellation cancels only one passenger of a PNR | 0.002 | PASS |
| `unknownPnr` | R5 / F1: an unknown PNR or passenger number is rejected | 0.002 | PASS |
| `mixedStatus` | R2: a PNR spanning the last seats is split CNF and WL per passenger | 0.002 | PASS |
| `multiPassenger` | R2: one PNR holds up to 4 passengers, each with their own seat | 0.004 | PASS |
| `ownerIndexAndHistory` | DS: My Bookings uses the owner index and history stays sorted by booking time | 0.002 | PASS |

### SortedListsTest (3)

| Test | Display name | Time (s) | Status |
|---|---|---|---|
| `insertKeepsOrder` | DS: binary-search insert keeps an ArrayList sorted | 0.001 | PASS |
| `upperBound` | DS: upper bound returns the first index strictly greater than the key | 0.002 | PASS |
| `stable` | DS: equal keys keep insertion order (stable insert) | 0.001 | PASS |

### TestLabApiTest (13)

| Test | Display name | Time (s) | Status |
|---|---|---|---|
| `runAllIsIsolated` | Lab: run-all reports 11 PASS and never changes the live station | 0.542 | PASS |
| `labIsRestricted` | Lab: only staff with RUN_TESTS may use it (Booking Clerk and passengers get 403) | 0.288 | PASS |
| `scenarioPasses` | T1–T7, F1–F4: every lab scenario passes (String)[1] | 0.148 | PASS |
| `scenarioPasses` | T1–T7, F1–F4: every lab scenario passes (String)[2] | 0.133 | PASS |
| `scenarioPasses` | T1–T7, F1–F4: every lab scenario passes (String)[3] | 0.133 | PASS |
| `scenarioPasses` | T1–T7, F1–F4: every lab scenario passes (String)[4] | 0.137 | PASS |
| `scenarioPasses` | T1–T7, F1–F4: every lab scenario passes (String)[5] | 0.141 | PASS |
| `scenarioPasses` | T1–T7, F1–F4: every lab scenario passes (String)[6] | 0.138 | PASS |
| `scenarioPasses` | T1–T7, F1–F4: every lab scenario passes (String)[7] | 0.142 | PASS |
| `scenarioPasses` | T1–T7, F1–F4: every lab scenario passes (String)[8] | 0.143 | PASS |
| `scenarioPasses` | T1–T7, F1–F4: every lab scenario passes (String)[9] | 0.134 | PASS |
| `scenarioPasses` | T1–T7, F1–F4: every lab scenario passes (String)[10] | 0.135 | PASS |
| `scenarioPasses` | T1–T7, F1–F4: every lab scenario passes (String)[11] | 0.173 | PASS |

### TrainServiceReservationTest (15)

| Test | Display name | Time (s) | Status |
|---|---|---|---|
| `bookingClosed` | R6: booking closes when the train has ARRIVED or DEPARTED | 0.003 | PASS |
| `transitions` | Train: status transitions are validated | 0.003 | PASS |
| `delays` | Train: delays move both times before arrival, only departure after arrival | 0.002 | PASS |
| `cannotCancelAfterDeparture` | R5: tickets of a departed train cannot be cancelled | 0.002 | PASS |
| `firstTenConfirmed` | R1: the first 10 tickets are CNF with seats 1-10 | 0.002 | PASS |
| `cancelWaitlisted` | R3: cancelling a waitlisted ticket only removes it from the queue | 0.002 | PASS |
| `validation` | Train: constructor validates its fields | 0.002 | PASS |
| `cancelPromotesOldest` | R3 / T1: cancelling CNF/S4 promotes the OLDEST waiting passenger into seat 4 | 0.005 | PASS |
| `cancelWithoutWaiting` | R3: cancelling with an empty waiting list just frees the seat | 0.001 | PASS |
| `wrongTrain` | Train: a ticket for another train is refused | 0.002 | PASS |
| `overflowWaitlisted` | R1: the 11th and 12th tickets get WL/1 and WL/2 in FIFO order | 0.002 | PASS |
| `reschedule` | Timetable: a SCHEDULED train can be edited, others cannot | 0.001 | PASS |
| `doubleCancel` | R5 / F2: double cancellation is rejected | 0.002 | PASS |
| `passengerValidation` | Passenger: name, age and gender are validated | 0.002 | PASS |
| `countBounds` | R5: seat count never exceeds 10 or drops below 0 | 0.001 | PASS |
