# Data structures

All reservation and platform logic runs on Java collections held in memory. The allocation loop, the overlap check,
the waiting-list promotion and the binary-search insert are written by hand. No scheduling library is used, and the
database never stores trains, PNRs, platforms or queues.

Every structure below has a `WHY` comment at its declaration in the code.

## Summary

| # | Structure | Where | What it holds | Key operations and cost |
|---|---|---|---|---|
| 1 | `HashMap<String, TrainService>` | `TrainRegistry.trains` | trainNo → train | `get` O(1) |
| 2 | `HashMap<String, Booking>` | `ReservationDesk.bookingsByPnr` | PNR → booking | `get` O(1) |
| 3 | `HashMap<String, Passenger>` | `ReservationDesk.passengersById` | "PNR/n" → passenger | `get` O(1) |
| 4 | `HashMap<Long, List<String>>` | `ReservationDesk.pnrsByOwner` | account id → that account's PNRs | `get` O(1), then O(k) for k PNRs |
| 5 | `HashMap<String, Platform>` | `PlatformScheduler.platforms` | "P1" → platform | `get` O(1) |
| 6 | `HashMap<String, PlatformRequest>` | `PlatformScheduler.activeRequests` | trainNo → its request object | `get` O(1) |
| 7 | `Reservation[10]` (fixed array) | `TrainService.seats` | seat i+1 → ticket or null | seat access O(1), lowest free seat O(10) |
| 8 | `ArrayDeque<Reservation>` | `TrainService.waitingList` | FIFO waiting list of one train | `addLast` O(1), `pollFirst` O(1), renumber O(w) |
| 9 | `PriorityQueue<PlatformRequest>` | `PlatformScheduler.waitingQueue` | trains waiting for a platform | `add` O(log n), `poll` O(log n), `peek` O(1), `remove(obj)` O(n) |
| 10 | `PriorityQueue<PlatformRelease>` (min-heap) | `PlatformScheduler.releaseHeap` | (platform, train, release time) | `peek` O(1), `poll` O(log n), `add` O(log n) |
| 11 | Sorted `ArrayList<OccupationInterval>` | `Platform.occupations` | intervals of one platform, by start | overlap test O(log n), insert O(log n) + O(n) shift |
| 12 | Sorted `ArrayList<TrainService>` | `TrainRegistry.timetable` | trains by expected arrival, then trainNo | read in order O(n), insert O(log n) + O(n) |
| 13 | Sorted `ArrayList<Booking>` | `ReservationDesk.history` | all PNRs by booking time | insert O(log n) + O(n) |
| 14 | Sorted `ArrayList<StationEvent>` | `EventLog.events` | station events by simulation time | insert O(log n) + O(n) |
| 15 | `ArrayList<StatusChange>` | `Reservation.timeline` | Booked → WL/2 → WL/1 → CNF/S7 | append O(1) amortised |

n = number of items in that structure. At this station's scale (6 trains, 10 seats, 3 platforms) every operation is
instant; the Big-O shows how the design would scale and why each structure fits its job.

---

## 1. HashMap: O(1) lookup by key

**Used for:** trains by number, bookings by PNR, passengers by id, an account's PNRs, platforms by id, and the active
platform request of each train.

**Why:** almost every request starts with "find X by its id": open PNR 4027212889, book on train 12635, confirm the
arrival of 16127, put P1 into maintenance. A hash map answers that in O(1) on average.

| Alternative | Cost | Why not |
|---|---|---|
| Scan an `ArrayList` | O(n) per lookup | Every booking, cancellation and status page would scan |
| `TreeMap` | O(log n) | Ordering by key is not needed for lookups; the ordered views are kept separately (sorted lists) |

`pnrsByOwner` is a secondary index: "My Bookings" needs only one user's PNRs, so the desk keeps account id → PNR list
instead of filtering every booking (O(total bookings)) on each page load.

## 2. Fixed array `Reservation[10]`: the seats

**Used for:** the 10 seats of a train (`TrainService.seats`; `seats[i]` is the ticket in seat i+1, or `null`).

