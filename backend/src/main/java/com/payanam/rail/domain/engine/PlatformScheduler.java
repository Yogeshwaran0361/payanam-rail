package com.payanam.rail.domain.engine;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

import com.payanam.rail.domain.model.ArrivalType;
import com.payanam.rail.domain.model.OccupationInterval;
import com.payanam.rail.domain.model.Platform;
import com.payanam.rail.domain.model.PlatformRelease;
import com.payanam.rail.domain.model.PlatformRequest;
import com.payanam.rail.domain.model.PlatformStatus;
import com.payanam.rail.domain.model.StationEvent;
import com.payanam.rail.domain.model.TrainService;
import com.payanam.rail.domain.model.TrainStatus;
import com.payanam.rail.domain.policy.PlatformAllocationPolicy;
import com.payanam.rail.exception.InvalidOperationException;
import com.payanam.rail.exception.PlatformConflictException;
public class PlatformScheduler {
    public static final int MAX_DELAY_MINUTES = 720;
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final TrainRegistry registry;
    private final SimulationClock clock;
    private final EventLog log;
    private PlatformAllocationPolicy policy;
    private int clearanceBufferMinutes;


    private final Map<String, Platform> platforms = new HashMap<>();


    private final PriorityQueue<PlatformRequest> waitingQueue = new PriorityQueue<>(PlatformRequest.QUEUE_ORDER);


    private final PriorityQueue<PlatformRelease> releaseHeap = new PriorityQueue<>();

    
    private final Map<String, PlatformRequest> activeRequests = new HashMap<>();

    public PlatformScheduler(TrainRegistry registry, SimulationClock clock, EventLog log,
                             PlatformAllocationPolicy policy, int clearanceBufferMinutes) {
        this.registry = registry;
        this.clock = clock;
        this.log = log;
        this.policy = policy;
        setClearanceBufferMinutes(clearanceBufferMinutes);
    }

    public void addPlatform(Platform platform) {
        if (platforms.containsKey(platform.getId())) {
            throw new InvalidOperationException("Platform " + platform.getId() + " already exists");
        }
        platforms.put(platform.getId(), platform);
    }


    LocalDateTime occupationStart(TrainService train) {
        return train.getExpectedArrival();
    }
    LocalDateTime occupationEnd(TrainService train) {
        return train.getExpectedDeparture().plusMinutes(clearanceBufferMinutes);
    }

    
    public AllocationResult requestPlatform(String trainNo, ArrivalType type) {
        TrainService train = registry.get(trainNo);
        if (train.getStatus() != TrainStatus.SCHEDULED) {
            throw new InvalidOperationException("Train " + train.getTrainNo() + " has already requested a platform"
                    + " (status " + train.getStatus() + ")");
        }
        ArrivalType requestType = type != null ? type : ArrivalType.fromPriority(train.getDeclaredPriority());

        int longest = 0;
        for (Platform p : platforms.values()) {
            longest = Math.max(longest, p.getLengthMeters());
        }
        if (longest < train.getLengthMeters()) {                 // P2
            train.markRejected();
            String message = "Overlong train – exceeds all platform lengths: " + train.getTrainNo() + " is "
                    + train.getLengthMeters() + " m, the longest platform is " + longest + " m";
            log.record(clock.now(), StationEvent.Category.PLATFORM, true, train.getTrainNo(), "REJECTED – " + message);
            return new AllocationResult(train.getTrainNo(), AllocationResult.Outcome.REJECTED, null, null, null, 0,
                    message);
        }

        PlatformRequest request = PlatformRequest.create(requestType, train, clock.now());
        activeRequests.put(train.getTrainNo(), request);
        logPlatform(train, false, requestType + " arrival request for " + train.getTrainNo() + " (rank "
                + request.priorityRank() + ", interval " + hm(occupationStart(train)) + "–" + hm(occupationEnd(train)) + ")");

        if (tryAllocate(request)) {
            return assignedResult(train);
        }
        train.markWaiting();                                     // P3
        waitingQueue.add(request);
        AllocationResult result = queuedResult(request);
        logPlatform(train, true, result.message());
        return result;
    }

