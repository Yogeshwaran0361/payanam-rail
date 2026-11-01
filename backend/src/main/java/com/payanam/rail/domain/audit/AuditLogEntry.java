package com.payanam.rail.domain.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** One audited action: who did what, when (real and simulated time), and whether it succeeded. */
@Entity
@Table(name = "audit_log", indexes = {@Index(columnList = "timestamp"), @Index(columnList = "action")})
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private LocalDateTime simulationTime;

    @Column(length = 120)
    private String actor;

    @Column(length = 30)
    private String role;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(length = 1000)
    private String details;

    private boolean success;

    protected AuditLogEntry() {
        // for JPA
    }

    public AuditLogEntry(LocalDateTime timestamp, LocalDateTime simulationTime, String actor, String role,
                         String action, String details, boolean success) {
        this.timestamp = timestamp;
        this.simulationTime = simulationTime;
        this.actor = actor;
        this.role = role;
        this.action = action;
        this.details = details == null ? null : details.substring(0, Math.min(1000, details.length()));
        this.success = success;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public LocalDateTime getSimulationTime() {
        return simulationTime;
    }

    public String getActor() {
        return actor;
    }

    public String getRole() {
        return role;
    }

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public boolean isSuccess() {
        return success;
    }
}
