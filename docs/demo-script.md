# Demo script (10 minutes)

A rehearsed flow for the project review. It starts from the seed data (station clock 06:30), so every number below is
what you will see. The flow was dry-run against the API with the seed data.

**Before you start**

1. `cd backend && ./mvnw spring-boot:run`, wait for "Started PayanamRailApplication", open <http://localhost:8080>.
2. Use two browser windows side by side: a normal window for the **passenger** and a private window for **staff**
   (each window keeps its own sign-in).
3. If the station was used before, sign in as the Station Master and press **Simulation → Load seed data**.
4. Optional: open <http://localhost:8080/swagger-ui.html> in a third tab.

| Account | Email | Password |
|---|---|---|
| Passenger (Arun Kumar) | `passenger@payanam.in` | `Passenger@123` |
| Station Master | `stationmaster@payanam.in` | `Station@123` |
| Platform Controller | `controller@payanam.in` | `Station@123` |
| Booking Clerk | `clerk@payanam.in` | `Station@123` |

---

## 0:00 – 1:00 · The problem and the landing page

* Landing page: "Payanam Junction runs on paper today. Cancelled seats are not offered to waiting passengers,
  platforms get double-booked, delays break the plan and urgent trains wait behind ordinary ones."
* Point at the live station numbers (6 trains, 3 platforms, seat occupancy) and the station clock 06:30.
* Say the core rule in one line: **the reservation and platform logic runs on hand-written Java collections**
  (HashMap, ArrayDeque, PriorityQueue, a min-heap, sorted lists); the H2 database only stores accounts, sessions,
  the audit log and snapshots.

## 1:00 – 4:00 · Passenger journey

1. **Sign in** as `passenger@payanam.in`. Show the password eye toggle and *remember me*.
2. **Dashboard:** KPI cards and the next journey. Arun already holds **WL/1 on Vaigai Express 12635**; remember this
   for later.
3. **Search trains:** filter by availability. Vaigai and Pearl City show 0 of 10 seats left.
4. **Book Tejas Express 22671** with 2 passengers (Arun Kumar 31 M, Meena Kumar 29 F):
   step 1 train, step 2 passengers, step 3 review, Confirm → success page with the PNR and **CNF/S6, CNF/S7** (R1, R2).
5. **Book Pearl City Express 12693** with 1 passenger → **WL/3**: the train is full, so the ticket joins the FIFO
   waiting list (R1).
6. **My Bookings → the Tejas PNR → Cancel passenger 2** → S7 is freed (R4, partial cancellation). The fare now counts
   only the active ticket.
7. **E-ticket:** QR code, "Download PDF" opens the browser's print-to-PDF.
8. **Seat Map** of Vaigai: 10 seats, no names (privacy), your own seats highlighted, waiting-list count.
9. Show the **public PNR page** (`/pnr`, signed out works too): timeline Booked → CNF/S6, names hidden.

## 4:00 – 8:00 · Station journey

Sign in as `stationmaster@payanam.in` in the second window.

1. **Control Centre (30 s):** KPIs (trains today, on platform, waiting, rejected, delayed, bookings, occupancy),
   the three charts and the alerts panel.
2. **Platform Board (45 s):** P2 Vaigai 07:00–07:25, P1 Guruvayur 07:05–07:30, P3 Tejas 07:30–07:50; the dashed
   queue row shows the **Medical Relief Special 06055 (EMERGENCY)**, waiting because only P1 (600 m) is long enough for
   its 560 m and P1 is busy. Explain the red "now" line and that each block includes the 10-minute buffer.
3. **Arrival Requests (45 s):**
   * Pearl City 12693, Scheduled → **Queued at position 2**: "Platforms long enough (P1, P2) are busy during
     07:20–07:45; next compatible release: P2 at 07:25" (P3). It is behind the emergency train (P4).
   * Pandian Express 12637 → **Rejected**: "Overlong train – exceeds all platform lengths: 12637 is 650 m, the longest
     platform is 600 m" (P2).
4. **Platform Queue (15 s):** rank 1 06055 EMERGENCY, rank 2 12693 NORMAL: the PriorityQueue in `poll()` order.
5. **Train Operations → Delay on Guruvayur 16127, 15 min, "Late running from Tambaram" (45 s):**
   press *Preview impact*: arrival 07:05 → 07:20, P1 occupation extended to 07:45. Nothing has changed yet: the
   preview ran on a copy of the station. Confirm. The emergency train's next compatible release moves to 07:45 (P7).
   Arun holds two tickets on Guruvayur (CNF/S1, CNF/S2), so the bell in the passenger window shows
   "Delay: 16127 Guruvayur Express".
6. **Reservations → Vaigai 12635 (45 s):** seats 1–10 and the FIFO list WL/1 Arun Kumar, WL/2 Rahul Iyer, ...
   Cancel **S1 (Priya Raman)** at the counter → "Promoted …: **WL/1 → CNF/S1**" (R3). Arun is now on S1, Rahul moves
   up to WL/1. In the passenger window the bell shows "Ticket confirmed: WL/1 → CNF/S1".
7. **Simulation → +30 min (30 s):** the clock moves to 07:00.
8. **Train Operations → Confirm arrival of Vaigai 12635**: booking closes (R6) and the train can never be displaced
   (P5). Then **Confirm departure** → "P2 is released at 07:10. Allocated from the queue: 12693 → P2" (P8: the release
   min-heap and the queue at work).
9. **Platform Board** again: Pearl City is now on P2 from 07:20; the emergency train still waits for P1.

## 8:00 – 9:00 · Failure cases

| Show | Where | Expected |
|---|---|---|
| Book on Vaigai after its arrival | Passenger → Book Ticket | Refused: "Booking is closed for train 12635 (ARRIVED)" (R6) |
| Cancel the same passenger twice | Passenger → My Bookings | "Ticket … is already cancelled", 409 (R5, F2) |
| Unknown PNR `4999999999` | `/pnr` | "PNR 4999999999 was not found", 404 (F1) |
| Passenger opens `/station` | Passenger window, type the URL | 403 page (F4); the API also returns 403 |
| Booking Clerk menu | Sign in as `clerk@payanam.in` | Only Control Centre, Reservations and Settings; `/station/board` gives 403 |
| Wrong password 5 times | Sign-in page (use a seeded passenger such as `karthik.s@example.in`) | "n attempts left", then locked for 15 minutes (423) |

## 9:00 – 10:00 · Test Lab and wrap-up

1. **Station → Test Lab → Run all:** 11 of 11 PASS. Open **T4** (delay extends the occupation, the later train is
   re-queued) and **T5** (emergency jumps the queue) to show the steps and *expected vs actual*. Each scenario runs on
   its own fresh station, so the live demo data is untouched.
2. **Audit Log:** every action of the demo with user, role and details; filter by action, export CSV.
3. Close with the numbers: 129 JUnit tests passing, JaCoCo line coverage 95–100% on domain and service, Swagger UI
   for the API, and the docs folder (UML, sequence diagrams, data structures, test results).

## If something goes wrong

| Problem | Fix |
|---|---|
| A step is not possible because the clock moved on | Simulation → **Load seed data** (or **Reset clock**) and repeat the step |
| The passenger window was signed out | Tokens last 60 minutes; sign in again (or tick *remember me*) |
| Port 8080 is busy | `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=9090` |
| A demo account is locked after the lockout demo | Wait 15 minutes, or stop the app and delete `backend/data` (accounts are re-seeded) |
