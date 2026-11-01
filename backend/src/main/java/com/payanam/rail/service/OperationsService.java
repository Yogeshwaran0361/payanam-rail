package com.payanam.rail.service;

import com.payanam.rail.domain.engine.DelayImpact;
import com.payanam.rail.domain.engine.DepartureResult;
import com.payanam.rail.domain.engine.OperationResult;
import com.payanam.rail.domain.engine.RailwayStation;
import com.payanam.rail.domain.model.ArrivalType;
import com.payanam.rail.domain.model.OccupationInterval;
import com.payanam.rail.domain.model.Platform;
import com.payanam.rail.domain.model.TrainService;
import com.payanam.rail.domain.model.TrainStatus;
import com.payanam.rail.dto.station.AllocationView;
import com.payanam.rail.dto.station.DelayImpactView;
import com.payanam.rail.dto.station.OperationView;
import com.payanam.rail.dto.station.PlatformBoardView;
import com.payanam.rail.dto.station.PlatformView;
import com.payanam.rail.dto.station.QueueEntryView;
import com.payanam.rail.dto.train.TrainView;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Station operations: arrival requests, arrival / departure confirmation, delays (with an exact impact
 * preview computed on a copy of the station), the platform board and queue, and platform management.
 */
@Service
public class OperationsService {
    private final StationService station;
    private final AuditService audit;
    private final OperationalPersistenceService operationalPersistence;

    public OperationsService(StationService station, AuditService audit,
                             OperationalPersistenceService operationalPersistence) {
        this.station = station;
        this.audit = audit;
        this.operationalPersistence = operationalPersistence;
    }

    // ------------------------------------------------------------ arrival requests (P1-P4)

    public AllocationView requestPlatform(String trainNo, ArrivalType type) {
        AllocationView view = station.write(s -> {
            var allocResult = s.requestPlatform(trainNo, type);
            if (allocResult.outcome() == com.payanam.rail.domain.engine.AllocationResult.Outcome.ASSIGNED) {
                TrainService train = s.trains().get(trainNo);
                Platform platform = s.platforms().getPlatform(train.getPlatformId());
                operationalPersistence.recordPlatformAllocation(train, platform, type,
                        train.getDeclaredPriority().rank(), allocResult.start(), allocResult.end(),
                        s.platforms().getClearanceBufferMinutes());
            }
            return StationViews.allocation(allocResult);
        });
        audit.record("ARRIVAL_REQUEST", trainNo + " (" + (type == null ? "declared priority" : type) + "): "
                + view.outcome() + " - " + view.message());
        return view;
    }

    // ------------------------------------------------------------ arrival / departure / delay

    public OperationView confirmArrival(String trainNo, boolean preview) {
        OperationView view = execute(preview, s -> {
            TrainService t = s.confirmArrival(trainNo);
            if (!preview) {
                operationalPersistence.recordArrival(t);
            }
            return new OperationResult("Train " + t.getTrainNo() + " arrived on " + t.getPlatformId() + " at "
                    + t.getActualArrival().toLocalTime() + ". Booking is now closed (R6) and the train cannot be"
                    + " displaced (P5).", List.of(), List.of());
        });
        if (!preview) {
            audit.record("ARRIVAL_CONFIRMED", view.message());
        }
        return view;
    }

    public OperationView confirmDeparture(String trainNo, boolean preview) {
        OperationView view = execute(preview, s -> {
            DepartureResult d = s.confirmDeparture(trainNo);
            if (!preview) {
                TrainService t = s.trains().get(trainNo);
                operationalPersistence.recordDeparture(t);
            }
            return new OperationResult(d.message(), d.allocatedFromQueue(), List.of());
        });
        if (!preview) {
            audit.record("DEPARTURE_CONFIRMED", view.message());
        }
        return view;
    }

    public DelayImpactView reportDelay(String trainNo, int minutes, String reason, boolean preview) {
        Function<RailwayStation, DelayImpact> op = s -> {
            DelayImpact impact = s.reportDelay(trainNo, minutes, reason);
            if (!preview) {
                TrainService t = s.trains().get(trainNo);
                operationalPersistence.recordDelay(t);
            }
            return impact;
        };
        DelayImpact impact = preview ? station.preview(op) : station.write(op);
        if (!preview) {
            audit.record("DELAY_REPORTED", impact.message());
        }
        return StationViews.delay(impact, preview);
    }

