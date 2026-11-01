package com.payanam.rail.entity;

import com.payanam.rail.domain.model.Priority;
import com.payanam.rail.domain.model.TrainStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity representing a train calling at Payanam Junction.
 * Maps to MySQL table 'trains'.
 */
@Entity
@Table(name = "trains")
public class TrainEntity {

    @Id
    @Column(name = "train_no", length = 5, nullable = false)
    private String trainNo;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String origin;

    @Column(nullable = false, length = 100)
    private String destination;

    @Column(name = "length_meters", nullable = false)
    private int lengthMeters;

    @Enumerated(EnumType.STRING)
    @Column(name = "declared_priority", nullable = false, length = 20)
    private Priority declaredPriority;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal fare;

    @Column(nullable = false)
    private int capacity = 10;

    @Column(name = "scheduled_arrival", nullable = false)
    private LocalDateTime scheduledArrival;

    @Column(name = "scheduled_departure", nullable = false)
    private LocalDateTime scheduledDeparture;

    @Column(name = "expected_arrival", nullable = false)
    private LocalDateTime expectedArrival;

    @Column(name = "expected_departure", nullable = false)
    private LocalDateTime expectedDeparture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TrainStatus status = TrainStatus.SCHEDULED;

    @Column(name = "platform_id", length = 10)
    private String platformId;

    @Column(name = "delay_minutes", nullable = false)
    private int delayMinutes = 0;

    @Column(name = "delay_reason", length = 255)
    private String delayReason;

    @Column(name = "actual_arrival")
    private LocalDateTime actualArrival;

    @Column(name = "actual_departure")
    private LocalDateTime actualDeparture;

    @Column(name = "confirmed_count", nullable = false)
    private int confirmedCount = 0;

    public TrainEntity() {
    }

    public TrainEntity(String trainNo, String name, String origin, String destination, int lengthMeters,
                       Priority declaredPriority, BigDecimal fare, int capacity, LocalDateTime scheduledArrival,
                       LocalDateTime scheduledDeparture, LocalDateTime expectedArrival, LocalDateTime expectedDeparture,
                       TrainStatus status, String platformId, int delayMinutes, String delayReason,
                       LocalDateTime actualArrival, LocalDateTime actualDeparture, int confirmedCount) {
        this.trainNo = trainNo;
        this.name = name;
        this.origin = origin;
        this.destination = destination;
        this.lengthMeters = lengthMeters;
        this.declaredPriority = declaredPriority;
        this.fare = fare;
        this.capacity = capacity;
        this.scheduledArrival = scheduledArrival;
        this.scheduledDeparture = scheduledDeparture;
        this.expectedArrival = expectedArrival;
        this.expectedDeparture = expectedDeparture;
        this.status = status;
        this.platformId = platformId;
        this.delayMinutes = delayMinutes;
        this.delayReason = delayReason;
        this.actualArrival = actualArrival;
        this.actualDeparture = actualDeparture;
        this.confirmedCount = confirmedCount;
    }

    public String getTrainNo() {
        return trainNo;
    }

    public void setTrainNo(String trainNo) {
        this.trainNo = trainNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public int getLengthMeters() {
        return lengthMeters;
    }

    public void setLengthMeters(int lengthMeters) {
        this.lengthMeters = lengthMeters;
    }

    public Priority getDeclaredPriority() {
        return declaredPriority;
    }

    public void setDeclaredPriority(Priority declaredPriority) {
        this.declaredPriority = declaredPriority;
    }

    public BigDecimal getFare() {
        return fare;
    }

    public void setFare(BigDecimal fare) {
        this.fare = fare;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public LocalDateTime getScheduledArrival() {
        return scheduledArrival;
    }

    public void setScheduledArrival(LocalDateTime scheduledArrival) {
        this.scheduledArrival = scheduledArrival;
    }

    public LocalDateTime getScheduledDeparture() {
        return scheduledDeparture;
    }

    public void setScheduledDeparture(LocalDateTime scheduledDeparture) {
        this.scheduledDeparture = scheduledDeparture;
    }

    public LocalDateTime getExpectedArrival() {
        return expectedArrival;
    }

    public void setExpectedArrival(LocalDateTime expectedArrival) {
        this.expectedArrival = expectedArrival;
    }

    public LocalDateTime getExpectedDeparture() {
        return expectedDeparture;
    }

    public void setExpectedDeparture(LocalDateTime expectedDeparture) {
        this.expectedDeparture = expectedDeparture;
    }

    public TrainStatus getStatus() {
        return status;
    }

    public void setStatus(TrainStatus status) {
        this.status = status;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public int getDelayMinutes() {
        return delayMinutes;
    }

    public void setDelayMinutes(int delayMinutes) {
        this.delayMinutes = delayMinutes;
    }

    public String getDelayReason() {
        return delayReason;
    }

    public void setDelayReason(String delayReason) {
        this.delayReason = delayReason;
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

    public int getConfirmedCount() {
        return confirmedCount;
    }

    public void setConfirmedCount(int confirmedCount) {
        this.confirmedCount = confirmedCount;
    }
}

