package com.payanam.rail.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payanam.rail.domain.model.ArrivalType;
import com.payanam.rail.domain.model.Booking;
import com.payanam.rail.domain.model.CancellationOutcome;
import com.payanam.rail.domain.model.Platform;
import com.payanam.rail.domain.model.Reservation;
import com.payanam.rail.domain.model.ReservationStatus;
import com.payanam.rail.domain.model.TrainService;
import com.payanam.rail.domain.user.UserAccount;
import com.payanam.rail.entity.BookingEntity;
import com.payanam.rail.entity.CancellationEntity;
import com.payanam.rail.entity.PlatformAllocationEntity;
import com.payanam.rail.entity.PlatformEntity;
import com.payanam.rail.entity.TrainEntity;
import com.payanam.rail.entity.WaitingListEntity;
import com.payanam.rail.repository.BookingRepository;
import com.payanam.rail.repository.CancellationRepository;
import com.payanam.rail.repository.PlatformAllocationRepository;
import com.payanam.rail.repository.PlatformRepository;
import com.payanam.rail.repository.TrainRepository;
import com.payanam.rail.repository.UserAccountRepository;
import com.payanam.rail.repository.WaitingListRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service that synchronizes in-memory operational domain state with MySQL database tables:
 * trains, platforms, bookings, waiting_list, platform_allocations, cancellations.
 */
