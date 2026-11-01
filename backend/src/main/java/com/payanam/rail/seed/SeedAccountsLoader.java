package com.payanam.rail.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.payanam.rail.domain.user.PassengerUser;
import com.payanam.rail.domain.user.StationStaffUser;
import com.payanam.rail.repository.UserAccountRepository;
import com.payanam.rail.service.StationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * At start-up (after all beans exist, before the web server accepts requests): creates the demo accounts from seed/users.json if they do not exist yet (accounts are
 * persistent in the H2 file), then loads the in-memory station from the seed files.
 */
@Component
public class SeedAccountsLoader implements SmartInitializingSingleton {
    private static final Logger log = LoggerFactory.getLogger(SeedAccountsLoader.class);

    private final StationFactory factory;
    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final StationService stationService;
    private final Clock clock;
    private final com.payanam.rail.service.OperationalPersistenceService operationalPersistence;

    public SeedAccountsLoader(StationFactory factory, UserAccountRepository users, PasswordEncoder encoder,
                              StationService stationService, Clock clock,
                              com.payanam.rail.service.OperationalPersistenceService operationalPersistence) {
        this.factory = factory;
        this.users = users;
        this.encoder = encoder;
        this.stationService = stationService;
        this.clock = clock;
        this.operationalPersistence = operationalPersistence;
    }

    @Override
    public void afterSingletonsInstantiated() {
        ensureDemoAccounts();
        stationService.loadSeed();
        syncOperationalSeedData();
    }

    private void syncOperationalSeedData() {
        try {
            stationService.read(s -> {
                operationalPersistence.syncPlatforms(s.platforms().sortedPlatforms());
                operationalPersistence.syncTrains(s.trains().timetable());
                for (com.payanam.rail.domain.model.Booking b : s.reservations().history()) {
                    operationalPersistence.saveBooking(b);
                }
                return null;
            });
            log.info("Operational data synchronized to MySQL tables");
        } catch (Exception e) {
            log.warn("Operational data sync to database skipped or failed: {}", e.getMessage());
        }
    }

    /** Idempotent: an account is only created when its e-mail is not registered yet. */
    public void ensureDemoAccounts() {
        SeedData.UsersSeed seed = factory.read("seed/users.json", new TypeReference<>() { });
        LocalDateTime now = LocalDateTime.now(clock);
        int created = 0;
        for (SeedData.PassengerSeed p : seed.passengers()) {
            if (!users.existsByEmailIgnoreCase(p.email())) {
                users.save(new PassengerUser(p.fullName(), p.email(), p.mobile(), encoder.encode(p.password()),
                        p.dateOfBirth(), p.gender(), p.city(), now));
                created++;
            }
        }
        for (SeedData.StaffSeed s : seed.staff()) {
            if (!users.existsByEmailIgnoreCase(s.email())) {
                users.save(new StationStaffUser(s.fullName(), s.email(), s.mobile(), encoder.encode(s.password()),
                        s.employeeId(), s.designation(), now));
                created++;
            }
        }
        log.info("Demo accounts ready ({} created now)", created);
    }
}