    /** Asks the policy for a platform and, if one is found, occupies it (Platform re-validates P1). */
    private boolean tryAllocate(PlatformRequest request) {
        TrainService train = request.getTrain();
        LocalDateTime start = occupationStart(train);
        LocalDateTime end = occupationEnd(train);
        Optional<Platform> choice = policy.selectPlatform(train, start, end, platforms.values());
        if (choice.isEmpty()) {
            return false;
        }
        Platform platform = choice.get();
        platform.occupyPlatform(train.getTrainNo(), start, end, train.getLengthMeters());
        train.assignPlatform(platform.getId());
        releaseHeap.add(new PlatformRelease(platform.getId(), train.getTrainNo(), end));
        logPlatform(train, false, "Train " + train.getTrainNo() + " assigned to " + platform.getId() + " ("
                + platform.getLengthMeters() + " m, " + policy.type() + ") for " + hm(start) + "–" + hm(end));
        return true;
    }

    /**
     * Serves the waiting queue in priority order. Each waiting train gets one try, most urgent first;
     * trains that still do not fit go back into the queue. A higher-ranked train always chooses first,
     * and a train that cannot fit does not block a lower-ranked train that fits elsewhere.
     */
    List<AllocationResult> processQueue() {
        List<AllocationResult> allocated = new ArrayList<>();
        List<PlatformRequest> stillWaiting = new ArrayList<>();
        while (!waitingQueue.isEmpty()) {
            PlatformRequest next = waitingQueue.poll();          // most urgent first, O(log n)
            if (tryAllocate(next)) {
                allocated.add(assignedResult(next.getTrain()));
            } else {
                stillWaiting.add(next);
            }
        }
        waitingQueue.addAll(stillWaiting);
        return allocated;
    }

    // ============================================================ arrival / departure

    /** Marks an assigned train as physically on its platform. From now on it is never displaced (P5). */
    public TrainService confirmArrival(String trainNo) {
        TrainService train = registry.get(trainNo);
        switch (train.getStatus()) {
            case PLATFORM_ASSIGNED -> { /* ok */ }
            case SCHEDULED -> throw new InvalidOperationException("Train " + train.getTrainNo()
                    + " has no platform yet; send an arrival request first");
            case WAITING_FOR_PLATFORM -> throw new InvalidOperationException("Train " + train.getTrainNo()
                    + " is still waiting for a platform");
            default -> throw new InvalidOperationException("Train " + train.getTrainNo() + " cannot arrive while "
                    + train.getStatus());
        }
        LocalDateTime now = clock.now();
        if (now.isBefore(train.getExpectedArrival())) {
            throw new InvalidOperationException("Train " + train.getTrainNo() + " is not due until "
                    + hm(train.getExpectedArrival()) + " (clock is " + hm(now) + "); advance the clock first");
        }
        Platform platform = platforms.get(train.getPlatformId());
        for (OccupationInterval interval : platform.getOccupations()) {
            TrainService other = registry.get(interval.trainNo());
            if (other != train && other.getStatus() == TrainStatus.ARRIVED) {
                throw new PlatformConflictException("Platform " + platform.getId() + " is still occupied by train "
                        + other.getTrainNo() + "; confirm its departure first");
            }
        }
        train.markArrived(now);
        log.record(now, StationEvent.Category.ARRIVAL, false, train.getTrainNo(), "Arrival confirmed: "
                + train.getTrainNo() + " on " + platform.getId() + " at " + hm(now) + "; booking closed");
        return train;
    }

