package com.payanam.rail.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * JPA entity representing a train platform occupation interval at Payanam Junction.
 * Maps to MySQL table 'platform_allocations'.
 */
@Entity
@Table(name = "platform_allocations", indexes = {
        @Index(name = "idx_pa_platform_time", columnList = "platform_id, occupation_start, occupation_end"),
        @Index(name = "idx_pa_train", columnList = "train_no")
})
public class PlatformAllocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_id", nullable = false)
    private PlatformEntity platform;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_no", nullable = false)
    private TrainEntity train;

    @Column(name = "request_type", nullable = false, length = 20)
    private String requestType;

    @Column(name = "priority_rank", nullable = false)
    private int priorityRank;

    @Column(name = "occupation_start", nullable = false)
    private LocalDateTime occupationStart;

    @Column(name = "occupation_end", nullable = false)
    private LocalDateTime occupationEnd;

    @Column(name = "clearance_buffer_minutes", nullable = false)
    private int clearanceBufferMinutes;

    @Column(name = "allocation_status", nullable = false, length = 20)
    private String allocationStatus = "ASSIGNED";

    @Column(name = "actual_arrival")
    private LocalDateTime actualArrival;

    @Column(name = "actual_departure")
    private LocalDateTime actualDeparture;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PlatformAllocationEntity() {
    }

    public PlatformAllocationEntity(PlatformEntity platform, TrainEntity train, String requestType,
                                    int priorityRank, LocalDateTime occupationStart, LocalDateTime occupationEnd,
                                    int clearanceBufferMinutes, String allocationStatus, LocalDateTime createdAt) {
        this.platform = platform;
        this.train = train;
        this.requestType = requestType;
        this.priorityRank = priorityRank;
        this.occupationStart = occupationStart;
        this.occupationEnd = occupationEnd;
        this.clearanceBufferMinutes = clearanceBufferMinutes;
        this.allocationStatus = allocationStatus;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PlatformEntity getPlatform() {
        return platform;
    }

    public void setPlatform(PlatformEntity platform) {
        this.platform = platform;
    }

    public TrainEntity getTrain() {
        return train;
    }

    public void setTrain(TrainEntity train) {
        this.train = train;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public int getPriorityRank() {
        return priorityRank;
    }

    public void setPriorityRank(int priorityRank) {
        this.priorityRank = priorityRank;
    }

    public LocalDateTime getOccupationStart() {
        return occupationStart;
    }

    public void setOccupationStart(LocalDateTime occupationStart) {
        this.occupationStart = occupationStart;
    }

    public LocalDateTime getOccupationEnd() {
        return occupationEnd;
    }

    public void setOccupationEnd(LocalDateTime occupationEnd) {
        this.occupationEnd = occupationEnd;
    }

    public int getClearanceBufferMinutes() {
        return clearanceBufferMinutes;
    }

    public void setClearanceBufferMinutes(int clearanceBufferMinutes) {
        this.clearanceBufferMinutes = clearanceBufferMinutes;
    }

    public String getAllocationStatus() {
        return allocationStatus;
    }

    public void setAllocationStatus(String allocationStatus) {
        this.allocationStatus = allocationStatus;
    }

    public LocalDateTime getActualArrival() {
        return actualArrival;
    }

    public void setActualArrival(LocalDateTime actualArrival) {
        this.actualArrival = actualArrival;
    }

    public LocalDateTime getActualDeparture() {
        return actualDeparture;
    }

    public void setActualDeparture(LocalDateTime actualDeparture) {
        this.actualDeparture = actualDeparture;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

