package com.payanam.rail.domain.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * A saved copy of the station's KPIs and timetable state (JSON) so results of a demo run can be
 * compared later. The live data structures themselves always stay in memory.
 */
@Entity
@Table(name = "station_snapshot")
public class StationSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime simulationTime;

    @Column(nullable = false, length = 80)
    private String label;

    @Column(length = 120)
    private String createdBy;

    @Lob
    @Column(nullable = false)
    private String summaryJson;

    protected StationSnapshot() {
        // for JPA
    }

    public StationSnapshot(LocalDateTime createdAt, LocalDateTime simulationTime, String label, String createdBy,
                           String summaryJson) {
        this.createdAt = createdAt;
        this.simulationTime = simulationTime;
        this.label = label;
        this.createdBy = createdBy;
        this.summaryJson = summaryJson;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getSimulationTime() {
        return simulationTime;
    }

    public String getLabel() {
        return label;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getSummaryJson() {
        return summaryJson;
    }
}