    /**
     * Rule P8: the platform is released at departure + buffer, the release min-heap is updated and the
     * waiting trains are allocated automatically from the PriorityQueue.
     */
    public DepartureResult confirmDeparture(String trainNo) {
        TrainService train = registry.get(trainNo);
        if (train.getStatus() != TrainStatus.ARRIVED) {
            throw new InvalidOperationException("Train " + train.getTrainNo()
                    + " must have a confirmed arrival before departure (status " + train.getStatus() + ")");
        }
        LocalDateTime now = clock.now();
        if (now.isAfter(train.getExpectedDeparture())) {
            // Left later than planned: treat the extra time as a delay so later trains are re-planned (P7).
            applyDelay(train, minutesBetween(train.getExpectedDeparture(), now), "departed late", false);
        }
        Platform platform = platforms.get(train.getPlatformId());
        LocalDateTime release = now.plusMinutes(clearanceBufferMinutes);
        OccupationInterval old = platform.findOccupation(train.getTrainNo()).orElseThrow();
        platform.replaceOccupation(train.getTrainNo(), old.start(), release);   // trim to the real departure
        removeRelease(train.getTrainNo());
        releaseHeap.add(new PlatformRelease(platform.getId(), train.getTrainNo(), release));
        train.markDeparted(now);
        log.record(now, StationEvent.Category.DEPARTURE, false, train.getTrainNo(), "Departure confirmed: "
                + train.getTrainNo() + " left " + platform.getId() + " at " + hm(now) + "; platform free from "
                + hm(release) + " (" + clearanceBufferMinutes + " min buffer)");

        List<AllocationResult> allocated = processQueue();
        String message = train.getTrainNo() + " departed at " + hm(now) + ". " + platform.getId() + " is released at "
                + hm(release) + ".";
        if (!allocated.isEmpty()) {
            message += " Allocated from the queue: " + describe(allocated) + ".";
        } else if (!waitingQueue.isEmpty()) {
            PlatformRequest head = waitingQueue.peek();          // O(1): most urgent waiting train
            message += " Next in the queue: " + head.getTrainNo() + " (" + head.getType() + "), admitted automatically"
                    + " once a compatible platform is free.";
        }
        return new DepartureResult(train.getTrainNo(), platform.getId(), now, release, allocated, message);
    }

    // ============================================================ P7: delay

    /** Staff report a delay (1–720 minutes). */
    public DelayImpact reportDelay(String trainNo, int minutes, String reason) {
        if (minutes < 1 || minutes > MAX_DELAY_MINUTES) {
            throw new IllegalArgumentException("Delay must be between 1 and " + MAX_DELAY_MINUTES + " minutes");
        }
        String why = reason == null || reason.isBlank() ? "reported by staff" : reason.trim();
        return applyDelay(registry.get(trainNo), minutes, why, true);
    }