**Why:** capacity is fixed at 10 by the case study. An array of exactly 10 cells makes the bound physical: there is no
eleventh cell. Promotion needs "put the oldest waiting passenger into **that exact seat**", which is `seats[k] =
promoted`, O(1). The `confirmedCount` field is changed only through `incrementConfirmed()` and `decrementConfirmed()`,
which throw if the count would leave 0–10 (rule R5).

## 3. ArrayDeque: the FIFO waiting list

**Used for:** `TrainService.waitingList`, one per train (composition: the train owns it).

```java
waitingList.addLast(ticket);                    // R1: join the back of the queue, O(1)
Reservation promoted = waitingList.pollFirst(); // R3: oldest waiting passenger, O(1)
```

**Why:** the rule is "first come, first served": the oldest waiting passenger gets the next freed seat. A queue with
O(1) insert at the back and O(1) removal at the front is exactly that.

| Alternative | Cost | Why not |
|---|---|---|
| `ArrayList` | `remove(0)` is O(n) | Shifts every element on each promotion |
| `LinkedList` | O(1) at both ends | One node object per passenger, worse memory locality; `ArrayDeque` is the JDK's recommended queue |
| `PriorityQueue` by booking time | O(log n) | FIFO order is already the insertion order; a heap adds cost for nothing |

Cancelling a **waitlisted** ticket uses `waitingList.remove(ticket)`, an O(w) scan. That is acceptable because it is
rare and w is small; everyone behind then moves up one place (`renumberWaitingList`, O(w)).

## 4. PriorityQueue: trains waiting for a platform

**Used for:** `PlatformScheduler.waitingQueue`, ordered by `PlatformRequest.QUEUE_ORDER`:

```java
Comparator.comparingInt(PlatformRequest::priorityRank)   // EMERGENCY 0 < CONNECTION 1 < NORMAL 2
          .thenComparing(PlatformRequest::getArrivalTime)
          .thenComparing(PlatformRequest::getTrainNo);    // deterministic tie-break (T6)
```

`priorityRank()` is abstract in `PlatformRequest` and overridden by `EmergencyArrival`, `ConnectionArrival` and
`ScheduledArrival` (polymorphism).

**Why:** the scheduler repeatedly needs "the most urgent waiting train". A binary heap gives `peek()` in O(1) and
`add()` / `poll()` in O(log n).

| Alternative | Cost | Why not |
|---|---|---|
| Sorted `ArrayList` | insert O(n) | Shifts on every request; the heap only needs the minimum |
| `TreeSet` | O(log n) | Would also work, but a `PriorityQueue` states the intent ("queue served by priority") and tolerates equal keys |
| Re-sorting a list each time | O(n log n) | Wasteful |

**Remove and reinsert on a key change (rule P7).** A heap only keeps its order when elements are inserted or removed.
If a delay changed a queued train's arrival time in place, the heap would be silently wrong. So `applyDelay` does:

```java
waitingQueue.remove(request);          // remove BEFORE the key changes, O(n)
registry.delay(train, minutes, reason);
waitingQueue.add(request);             // reinsert at its new position, O(log n)
```

**Serving the queue (`processQueue`).** Requests are polled in priority order and each gets one allocation attempt;
those that still do not fit are collected and added back. A higher-ranked train always chooses first, and a long
train that cannot fit anywhere does not block a shorter, lower-ranked train that fits elsewhere.
Cost: O(n log n) heap work plus n policy calls.

**Showing the queue.** Iterating a `PriorityQueue` is **not** in priority order (it walks the heap array). The Platform
Queue page therefore uses `queueInOrder()`, which polls a copy: O(n log n), done only for display.

## 5. Min-heap of release times

**Used for:** `PlatformScheduler.releaseHeap`, a `PriorityQueue<PlatformRelease>` where
`PlatformRelease(platformId, trainNo, releaseTime)` compares by release time, then platform id.

Every assigned interval pushes its end (`departure + buffer`). A departure moves the release to the real
departure + buffer (rule P8): the old entry is removed and the new one added.

**Why:**

* **Next release in O(1).** `peek()` answers "which platform is released next, and when?" for the Control Centre.
* **Clock advance.** `advanceClock()` pops every release whose time has come (`poll()`, O(log n) each) and then serves
  the waiting queue. Releases are processed in time order without scanning the platforms.
