package com.payanam.rail.service;

import com.payanam.rail.domain.model.Gender;
import com.payanam.rail.domain.model.Priority;
import com.payanam.rail.domain.model.TrainStatus;
import com.payanam.rail.domain.user.PassengerUser;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OperationalPersistenceTest {

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private PlatformRepository platformRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private WaitingListRepository waitingListRepository;

    @Autowired
    private PlatformAllocationRepository allocationRepository;

    @Autowired
    private CancellationRepository cancellationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    @DisplayName("Verify all 6 operational tables can save, retrieve and relate records cleanly")
    void testOperationalEntitiesCRUD() {
        // 1. Create a UserAccount
        UserAccount user = userAccountRepository.save(new PassengerUser(
                "Test Passenger", "test.pers@example.com", "9998887776", "hash123",
                LocalDate.of(1995, 5, 20), Gender.MALE, "Madurai", LocalDateTime.now()
        ));
        assertNotNull(user.getId());

        // 2. Create TrainEntity (trains table)
        LocalDateTime now = LocalDateTime.now();
        TrainEntity train = new TrainEntity(
                "12635", "Vaigai Express", "Chennai Egmore", "Madurai Jn", 420,
                Priority.NORMAL, BigDecimal.valueOf(485.00), 10, now.plusHours(1), now.plusHours(2),
                now.plusHours(1), now.plusHours(2), TrainStatus.SCHEDULED, null, 0, null,
                null, null, 0
        );
        TrainEntity savedTrain = trainRepository.save(train);
        assertEquals("12635", savedTrain.getTrainNo());
        assertTrue(trainRepository.findById("12635").isPresent());

        // 3. Create PlatformEntity (platforms table)
        PlatformEntity platform = new PlatformEntity("P1", "Platform 1", 600, false);
        PlatformEntity savedPlatform = platformRepository.save(platform);
        assertEquals("P1", savedPlatform.getPlatformId());
        assertTrue(platformRepository.findById("P1").isPresent());

        // 4. Create BookingEntity (bookings table)
        BookingEntity booking = new BookingEntity(
                "4000999001", user, user.getFullName(), savedTrain, now,
                BigDecimal.valueOf(485.00), BigDecimal.valueOf(485.00), 1,
                "UPCOMING", "1 CNF", "[]"
        );
        BookingEntity savedBooking = bookingRepository.save(booking);
        assertEquals("4000999001", savedBooking.getPnr());
        assertEquals(user.getId(), savedBooking.getOwner().getId());
        assertEquals("12635", savedBooking.getTrain().getTrainNo());

        // 5. Create WaitingListEntity (waiting_list table)
        WaitingListEntity wl = new WaitingListEntity(
                savedTrain, user, savedBooking.getPnr(), "4000999001/1",
                "Test Passenger", 29, "MALE", 1, now, "WAITING"
        );
        WaitingListEntity savedWl = waitingListRepository.save(wl);
        assertNotNull(savedWl.getId());
        assertEquals(1, savedWl.getWaitingPosition());
        assertEquals("12635", savedWl.getTrain().getTrainNo());
        assertEquals(user.getId(), savedWl.getUser().getId());

        // 6. Create PlatformAllocationEntity (platform_allocations table)
        PlatformAllocationEntity alloc = new PlatformAllocationEntity(
                savedPlatform, savedTrain, "SCHEDULED", 2, now.plusHours(1),
                now.plusHours(2).plusMinutes(10), 10, "ASSIGNED", now
        );
        PlatformAllocationEntity savedAlloc = allocationRepository.save(alloc);
        assertNotNull(savedAlloc.getId());
        assertEquals("P1", savedAlloc.getPlatform().getPlatformId());
        assertEquals("12635", savedAlloc.getTrain().getTrainNo());

        // 7. Create CancellationEntity (cancellations table)
        CancellationEntity cancellation = new CancellationEntity(
                savedBooking, "4000999001/1", "Test Passenger", savedTrain,
                user, false, 1, null, null, null, now
        );
        CancellationEntity savedCanc = cancellationRepository.save(cancellation);
        assertNotNull(savedCanc.getId());
        assertEquals("4000999001", savedCanc.getBooking().getPnr());
        assertEquals("12635", savedCanc.getTrain().getTrainNo());
        assertEquals(user.getId(), savedCanc.getCancelledBy().getId());

        // 8. Verify querying by relationship
        List<BookingEntity> userBookings = bookingRepository.findByOwner_IdOrderByBookedAtDesc(user.getId());
        assertFalse(userBookings.isEmpty());
        assertEquals("4000999001", userBookings.get(0).getPnr());

        List<CancellationEntity> trainCancellations = cancellationRepository.findByTrain_TrainNoOrderByCancelledAtDesc("12635");
        assertFalse(trainCancellations.isEmpty());
        assertEquals("4000999001/1", trainCancellations.get(0).getTicketId());
    }
}