    /**
     * Rule P7.
     * <ul>
     *   <li>SCHEDULED: only the timetable changes.</li>
     *   <li>WAITING: REMOVE from the PriorityQueue, change the times, REINSERT (its ranking key changed).</li>
     *   <li>ASSIGNED / ARRIVED: extend the occupation. Every LATER train that has not arrived and now
     *       overlaps is un-assigned, re-queued and re-allocated, with an alert for each. If the new interval
     *       instead clashes with a train that may not be moved, the delayed (not yet arrived) train itself
     *       gives up the platform; a train on a platform is never displaced (P5).</li>
     * </ul>
     */
    DelayImpact applyDelay(TrainService train, int minutes, String reason, boolean reallocate) {
        if (train.getStatus() == TrainStatus.DEPARTED || train.getStatus() == TrainStatus.REJECTED) {
            throw new InvalidOperationException("Train " + train.getTrainNo() + " is " + train.getStatus()
                    + "; a delay cannot be applied");
        }
        String trainNo = train.getTrainNo();
        LocalDateTime oldArrival = train.getExpectedArrival();
        LocalDateTime oldDeparture = train.getExpectedDeparture();
        String platformId = train.getPlatformId();
        LocalDateTime oldEnd = null;
        LocalDateTime newEnd = null;
        boolean extended = false;
        boolean requeued = false;
        boolean lostPlatform = false;
        List<String> bumpedTrainNos = new ArrayList<>();
        List<String> alerts = new ArrayList<>();

        switch (train.getStatus()) {
            case WAITING_FOR_PLATFORM -> {
                PlatformRequest request = activeRequests.get(trainNo);
                waitingQueue.remove(request);              // remove BEFORE the key changes
                registry.delay(train, minutes, reason);
                waitingQueue.add(request);                 // reinsert at its new rank position
                requeued = true;
            }
            case PLATFORM_ASSIGNED, ARRIVED -> {
                Platform platform = platforms.get(platformId);
                OccupationInterval old = platform.removeOccupation(trainNo).orElseThrow();
                removeRelease(trainNo);
                oldEnd = old.end();
                registry.delay(train, minutes, reason);
                LocalDateTime start = occupationStart(train);
                LocalDateTime end = occupationEnd(train);

                List<OccupationInterval> clashes = platform.findOverlapping(start, end);
                List<OccupationInterval> blocking = new ArrayList<>();
                for (OccupationInterval c : clashes) {
                    if (!isBumpable(c, old.start())) {
                        blocking.add(c);
                    }
                }
                if (!blocking.isEmpty() && train.getStatus() == TrainStatus.ARRIVED) {
                    throw new PlatformConflictException("Arrived train " + trainNo + " clashes with "
                            + blocking.get(0).trainNo() + " on " + platformId);
                }
                if (!blocking.isEmpty()) {
                    train.markWaiting();
                    waitingQueue.add(activeRequests.get(trainNo));
                    lostPlatform = true;
                    String alert = trainNo + " gave up " + platformId + ": its delayed interval " + hm(start) + "–"
                            + hm(end) + " clashes with " + blocking.get(0).trainNo() + "; sent to the waiting queue";
                    alerts.add(alert);
                    logPlatform(train, true, alert);
                } else {
                    for (OccupationInterval clash : clashes) {
                        String alert = unassign(registry.get(clash.trainNo()), "bumped by the delay of " + trainNo);
                        alerts.add(alert);
                        bumpedTrainNos.add(clash.trainNo());
                    }
                    // The train keeps its own platform even if that platform is now in maintenance (P9).
                    platform.reoccupy(trainNo, start, end);
                    releaseHeap.add(new PlatformRelease(platformId, trainNo, end));
                    newEnd = end;
                    extended = true;
                }
            }
            default -> registry.delay(train, minutes, reason);
        }

        StringBuilder message = new StringBuilder("Train " + trainNo + " delayed by " + minutes + " min (" + reason
                + "): arrival " + hm(oldArrival) + " → " + hm(train.getExpectedArrival()) + ", departure "
                + hm(oldDeparture) + " → " + hm(train.getExpectedDeparture()) + ".");
        if (extended) {
            message.append(' ').append(platformId).append(" occupation extended to ").append(hm(newEnd)).append('.');
        }
        if (requeued) {
            message.append(" Removed and re-inserted into the platform queue with its new arrival time.");
        }
        if (lostPlatform) {
            message.append(" It lost ").append(platformId).append(" and joined the waiting queue.");
        }
        if (!bumpedTrainNos.isEmpty()) {
            message.append(" Un-assigned and re-queued: ").append(String.join(", ", bumpedTrainNos)).append('.');
        }
        log.record(clock.now(), StationEvent.Category.DELAY, !bumpedTrainNos.isEmpty() || lostPlatform, trainNo,
                message.toString());

        List<AllocationResult> reallocated = reallocate ? processQueue() : List.of();
        if (!reallocated.isEmpty()) {
            message.append(" Re-allocated: ").append(describe(reallocated)).append('.');
        }
        List<AllocationResult> bumped = new ArrayList<>();
        for (String no : bumpedTrainNos) {
            TrainService t = registry.get(no);
            bumped.add(t.getStatus() == TrainStatus.PLATFORM_ASSIGNED ? assignedResult(t)
                    : queuedResult(activeRequests.get(no)));
        }
        return new DelayImpact(trainNo, minutes, reason, platformId, oldArrival, oldDeparture,
                train.getExpectedArrival(), train.getExpectedDeparture(), oldEnd, newEnd, extended, requeued,
                lostPlatform, bumped, reallocated, alerts, message.toString());
    }

    /** Only a LATER train (planned after the delayed one) that has NOT arrived may be bumped (P5, P7). */
    private boolean isBumpable(OccupationInterval clash, LocalDateTime delayedTrainOriginalStart) {
        TrainService other = registry.get(clash.trainNo());
        return other.getStatus() == TrainStatus.PLATFORM_ASSIGNED && clash.start().isAfter(delayedTrainOriginalStart);
    }

