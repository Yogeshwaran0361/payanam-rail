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
 * JPA entity representing a passenger on a train's waiting list (FIFO order).
 * Maps to MySQL table 'waiting_list'.
 */
@Entity
@Table(name = "waiting_list", indexes = {
        @Index(name = "idx_wl_train_pos", columnList = "train_no, queue_status, waiting_position"),
        @Index(name = "idx_wl_pnr", columnList = "pnr"),
        @Index(name = "idx_wl_user", columnList = "user_id")
})
public class WaitingListEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_no", nullable = false)
    private TrainEntity train;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(nullable = false, length = 10)
    private String pnr;

    @Column(name = "ticket_id", nullable = false, length = 20)
    private String ticketId;

    @Column(name = "passenger_name", nullable = false, length = 80)
    private String passengerName;

    @Column(name = "passenger_age", nullable = false)
    private int passengerAge;

    @Column(name = "passenger_gender", nullable = false, length = 10)
    private String passengerGender;

    @Column(name = "waiting_position", nullable = false)
    private int waitingPosition;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "queue_status", nullable = false, length = 20)
    private String queueStatus = "WAITING";

    @Column(name = "promoted_at")
    private LocalDateTime promotedAt;

    @Column(name = "promoted_seat_no")
    private Integer promotedSeatNo;

    public WaitingListEntity() {
    }

    public WaitingListEntity(TrainEntity train, UserAccount user, String pnr, String ticketId,
                             String passengerName, int passengerAge, String passengerGender,
                             int waitingPosition, LocalDateTime joinedAt, String queueStatus) {
        this.train = train;
        this.user = user;
        this.pnr = pnr;
        this.ticketId = ticketId;
        this.passengerName = passengerName;
        this.passengerAge = passengerAge;
        this.passengerGender = passengerGender;
        this.waitingPosition = waitingPosition;
        this.joinedAt = joinedAt;
        this.queueStatus = queueStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TrainEntity getTrain() {
        return train;
    }

    public void setTrain(TrainEntity train) {
        this.train = train;
    }

    public UserAccount getUser() {
        return user;
    }

    public void setUser(UserAccount user) {
        this.user = user;
    }

    public String getPnr() {
        return pnr;
    }

    public void setPnr(String pnr) {
        this.pnr = pnr;
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

    public int getPassengerAge() {
        return passengerAge;
    }

    public void setPassengerAge(int passengerAge) {
        this.passengerAge = passengerAge;
    }

    public String getPassengerGender() {
        return passengerGender;
    }

    public void setPassengerGender(String passengerGender) {
        this.passengerGender = passengerGender;
    }

    public int getWaitingPosition() {
        return waitingPosition;
    }

    public void setWaitingPosition(int waitingPosition) {
        this.waitingPosition = waitingPosition;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getQueueStatus() {
        return queueStatus;
    }

    public void setQueueStatus(String queueStatus) {
        this.queueStatus = queueStatus;
    }

    public LocalDateTime getPromotedAt() {
        return promotedAt;
    }

    public void setPromotedAt(LocalDateTime promotedAt) {
        this.promotedAt = promotedAt;
    }

    public Integer getPromotedSeatNo() {
        return promotedSeatNo;
    }

    public void setPromotedSeatNo(Integer promotedSeatNo) {
        this.promotedSeatNo = promotedSeatNo;
    }
}

