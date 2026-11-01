package com.payanam.rail.entity;

import com.payanam.rail.domain.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity representing a booking (PNR) made by a user for 1-4 passengers.
 * Maps to MySQL table 'bookings'.
 */
@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_bookings_owner", columnList = "owner_user_id"),
        @Index(name = "idx_bookings_train", columnList = "train_no")
})
public class BookingEntity {

    @Id
    @Column(length = 10, nullable = false)
    private String pnr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private UserAccount owner;

    @Column(name = "owner_name", nullable = false, length = 80)
    private String ownerName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_no", nullable = false)
    private TrainEntity train;

    @Column(name = "booked_at", nullable = false)
    private LocalDateTime bookedAt;

    @Column(name = "fare_per_passenger", nullable = false, precision = 8, scale = 2)
    private BigDecimal farePerPassenger;

    @Column(name = "total_fare", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalFare;

    @Column(name = "passenger_count", nullable = false)
    private int passengerCount;

    @Column(nullable = false, length = 20)
    private String status = "UPCOMING";

    @Column(length = 255)
    private String summary;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String passengersJson;

    public BookingEntity() {
    }

    public BookingEntity(String pnr, UserAccount owner, String ownerName, TrainEntity train,
                         LocalDateTime bookedAt, BigDecimal farePerPassenger, BigDecimal totalFare,
                         int passengerCount, String status, String summary, String passengersJson) {
        this.pnr = pnr;
        this.owner = owner;
        this.ownerName = ownerName;
        this.train = train;
        this.bookedAt = bookedAt;
        this.farePerPassenger = farePerPassenger;
        this.totalFare = totalFare;
        this.passengerCount = passengerCount;
        this.status = status;
        this.summary = summary;
        this.passengersJson = passengersJson;
    }

    public String getPnr() {
        return pnr;
    }

    public void setPnr(String pnr) {
        this.pnr = pnr;
    }

    public UserAccount getOwner() {
        return owner;
    }

    public void setOwner(UserAccount owner) {
        this.owner = owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public TrainEntity getTrain() {
        return train;
    }

    public void setTrain(TrainEntity train) {
        this.train = train;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    public void setBookedAt(LocalDateTime bookedAt) {
        this.bookedAt = bookedAt;
    }

    public BigDecimal getFarePerPassenger() {
        return farePerPassenger;
    }

    public void setFarePerPassenger(BigDecimal farePerPassenger) {
        this.farePerPassenger = farePerPassenger;
    }

    public BigDecimal getTotalFare() {
        return totalFare;
    }

    public void setTotalFare(BigDecimal totalFare) {
        this.totalFare = totalFare;
    }

    public int getPassengerCount() {
        return passengerCount;
    }

    public void setPassengerCount(int passengerCount) {
        this.passengerCount = passengerCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getPassengersJson() {
        return passengersJson;
    }

    public void setPassengersJson(String passengersJson) {
        this.passengersJson = passengersJson;
    }
}