    /** Takes an assigned (not arrived) train off its platform and puts it back in the waiting queue. */
    private String unassign(TrainService train, String reason) {
        Platform platform = platforms.get(train.getPlatformId());
        platform.removeOccupation(train.getTrainNo());
        removeRelease(train.getTrainNo());
        train.markWaiting();
        waitingQueue.add(activeRequests.get(train.getTrainNo()));
        String alert = train.getTrainNo() + " un-assigned from " + platform.getId() + " (" + reason
                + ") and re-queued";
        logPlatform(train, true, alert);
        return alert;
    }

    private void removeRelease(String trainNo) {
        releaseHeap.removeIf(r -> r.trainNo().equals(trainNo));
    }

    // ============================================================ P9 & platform management

    /**
     * P9: a platform in maintenance gets no new allocations. A train already ON it (arrived) is not
     * displaced. Trains only planned on it (assigned, not arrived) are re-queued and re-allocated elsewhere.
     */
    public OperationResult setMaintenance(String platformId, boolean maintenance) {
        Platform platform = getPlatform(platformId);
        platform.setUnderMaintenance(maintenance);
        List<String> alerts = new ArrayList<>();
        if (maintenance) {
            for (OccupationInterval interval : platform.getOccupations()) {
                TrainService t = registry.get(interval.trainNo());
                if (t.getStatus() == TrainStatus.PLATFORM_ASSIGNED) {
                    alerts.add(unassign(t, platformId + " under maintenance"));
                }
            }
        }
        String message = "Platform " + platformId + (maintenance ? " set to MAINTENANCE" : " back in service");
        log.record(clock.now(), StationEvent.Category.PLATFORM, maintenance, null, message);
        List<AllocationResult> allocations = processQueue();
        return new OperationResult(message, allocations, alerts);
    }

    /** Platform Management: change the length (never below a train that is planned on it). */
    public OperationResult changePlatformLength(String platformId, int newLength) {
        Platform platform = getPlatform(platformId);
        int longestUsing = 0;
        for (OccupationInterval interval : platform.getOccupations()) {
            TrainService t = registry.get(interval.trainNo());
            if (t.getStatus() != TrainStatus.DEPARTED) {
                longestUsing = Math.max(longestUsing, t.getLengthMeters());
            }
        }
        int oldLength = platform.getLengthMeters();
        platform.changeLength(newLength, longestUsing);
        String message = "Platform " + platformId + " length changed " + oldLength + " m → " + newLength + " m";
        log.record(clock.now(), StationEvent.Category.PLATFORM, false, null, message);
        return new OperationResult(message, processQueue(), List.of());
    }

    // ============================================================ clock

    /**
     * Advances the simulation clock and applies its consequences in this order:
     * <ol>
     *   <li>pop every release from the min-heap whose time has come (the platform is free again);</li>
     *   <li>an arrived train whose departure time has passed still holds its platform, so its occupation
     *       is extended (P5: never displace an occupying train);</li>
     *   <li>a waiting train whose arrival time has passed is held at the signal, so its times move to
     *       "now" (remove + reinsert in the queue, as in P7);</li>
     *   <li>try to allocate the waiting trains.</li>
     * </ol>
     */
    public List<String> advanceClock(int minutes) {
        LocalDateTime now = clock.advance(minutes);
        List<String> events = new ArrayList<>();
        log.record(now, StationEvent.Category.CLOCK, false, null, "Clock advanced by " + minutes + " min to " + hm(now));

        while (!releaseHeap.isEmpty() && !releaseHeap.peek().releaseTime().isAfter(now)) {
            PlatformRelease release = releaseHeap.poll();          // O(log n)
            if (registry.get(release.trainNo()).getStatus() == TrainStatus.DEPARTED) {
                String message = "Platform " + release.platformId() + " released at " + hm(release.releaseTime())
                        + " (after " + release.trainNo() + ")";
                events.add(message);
                log.record(release.releaseTime(), StationEvent.Category.PLATFORM, false, release.trainNo(), message);
            }
        }
        for (TrainService train : registry.timetable()) {
            if (train.getStatus() == TrainStatus.ARRIVED && train.getExpectedDeparture().isBefore(now)) {
                DelayImpact impact = applyDelay(train, minutesBetween(train.getExpectedDeparture(), now),
                        "still on the platform, departure not confirmed", false);
                events.add(impact.message());
            }
        }
        for (PlatformRequest request : new ArrayList<>(waitingQueue)) {
            TrainService train = request.getTrain();
            if (train.getStatus() == TrainStatus.WAITING_FOR_PLATFORM && train.getExpectedArrival().isBefore(now)) {
                DelayImpact impact = applyDelay(train, minutesBetween(train.getExpectedArrival(), now),
                        "held at the signal waiting for a platform", false);
                events.add(impact.message());
            }
        }
        for (AllocationResult allocation : processQueue()) {
            events.add(allocation.message());
        }
        return events;
    }

