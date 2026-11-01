package com.payanam.rail.entity;

import com.payanam.rail.domain.user.UserAccount;
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
 * JPA entity representing a cancellation record.
 * Maps to MySQL table 'cancellations'.
 */
@Entity
@Table(name = "cancellations", indexes = {
        @Index(name = "idx_canc_pnr", columnList = "pnr"),
        @Index(name = "idx_canc_train", columnList = "train_no")
})
public class CancellationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pnr", referencedColumnName = "pnr", nullable = false)
    private BookingEntity booking;

    @Column(name = "ticket_id", nullable = false, length = 20)
    private String ticketId;

    @Column(name = "passenger_name", nullable = false, length = 80)
    private String passengerName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_no", nullable = false)
    private TrainEntity train;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by_user_id")
    private UserAccount cancelledBy;

    @Column(name = "staff_override", nullable = false)
    private boolean staffOverride = false;

    @Column(name = "freed_seat_no")
    private Integer freedSeatNo;

    @Column(name = "promoted_ticket_id", length = 20)
    private String promotedTicketId;

    @Column(name = "promoted_pnr", length = 10)
    private String promotedPnr;

    @Column(name = "previous_status_label", length = 20)
    private String previousStatusLabel;

    @Column(name = "cancelled_at", nullable = false)
    private LocalDateTime cancelledAt;

    public CancellationEntity() {
    }

    public CancellationEntity(BookingEntity booking, String ticketId, String passengerName, TrainEntity train,
                              UserAccount cancelledBy, boolean staffOverride, Integer freedSeatNo,
                              String promotedTicketId, String promotedPnr, String previousStatusLabel,
                              LocalDateTime cancelledAt) {
        this.booking = booking;
        this.ticketId = ticketId;
        this.passengerName = passengerName;
        this.train = train;
        this.cancelledBy = cancelledBy;
        this.staffOverride = staffOverride;
        this.freedSeatNo = freedSeatNo;
        this.promotedTicketId = promotedTicketId;
        this.promotedPnr = promotedPnr;
        this.previousStatusLabel = previousStatusLabel;
        this.cancelledAt = cancelledAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BookingEntity getBooking() {
        return booking;
    }

    public void setBooking(BookingEntity booking) {
        this.booking = booking;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public TrainEntity getTrain() {
        return train;
    }

    public void setTrain(TrainEntity train) {
        this.train = train;
    }

    public UserAccount getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(UserAccount cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public boolean isStaffOverride() {
        return staffOverride;
    }

    public void setStaffOverride(boolean staffOverride) {
        this.staffOverride = staffOverride;
    }

    public Integer getFreedSeatNo() {
        return freedSeatNo;
    }

    public void setFreedSeatNo(Integer freedSeatNo) {
        this.freedSeatNo = freedSeatNo;
    }

    public String getPromotedTicketId() {
        return promotedTicketId;
    }

    public void setPromotedTicketId(String promotedTicketId) {
        this.promotedTicketId = promotedTicketId;
    }

    public String getPromotedPnr() {
        return promotedPnr;
    }

    public void setPromotedPnr(String promotedPnr) {
        this.promotedPnr = promotedPnr;
    }

    public String getPreviousStatusLabel() {
        return previousStatusLabel;
    }

    public void setPreviousStatusLabel(String previousStatusLabel) {
        this.previousStatusLabel = previousStatusLabel;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
}