    /** Runs an operation live or on a copy, and returns the station events it produced. */
    private OperationView execute(boolean preview, Function<RailwayStation, OperationResult> op) {
        Function<RailwayStation, OperationView> withEvents = s -> {
            long before = s.events().lastSequence();
            OperationResult r = op.apply(s);
            return new OperationView(preview, r.message(), StationViews.allocations(r.allocations()), r.alerts(),
                    StationViews.events(s.events().since(before)));
        };
        return preview ? station.preview(withEvents) : station.write(withEvents);
    }

    // ------------------------------------------------------------ platform management (P9)

    public OperationView setMaintenance(String platformId, boolean maintenance) {
        OperationView view = execute(false, s -> {
            OperationResult r = s.setMaintenance(platformId, maintenance);
            operationalPersistence.updatePlatform(s.platforms().getPlatform(platformId));
            return r;
        });
        audit.record(maintenance ? "PLATFORM_MAINTENANCE_ON" : "PLATFORM_MAINTENANCE_OFF", view.message());
        return view;
    }

    public OperationView changeLength(String platformId, int lengthMeters) {
        OperationView view = execute(false, s -> {
            OperationResult r = s.changePlatformLength(platformId, lengthMeters);
            operationalPersistence.updatePlatform(s.platforms().getPlatform(platformId));
            return r;
        });
        audit.record("PLATFORM_EDITED", view.message());
        return view;
    }

    // ------------------------------------------------------------ read models

    public List<TrainView> trains() {
        return station.read(s -> s.trains().timetable().stream().map(t -> StationViews.train(s, t)).toList());
    }

    public List<QueueEntryView> queue() {
        return station.read(StationViews::queue);
    }

    /** Platform cards + Gantt data. The window covers the clock start up to the last planned interval. */
    public PlatformBoardView board() {
        return station.read(s -> {
            LocalDateTime now = s.now();
            LocalDateTime windowStart = s.clock().start().truncatedTo(ChronoUnit.HOURS);
            LocalDateTime windowEnd = windowStart.plusHours(3);
            List<PlatformView> platforms = new ArrayList<>();
            for (Platform p : s.platforms().sortedPlatforms()) {
                PlatformView view = platform(s, p, now);
                for (PlatformView.BlockView b : view.blocks()) {
                    windowEnd = later(windowEnd, b.end());
                }
                platforms.add(view);
            }
            List<QueueEntryView> waiting = StationViews.queue(s);
            for (QueueEntryView q : waiting) {
                windowEnd = later(windowEnd, q.desiredEnd());
            }
            windowEnd = later(windowEnd, now.plusMinutes(30));
            if (windowEnd.getMinute() != 0) {
                windowEnd = windowEnd.truncatedTo(ChronoUnit.HOURS).plusHours(1);
            }
            return new PlatformBoardView(now, windowStart, windowEnd, s.platforms().getClearanceBufferMinutes(),
                    s.platforms().getPolicy().type().name(), platforms, waiting, StationViews.nextRelease(s));
        });
    }

    public List<PlatformView> platforms() {
        return board().platforms();
    }

    private static PlatformView platform(RailwayStation s, Platform p, LocalDateTime now) {
        List<PlatformView.BlockView> blocks = new ArrayList<>();
        TrainService next = null;
        for (OccupationInterval i : p.getOccupations()) {
            TrainService t = s.trains().get(i.trainNo());
            blocks.add(new PlatformView.BlockView(t.getTrainNo(), t.getName(), i.start(), i.end(), t.getStatus().name(),
                    t.getDeclaredPriority().name(), t.getLengthMeters()));
            if (t.getStatus() == TrainStatus.PLATFORM_ASSIGNED && next == null) {
                next = t;
            }
        }
        TrainService current = s.platforms().trainOn(p).orElse(null);
        OccupationInterval currentInterval = current == null ? null : p.findOccupation(current.getTrainNo()).orElse(null);
        return new PlatformView(p.getId(), p.getName(), p.getLengthMeters(), s.platforms().statusOf(p).name(),
                p.isUnderMaintenance(), current == null ? null : current.getTrainNo(),
                current == null ? null : current.getName(), currentInterval == null ? null : currentInterval.start(),
                currentInterval == null ? null : currentInterval.end(), p.nextFreeTime(now),
                next == null ? null : next.getTrainNo(), next == null ? null : next.getExpectedArrival(), blocks);
    }

    private static LocalDateTime later(LocalDateTime a, LocalDateTime b) {
        return b != null && b.isAfter(a) ? b : a;
    }
}