    // ============================================================ settings

    public void setPolicy(PlatformAllocationPolicy policy) {
        this.policy = policy;
        log.record(clock.now(), StationEvent.Category.SYSTEM, false, null, "Allocation policy set to " + policy.type());
    }

    /** New buffer applies to allocations made from now on; existing intervals keep their buffer. */
    public void setClearanceBufferMinutes(int minutes) {
        if (minutes < 0 || minutes > 60) {
            throw new IllegalArgumentException("Clearance buffer must be between 0 and 60 minutes");
        }
        this.clearanceBufferMinutes = minutes;
    }

    public PlatformAllocationPolicy getPolicy() {
        return policy;
    }

    public int getClearanceBufferMinutes() {
        return clearanceBufferMinutes;
    }

    // ============================================================ read models

    /** Waiting queue in TRUE PriorityQueue order (polls a copy: iterating a heap is not sorted). */
    public List<PlatformRequest> queueInOrder() {
        PriorityQueue<PlatformRequest> copy = new PriorityQueue<>(waitingQueue);
        List<PlatformRequest> ordered = new ArrayList<>();
        while (!copy.isEmpty()) {
            ordered.add(copy.poll());
        }
        return ordered;
    }

    public List<String> queueOrder() {
        List<String> order = new ArrayList<>();
        for (PlatformRequest r : queueInOrder()) {
            order.add(r.getTrainNo());
        }
        return order;
    }

    public int queueSize() {
        return waitingQueue.size();
    }

    /** Why a queued train is waiting, using the release min-heap for the "next compatible release". */
    public String waitingReason(PlatformRequest request) {
        TrainService train = request.getTrain();
        List<String> suitable = new ArrayList<>();
        List<String> maintenance = new ArrayList<>();
        for (Platform p : sortedPlatforms()) {
            if (p.canFit(train.getLengthMeters())) {
                (p.isUnderMaintenance() ? maintenance : suitable).add(p.getId());
            }
        }
        StringBuilder reason = new StringBuilder();
        if (suitable.isEmpty()) {
            reason.append("No platform long enough is in service");
        } else {
            reason.append("Platforms long enough (").append(String.join(", ", suitable)).append(") are busy during ")
                    .append(hm(occupationStart(train))).append("–").append(hm(occupationEnd(train)));
        }
        if (!maintenance.isEmpty()) {
            reason.append("; under maintenance: ").append(String.join(", ", maintenance));
        }
        earliestCompatibleRelease(train).ifPresent(r -> reason.append("; next compatible release: ")
                .append(r.platformId()).append(" at ").append(hm(r.releaseTime())));
        return reason.toString();
    }

    /**
     * Min-heap + length compatibility check: walks a copy of the release heap in time order and returns
     * the first release of an in-service platform that is long enough for the train.
     */
    public Optional<PlatformRelease> earliestCompatibleRelease(TrainService train) {
        PriorityQueue<PlatformRelease> copy = new PriorityQueue<>(releaseHeap);
        while (!copy.isEmpty()) {
            PlatformRelease release = copy.poll();
            Platform p = platforms.get(release.platformId());
            if (p.canFit(train.getLengthMeters()) && p.isAvailableForAllocation()) {
                return Optional.of(release);
            }
        }
        return Optional.empty();
    }

