package com.payanam.rail.service;

import com.payanam.rail.domain.audit.AuditLogEntry;
import com.payanam.rail.dto.PageView;
import com.payanam.rail.dto.station.AuditEntryView;
import com.payanam.rail.repository.AuditLogRepository;
import com.payanam.rail.security.AuthUser;
import com.payanam.rail.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Writes and reads the audit trail (H2). Every state-changing action is recorded with real time,
 * simulation time, the user, their role, the action name and details.
 */
@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repository;
    private final StationService station;
    private final Clock clock;

    public AuditService(AuditLogRepository repository, @Lazy StationService station, Clock clock) {
        this.repository = repository;
        this.station = station;
        this.clock = clock;
    }

    /** Records a successful action of the signed-in user. */
    public void record(String action, String details) {
        Optional<AuthUser> user = CurrentUser.get();
        save(user.map(AuthUser::email).orElse("anonymous"), user.map(this::roleLabel).orElse("PUBLIC"), action,
                details, true);
    }

    /** Records an action for an explicit actor (sign-in attempts, where nobody is signed in yet). */
    public void recordAs(String actor, String role, String action, String details, boolean success) {
        save(actor, role, action, details, success);
    }

    /** Records a failed action of the signed-in user (called by the global exception handler). */
    public void recordFailure(String action, String details) {
        Optional<AuthUser> user = CurrentUser.get();
        save(user.map(AuthUser::email).orElse("anonymous"), user.map(this::roleLabel).orElse("PUBLIC"), action,
                details, false);
    }

    /** A 403: someone tried to use an API their role or designation does not allow. */
    public void recordDenied(String endpoint) {
        recordFailure("ACCESS_DENIED", "Blocked request to " + endpoint);
    }

    private void save(String actor, String role, String action, String details, boolean success) {
        LocalDateTime sim = null;
        try {
            sim = station.now();
        } catch (RuntimeException ignored) {
            // the station is not loaded yet during start-up
        }
        repository.save(new AuditLogEntry(LocalDateTime.now(clock), sim, actor, role, action, details, success));
        log.info("AUDIT {} {} {} {}: {}", success ? "OK  " : "FAIL", actor, role, action, details);
    }

    private String roleLabel(AuthUser u) {
        return u.designation() != null ? u.designation() : u.role().name();
    }

    /** Filtered, newest-first page of the audit log. Blank filters are ignored. */
    public PageView<AuditEntryView> search(String action, String role, String q, int page, int size) {
        return PageView.of(repository.search(blankToNull(action), blankToNull(role), blankToNull(q),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200))), AuditService::toView);
    }

    public List<String> actions() {
        return repository.findDistinctActions();
    }

    /** CSV export of the filtered log (up to 5000 rows), with quotes escaped. */
    public String exportCsv(String action, String role, String q) {
        StringBuilder csv = new StringBuilder("id,timestamp,simulation_time,user,role,action,details,success\n");
        for (AuditEntryView e : search(action, role, q, 0, 5000).content()) {
            csv.append(e.id()).append(',').append(e.timestamp()).append(',')
                    .append(e.simulationTime() == null ? "" : e.simulationTime()).append(',')
                    .append(quote(e.actor())).append(',').append(quote(e.role())).append(',')
                    .append(quote(e.action())).append(',').append(quote(e.details())).append(',')
                    .append(e.success()).append('\n');
        }
        return csv.toString();
    }

    private static String quote(String value) {
        String v = value == null ? "" : value;
        return '"' + v.replace("\"", "\"\"") + '"';
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static AuditEntryView toView(AuditLogEntry e) {
        return new AuditEntryView(e.getId(), e.getTimestamp(), e.getSimulationTime(), e.getActor(), e.getRole(),
                e.getAction(), e.getDetails(), e.isSuccess());
    }
}