* **Length-compatibility check.** `earliestCompatibleRelease(train)` walks a copy of the heap in time order and
  returns the first release of a platform that is **long enough** for the train and not in maintenance. That is the
  "next compatible release: P2 at 07:25" text in the queue and in the QUEUED message. Cost O(r log r) on a copy.

| Alternative | Cost | Why not |
|---|---|---|
| Scan all platforms' intervals | O(total intervals) each time | The heap keeps the answer at the top |
| Sorted list of releases | insert O(n) | Only the minimum is needed most of the time |

## 6. Sorted ArrayList with binary-search insert

**Helper:** `SortedLists.upperBound()` (hand-written binary search) and `SortedLists.insertSorted()`:

```java
int low = 0, high = list.size();          // search window [low, high)
while (low < high) {
    int mid = (low + high) >>> 1;          // unsigned shift avoids int overflow
    if (order.compare(list.get(mid), key) <= 0) low = mid + 1;   // mid <= key: go right
    else high = mid;                                             // mid > key: mid may be the answer
}
list.add(low, item);                       // first index strictly greater: stable insert
```

Inserting at the **upper bound** keeps items with equal keys in insertion order (a stable insert), which is tested in
`SortedListsTest.stable`.

### 6a. Platform occupation intervals (rule P1)

`Platform.occupations` is sorted by start time. Because intervals on one platform never overlap, a new interval
`[s, e)` can only overlap its two neighbours at the insert position:

```java
int i = upperBound(occupations, [s, e), BY_START);
clash = (i > 0 && occupations[i-1].overlaps(s, e)) || (i < size && occupations[i].overlaps(s, e));
```

So the overlap test is O(log n) instead of comparing with every interval. The half-open check is
`start < otherEnd && otherStart < end`, so back-to-back intervals such as `[07:00, 07:25)` and `[07:25, 07:40)` do
**not** overlap. The same order is exactly what the Gantt timeline draws, so the UI never sorts.

### 6b. Timetable

`TrainRegistry.timetable` is sorted by expected arrival, then trainNo. Every screen reads the timetable in order, so
it is kept sorted on write. When a delay or an edit changes a train's arrival, the train is **removed and reinserted**
at its new position (the same rule as the queue).

### 6c. Reservation history and event log

`ReservationDesk.history` (by booking time) and `EventLog.events` (by simulation time) receive items **out of order**:
seed bookings carry past timestamps, and a platform release is stamped with its future release time. A binary-search
insert keeps them ordered for "bookings over time", the history screens and the event feed.

| Alternative | Cost | Why not |
|---|---|---|
| Append, then `Collections.sort()` on each read | O(n log n) per read | Reads are far more frequent than writes |
| `TreeMap` / `TreeSet` | O(log n) insert | Cannot hold equal keys without extra tie-breakers; no index access for the binary-search neighbour check |
| `LinkedList` | O(n) to find the position | No random access, so no binary search |

## 7. How the structures work together

| Rule | Structures involved |
|---|---|
| R1 book | HashMap (train) → seats array (lowest free seat) or ArrayDeque (`addLast`) → HashMaps + sorted history (PNR) |
| R3 cancel and promote | HashMap (PNR) → seats array (`null`) → ArrayDeque (`pollFirst`) → seats array (same seat) |
| P1 allocate | HashMap (platforms) → policy → sorted intervals (binary-search overlap check) → min-heap (`add` release) |
| P3/P4 queue | PriorityQueue (`add`, ordered by rank → arrival → trainNo) + min-heap walk for the wait reason |
| P7 delay | PriorityQueue `remove` + `add`; sorted timetable remove + reinsert; sorted intervals `findOverlapping` |
| P8 departure | min-heap: remove old release, add departure + buffer, later `poll` when due; PriorityQueue `poll` to allocate |

## 8. Deep copy for previews

`RailwayStation.copy()` rebuilds every structure above (trains with their seats and waiting lists, bookings, platform
intervals, both heaps and the request map) using an identity map so that a ticket shared by a booking and a train
stays shared in the copy. A delay preview runs the real algorithm on the copy, so the preview is exactly what will
happen, and the live station is untouched (`PlatformSchedulerTest.copyIsIsolated`).