    /** O(1) peek at the min-heap: the next platform to be released. */
    public Optional<PlatformRelease> nextRelease() {
        return Optional.ofNullable(releaseHeap.peek());
    }

    /** Live status: MAINTENANCE if set by staff, OCCUPIED if an arrived train stands on it, else FREE. */
    public PlatformStatus statusOf(Platform platform) {
        if (platform.isUnderMaintenance()) {
            return PlatformStatus.MAINTENANCE;
        }
        return trainOn(platform).isPresent() ? PlatformStatus.OCCUPIED : PlatformStatus.FREE;
    }

    /** The train physically standing on the platform, if any. */
    public Optional<TrainService> trainOn(Platform platform) {
        for (OccupationInterval interval : platform.getOccupations()) {
            TrainService t = registry.get(interval.trainNo());
            if (t.getStatus() == TrainStatus.ARRIVED) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    public Optional<PlatformRequest> requestOf(String trainNo) {
        return Optional.ofNullable(activeRequests.get(trainNo));
    }

    public Platform getPlatform(String id) {
        Platform p = id == null ? null : platforms.get(id.trim().toUpperCase());
        if (p == null) {
            throw new InvalidOperationException("Unknown platform " + id);
        }
        return p;
    }

    /** Platforms ordered P1, P2, P3. */
    public List<Platform> sortedPlatforms() {
        List<Platform> list = new ArrayList<>(platforms.values());
        list.sort((a, b) -> Platform.compareIds(a.getId(), b.getId()));
        return list;
    }

    PlatformScheduler copy(TrainRegistry copiedRegistry, SimulationClock copiedClock, EventLog copiedLog,
                           Map<String, TrainService> copiedTrains) {
        PlatformScheduler c = new PlatformScheduler(copiedRegistry, copiedClock, copiedLog, policy,
                clearanceBufferMinutes);
        for (Platform p : platforms.values()) {
            c.platforms.put(p.getId(), p.copy());
        }
        for (Map.Entry<String, PlatformRequest> e : activeRequests.entrySet()) {
            c.activeRequests.put(e.getKey(), e.getValue().copyFor(copiedTrains.get(e.getKey())));
        }
        for (PlatformRequest r : waitingQueue) {
            c.waitingQueue.add(c.activeRequests.get(r.getTrainNo()));
        }
        c.releaseHeap.addAll(releaseHeap);
        return c;
    }


    private AllocationResult assignedResult(TrainService train) {
        OccupationInterval interval = platforms.get(train.getPlatformId()).findOccupation(train.getTrainNo())
                .orElseThrow();
        return new AllocationResult(train.getTrainNo(), AllocationResult.Outcome.ASSIGNED, train.getPlatformId(),
                interval.start(), interval.end(), 0, "Assigned " + train.getTrainNo() + " to " + train.getPlatformId()
                + " for " + hm(interval.start()) + "–" + hm(interval.end()));
    }

    private AllocationResult queuedResult(PlatformRequest request) {
        TrainService train = request.getTrain();
        int position = queueOrder().indexOf(train.getTrainNo()) + 1;
        return new AllocationResult(train.getTrainNo(), AllocationResult.Outcome.QUEUED, null,
                occupationStart(train), occupationEnd(train), position,
                "Queued " + train.getTrainNo() + " at position " + position + ". " + waitingReason(request));
    }

    private void logPlatform(TrainService train, boolean alert, String message) {
        log.record(clock.now(), StationEvent.Category.PLATFORM, alert, train.getTrainNo(), message);
    }

    private static String describe(List<AllocationResult> results) {
        List<String> parts = new ArrayList<>();
        for (AllocationResult r : results) {
            parts.add(r.trainNo() + " → " + r.platformId());
        }
        return String.join(", ", parts);
    }

    static String hm(LocalDateTime time) {
        return time == null ? "-" : time.format(HM);
    }

    private static int minutesBetween(LocalDateTime from, LocalDateTime to) {
        return (int) Math.max(1, Duration.between(from, to).toMinutes());
    }
}
