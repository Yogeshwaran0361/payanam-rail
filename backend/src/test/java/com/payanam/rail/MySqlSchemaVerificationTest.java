package com.payanam.rail;

import com.payanam.rail.domain.model.Gender;
import com.payanam.rail.domain.model.Priority;
import com.payanam.rail.domain.model.TrainStatus;
import com.payanam.rail.domain.user.PassengerUser;
import com.payanam.rail.domain.user.UserAccount;
import com.payanam.rail.dto.booking.BookingRequest;
import com.payanam.rail.dto.booking.BookingView;
import com.payanam.rail.dto.booking.CancellationView;
import com.payanam.rail.dto.booking.PassengerInput;
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
import com.payanam.rail.security.AuthUser;
import com.payanam.rail.service.BookingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/payanam_rail_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true",
        "spring.datasource.username=${MYSQL_USER:root}",
        "spring.datasource.password=${MYSQL_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=update"
})
public class MySqlSchemaVerificationTest {

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

    @Autowired
    private BookingService bookingService;

    @Test
    @DisplayName("Verify MySQL operational tables exist and perform CRUD operations against payanam_rail_db")
    void testMySqlTablesAndOperations() {
        // 1. Verify existing user account can be retrieved from MySQL
        UserAccount owner = userAccountRepository.findAll().stream().findFirst().orElse(null);
        if (owner == null) {
            owner = userAccountRepository.save(new PassengerUser(
                    "Test User", "test.user@payanam.in", "9876543210", "pwd_hash",
                    LocalDate.of(1990, 1, 1), null, "Chennai", LocalDateTime.now()
            ));
        }
        assertNotNull(owner);

        // 2. Insert or retrieve TrainEntity
        TrainEntity train = trainRepository.findById("99999").orElse(null);
        if (train == null) {
            train = new TrainEntity(
                    "99999", "Mysql Test Express", "Origin", "Destination", 400,
                    Priority.EMERGENCY, BigDecimal.valueOf(350.00), 10,
                    LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(3),
                    LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(3),
                    TrainStatus.SCHEDULED, "P1", 0, null, null, null, 0
            );
            train = trainRepository.save(train);
        }
        assertNotNull(train);

        // 3. Insert or retrieve PlatformEntity
        PlatformEntity platform = platformRepository.findById("P1").orElse(null);
        if (platform == null) {
            platform = platformRepository.save(new PlatformEntity("P1", "Platform 1", 500, false));
        }
        assertNotNull(platform);

        // 4. Test BookingEntity (bookings table)
        String testPnr = "PNR999TEST";
        bookingRepository.findById(testPnr).ifPresent(bookingRepository::delete);
        BookingEntity booking = new BookingEntity(
                testPnr, owner, owner.getFullName(), train, LocalDateTime.now(),
                BigDecimal.valueOf(350.00), BigDecimal.valueOf(350.00), 1,
                "UPCOMING", "1 CNF", "[]"
        );
        BookingEntity savedBooking = bookingRepository.save(booking);
        assertEquals(testPnr, savedBooking.getPnr());
        assertTrue(bookingRepository.findById(testPnr).isPresent());

        // 5. Test WaitingListEntity (waiting_list table)
        WaitingListEntity wl = new WaitingListEntity(
                train, owner, testPnr, "TKT-TEST-01", "Test Passenger",
                30, "MALE", 1, LocalDateTime.now(), "WAITING"
        );
        WaitingListEntity savedWl = waitingListRepository.save(wl);
        assertNotNull(savedWl.getId());
        assertEquals("WAITING", savedWl.getQueueStatus());

        // 6. Test PlatformAllocationEntity (platform_allocations table)
        PlatformAllocationEntity allocation = new PlatformAllocationEntity(
                platform, train, "SCHEDULED", 1,
                LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(3),
                10, "ASSIGNED", LocalDateTime.now()
        );
        PlatformAllocationEntity savedAllocation = allocationRepository.save(allocation);
        assertNotNull(savedAllocation.getId());
        assertEquals("ASSIGNED", savedAllocation.getAllocationStatus());

        // 7. Test CancellationEntity (cancellations table)
        CancellationEntity cancellation = new CancellationEntity(
                savedBooking, "TKT-TEST-01", "Test Passenger", train,
                owner, false, 1, null, null, "CNF", LocalDateTime.now()
        );
        CancellationEntity savedCancellation = cancellationRepository.save(cancellation);
        assertNotNull(savedCancellation.getId());
        assertEquals(testPnr, savedCancellation.getBooking().getPnr());

        // Clean up sample test entities created in this test run
        cancellationRepository.delete(savedCancellation);
        waitingListRepository.delete(savedWl);
        allocationRepository.delete(savedAllocation);
        bookingRepository.delete(savedBooking);
        trainRepository.delete(train);
    }

    @Test
    @DisplayName("Verify end-to-end booking and cancellation operations write into MySQL tables")
    void testEndToEndBookingAndCancellationPersistence() {
        UserAccount user = userAccountRepository.findAll().stream()
                .filter(u -> u instanceof PassengerUser)
                .findFirst()
                .orElseThrow();
        AuthUser authUser = AuthUser.of(user, "test-session-id");

        // Book 1 passenger on seed train 12635
        BookingRequest request = new BookingRequest("12635", List.of(
                new PassengerInput("MySQL Integration Passenger", 25, Gender.FEMALE)
        ));
        BookingView view = bookingService.book(authUser, request);
        assertNotNull(view);
        String pnr = view.pnr();

        // Verify booking exists in MySQL
        BookingEntity inDb = bookingRepository.findById(pnr).orElse(null);
        assertNotNull(inDb, "Booking must be persisted in MySQL 'bookings' table");
        assertEquals("UPCOMING", inDb.getStatus());
        assertEquals("12635", inDb.getTrain().getTrainNo());
        assertEquals(user.getId(), inDb.getOwner().getId());

        // Cancel the booking
        CancellationView cancelView = bookingService.cancel(authUser, pnr);
        assertNotNull(cancelView);

        // Verify cancellation exists in MySQL
        List<CancellationEntity> cancellations = cancellationRepository.findByBooking_PnrOrderByCancelledAtDesc(pnr);
        assertFalse(cancellations.isEmpty(), "Cancellation must be persisted in MySQL 'cancellations' table");
        assertEquals("MySQL Integration Passenger", cancellations.get(0).getPassengerName());

        // Verify booking status was updated to CANCELLED in MySQL
        BookingEntity cancelledDb = bookingRepository.findById(pnr).orElseThrow();
        assertEquals("CANCELLED", cancelledDb.getStatus());

        // Clean up sample records from database
        cancellationRepository.deleteAll(cancellations);
        bookingRepository.delete(cancelledDb);
    }
}