@Service
public class OperationalPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(OperationalPersistenceService.class);

    private final TrainRepository trainRepository;
    private final PlatformRepository platformRepository;
    private final BookingRepository bookingRepository;
    private final WaitingListRepository waitingListRepository;
    private final PlatformAllocationRepository allocationRepository;
    private final CancellationRepository cancellationRepository;
    private final UserAccountRepository userAccountRepository;
    private final ObjectMapper mapper;

    public OperationalPersistenceService(TrainRepository trainRepository,
                                         PlatformRepository platformRepository,
                                         BookingRepository bookingRepository,
                                         WaitingListRepository waitingListRepository,
                                         PlatformAllocationRepository allocationRepository,
                                         CancellationRepository cancellationRepository,
                                         UserAccountRepository userAccountRepository,
                                         ObjectMapper mapper) {
        this.trainRepository = trainRepository;
        this.platformRepository = platformRepository;
        this.bookingRepository = bookingRepository;
        this.waitingListRepository = waitingListRepository;
        this.allocationRepository = allocationRepository;
        this.cancellationRepository = cancellationRepository;
        this.userAccountRepository = userAccountRepository;
        this.mapper = mapper;
    }

    /**
     * Synchronizes platforms to MySQL. Idempotent: updates existing, inserts missing.
     */
    @Transactional
    public void syncPlatforms(List<Platform> platforms) {
        for (Platform p : platforms) {
            PlatformEntity entity = platformRepository.findById(p.getId())
                    .orElseGet(PlatformEntity::new);
            entity.setPlatformId(p.getId());
            entity.setName(p.getName());
            entity.setLengthMeters(p.getLengthMeters());
            entity.setUnderMaintenance(p.isUnderMaintenance());
            platformRepository.save(entity);
        }
    }

    /**
     * Synchronizes trains to MySQL. Idempotent: updates existing, inserts missing.
     */
    @Transactional
    public void syncTrains(List<TrainService> trains) {
        for (TrainService t : trains) {
            saveOrUpdateTrain(t);
        }
    }

    @Transactional
    public TrainEntity saveOrUpdateTrain(TrainService t) {
        TrainEntity entity = trainRepository.findById(t.getTrainNo())
                .orElseGet(TrainEntity::new);
        entity.setTrainNo(t.getTrainNo());
        entity.setName(t.getName());
        entity.setOrigin(t.getOrigin());
        entity.setDestination(t.getDestination());
        entity.setLengthMeters(t.getLengthMeters());
        entity.setDeclaredPriority(t.getDeclaredPriority());
        entity.setFare(t.getFare());
        entity.setCapacity(TrainService.CAPACITY);
        entity.setScheduledArrival(t.getScheduledArrival());
        entity.setScheduledDeparture(t.getScheduledDeparture());
        entity.setExpectedArrival(t.getExpectedArrival());
        entity.setExpectedDeparture(t.getExpectedDeparture());
        entity.setStatus(t.getStatus());
        entity.setPlatformId(t.getPlatformId());
        entity.setDelayMinutes(t.getDelayMinutes());
        entity.setDelayReason(t.getDelayReason());
        entity.setActualArrival(t.getActualArrival());
        entity.setActualDeparture(t.getActualDeparture());
        entity.setConfirmedCount(t.getConfirmedCount());
        return trainRepository.save(entity);
    }

    /**
     * Saves a booking and its waitlisted passengers to MySQL.
     */
    @Transactional
    public void saveBooking(Booking booking) {
        UserAccount owner = userAccountRepository.findById(booking.getOwnerUserId()).orElse(null);
        TrainEntity train = trainRepository.findById(booking.getTrainNo()).orElse(null);
        if (owner == null || train == null) {
            log.warn("Cannot persist booking {}: owner or train not found in database", booking.getPnr());
            return;
        }

        List<String> labels = new ArrayList<>();
        int cnf = 0, wl = 0;
        for (Reservation r : booking.getReservations()) {
            if (r.getStatus() == ReservationStatus.CNF) {
                cnf++;
            } else if (r.getStatus() == ReservationStatus.WL) {
                wl++;
            }
        }
        if (cnf > 0) labels.add(cnf + " CNF");
        if (wl > 0) labels.add(wl + " WL");
        String summary = String.join(", ", labels);

        String json;
        try {
            json = mapper.writeValueAsString(booking.getReservations());
        } catch (JsonProcessingException e) {
            json = "[]";
        }

        BookingEntity entity = bookingRepository.findById(booking.getPnr())
                .orElseGet(BookingEntity::new);
        entity.setPnr(booking.getPnr());
        entity.setOwner(owner);
        entity.setOwnerName(booking.getOwnerName());
        entity.setTrain(train);
        entity.setBookedAt(booking.getBookedAt());
        entity.setFarePerPassenger(booking.getFarePerPassenger());
        entity.setTotalFare(booking.totalFare());
        entity.setPassengerCount(booking.getReservations().size());
        entity.setStatus(booking.isFullyCancelled() ? "CANCELLED" : "UPCOMING");
        entity.setSummary(summary);
        entity.setPassengersJson(json);
        bookingRepository.save(entity);

        // Record any waitlisted passengers
        for (Reservation r : booking.getReservations()) {
            if (r.getStatus() == ReservationStatus.WL) {
                WaitingListEntity wlEntity = waitingListRepository.findByTicketId(r.getId())
                        .orElseGet(WaitingListEntity::new);
                wlEntity.setTrain(train);
                wlEntity.setUser(owner);
                wlEntity.setPnr(booking.getPnr());
                wlEntity.setTicketId(r.getId());
                wlEntity.setPassengerName(r.getPassenger().name());
                wlEntity.setPassengerAge(r.getPassenger().age());
                wlEntity.setPassengerGender(r.getPassenger().gender().name());
                wlEntity.setWaitingPosition(r.getWaitingPosition());
                wlEntity.setJoinedAt(r.getBookedAt());
                wlEntity.setQueueStatus("WAITING");
                waitingListRepository.save(wlEntity);
            }
        }
    }

    /**
     * Records a cancellation and subsequent FIFO promotion into MySQL.
     */
    @Transactional
    public void recordCancellation(Booking booking, CancellationOutcome outcome, Long actorUserId,
                                   boolean staffOverride, LocalDateTime when) {
        BookingEntity bookingEntity = bookingRepository.findById(booking.getPnr()).orElse(null);
        TrainEntity trainEntity = trainRepository.findById(booking.getTrainNo()).orElse(null);
        UserAccount actor = actorUserId != null ? userAccountRepository.findById(actorUserId).orElse(null) : null;

        if (bookingEntity == null || trainEntity == null) {
            log.warn("Cannot persist cancellation on PNR {}: booking or train not found", booking.getPnr());
            return;
        }

        Reservation cancelledTicket = outcome.cancelled();

        CancellationEntity cancellation = new CancellationEntity(
                bookingEntity,
                cancelledTicket.getId(),
                cancelledTicket.getPassenger().name(),
                trainEntity,
                actor,
                staffOverride,
                outcome.freedSeat(),
                outcome.hasPromotion() ? outcome.promoted().getId() : null,
                outcome.hasPromotion() ? outcome.promoted().getPnr() : null,
                outcome.previousLabel(),
                when
        );
        cancellationRepository.save(cancellation);

        // If the cancelled ticket was on the waiting list, update its entry
        waitingListRepository.findByTicketId(cancelledTicket.getId()).ifPresent(wl -> {
            wl.setQueueStatus("CANCELLED");
            waitingListRepository.save(wl);
        });

        // If a waiting passenger was promoted to confirmed seat
        if (outcome.hasPromotion()) {
            Reservation promoted = outcome.promoted();
            waitingListRepository.findByTicketId(promoted.getId()).ifPresent(wl -> {
                wl.setQueueStatus("PROMOTED");
                wl.setPromotedAt(when);
                wl.setPromotedSeatNo(promoted.getSeatNo());
                waitingListRepository.save(wl);
            });
        }

        // Renumber remaining waiting list entries for this train
        List<WaitingListEntity> waitingQueue = waitingListRepository
                .findByTrain_TrainNoAndQueueStatusOrderByWaitingPositionAsc(trainEntity.getTrainNo(), "WAITING");
        int pos = 1;
        for (WaitingListEntity entry : waitingQueue) {
            entry.setWaitingPosition(pos++);
            waitingListRepository.save(entry);
        }

        // Update the booking entity status and JSON
        try {
            bookingEntity.setPassengersJson(mapper.writeValueAsString(booking.getReservations()));
        } catch (JsonProcessingException ignored) {
        }
        if (booking.isFullyCancelled()) {
            bookingEntity.setStatus("CANCELLED");
        }
        bookingRepository.save(bookingEntity);
    }

    /**
     * Records a platform allocation in MySQL.
     */
    @Transactional
    public void recordPlatformAllocation(TrainService train, Platform platform, ArrivalType type,
                                         int priorityRank, LocalDateTime start, LocalDateTime end,
                                         int bufferMinutes) {
        TrainEntity trainEntity = saveOrUpdateTrain(train);
        PlatformEntity platformEntity = platformRepository.findById(platform.getId())
                .orElseGet(() -> platformRepository.save(new PlatformEntity(platform.getId(), platform.getName(),
                        platform.getLengthMeters(), platform.isUnderMaintenance())));

        PlatformAllocationEntity allocation = new PlatformAllocationEntity(
                platformEntity,
                trainEntity,
                type != null ? type.name() : "SCHEDULED",
                priorityRank,
                start,
                end,
                bufferMinutes,
                "ASSIGNED",
                LocalDateTime.now()
        );
        allocationRepository.save(allocation);
    }

    /**
     * Updates train arrival in MySQL.
     */
    @Transactional
    public void recordArrival(TrainService train) {
        saveOrUpdateTrain(train);
        allocationRepository.findTopByTrain_TrainNoOrderByCreatedAtDesc(train.getTrainNo()).ifPresent(alloc -> {
            alloc.setActualArrival(train.getActualArrival());
            alloc.setAllocationStatus("OCCUPIED");
            allocationRepository.save(alloc);
        });
    }

    /**
     * Updates train departure in MySQL.
     */
    @Transactional
    public void recordDeparture(TrainService train) {
        saveOrUpdateTrain(train);
        allocationRepository.findTopByTrain_TrainNoOrderByCreatedAtDesc(train.getTrainNo()).ifPresent(alloc -> {
            alloc.setActualDeparture(train.getActualDeparture());
            alloc.setAllocationStatus("RELEASED");
            allocationRepository.save(alloc);
        });
    }

    /**
     * Updates train delay in MySQL.
     */
    @Transactional
    public void recordDelay(TrainService train) {
        saveOrUpdateTrain(train);
    }

    /**
     * Updates platform details (length / maintenance) in MySQL.
     */
    @Transactional
    public void updatePlatform(Platform platform) {
        PlatformEntity entity = platformRepository.findById(platform.getId())
                .orElseGet(PlatformEntity::new);
        entity.setPlatformId(platform.getId());
        entity.setName(platform.getName());
        entity.setLengthMeters(platform.getLengthMeters());
        entity.setUnderMaintenance(platform.isUnderMaintenance());
        platformRepository.save(entity);
    }
}

